package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class SourceBiomePortal {
    private static final int SEARCH_RADIUS = 6400;
    private static final int LOCAL_RADIUS = 64;
    private static final int MAX_CHUNKS = 6;
    private static final int MAX_WARMING = 2;
    private static final int MAX_CONCURRENT_CHUNKS = 2;
    private static final int MAX_DISCOVERY_CHUNKS = 128;
    private static final long WARM_TICKS = 200;
    private static final long APPROACH_SCAN_TICKS = 10;
    private static final double APPROACH = 48 * 48;
    private static final long RETRY_TICKS = 100;
    private static final long CACHE_TICKS = 6000;
    private static final long NEGATIVE_CACHE_TICKS = 200;
    private static final int TICKET_TICKS = 2400;
    private static final int NATURAL_WAIT_TICKS = 160;
    private static final int TARGET_WAIT_TICKS = 240;
    private static final int GENERATION_WAIT_TICKS = 200;
    private static final int TARGET_COLUMNS = 13 * 13;
    private static final int FAR_SAMPLE_STEP = 128;
    private static final int FAR_SEARCH_RADIUS = 25_600;
    private static final int[] FAR_SAMPLE_HEIGHTS = {0, 32, 64, 96, -48};
    private static final TicketType<UUID> PREPARE = TicketType.create("mite_biome_portal", UUID::compareTo, TICKET_TICKS);
    private static final Map<ServerLevel, WorldRoutes> ROUTES = new WeakHashMap<>();
    private static final Map<ServerPlayer, Search> SEARCHES = new WeakHashMap<>();

    private record Route(BlockPos frame, Direction.Axis axis, BiomePortalStone kind) {}
    private record Located(CompletableFuture<BlockPos> anchor, long started) {}

    private static final class WorldRoutes {
        final Map<Route, Located> locations = new LinkedHashMap<>(16, .75f, true);
        final Map<Route, Warmup> warmups = new LinkedHashMap<>(16, .75f, true);
        final LinkedHashSet<ChunkPos> discovery = new LinkedHashSet<>();
        final Map<ServerPlayer, ChunkPos> lastPlayerChunks = new WeakHashMap<>();
        final LinkedHashSet<Route> discovered = new LinkedHashSet<>();
    }

    private static final class Warmup {
        final UUID ticket = UUID.randomUUID();
        final Map<ChunkPos, List<BlockPos>> loading = new LinkedHashMap<>();
        final List<Vec3> landings = new ArrayList<>();
        ChunkPos reservedFallback;
        List<List<BlockPos>> candidates;
        long keepUntil;
        int next;
        long renewAt;
    }

    private static final class Search {
        final ServerLevel level;
        final BlockPos entrance;
        final Route route;
        final long started;
        CompletableFuture<BlockPos> anchor;
        List<List<BlockPos>> candidates;
        int index;
        ChunkPos loading;
        long renewAt;
        long loadingSince;
        int loadingRadius;
        Vec3 rejectedBorderColumn;
        long retryBorderAt;
        int targetColumn;
        int emergencyColumn;
        BlockPos landingColumn;
        final Set<ChunkPos> rejectedTargetChunks = new LinkedHashSet<>();

        boolean matchingBiome;

        Search(ServerLevel level, BlockPos entrance, Route route) {
            this.level = level;
            this.entrance = entrance.immutable();
            this.route = route;
            started = level.getGameTime();
        }
    }

    static BiomePortalStone marker(SourcePortalFrames.Frame frame) {
        var first = frame.level().getBlockState(frame.at(-1, -1)).getBlock();
        if (!(first instanceof BiomePortalStone.Marker marker)
                || !frame.level().getBlockState(frame.at(2, -1)).is(first)
                || !frame.level().getBlockState(frame.at(-1, 3)).is(first)
                || !frame.level().getBlockState(frame.at(2, 3)).is(first)) return null;
        return marker.kind;
    }

    static boolean contact(ServerPlayer player, BlockPos entrance) {
        var level = player.serverLevel();
        if (!WorldContentMode.rebuilt(level)) return false;
        var frame = SourcePortalFrames.complete(level, entrance).orElse(null);
        if (frame == null) return false;
        var kind = marker(frame);
        if (kind == null) {
            for (int across : new int[]{-1, 2}) for (int up : new int[]{-1, 3}) {
                if (frame.level().getBlockState(frame.at(across, up)).getBlock() instanceof BiomePortalStone.Marker) {
                    reject(player, "mite_reforged.biome_portal.mismatched");
                    return true;
                }
            }
            return false;
        }
        if (BossArenaPortals.blockedPortal(level, frame.rectangle(), frame.axis())) return true;
        var route = route(frame, kind);
        var current = SEARCHES.get(player);
        if (current != null && current.level == level && current.route.equals(route)) return true;
        cancel(player);
        var warmup = warm(level, route);
        if (warmup != null && enterPrepared(player, entrance, route, warmup)) return true;
        SEARCHES.put(player, new Search(level, entrance, route));
        return true;
    }

    static void reject(ServerPlayer player, String message) {
        player.setPortalCooldown((int)RETRY_TICKS);
        player.displayClientMessage(Component.translatable(message), true);
    }

    private static CompletableFuture<BlockPos> locate(ServerLevel level, Route route) {
        var cache = world(level).locations;
        long now = level.getGameTime();
        var known = cache.get(route);
        if (known != null) {
            if (!known.anchor().isDone()) return known.anchor();
            boolean completed = !known.anchor().isCompletedExceptionally() && !known.anchor().isCancelled();
            var cached = completed ? known.anchor().getNow(null) : null;
            if (cached != null && level.getWorldBorder().isWithinBounds(cached)
                    && now - known.started() < CACHE_TICKS) return known.anchor();
            if (cached == null && now - known.started() < NEGATIVE_CACHE_TICKS) return known.anchor();
        }
        var frame = route.frame();
        var start = new BlockPos(frame.getX(), level.getSeaLevel() + 8, frame.getZ());
        // This vanilla locator samples the biome source and climate sampler without accessing chunks.
        if (cache.values().stream().filter(entry -> !entry.anchor().isDone()).count() >= MAX_WARMING) {
            return null;
        }
        var generator = level.getChunkSource();
        var biomes = generator.getGenerator().getBiomeSource();
        var climate = generator.randomState().sampler();
        boolean supported = biomes.possibleBiomes().stream().anyMatch(route.kind()::matches);
        var innerBorder = new AABB(level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY,
            level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(),
            Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ());
        CompletableFuture<BlockPos> anchor = supported ? CompletableFuture.supplyAsync(() -> {
            var found = level.findClosestBiome3d(route.kind()::matches, start, SEARCH_RADIUS, 32, 32);
            if (found != null && innerBorder.contains(Vec3.atCenterOf(found.getFirst()))) {
                return found.getFirst().immutable();
            }
            int maxRing = FAR_SEARCH_RADIUS / FAR_SAMPLE_STEP;
            for (int ring = 1; ring <= maxRing; ring++) {
                int side = ring * 2 + 1;
                for (int index = (side - 2) * (side - 2); index < side * side; index++) {
                    var probe = spiral(start, index, FAR_SAMPLE_STEP);
                    if (!innerBorder.contains(Vec3.atCenterOf(probe))) continue;
                    for (int height : FAR_SAMPLE_HEIGHTS) {
                        var candidate = probe.above(height);
                        if (route.kind().matches(biomes.getNoiseBiome(
                                QuartPos.fromBlock(candidate.getX()), QuartPos.fromBlock(candidate.getY()),
                                QuartPos.fromBlock(candidate.getZ()), climate))) return candidate;
                    }
                }
            }
            return null;
        }, Util.backgroundExecutor()) : CompletableFuture.completedFuture(null);
        cache.put(route, new Located(anchor, now));
        if (cache.size() > 64) {
            for (var entries = cache.entrySet().iterator(); entries.hasNext();) {
                var entry = entries.next();
                if (!entry.getValue().anchor().isDone() || entry.getKey().equals(route)
                        || world(level).warmups.containsKey(entry.getKey())) continue;
                entries.remove();
                break;
            }
        }
        return anchor;
    }

    static void ignite(ServerLevel level, SourcePortalFrames.Frame frame) {
        if (!WorldContentMode.rebuilt(level)) return;
        var kind = marker(frame);
        if (kind != null) warm(level, route(frame, kind));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !WorldContentMode.rebuilt(level) || level.dimension() != Level.OVERWORLD) return;
        if (!event.getPlacedBlock().is(Blocks.OBSIDIAN)
                && !(event.getPlacedBlock().getBlock() instanceof BiomePortalStone.Marker)) return;
        for (var axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            for (int x = -2; x <= 1; x++) for (int y = -3; y <= 1; y++) {
                var bottom = axis == Direction.Axis.X ? event.getPos().offset(x, y, 0) : event.getPos().offset(0, y, x);
                var frame = frame(level, bottom, axis);
                if (!loadedFrame(level, frame)) continue;
                var kind = marker(frame);
                if (kind != null && frameReady(level, frame)) warm(level, route(frame, kind));
            }
        }
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !WorldContentMode.rebuilt(level) || level.dimension() != Level.OVERWORLD
                || !(event.getChunk() instanceof LevelChunk)) return;
        queueDiscovery(level, event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && WorldContentMode.rebuilt(player.level())
                && player.serverLevel().dimension() == Level.OVERWORLD) {
            queueAroundPlayer(world(player.serverLevel()), player);
        }
    }

    private static void queueAroundPlayer(WorldRoutes world, ServerPlayer player) {
        var at = player.chunkPosition();
        if (at.equals(world.lastPlayerChunks.put(player, at))) return;
        if (world.discovery.size() == MAX_DISCOVERY_CHUNKS) world.discovery.remove(world.discovery.iterator().next());
        world.discovery.add(at);
        for (int ring = 1; ring <= 3; ++ring) {
            for (int x = -ring; x <= ring; ++x) for (int z = -ring; z <= ring; ++z) {
                if (Math.max(Math.abs(x), Math.abs(z)) != ring || world.discovery.size() >= MAX_DISCOVERY_CHUNKS) continue;
                world.discovery.add(new ChunkPos(at.x + x, at.z + z));
            }
        }
    }

    private static void queueDiscovery(ServerLevel level, ChunkPos chunk) {
        var discovery = world(level).discovery;
        if (discovery.size() + 5 > MAX_DISCOVERY_CHUNKS) return;
        discovery.add(chunk);
        for (var side : Direction.Plane.HORIZONTAL) {
            discovery.add(new ChunkPos(chunk.x + side.getStepX(), chunk.z + side.getStepZ()));
        }
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var world = ROUTES.get(level);
        if (world == null) return;
        long now = level.getGameTime();
        if (now % APPROACH_SCAN_TICKS == 0) {
            for (var player : level.players()) queueAroundPlayer(world, player);
            tryNearbyDiscovered(level, world);
        }
        scanOneChunk(level, world);
        for (var entries = world.warmups.entrySet().iterator(); entries.hasNext();) {
            var entry = entries.next();
            var route = entry.getKey();
            var warmup = entry.getValue();
            var frame = frame(level, route.frame(), route.axis());
            boolean intact = loadedFrame(level, frame) && marker(frame) == route.kind() && frameReady(level, frame);
            if (!intact || now >= warmup.keepUntil && !playerNear(level, route)) {
                releaseWarmup(level, warmup);
                entries.remove();
                continue;
            }
            if (warmup.candidates == null) {
                var locator = world.locations.get(route);
                if (locator == null) {
                    locate(level, route);
                    continue;
                }
                if (!locator.anchor().isDone()) continue;
                try {
                    var anchor = locator.anchor().getNow(null);
                    if (anchor != null && level.getWorldBorder().isWithinBounds(anchor)) {
                        warmup.reservedFallback = new ChunkPos(anchor);
                        warmup.candidates = prioritizeWarmupCandidates(level, warmup.reservedFallback,
                            candidates(level, route, anchor));
                        level.getChunkSource().addRegionTicket(PREPARE, warmup.reservedFallback, 0, warmup.ticket);
                    } else {
                        warmup.candidates = List.of();
                    }
                } catch (CompletionException | CancellationException failed) {
                    warmup.candidates = List.of();
                }
            }
            advanceWarmup(level, route, warmup);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var world = ROUTES.remove(level);
        if (world != null) for (var warmup : world.warmups.values()) releaseWarmup(level, warmup);
        SEARCHES.entrySet().removeIf(entry -> entry.getValue().level == level);
    }

    private static WorldRoutes world(ServerLevel level) {
        return ROUTES.computeIfAbsent(level, ignored -> new WorldRoutes());
    }

    private static Route route(SourcePortalFrames.Frame frame, BiomePortalStone kind) {
        return new Route(frame.rectangle().minCorner, frame.axis(), kind);
    }

    private static SourcePortalFrames.Frame frame(ServerLevel level, BlockPos bottom, Direction.Axis axis) {
        return new SourcePortalFrames.Frame(level, bottom, axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, axis);
    }

    private static boolean loadedFrame(ServerLevel level, SourcePortalFrames.Frame frame) {
        var first = new ChunkPos(frame.at(-1, -1));
        var last = new ChunkPos(frame.at(2, 3));
        var source = level.getChunkSource();
        return source.getChunkNow(first.x, first.z) != null && source.getChunkNow(last.x, last.z) != null;
    }

    private static boolean frameReady(ServerLevel level, SourcePortalFrames.Frame frame) {
        if (SourcePortalFrames.initial(level, frame) != SourcePortalState.Destination.OVERWORLD) return false;
        for (int x = -1; x <= 2; ++x) for (int y = -1; y <= 3; ++y) {
            boolean side = x == -1 || x == 2, cap = y == -1 || y == 3;
            if (side == cap) continue;
            if (!level.getBlockState(frame.at(x, y)).is(Blocks.OBSIDIAN)) return false;
        }
        return true;
    }

    private static boolean playerNear(ServerLevel level, Route route) {
        return level.players().stream().anyMatch(player -> player.isAlive() && !player.isSpectator()
            && player.distanceToSqr(Vec3.atCenterOf(route.frame())) <= APPROACH);
    }

    private static void tryNearbyDiscovered(ServerLevel level, WorldRoutes world) {
        for (var entries = world.discovered.iterator(); entries.hasNext();) {
            var route = entries.next();
            if (!playerNear(level, route) || world.warmups.containsKey(route)) continue;
            var frame = frame(level, route.frame(), route.axis());
            if (!loadedFrame(level, frame)) continue;
            if (marker(frame) != route.kind() || !frameReady(level, frame)
                    || !level.getBlockState(route.frame()).is(Blocks.NETHER_PORTAL)) {
                entries.remove();
                continue;
            }
            warm(level, route);
        }
    }

    private static void scanOneChunk(ServerLevel level, WorldRoutes world) {
        var chunks = world.discovery.iterator();
        if (!chunks.hasNext()) return;
        var pos = chunks.next();
        chunks.remove();
        var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null || level.players().stream().noneMatch(player ->
                Math.abs(player.chunkPosition().x - pos.x) <= 4 && Math.abs(player.chunkPosition().z - pos.z) <= 4)) return;
        var at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < chunk.getSections().length; i++) {
            var section = chunk.getSection(i);
            if (!section.maybeHas(state -> state.is(Blocks.NETHER_PORTAL))) continue;
            int height = chunk.getSectionYFromSectionIndex(i) * 16;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 16; y++) {
                var state = section.getBlockState(x, y, z);
                if (!state.is(Blocks.NETHER_PORTAL) || state.getValue(SourcePortalState.RUNEGATE)) continue;
                var role = state.getValue(SourcePortalState.DESTINATION);
                if (role != SourcePortalState.Destination.OVERWORLD && role != SourcePortalState.Destination.UNSET) continue;
                at.set(pos.getMinBlockX() + x, height + y, pos.getMinBlockZ() + z);
                var axis = state.getValue(NetherPortalBlock.AXIS);
                var frame = frame(level, at.immutable(), axis);
                if (!loadedFrame(level, frame)) continue;
                var kind = marker(frame);
                if (kind == null || !frameReady(level, frame)) continue;
                var route = route(frame, kind);
                if (world.discovered.add(route) && world.discovered.size() > 64) {
                    var oldest = world.discovered.iterator();
                    oldest.next();
                    oldest.remove();
                }
                if (playerNear(level, route)) warm(level, route);
            }
        }
    }

    private static Warmup warm(ServerLevel level, Route route) {
        var world = world(level);
        var known = world.warmups.get(route);
        if (known != null) {
            known.keepUntil = level.getGameTime() + WARM_TICKS;
            return known;
        }
        if (world.warmups.size() >= MAX_WARMING) {
            for (var entries = world.warmups.entrySet().iterator(); entries.hasNext();) {
                var entry = entries.next();
                if (playerNear(level, entry.getKey())) continue;
                releaseWarmup(level, entry.getValue());
                entries.remove();
                break;
            }
        }
        if (world.warmups.size() >= MAX_WARMING) return null;
        var candidate = new Warmup();
        candidate.keepUntil = level.getGameTime() + WARM_TICKS;
        locate(level, route);
        world.warmups.put(route, candidate);
        return candidate;
    }

    private static List<List<BlockPos>> prioritizeWarmupCandidates(ServerLevel level, ChunkPos reserved,
                                                                  List<List<BlockPos>> candidates) {
        var source = level.getChunkSource();
        var ready = new ArrayList<List<BlockPos>>(candidates.size());
        var cold = new ArrayList<List<BlockPos>>();
        List<BlockPos> reservedCandidate = null;
        for (var columns : candidates) {
            var chunk = new ChunkPos(columns.get(0));
            if (source.getChunkNow(chunk.x, chunk.z) != null) {
                ready.add(columns);
            } else if (chunk.equals(reserved)) {
                reservedCandidate = columns;
            } else {
                cold.add(columns);
            }
        }
        if (reservedCandidate != null) ready.add(reservedCandidate);
        ready.addAll(cold);
        return ready;
    }

    private static void advanceWarmup(ServerLevel level, Route route, Warmup warmup) {
        var source = level.getChunkSource();
        if (level.getGameTime() >= warmup.renewAt) {
            for (var chunk : warmup.loading.keySet()) source.addRegionTicket(PREPARE, chunk, 0, warmup.ticket);
            if (warmup.reservedFallback != null) {
                source.addRegionTicket(PREPARE, warmup.reservedFallback, 0, warmup.ticket);
            }
            warmup.renewAt = level.getGameTime() + 100;
        }
        for (var entries = warmup.loading.entrySet().iterator(); entries.hasNext();) {
            var entry = entries.next();
            if (entry.getValue().isEmpty()) continue;
            var pos = entry.getKey();
            if (source.getChunkNow(pos.x, pos.z) == null) continue;
            for (var column : entry.getValue()) {
                var at = BiomePortalLanding.natural(level, null, route.kind(), column);
                if (at != null) warmup.landings.add(at);
            }
            entry.setValue(List.of());
        }
        if (!warmup.landings.isEmpty()) return;
        for (var entries = warmup.loading.entrySet().iterator(); entries.hasNext();) {
            var entry = entries.next();
            if (!entry.getValue().isEmpty()) continue;
            if (!entry.getKey().equals(warmup.reservedFallback)) {
                source.removeRegionTicket(PREPARE, entry.getKey(), 0, warmup.ticket);
            }
            entries.remove();
        }
        while (warmup.loading.size() < MAX_CONCURRENT_CHUNKS && warmup.next < warmup.candidates.size()) {
            var columns = warmup.candidates.get(warmup.next++);
            var chunk = new ChunkPos(columns.get(0));
            warmup.loading.put(chunk, columns);
            source.addRegionTicket(PREPARE, chunk, 0, warmup.ticket);
        }
    }

    private static boolean enterPrepared(ServerPlayer player, BlockPos entrance, Route route, Warmup warmup) {
        var at = choosePrepared(player, route.kind(), warmup);
        if (at == null) return false;
        begin(player, entrance, route.kind(), at, true);
        return true;
    }

    static Vec3 preparedReplacement(ServerPlayer player, BlockPos entrance, BiomePortalStone kind) {
        var level = player.serverLevel();
        var known = ROUTES.get(level);
        if (known == null) return null;
        var frame = SourcePortalFrames.complete(level, entrance).orElse(null);
        if (frame == null || marker(frame) != kind) return null;
        var warmup = known.warmups.get(route(frame, kind));
        if (warmup == null) return null;
        var at = choosePrepared(player, kind, warmup);
        if (at != null) reserveLanding(level, at);
        return at;
    }

    private static Vec3 choosePrepared(ServerPlayer player, BiomePortalStone kind, Warmup warmup) {
        int count = warmup.landings.size();
        if (count == 0) return null;
        int first = player.serverLevel().random.nextInt(count);
        for (int choice = 0; choice < count; ++choice) {
            var at = warmup.landings.get((first + choice) % count);
            if (safeLanding(player, kind, at)) return at;
        }
        warmup.landings.clear();
        return null;
    }

    private static void releaseWarmup(ServerLevel level, Warmup warmup) {
        for (var chunk : warmup.loading.keySet()) level.getChunkSource().removeRegionTicket(PREPARE, chunk, 0, warmup.ticket);
        if (warmup.reservedFallback != null && !warmup.loading.containsKey(warmup.reservedFallback)) {
            level.getChunkSource().removeRegionTicket(PREPARE, warmup.reservedFallback, 0, warmup.ticket);
        }
        warmup.loading.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var search = SEARCHES.get(player);
        if (search == null) return;
        var level = search.level;
        if (!player.isAlive() || player.serverLevel() != level || BossArenaPortals.blockedNetherEntrance(level, search.entrance)
                || !stillInside(player, search.entrance, search.route.kind())) {
            cancel(player);
            return;
        }
        long elapsed = level.getGameTime() - search.started;
        if (elapsed % 80 == 20) player.displayClientMessage(Component.translatable("mite_reforged.biome_portal.searching"), true);
        var warmup = search.landingColumn == null && search.loading == null
            ? warm(level, search.route) : world(level).warmups.get(search.route);
        if (warmup != null && enterPrepared(player, search.entrance, search.route, warmup)) {
            cancel(player);
            return;
        }
        if (search.anchor == null) search.anchor = locate(level, search.route);
        BlockPos anchor = null;
        boolean targetMissing = false;
        if (search.anchor != null && search.anchor.isDone()) {
            try {
                anchor = search.anchor.getNow(null);
                targetMissing = anchor == null;
            } catch (CompletionException | CancellationException failed) {
                targetMissing = true;
            }
        }
        if (search.landingColumn == null && !targetMissing && elapsed < NATURAL_WAIT_TICKS) {
            if (anchor == null) return;
            if (warmup != null) {
                if (warmup.candidates == null || !warmup.loading.isEmpty() || warmup.next < warmup.candidates.size()) return;
            } else {
                if (search.candidates == null) search.candidates = candidates(level, search.route, anchor);
                if (search.index < search.candidates.size()) {
                    var columns = search.candidates.get(search.index);
                    if (!loadColumn(player, search, columns.get(0))) return;
                    for (var column : columns) {
                        var at = BiomePortalLanding.natural(level, player, search.route.kind(), column);
                        if (at != null) {
                            begin(player, search.entrance, search.route.kind(), at, true);
                            return;
                        }
                    }
                    release(player, search);
                    search.index++;
                    return;
                }
            }
        }

        if (search.landingColumn == null && anchor == null && !targetMissing && elapsed < TARGET_WAIT_TICKS) return;
        if (search.landingColumn == null) {
            if (anchor != null && search.emergencyColumn == 0) {
                search.landingColumn = nextMatchingColumn(level, search, anchor);
            }
            search.matchingBiome = search.landingColumn != null;
            if (!search.matchingBiome) search.landingColumn = emergencyColumn(level, search);
        }
        var column = search.landingColumn;
        Vec3 borderColumn = null;
        if (!search.matchingBiome) {
            var centered = Vec3.atBottomCenterOf(column);
            var body = BiomePortalBorderLanding.standing(player, centered);
            if (!fitsBorder(level, body)) {
                borderColumn = BiomePortalBorderLanding.fit(level.getWorldBorder(), player.getDimensions(Pose.STANDING).width, centered);
                if (borderColumn == null || (sameHorizontal(borderColumn, search.rejectedBorderColumn)
                        && level.getGameTime() < search.retryBorderAt)) {
                    search.landingColumn = null;
                    release(player, search);
                    return;
                }
                column = BlockPos.containing(borderColumn);
            }
        }
        if (!level.getWorldBorder().isWithinBounds(column)) {
            search.landingColumn = null;
            release(player, search);
            return;
        }
        boolean fractional = borderColumn != null && BiomePortalBorderLanding.fractional(borderColumn);
        if (!loadColumn(player, search, column, fractional ? 1 : 0)
                || fractional && !BiomePortalBorderLanding.loaded(level, BiomePortalBorderLanding.standing(player, borderColumn))) {
            if (level.getGameTime() - search.loadingSince >= GENERATION_WAIT_TICKS) {
                if (search.matchingBiome) search.rejectedTargetChunks.add(search.loading);
                search.landingColumn = null;
                release(player, search);
            }
            return;
        }
        var targetKind = search.matchingBiome ? search.route.kind() : null;
        Vec3 landing;
        if (fractional) {
            landing = BiomePortalBorderLanding.find(player, borderColumn);
            if (landing == null) {
                search.rejectedBorderColumn = borderColumn;
                search.retryBorderAt = level.getGameTime() + 20;
            }
        } else {
            landing = BiomePortalLanding.natural(level, player, targetKind, column);
            if (landing == null) landing = BiomePortalLanding.prepare(player, targetKind, column);
        }
        if (landing != null) {
            begin(player, search.entrance, search.route.kind(), landing, search.matchingBiome);
        } else {
            search.landingColumn = null;
            release(player, search);
        }
    }

    private static boolean sameHorizontal(Vec3 a, Vec3 b) {
        return b != null && a.x == b.x && a.z == b.z;
    }

    private static BlockPos nextMatchingColumn(ServerLevel level, Search search, BlockPos anchor) {
        var border = level.getWorldBorder();
        while (search.targetColumn < TARGET_COLUMNS) {
            var column = spiral(anchor, search.targetColumn++, 1);
            if (border.isWithinBounds(column) && !search.rejectedTargetChunks.contains(new ChunkPos(column))) {
                return column;
            }
        }
        return null;
    }

    private static BlockPos emergencyColumn(ServerLevel level, Search search) {
        int index = search.emergencyColumn++;
        BlockPos origin = search.route.frame();
        var at = spiral(origin, index + 25, 1);
        if (index == 24) {
            var spawn = level.getSharedSpawnPos();
            if (horizontalDistanceSquared(origin, spawn) > 64 && level.getWorldBorder().isWithinBounds(spawn)
                    && level.getChunkSource().getChunkNow(spawn.getX() >> 4, spawn.getZ() >> 4) != null) {
                return spawn;
            }
        }
        return level.getWorldBorder().clampToBounds(at.getX() + .5, at.getY(), at.getZ() + .5);
    }

    private static BlockPos spiral(BlockPos center, int index, int step) {
        if (index == 0) return center;
        int ring = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int side = ring * 2;
        int offset = index - (side - 1) * (side - 1);
        return switch (offset / side) {
            case 0 -> center.offset((offset - ring) * step, 0, -ring * step);
            case 1 -> center.offset(ring * step, 0, (offset % side - ring) * step);
            case 2 -> center.offset((ring - offset % side) * step, 0, ring * step);
            default -> center.offset(-ring * step, 0, (ring - offset % side) * step);
        };
    }

    private static boolean loadColumn(ServerPlayer player, Search search, BlockPos column) {
        return loadColumn(player, search, column, 0);
    }

    private static boolean loadColumn(ServerPlayer player, Search search, BlockPos column, int radius) {
        int localX = column.getX() & 15;
        int localZ = column.getZ() & 15;
        boolean edgeLanding = radius == 0 && search.landingColumn != null
            && (localX == 0 || localX == 15 || localZ == 0 || localZ == 15);
        if (edgeLanding) radius = 1;

        var chunk = new ChunkPos(column);
        var source = search.level.getChunkSource();
        long now = search.level.getGameTime();
        if (!chunk.equals(search.loading) || radius != search.loadingRadius) {
            release(player, search);
            search.loading = chunk;
            search.loadingRadius = radius;
            search.renewAt = 0;
            search.loadingSince = now;
        }
        if (now >= search.renewAt) {
            source.addRegionTicket(PREPARE, chunk, radius, player.getUUID());
            search.renewAt = now + 100;
        }
        return source.getChunkNow(chunk.x, chunk.z) != null && (!edgeLanding
            || BiomePortalBorderLanding.loaded(search.level, BiomePortalBorderLanding.standing(player, Vec3.atBottomCenterOf(column))));
    }

    private static void begin(ServerPlayer player, BlockPos entrance, BiomePortalStone kind, Vec3 at, boolean matchingBiome) {
        reserveLanding(player.serverLevel(), at);
        cancel(player);
        SourcePortalTransition.beginBiome(player, at, entrance, kind, matchingBiome);
    }

    private static void reserveLanding(ServerLevel level, Vec3 at) {
        var pos = BlockPos.containing(at);
        level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(pos), 1, pos);
    }

    static void retryLanding(ServerPlayer player, BlockPos entrance, BiomePortalStone kind) {
        var level = player.serverLevel();
        var frame = SourcePortalFrames.complete(level, entrance).orElse(null);
        if (frame == null || marker(frame) != kind || !stillInside(player, entrance, kind)
                || BossArenaPortals.blockedPortal(level, frame.rectangle(), frame.axis())) return;
        SEARCHES.put(player, new Search(level, entrance, route(frame, kind)));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    static void cancelLocked(ServerLevel level) {
        for (var entries = SEARCHES.entrySet().iterator(); entries.hasNext();) {
            var entry = entries.next();
            var search = entry.getValue();
            if (search.level != level || !BossArenaPortals.blockedNetherEntrance(level, search.entrance)) continue;
            var player = entry.getKey();
            entries.remove();
            release(player, search);
        }
    }

    private static void cancel(ServerPlayer player) {
        var search = SEARCHES.remove(player);
        if (search != null) release(player, search);
    }

    private static void release(ServerPlayer player, Search search) {
        if (search.loading == null) return;
        search.level.getChunkSource().removeRegionTicket(PREPARE, search.loading, search.loadingRadius, player.getUUID());
        search.loading = null;
    }

    private static List<List<BlockPos>> candidates(ServerLevel level, Route route, BlockPos anchor) {
        var source = level.getChunkSource();
        var biomes = source.getGenerator().getBiomeSource();
        var climate = source.randomState().sampler();
        var chunks = new LinkedHashMap<ChunkPos, List<BlockPos>>();
        for (int attempt = 0; attempt < 32; ++attempt) {
            int dx = level.random.nextInt(2 * LOCAL_RADIUS + 1) - LOCAL_RADIUS;
            int dz = level.random.nextInt(2 * LOCAL_RADIUS + 1) - LOCAL_RADIUS;
            if (dx * dx + dz * dz > LOCAL_RADIUS * LOCAL_RADIUS) continue;
            var column = anchor.offset(dx, 0, dz);
            int x = column.getX(), z = column.getZ();
            if (!level.getWorldBorder().isWithinBounds(column) || horizontalDistanceSquared(route.frame(), column) < 64) continue;
            var chunk = new ChunkPos(column);
            if (chunks.size() >= MAX_CHUNKS - 1 && !chunks.containsKey(chunk)) continue;
            if (!route.kind().matches(biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(anchor.getY()),
                    QuartPos.fromBlock(z), climate))) continue;
            chunks.computeIfAbsent(chunk, key -> new ArrayList<>()).add(column);
        }
        if (horizontalDistanceSquared(route.frame(), anchor) >= 64)
            chunks.computeIfAbsent(new ChunkPos(anchor), key -> new ArrayList<>()).add(anchor);
        appendInwardCandidates(level, route.frame(), anchor, chunks);
        return new ArrayList<>(chunks.values());
    }

    private static void appendInwardCandidates(ServerLevel level, BlockPos entrance, BlockPos anchor,
                                               Map<ChunkPos, List<BlockPos>> chunks) {
        for (var entry : chunks.entrySet()) {
            var chunk = entry.getKey();
            var columns = entry.getValue();
            int originalCount = columns.size();
            for (int index = 0; index < originalCount; index++) {
                var edge = columns.get(index);
                int x = Mth.clamp(edge.getX(), chunk.getMinBlockX() + 1, chunk.getMaxBlockX() - 1);
                int z = Mth.clamp(edge.getZ(), chunk.getMinBlockZ() + 1, chunk.getMaxBlockZ() - 1);
                if (x == edge.getX() && z == edge.getZ()) continue;
                var inside = new BlockPos(x, edge.getY(), z);
                if (horizontalDistanceSquared(anchor, inside) <= LOCAL_RADIUS * LOCAL_RADIUS
                        && horizontalDistanceSquared(entrance, inside) >= 64 && level.getWorldBorder().isWithinBounds(inside)
                        && !columns.contains(inside)) columns.add(inside);
            }
        }
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long x = first.getX() - (long)second.getX(), z = first.getZ() - (long)second.getZ();
        return x * x + z * z;
    }

    static boolean stillInside(ServerPlayer player, BlockPos entrance, BiomePortalStone kind) {
        // The touched block is part of the two-by-three interior. A distant player cannot still intersect it.
        if (!player.getBoundingBox().inflate(4).contains(Vec3.atCenterOf(entrance))) return false;
        var frame = SourcePortalFrames.complete(player.serverLevel(), entrance).orElse(null);
        if (frame == null || marker(frame) != kind) return false;
        var min = frame.rectangle().minCorner;
        boolean x = frame.axis() == Direction.Axis.X;
        return player.getBoundingBox().intersects(new AABB(min.getX(), min.getY(), min.getZ(),
            min.getX() + (x ? 2 : 1), min.getY() + 3, min.getZ() + (x ? 1 : 2)));
    }

    static boolean safeLanding(ServerPlayer player, BiomePortalStone kind, Vec3 at) {
        var level = player.serverLevel();
        return safeLanding(level, player, kind, at);
    }

    static boolean safeLanding(ServerLevel level, ServerPlayer player, BiomePortalStone kind, Vec3 at) {
        if (player != null && kind == null && BiomePortalBorderLanding.fractional(at)) {
            return BiomePortalBorderLanding.safe(level, player, at);
        }
        var dimensions = player == null ? EntityType.PLAYER.getDimensions() : player.getDimensions(Pose.STANDING);
        var body = dimensions.makeBoundingBox(at);
        var feet = BlockPos.containing(at);
        if (at.y != feet.getY() || feet.getY() <= level.getMinBuildHeight() || body.maxY >= level.getMaxBuildHeight()
                || !fitsBorder(level, body) || BossArenaPortals.blocked(level, feet)) return false;
        if (!BiomePortalBorderLanding.onSurface(level, body, feet.getY())) return false;
        var below = feet.below();
        var support = level.getBlockState(below);
        if (!BiomePortalBorderLanding.solidFloor(level, below, support)
                || kind != null && !kind.matches(level.getBiome(feet))
                || !BiomePortalBorderLanding.clearBody(level, player, body)) return false;
        if (unsafe(level.getBlockState(feet)) || unsafe(level.getBlockState(feet.above()))) return false;

        // Arrival can activate neighboring fluid ticks. Do not treat an unavailable side as dry.
        var chunks = level.getChunkSource();
        for (var side : Direction.Plane.HORIZONTAL) {
            var neighbor = feet.relative(side);
            var adjacent = chunks.getChunkNow(neighbor.getX() >> 4, neighbor.getZ() >> 4);
            if (adjacent == null || unsafeLandingSide(adjacent.getBlockState(neighbor))
                    || unsafeLandingSide(adjacent.getBlockState(neighbor.above()))) return false;
        }
        return true;
    }

    private static boolean unsafeLandingSide(BlockState state) {
        return state.is(Blocks.CACTUS) || !state.getFluidState().isEmpty();
    }

    static boolean fitsBorder(ServerLevel level, AABB body) {
        var border = level.getWorldBorder();
        return body.minX >= border.getMinX() && body.maxX <= border.getMaxX()
            && body.minZ >= border.getMinZ() && body.maxZ <= border.getMaxZ();
    }

    static void loadArrival(ServerPlayer player, Vec3 at) {
        var level = player.serverLevel();
        var feet = BlockPos.containing(at);
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(feet), 1, player.getId());
        player.setDeltaMovement(Vec3.ZERO);
    }

    static boolean unsafe(BlockState state) {
        return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
            || state.is(Blocks.SCAFFOLDING) || state.is(Blocks.MOVING_PISTON)
            || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
            || CampfireBlock.isLitCampfire(state);
    }

    private SourceBiomePortal() {}
}

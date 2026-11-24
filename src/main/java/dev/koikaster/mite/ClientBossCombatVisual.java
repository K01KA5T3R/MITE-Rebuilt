package dev.koikaster.mite;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
final class ClientBossCombatVisual {
    private static final int MAX_ACTIVE = 128;
    private static final int PARTICLES_PER_TICK = 192;
    private static final List<ActiveEffect> effects = new ArrayList<>();
    private static final Map<Integer, BossSkullTrail> skullTrails = new LinkedHashMap<>();
    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
    private static WeakReference<ClientLevel> owner = new WeakReference<>(null);
    private static long visualTick;
    private static int remainingParticles;

    static void receive(BossCombatVisual effect) {
        receive(effect, null);
    }

    static void receive(BossCombatVisual effect, Vec3 sectorEdgeOrWaterCenter) {
        ClientLevel level = Minecraft.getInstance().level;
        enter(level);
        if (level == null || !level.dimension().location().equals(effect.dimension())
                || Math.abs(level.getGameTime() - effect.startedAt()) > 20) return;
        boolean sector = effect.shape() == BossCombatVisual.Shape.CORE_SLICE;
        boolean waterRing = effect.shape() == BossCombatVisual.Shape.RING_SLICE && effect.theme() == BossCombatVisual.Theme.ELDER;
        if ((sector || waterRing) && sectorEdgeOrWaterCenter == null) return;
        Vec3 waterCenter = waterRing ? sectorEdgeOrWaterCenter : effect.origin();

        // A server time update cannot jump past the first frame of a newly received attack.
        effects.add(new ActiveEffect(effect, sector ? sectorEdgeOrWaterCenter : null, waterCenter, visualTick + 1));
        if (effects.size() > MAX_ACTIVE) effects.remove(0);
    }

    record ActiveEffect(BossCombatVisual visual, Vec3 secondEdge, Vec3 waterCenter, long startsAt) {
        long age() { return visualTick - startsAt; }
    }

    static void receive(BossSkullTrail trail) {
        ClientLevel level = Minecraft.getInstance().level;
        enter(level);
        if (level == null || !level.dimension().location().equals(trail.dimension())
                || Math.abs(level.getGameTime() - trail.startedAt()) > 104) return;
        if (skullTrails.size() >= 96 && !skullTrails.containsKey(trail.projectileId()))
            skullTrails.remove(skullTrails.keySet().iterator().next());
        skullTrails.put(trail.projectileId(), trail);
    }

    private static void enter(ClientLevel level) {
        if (owner.get() == level) return;
        effects.clear();
        skullTrails.clear();
        visualTick = 0;
        owner = new WeakReference<>(level);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        ClientLevel level = client.level;
        enter(level);
        if (level == null || client.isPaused()) return;

        long now = level.getGameTime();
        ++visualTick;
        remainingParticles = PARTICLES_PER_TICK;
        effects.removeIf(active -> active.age() >= active.visual().ticks());
        for (ActiveEffect active : effects) {
            if (remainingParticles == 0) break;
            var effect = active.visual();
            long age = active.age();
            var entity = level.getEntity(effect.actorId());
            if (age < 0 || !(entity instanceof LivingEntity actor) || !actor.isAlive()
                    || !actor.getUUID().equals(effect.actorUuid())) continue;
            draw(level, actor, active, (int)age);
        }
        skullTrails.values().removeIf(trail -> {
            var actor = level.getEntity(trail.ownerId());
            var entity = level.getEntity(trail.projectileId());
            long age = now - trail.startedAt();
            if (age > 104 || actor == null || !actor.isAlive() || !actor.getUUID().equals(trail.ownerUuid()))
                return true;
            if (entity == null) return age > 8;
            if (!(entity instanceof SourceWitherSkull skull) || !skull.isAlive()
                    || !skull.getUUID().equals(trail.projectileUuid())) return true;
            Vec3 tail = skull.position().add(0, skull.getBbHeight() * .5, 0)
                .subtract(skull.getDeltaMovement().scale(.28));
            spawn(level, skull.isDangerous() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, tail);
            return false;
        });
    }

    private static void draw(ClientLevel level, LivingEntity actor, ActiveEffect active, int age) {
        var effect = active.visual();
        Vec3 from = effect.origin();
        Vec3 to = effect.end();
        var shape = effect.shape();
        ParticleOptions particle = switch (effect.theme()) {
            case DRAGON -> shape == BossCombatVisual.Shape.CUE ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.DRAGON_BREATH;
            case WITHER -> shape == BossCombatVisual.Shape.WIND_STREAK ? ParticleTypes.POOF : ParticleTypes.SOUL_FIRE_FLAME;
            case GIANT -> shape == BossCombatVisual.Shape.BOULDER ? STONE : groundDebris(level, from);
            case ELDER -> shape == BossCombatVisual.Shape.BEAM || shape == BossCombatVisual.Shape.CUE
                ? ParticleTypes.GLOW : ParticleTypes.BUBBLE_POP;
            case QUEEN -> ParticleTypes.END_ROD;
        };
        switch (shape) {
            case CUE, CLOUD, WAVE -> {
                if (effect.theme() == BossCombatVisual.Theme.QUEEN) {
                    boolean warning = shape == BossCombatVisual.Shape.CUE;
                    var action = warning ? QueenBeeCombat.Action.WINDUP : QueenBeeCombat.Action.ACTIVE;
                    if (actor instanceof SourceQueenBee queen && queen.attackPose() == QueenBeeCombat.Move.HONEY_RING
                            && queen.actionPose() == action)
                        honeyMarker(level, particle, from, warning);
                    return;
                }
                if (age % 4 != 0) return;
                double radius = Mth.lerp(age / (float)effect.ticks(), effect.startRadius(), effect.endRadius());
                if (shape == BossCombatVisual.Shape.CUE) radius *= .55;
                int count = Mth.clamp(Mth.ceil(radius * 4), 6, 28);
                for (int i = 0; i < count && remainingParticles > 0; i++) {
                    double angle = i * Math.PI * 2 / count + age * .43;
                    double spread = shape == BossCombatVisual.Shape.CLOUD ? radius * ((i % 3 + 2) * .25) : radius;
                    Vec3 at = from.add(Math.cos(angle) * spread, .10 + (i % 3) * .19, Math.sin(angle) * spread);
                    if (shape == BossCombatVisual.Shape.CLOUD && effect.theme() == BossCombatVisual.Theme.DRAGON)
                        spawnClipped(level, actor, particle, from, at, false);
                    else spawn(level, particle, at);
                }
            }
            case BEAM, RING_SLICE, CORE_SLICE -> {
                if (age != 0) return;
                boolean inWater = effect.theme() == BossCombatVisual.Theme.ELDER;
                BiConsumer<Vec3, Vec3> sample = (anchor, at) ->
                    spawnClipped(level, actor, particle, inWater ? active.waterCenter() : anchor, at, inWater);
                if (shape == BossCombatVisual.Shape.BEAM)
                    BossParticlePattern.beam(from, to, effect.startRadius(), sample);
                else if (shape == BossCombatVisual.Shape.RING_SLICE)
                    BossParticlePattern.strip(from, to, effect.startRadius(), effect.endRadius(), inWater, sample);
                else
                    BossParticlePattern.sector(from, to, active.secondEdge(), effect.endRadius(), inWater, sample);
            }
            case WIND_STREAK, BURST_RAY -> {
                if (age != 0) return;
                Vec3 start = shape == BossCombatVisual.Shape.BURST_RAY ? from.lerp(to, .24) : from;
                int steps = Mth.clamp(Mth.ceil(start.distanceTo(to)), 1, 48);
                boolean inWater = effect.theme() == BossCombatVisual.Theme.ELDER;
                for (int i = 0; i <= steps && remainingParticles > 0; i++) {
                    Vec3 at = start.lerp(to, i / (double)steps);
                    if (inWater) spawnClipped(level, actor, particle, from, at, true);
                    else spawn(level, particle, at);
                }
            }
            case BOULDER -> {
                if (age != 0) return;
                double spread = Math.min(.72, effect.startRadius() * .58);
                boolean dragon = effect.theme() == BossCombatVisual.Theme.DRAGON;
                Vec3 travel = to.subtract(from);
                Vec3 front = dragon ? to.subtract(travel.normalize().scale(Math.min(.075, travel.length() * .25))) : to;
                spawn(level, dragon ? ParticleTypes.END_ROD : ParticleTypes.POOF, front);
                for (int i = 0; i < 7 && remainingParticles > 0; ++i) {
                    double angle = i * Math.PI * 2 / 7;
                    Vec3 center = from.lerp(front, i % 2 == 0 ? .75 : 1);
                    Vec3 at = center.add(Math.cos(angle) * spread, (i % 3 - 1) * spread, Math.sin(angle) * spread);
                    if (dragon) spawnClipped(level, actor, particle, center, at, false);
                    else spawn(level, particle, at);
                }
            }
            case GROUND_RIFT, PLUME -> {
                if (age != 0) return;
                double radius = effect.startRadius() * .76;
                for (int i = 0; i < 6 && remainingParticles > 0; ++i) {
                    double angle = i * Math.PI / 3;
                    Vec3 at = shape == BossCombatVisual.Shape.PLUME ? from.lerp(to, i / 6d) : from;
                    spawn(level, particle, at.add(Math.cos(angle) * radius, .13 + (i % 3) * .14, Math.sin(angle) * radius));
                }
            }
        }
    }

    private static ParticleOptions groundDebris(ClientLevel level, Vec3 at) {
        BlockPos position = BlockPos.containing(at.x, at.y - .1, at.z);
        if (level.hasChunkAt(position)) {
            var state = level.getBlockState(position);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft"))
                return new BlockParticleOption(ParticleTypes.BLOCK, state);
        }
        return STONE;
    }

    private static void spawn(ClientLevel level, ParticleOptions particle, Vec3 at) {
        if (remainingParticles <= 0) return;
        BlockPos position = BlockPos.containing(at);
        if (!level.hasChunkAt(position) || level.getBlockState(position).isCollisionShapeFullBlock(level, position))
            return;
        remainingParticles--;
        level.addAlwaysVisibleParticle(particle, true, at.x, at.y, at.z, 0, 0, 0);
    }

    private static void honeyMarker(ClientLevel level, ParticleOptions type, Vec3 at, boolean warning) {
        BlockPos position = BlockPos.containing(at);
        if (remainingParticles <= 0 || !level.hasChunkAt(position)
                || level.getBlockState(position).isCollisionShapeFullBlock(level, position)) return;
        // The safe opening must remain readable in unlit caves with minimal particles.
        var marker = Minecraft.getInstance().particleEngine.createParticle(type, at.x, at.y, at.z, 0, 0, 0);
        if (marker == null) return;
        remainingParticles--;
        marker.setParticleSpeed(0, 0, 0);
        marker.setColor(1, .64f, .16f);
        marker.scale(warning ? 2.1f : 1.85f);
        marker.setLifetime(warning ? 6 : 3);
    }

    private static void spawnClipped(ClientLevel level, LivingEntity actor, ParticleOptions particle,
                                     Vec3 anchor, Vec3 at, boolean inWater) {
        if (remainingParticles == 0 || !level.hasChunkAt(BlockPos.containing(at))) return;
        double length = anchor.distanceTo(at);
        if (length > .0001 && level.clip(new ClipContext(anchor, at, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, actor)).getType() != HitResult.Type.MISS) return;
        if (inWater) {
            int steps = Math.max(1, Mth.ceil(length * 2));
            var position = new BlockPos.MutableBlockPos();
            for (int i = 0; i <= steps; i++) {
                double progress = i / (double)steps;
                position.set(Mth.lerp(progress, anchor.x, at.x), Mth.lerp(progress, anchor.y, at.y),
                    Mth.lerp(progress, anchor.z, at.z));
                if (!level.hasChunkAt(position) || !level.getFluidState(position).is(FluidTags.WATER)) return;
            }
        }
        spawn(level, particle, at);
    }

    private ClientBossCombatVisual() {}
}

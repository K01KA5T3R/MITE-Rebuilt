package dev.koikaster.mite;

import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

record BossCombatVisual(ResourceLocation dimension, int actorId, UUID actorUuid, Theme theme, Shape shape, Vec3 origin, Vec3 end, float startRadius, float endRadius,
                        long startedAt, int ticks) {
    BossCombatVisual {
        if (ticks < 1 || ticks > 120 || !Float.isFinite(startRadius) || !Float.isFinite(endRadius)
                || startRadius < 0 || endRadius < 0 || startRadius > 32 || endRadius > 32
                || !finite(origin) || !finite(end)) throw new IllegalArgumentException("Invalid boss visual");
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    enum Theme { DRAGON, WITHER, GIANT, ELDER, QUEEN }

    enum Shape { CUE, WAVE, PLUME, CLOUD, BEAM, GROUND_RIFT, RING_SLICE, WIND_STREAK, BURST_RAY, CORE_SLICE, BOULDER }

    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(BossCombatVisual.class, 50, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(BossCombatVisual::write).decoder(BossCombatVisual::read)
            .consumerMainThread(BossCombatVisual::receive).add();
        value.messageBuilder(GroundBatch.class, 51, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GroundBatch::write).decoder(GroundBatch::read)
            .consumerMainThread(GroundBatch::receive).add();
        value.messageBuilder(SegmentBatch.class, 52, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SegmentBatch::write).decoder(SegmentBatch::read)
            .consumerMainThread(SegmentBatch::receive).add();
    }

    // One bounded packet per ground-wave step, carrying only confirmed surface contacts.
    static void ground(ServerLevel level, LivingEntity actor, List<Vec3> contacts) {
        if (contacts.isEmpty()) return;
        Vec3 origin = contacts.get(0);
        var visual = new BossCombatVisual(level.dimension().location(), actor.getId(), actor.getUUID(),
            Theme.WITHER, Shape.GROUND_RIFT, origin, origin, .77f, .77f, level.getGameTime(), 5);
        var packet = new GroundBatch(visual, contacts);
        for (var player : level.players()) {
            if (player.distanceToSqr(actor) <= 96 * 96)
                channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    static void honeyRing(ServerLevel level, SourceQueenBee actor, Vec3 origin, List<Vec3> contacts, boolean warning) {
        if (contacts.isEmpty()) return;
        var visual = new BossCombatVisual(level.dimension().location(), actor.getId(), actor.getUUID(),
            Theme.QUEEN, warning ? Shape.CUE : Shape.WAVE, origin, origin, 0, 0, level.getGameTime(), 1);
        var packet = new GroundBatch(visual, contacts);
        for (var player : level.players()) {
            if (player.distanceToSqr(origin) <= 46 * 46)
                channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private record GroundBatch(BossCombatVisual visual, List<Vec3> contacts) {
        GroundBatch {
            contacts = List.copyOf(contacts);
            if (contacts.isEmpty() || contacts.size() > contactLimit(visual)
                    || contacts.stream().anyMatch(point -> !finite(point)
                        || point.distanceToSqr(visual.origin) > 64 * 64))
                throw new IllegalArgumentException("Invalid ground visual batch");
        }

        private static int contactLimit(BossCombatVisual visual) {
            if (visual.theme == Theme.QUEEN)
                return visual.shape == Shape.CUE ? 32 : visual.shape == Shape.WAVE ? 16 : 0;
            return visual.shape == Shape.GROUND_RIFT ? 12 : 0;
        }

        private void write(FriendlyByteBuf buffer) {
            visual.write(buffer);
            buffer.writeVarInt(contacts.size());
            for (Vec3 point : contacts) {
                Vec3 offset = point.subtract(visual.origin);
                buffer.writeFloat((float)offset.x).writeFloat((float)offset.y).writeFloat((float)offset.z);
            }
        }

        private static GroundBatch read(FriendlyByteBuf buffer) {
            var visual = BossCombatVisual.read(buffer);
            int size = buffer.readVarInt();
            if (size < 1 || size > contactLimit(visual)) throw new IllegalArgumentException("Invalid ground visual count");
            var contacts = new ArrayList<Vec3>(size);
            for (int i = 0; i < size; ++i)
                contacts.add(visual.origin.add(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
            return new GroundBatch(visual, contacts);
        }

        private static void receive(GroundBatch packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var v = packet.visual;
                for (Vec3 point : packet.contacts)
                    ClientBossCombatVisual.receive(new BossCombatVisual(v.dimension, v.actorId, v.actorUuid,
                        v.theme, v.shape, point, point, v.startRadius, v.endRadius, v.startedAt, v.ticks));
            });
            context.get().setPacketHandled(true);
        }
    }

    static void ringSegments(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints,
                             double halfWidth, double height) {
        segments(level, actor, center, endpoints, Theme.WITHER, Shape.RING_SLICE, halfWidth, height, 3);
    }

    static void coreSegments(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints, double height) {
        segments(level, actor, center, endpoints, Theme.WITHER, Shape.CORE_SLICE, 0, height, 3);
    }

    static void windSegments(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints) {
        segments(level, actor, center, endpoints, Theme.WITHER, Shape.WIND_STREAK, .18, .22, 4);
    }

    static void burst(ServerLevel level, LivingEntity actor, Vec3 center, double radius) {
        var endpoints = new ArrayList<Vec3>(48);
        if (!level.noCollision(actor, new net.minecraft.world.phys.AABB(center, center).inflate(.3))) return;
        // Thick flame lobes stop before solid cover. This is release/aftermath only, not a forecast.
        for (int i = 0; i < 24; ++i) {
            double y = 1 - 2 * (i + .5) / 24;
            double ring = Math.sqrt(1 - y * y), angle = i * 2.399963229728653;
            Vec3 direction = new Vec3(Math.cos(angle) * ring, y, Math.sin(angle) * ring);
            Vec3 end = center;
            for (double length = .2; length <= radius * (.72 + .28 * Math.sin(i * 1.71) * Math.sin(i * 1.71)); length += .2) {
                Vec3 point = center.add(direction.scale(length));
                if (!level.noCollision(actor, new net.minecraft.world.phys.AABB(point, point).inflate(.3))) break;
                end = point;
            }
            if (center.distanceToSqr(end) > .16) { endpoints.add(center); endpoints.add(end); }
        }
        segments(level, actor, center, endpoints, Theme.WITHER, Shape.BURST_RAY, .26, .26, 8);
    }

    static void earthRing(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints, boolean core) {
        if(endpoints.isEmpty())return;
        var template=new BossCombatVisual(level.dimension().location(),actor.getId(),actor.getUUID(),
            Theme.GIANT,core?Shape.CORE_SLICE:Shape.RING_SLICE,center,center,core?0:.94f,.48f,level.getGameTime(),3);
        var packet=new SegmentBatch(template,endpoints);
        for(var player:level.players())if(player.distanceToSqr(center)<96*96)
            channel.send(PacketDistributor.PLAYER.with(()->player),packet);
    }

    static void earthStrip(ServerLevel level, LivingEntity actor, Vec3 origin, List<Vec3> endpoints,
                           double width, double height) {
        var template = new BossCombatVisual(level.dimension().location(), actor.getId(), actor.getUUID(),
            Theme.GIANT, Shape.RING_SLICE, origin, origin, (float) width, (float) height, level.getGameTime(), 3);
        var packet = new SegmentBatch(template, endpoints);
        for (var player : level.players()) if (player.distanceToSqr(origin) < 96 * 96)
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    static void waterWave(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints,
                          double halfWidth, double halfHeight, boolean filled) {
        Shape shape = filled ? Shape.CORE_SLICE : Shape.RING_SLICE;
        segments(level, actor, center, endpoints, Theme.ELDER, shape, halfWidth, halfHeight, 3);
    }

    static void waterBurst(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints) {
        segments(level, actor, center, endpoints, Theme.ELDER, Shape.BURST_RAY, 0, 0, 3);
    }

    private static void segments(ServerLevel level, LivingEntity actor, Vec3 center, List<Vec3> endpoints,
                                 Theme theme, Shape shape, double halfWidth, double height, int ticks) {
        if (endpoints.isEmpty()) return;
        var template = new BossCombatVisual(level.dimension().location(), actor.getId(), actor.getUUID(),
            theme, shape, center, center, (float)halfWidth, (float)height, level.getGameTime(), ticks);
        var packet = new SegmentBatch(template, endpoints);
        for (var player : level.players()) if (player.distanceToSqr(center) < 96 * 96)
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private record SegmentBatch(BossCombatVisual template, List<Vec3> endpoints) {
        SegmentBatch {
            endpoints = List.copyOf(endpoints);
            if (endpoints.isEmpty() || endpoints.size() > 64 || endpoints.size() % 2 != 0
                    || (template.shape != Shape.RING_SLICE && template.shape != Shape.WIND_STREAK && template.shape != Shape.BURST_RAY && template.shape != Shape.CORE_SLICE) || endpoints.stream().anyMatch(v -> !finite(v)
                        || v.distanceToSqr(template.origin) > 64 * 64))
                throw new IllegalArgumentException("Invalid ring segments");
        }
        private void write(FriendlyByteBuf buffer) {
            template.write(buffer); buffer.writeVarInt(endpoints.size());
            for (Vec3 endpoint : endpoints) {
                Vec3 offset = endpoint.subtract(template.origin);
                buffer.writeFloat((float)offset.x).writeFloat((float)offset.y).writeFloat((float)offset.z);
            }
        }
        private static SegmentBatch read(FriendlyByteBuf buffer) {
            var template = BossCombatVisual.read(buffer);
            int size = buffer.readVarInt();
            if (size < 2 || size > 64 || size % 2 != 0) throw new IllegalArgumentException("Invalid ring segment count");
            var endpoints = new ArrayList<Vec3>(size);
            for (int i = 0; i < size; ++i) endpoints.add(template.origin.add(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
            return new SegmentBatch(template, endpoints);
        }
        private static void receive(SegmentBatch packet, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.forEachSegment(ClientBossCombatVisual::receive));
            context.get().setPacketHandled(true);
        }

        private void forEachSegment(BiConsumer<BossCombatVisual, Vec3> consumer) {
            boolean sector = template.shape == Shape.CORE_SLICE;
            boolean waterRing = template.shape == Shape.RING_SLICE && template.theme == Theme.ELDER;
            for (int i = 0; i < endpoints.size(); i += 2) {
                Vec3 first = endpoints.get(i);
                Vec3 last = endpoints.get(i + 1);
                consumer.accept(new BossCombatVisual(template.dimension, template.actorId, template.actorUuid,
                    template.theme, template.shape, sector ? template.origin : first, sector ? first : last,
                    template.startRadius, template.endRadius, template.startedAt, template.ticks),
                    sector ? last : waterRing ? template.origin : null);
            }
        }
    }

    static void cue(ServerLevel level, LivingEntity actor, Theme theme, Vec3 at, double radius, int ticks) {
        effect(level, actor, theme, Shape.CUE, at, at, radius, radius, ticks);
    }

    static void area(ServerLevel level, LivingEntity actor, Theme theme, Shape shape, Vec3 at, double start, double end, int ticks) {
        effect(level, actor, theme, shape, at, at, start, end, ticks);
    }

    static void beam(ServerLevel level, LivingEntity actor, Theme theme, Vec3 from, Vec3 to, double radius, int ticks) {
        double lengthSquared = from.distanceToSqr(to);
        boolean visibleLength = theme == Theme.ELDER ? lengthSquared >= .01 : lengthSquared > .06;
        if (visibleLength) effect(level, actor, theme, Shape.BEAM, from, to, radius, radius, ticks);
    }

    static void boulder(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 to) {
        effect(level, actor, Theme.GIANT, Shape.BOULDER, from, to, 1.05, 1.05, 2);
    }

    static void dragonEmber(ServerLevel level, SourceEnderDragon dragon, Vec3 from, Vec3 to, double radius) {
        effect(level, dragon, Theme.DRAGON, Shape.BOULDER, from, to, radius, radius, 3);
    }

    static void earthRift(ServerLevel level, LivingEntity actor, Vec3 at) {
        effect(level,actor,Theme.GIANT,Shape.GROUND_RIFT,at,at,1.25,1.25,5);
    }

    static void earthColumn(ServerLevel level, LivingEntity actor, Vec3 base, Vec3 tip) {
        double height=Math.max(0,Math.min(2.5,tip.y-base.y));
        if(height>.1)effect(level,actor,Theme.GIANT,Shape.PLUME,base,tip,1.1,height,6);
    }

    static void earthBrace(ServerLevel level, LivingEntity actor, Vec3 base, Vec3 palm) {
        effect(level, actor, Theme.GIANT, Shape.PLUME, base, palm, .95, palm.y - base.y, 6);
    }

    private static void effect(ServerLevel level, LivingEntity actor, Theme theme, Shape shape, Vec3 from, Vec3 to,
                               double start, double end, int ticks) {
        var packet = new BossCombatVisual(level.dimension().location(), actor.getId(), actor.getUUID(), theme, shape, from, to, (float)start, (float)end, level.getGameTime(), ticks);
        Vec3 center = from.lerp(to, .5);
        double range = 56 + from.distanceTo(to) * .5;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= range * range) channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(actorId);
        buffer.writeUUID(actorUuid);
        buffer.writeEnum(theme);
        buffer.writeEnum(shape);
        buffer.writeDouble(origin.x).writeDouble(origin.y).writeDouble(origin.z);
        buffer.writeDouble(end.x).writeDouble(end.y).writeDouble(end.z);
        buffer.writeFloat(startRadius).writeFloat(endRadius).writeLong(startedAt);
        buffer.writeVarInt(ticks);
    }

    private static BossCombatVisual read(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int actorId = buffer.readVarInt();
        UUID actorUuid = buffer.readUUID();
        Theme theme = buffer.readEnum(Theme.class);
        Shape shape = buffer.readEnum(Shape.class);
        Vec3 from = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 to = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new BossCombatVisual(dimension, actorId, actorUuid, theme, shape, from, to, buffer.readFloat(), buffer.readFloat(), buffer.readLong(), buffer.readVarInt());
    }

    private static void receive(BossCombatVisual packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBossCombatVisual.receive(packet));
        context.get().setPacketHandled(true);
    }
}

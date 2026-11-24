package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** abn marks the source before tm's light test and on's separate foot test. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceSpawnerLight {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void checkPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.SPAWNER || event.getResult() == Event.Result.DENY) return;

        Mob candidate = event.getEntity();
        if (!usesStandardFootLight(candidate)) return;

        var feet = BlockPos.containing(candidate.getX(), candidate.getBoundingBox().minY, candidate.getZ());
        if (wouldIgnite(candidate) || SourceArachnidWorld.pointBrightness(candidate.level(), feet) > .7f) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean usesStandardFootLight(Mob candidate) {
        if (candidate instanceof Mobs.Undead humanoid) return !humanoid.earthBody();
        return candidate instanceof SourceZombieOwner
            || candidate instanceof SourceSkeleton
            || candidate instanceof SourceWitherSkeleton
            || candidate instanceof SourceLongdead
            || candidate instanceof SourceCreeper
            || candidate instanceof SourceBlaze
            || candidate instanceof SourceEnderman
            || candidate instanceof SourceInvisibleStalker
            || SourceArachnidOwners.species(candidate) != null;
    }

    private static boolean wouldIgnite(Mob candidate) {
        var level = candidate.level();
        if (candidate.getMobType() != MobType.UNDEAD || !level.dimensionType().hasSkyLight()
                || !SourceSpecialSpawns.sourceDay(level) || !candidate.isAlive() || candidate.isBaby()
                || !candidate.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return false;

        var crown = BlockPos.containing(candidate.getX(), candidate.getY() + candidate.getBbHeight(), candidate.getZ());
        return !OriginalWeather.rain(level, crown) && level.canSeeSky(crown)
            && OriginalWeather.brightness(candidate) > .5f;
    }

    @Mod.EventBusSubscriber(modid = MiteReforged.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Placements {
        @SubscribeEvent
        public static void register(SpawnPlacementRegisterEvent event) {
            register(event, EntityType.ZOMBIE);
            register(event, EntityType.ZOMBIE_VILLAGER);
            register(event, EntityType.ENDERMAN);
        }

        private static <T extends Monster> void register(SpawnPlacementRegisterEvent event, EntityType<T> type) {
            event.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Placements::allow, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        private static boolean allow(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
            // A template's actual body/equipment is available in PositionCheck, not here.
            if (reason == MobSpawnType.SPAWNER) return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
            return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
        }

        private Placements() {
        }
    }

    /** The additional 1.20.1 PathfinderMob check must use the same foot weight as R195 tm. */
    public static float nativeWalkWeight(float inherited, Object entity, Object reason) {
        if (reason != MobSpawnType.SPAWNER || !(entity instanceof Mob candidate)
                || !usesStandardFootLight(candidate)) return inherited;

        var feet = BlockPos.containing(candidate.getX(), candidate.getBoundingBox().minY, candidate.getZ());
        return .7f - SourceArachnidWorld.pointBrightness(candidate.level(), feet);
    }

    private SourceSpawnerLight() {
    }
}

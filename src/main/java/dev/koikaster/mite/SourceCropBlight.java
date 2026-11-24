package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restores classic R195/R196 crop blight and Plant Doctor curing when Extra Settings are disabled. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceCropBlight {
    public static final BooleanProperty BLIGHTED = BooleanProperty.create("mite_blighted");

    private record PendingCure(ServerPlayer player, ServerLevel level, BlockPos pos) {}

    private static final ThreadLocal<PendingCure> PENDING = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    public static void define(Object builder) {
        ((StateDefinition.Builder<Block, BlockState>) builder).add(BLIGHTED);
    }

    public static BlockState defaults(BlockState state) {
        return state.hasProperty(BLIGHTED) ? state.setValue(BLIGHTED, false) : state;
    }

    public static boolean blighted(BlockState state) {
        return state.hasProperty(BLIGHTED) && state.getValue(BLIGHTED);
    }

    public static void set(Level level, BlockPos pos, boolean blighted) {
        if (level.isClientSide || (blighted && WorldContentMode.rebuilt(level))) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!Agriculture.supports(state) || !state.hasProperty(BLIGHTED)) {
            return;
        }
        if (state.getValue(BLIGHTED) != blighted) {
            level.setBlock(pos, state.setValue(BLIGHTED, blighted), 3);
        }
    }

    public static boolean beforeGrowth(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (WorldContentMode.rebuilt(level) || !Agriculture.supports(state) || !state.hasProperty(BLIGHTED)) {
            return false;
        }
        var block = (CropBlock) state.getBlock();
        if (blighted(state)) {
            if (random.nextInt(64) == 0) {
                int age = block.getAge(state);
                if (block.isMaxAge(state) && age > 0) {
                    age--;
                }
                level.setBlock(pos, Agriculture.deadFor(state).defaultBlockState().setValue(CropBlock.AGE, age), 3);
            } else if (random.nextInt(2) == 0) {
                int ox = random.nextInt(3) - 1;
                int oy = random.nextInt(3) - 1;
                int oz = random.nextInt(3) - 1;
                if (ox != 0 || oy != 0 || oz != 0) {
                    BlockPos neighbor = pos.offset(ox, oy, oz);
                    BlockState target = level.getBlockState(neighbor);
                    if (Agriculture.supports(target) && target.hasProperty(BLIGHTED) && !blighted(target)) {
                        level.setBlock(neighbor, target.setValue(BLIGHTED, true), 3);
                    }
                }
            }
            return true;
        }
        if (bloodMoonBlight(level, pos, random) || naturalBlight(level, pos, random)) {
            level.setBlock(pos, state.setValue(BLIGHTED, true), 3);
            return true;
        }
        return false;
    }

    static boolean bloodMoonBlight(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.dimension() != Level.OVERWORLD || level.getServer().isDedicatedServer()) {
            return false;
        }
        if (!SourceMoonCycle.blood(level.getDayTime(), true) || random.nextFloat() >= 0.25f) {
            return false;
        }
        BlockPos above = pos.above();
        return !SourceBlockForms716.standard(level.getBlockState(above), level, above)
            || SourceSpawnOutdoors.isOutdoors(level, pos);
    }

    private static boolean naturalBlight(ServerLevel level, BlockPos pos, RandomSource random) {
        var biome = level.getBiome(pos).value();
        float factor = biome.hasPrecipitation()
            ? Math.max(0.2f, Math.min(2.0f, 1.0f + (biome.getBaseTemperature() - 0.8f) * 0.5f))
            : 0.2f;
        float darkness = 1.0f - level.getMaxLocalRawBrightness(pos) / 16.0f;
        return random.nextFloat() < 0.0005f * factor * darkness;
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        PENDING.remove();
        if (event.getEntity() instanceof ServerPlayer player
            && event.getLevel() instanceof ServerLevel level
            && !WorldContentMode.rebuilt(level)
            && blighted(event.getBlock())) {
            PENDING.set(new PendingCure(player, level, event.getPos().immutable()));
        }
    }

    public static boolean fertilize(Object rawLevel, Object rawPos, Object rawState) {
        var level = (ServerLevel) rawLevel;
        var pos = (BlockPos) rawPos;
        var state = (BlockState) rawState;
        PendingCure cure = PENDING.get();
        PENDING.remove();
        if (!blighted(state)) {
            return false;
        }
        level.setBlock(pos, state.setValue(BLIGHTED, false), 3);
        if (!WorldContentMode.rebuilt(level) && cure != null && cure.level() == level && cure.pos().equals(pos)) {
            SourceAchievementService570.grant(cure.player(), "plantDoctor");
        }
        return true;
    }

    private SourceCropBlight() {}
}

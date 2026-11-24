package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

final class BossTrophies {
    private BossTrophies() {}

    static RegistryObject<Item> register(String id, Block material) {
        RegistryObject<Block> block = Content.BLOCKS.register(id, () -> new Trophy(BlockBehaviour.Properties.of().mapColor(material.defaultMapColor())
            .sound(SoundType.STONE).strength(1.3f).noOcclusion()));
        Content.ALL_BLOCKS.put(id, block);
        return Content.item(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    static void first(ServerLevel level, LivingEntity defeated, BossMusicTrack track, RegistryObject<Item> item) {
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(Firsts::load, Firsts::new, "mite_boss_trophies");
        int flag = 1 << track.ordinal();
        if ((data.earned & flag) != 0 || defeated.spawnAtLocation(item.get()) == null) return;
        data.earned |= flag;
        data.setDirty();
    }

    private static final class Trophy extends HorizontalDirectionalBlock {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 11, 14);

        Trophy(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> state) { state.add(FACING); }

        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    }

    private static final class Firsts extends SavedData {
        private int earned;

        private static Firsts load(CompoundTag tag) {
            var firsts = new Firsts();
            firsts.earned = tag.getInt("Earned");
            return firsts;
        }

        @Override public CompoundTag save(CompoundTag tag) {
            tag.putInt("Earned", earned);
            return tag;
        }
    }
}

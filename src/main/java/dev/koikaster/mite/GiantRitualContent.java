package dev.koikaster.mite;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

final class GiantRitualContent {
    static final RegistryObject<Item> CALL = Content.item("giant_call", Call::new);
    static final RegistryObject<Item> TROPHY = BossTrophies.register("giant_trophy", Blocks.MOSSY_STONE_BRICKS);

    private GiantRitualContent() {}

    static void register() {}

    private static final class Call extends Item {
        Call() { super(new Properties().stacksTo(16)); }

        @Override public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var ground = context.getClickedPos();
            var foot = ground.above();
            var player = context.getPlayer();
            if (player == null || context.getClickedFace() != Direction.UP || level.dimension() != Level.OVERWORLD
                    || SourceMoonCycle.daytime(level.getDayTime()) || !level.canSeeSky(foot) || level.isOutsideBuildHeight(foot.above(12))
                    || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP) || !level.mayInteract(player, ground)) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                Vec3 at = Vec3.atBottomCenterOf(foot);
                if (!(player instanceof ServerPlayer) || !SourceGiantZombieBoss.summon(server, at, player.getYRot() + 180)) {
                    return InteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}

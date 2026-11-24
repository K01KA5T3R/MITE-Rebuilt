package dev.koikaster.mite;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Selects the original material change without changing the registries or item tags. */
final class R196Gold {
    static final float DURABILITY = 4f;

    static boolean equipment(String material) {
        if (!"gold".equals(material)) return false;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.isShutdown()) {
            return WorldContentMode.settings(server.getWorldData()).r196();
        }
        return FMLEnvironment.dist == Dist.CLIENT && ClientWorldContentMode.r196Equipment();
    }

    static Float hardness(BlockState state, BlockGetter world) {
        if (!(world instanceof Level level)) return null;
        var block = state.getBlock();
        float wood;
        if (block instanceof SourceMetalBuildings.MetalBars bars && bars.material.equals("gold")) {
            wood = .4f;
        } else if (block instanceof SourceMetalBuildings.MetalDoor door && door.material.equals("gold")) {
            wood = .5f;
        } else if (state.is(Blocks.GOLD_BLOCK)
            || block instanceof MetalAnvilBlock anvil && anvil.material.equals("gold")
            || block instanceof SourceMetalBuildings.Strongbox box && box.material.equals("gold")) {
            wood = 1.2f;
        } else {
            return null;
        }
        return WorldContentMode.r196(level) ? wood * DURABILITY : null;
    }

    private R196Gold() {}
}
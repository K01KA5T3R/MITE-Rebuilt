package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.google.gson.JsonParser;
import dev.koikaster.mite.Content;
import dev.koikaster.mite.MiteReforged;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.BakedModelWrapper;

public final class ClassicGroundItemRenderer2140 {
    private static final Random SPREAD = new Random(187L);
    private static final ResourceLocation BUSH = Content.id("bush");
    private static final ResourceLocation BUSH_ICON = Content.id("block/source_bowls_042/blueberry_picked");
    private static final ResourceLocation BED_ICON = Content.id("item/r195_ground_bed");
    private static final Set<ResourceLocation> QUARTER_TURN = originalQuarterTurn();

    public static boolean render(Object rawRenderer, Object rawItemRenderer, Object rawEntity, float partialTick,
                                 Object rawPose, Object rawBuffers, int light) {
        if (rawRenderer.getClass() != ItemEntityRenderer.class || !ClassicVisualState.ready || !ClassicVisuals.entities()) {
            return false;
        }
        var entity = (ItemEntity) rawEntity;
        var stack = entity.getItem();
        if (stack.isEmpty() || IClientItemExtensions.of(stack) != IClientItemExtensions.DEFAULT) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("minecraft") && !key.getNamespace().equals(MiteReforged.ID)) return false;

        var mc = Minecraft.getInstance();
        var renderer = (ItemRenderer) rawItemRenderer;
        var model = renderer.getModel(stack, entity.level(), null, entity.getId());
        if (model == mc.getModelManager().getMissingModel()) return false;
        var block = stack.getItem() instanceof BlockItem item ? item.getBlock() : null;
        boolean direct = !(block instanceof HalfTransparentBlock || block instanceof StainedGlassPaneBlock);
        var icons = model.isCustomRenderer() ? null : ClassicFlatItemRenderer.prepareGround(model, stack, direct);
        if (icons == null) {
            var sprite = originalSprite(mc, stack, key);
            if (sprite != null) icons = ClassicFlatItemRenderer.groundSprite(sprite);
        }
        boolean volume = icons == null && block != null && model.isGui3d()
            && (!model.isCustomRenderer() || block instanceof ChestBlock || block instanceof EnderChestBlock);
        if (!volume && icons == null) return false;

        var pose = (PoseStack) rawPose;
        var buffers = (MultiBufferSource) rawBuffers;
        boolean fancy = mc.options.graphicsMode().get() != GraphicsStatus.FAST;
        pose.pushPose();
        try {
            pose.translate(0f, ClassicGroundItemLayout2140.bob(entity.getAge(), partialTick, entity.bobOffs), 0f);
            float rotation = ClassicGroundItemLayout2140.spin(entity.getAge(), partialTick, entity.bobOffs);
            if (volume) {
                drawVolume(renderer, stack, model, pose, buffers, light, rotation,
                    QUARTER_TURN.contains(BuiltInRegistries.BLOCK.getKey(block)));
            } else {
                drawIcons(icons, stack, pose, buffers, light, fancy,
                    fancy ? rotation : 180f - mc.gameRenderer.getMainCamera().getYRot());
            }
        } finally {
            pose.popPose();
        }
        return true;
    }

    private static TextureAtlasSprite originalSprite(Minecraft mc, ItemStack stack, ResourceLocation key) {
        // The registered old bush used its picked icon, and the red bed was a separate item sprite.
        ResourceLocation icon = stack.is(Items.RED_BED) ? BED_ICON : BUSH.equals(key) ? BUSH_ICON : null;
        if (icon == null) return null;
        var sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(icon);
        return sprite.contents().name().equals(icon) ? sprite : null;
    }

    private static Set<ResourceLocation> originalQuarterTurn() {
        try (var input = ClassicGroundItemRenderer2140.class.getResourceAsStream("/assets/mite_reforged/classic_ground_quarter_turn2140.json")) {
            if (input == null) throw new IOException("Missing R195 ground-item block rotation list");
            var entries = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            Set<ResourceLocation> keys = new HashSet<>();
            for (var entry : entries) keys.add(new ResourceLocation(entry.getAsString()));
            return Set.copyOf(keys);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void drawVolume(ItemRenderer renderer, ItemStack stack, BakedModel model, PoseStack pose,
                                   MultiBufferSource buffers, int light, float rotation, boolean quarterTurn) {
        float scale = ClassicGroundItemLayout2140.VOLUME_SCALE;
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(scale, scale, scale);
        var withoutGroundTransform = new Untransformed(model);
        SPREAD.setSeed(187L);
        int copies = ClassicGroundItemLayout2140.copies(stack.getCount(), false);
        for (int i = 0; i < copies; i++) {
            pose.pushPose();
            try {
                if (i > 0) spread(pose, true);
                if (quarterTurn) pose.mulPose(Axis.YP.rotationDegrees(90f));
                renderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffers, light,
                    OverlayTexture.NO_OVERLAY, withoutGroundTransform);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void drawIcons(List<ClassicFlatItemRenderer.Pass> icons, ItemStack stack, PoseStack pose,
                                  MultiBufferSource buffers, int light, boolean fancy, float rotation) {
        float scale = ClassicGroundItemLayout2140.ICON_SCALE;
        pose.scale(scale, scale, scale);
        int copies = ClassicGroundItemLayout2140.copies(stack.getCount(), fancy);
        if (fancy) {
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            float step = ClassicGroundItemLayout2140.ICON_DEPTH_STEP;
            pose.translate(-.5f, -.25f, -(step * copies / 2f));
            for (int i = 0; i < copies; i++) {
                pose.translate(0f, 0f, step);
                ClassicFlatItemRenderer.render(icons, stack, pose, buffers, light, OverlayTexture.NO_OVERLAY, false);
            }
            return;
        }
        SPREAD.setSeed(187L);
        for (int i = 0; i < copies; i++) {
            pose.pushPose();
            try {
                if (i > 0) spread(pose, false);
                pose.mulPose(Axis.YP.rotationDegrees(rotation));
                pose.translate(-.5f, -.25f, 0f);
                ClassicFlatItemRenderer.renderBillboard(icons, stack, pose, buffers, light);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void spread(PoseStack pose, boolean volume) {
        pose.translate(ClassicGroundItemLayout2140.spread(SPREAD, volume), ClassicGroundItemLayout2140.spread(SPREAD, volume),
            ClassicGroundItemLayout2140.spread(SPREAD, volume));
    }

    private static final class Untransformed extends BakedModelWrapper<BakedModel> {
        Untransformed(BakedModel model) { super(model); }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) { return this; }
    }

    private ClassicGroundItemRenderer2140() {}
}


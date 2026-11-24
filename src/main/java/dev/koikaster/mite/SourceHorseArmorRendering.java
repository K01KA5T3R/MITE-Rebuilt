package dev.koikaster.mite;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.api.distmarker.*;
/** Render only the newly added original128x128 horse armor; native armor retains its existing renderer. */
@OnlyIn(Dist.CLIENT)
public final class SourceHorseArmorRendering {
 private static final Map<HorseArmorLayer,SourceHorseArmorModel> MODELS=new WeakHashMap<>();
 public static boolean render(Object rawLayer,Object rawPose,Object rawBuffers,int light,Object rawHorse,float swing,float amount,float partial,float age,float yaw,float pitch){var horse=(Horse)rawHorse;if(!(horse.getArmor().getItem() instanceof OriginalArmorItems.HorseArmor armor))return false;var layer=(HorseArmorLayer)rawLayer;var model=MODELS.computeIfAbsent(layer,key->new SourceHorseArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(SourceHorseArmorModel.LAYER)));layer.getParentModel().copyPropertiesTo(model);model.prepareMobModel(horse,swing,amount,partial);model.setupAnim(horse,swing,amount,age,yaw,pitch);var vertices=((MultiBufferSource)rawBuffers).getBuffer(RenderType.entityCutoutNoCull(armor.getTexture()));model.renderToBuffer((PoseStack)rawPose,vertices,light,OverlayTexture.NO_OVERLAY,1,1,1,1);return true;}
 private SourceHorseArmorRendering(){}
}

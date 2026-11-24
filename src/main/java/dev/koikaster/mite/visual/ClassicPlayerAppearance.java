package dev.koikaster.mite.visual;
import dev.koikaster.mite.MiteReforged;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.*;
/** Only the native fallback skin/model; PlayerInfo custom skin and model remain authoritative. */
@OnlyIn(Dist.CLIENT)
public final class ClassicPlayerAppearance {
 public static final ResourceLocation TEXTURE=new ResourceLocation(MiteReforged.ID,"textures/entity/classic_default_player.png");
 public static ResourceLocation fallbackTexture(){return ClassicVisualState.ready&&ClassicVisuals.entities()?TEXTURE:null;}
 public static String fallbackModel(){return ClassicVisualState.ready&&ClassicVisuals.entities()?"default":null;}
 private ClassicPlayerAppearance(){}
}

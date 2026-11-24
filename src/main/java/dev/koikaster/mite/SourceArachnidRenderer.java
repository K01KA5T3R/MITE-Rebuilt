package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Spider;
/** Source RenderArachnid/RenderWoodSpider/RenderPhaseSpider/bgd scale and shadow.
 * The established SpiderModel geometry is reused without changing its mesh. */
public final class SourceArachnidRenderer extends SpiderRenderer<Spider> {
 private final String name;private final float sourceScale;
 public SourceArachnidRenderer(EntityRendererProvider.Context context,String name){
  super(context);this.name=name;sourceScale=switch(name){case "wood_spider","phase_spider","black_widow_spider"->.6f;case "cave_spider"->.7f;default->1;};shadowRadius=sourceScale;
  if(!name.equals("spider")&&!name.equals("cave_spider")){layers.removeIf(layer->layer instanceof SpiderEyesLayer);addLayer(ClassicMobModels.glow(this,name,new ResourceLocation("minecraft:textures/entity/spider_eyes.png")));}
 }
 @Override protected void scale(Spider entity,PoseStack poses,float partial){poses.scale(sourceScale,sourceScale,sourceScale);}
 @Override protected float getFlipDegrees(Spider entity){return 180;}
 @Override public ResourceLocation getTextureLocation(Spider entity){return name.equals("spider")||name.equals("cave_spider")?new ResourceLocation("minecraft","textures/entity/spider/"+name+".png"):Content.id("textures/entity/"+name+".png");}
 public float sourceScale(){return sourceScale;}
 public float sourceShadow(){return shadowRadius;}
}

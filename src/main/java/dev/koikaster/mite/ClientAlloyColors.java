package dev.koikaster.mite;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 silhouettes with metal-only alloy tint. Wood and fluids stay untinted.
 * Unique baked IDs prevent old bundled packs reinstating the iron fallback. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientAlloyColors {
 static final String[] MATERIALS={"bronze","steel"};
 static final String[] ITEMS={"hatchet","knife","shears","dagger","battleaxe","mattock","coin","bucket"};
 static final String[] FILLED={"water","lava","milk","stone"};
 static ResourceLocation model(String name){return Content.id("item/fixed_alloys_046/"+name);}
 static int color(String material){return material.equals("bronze")?0xffffb96f:0xffcddadc;}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){
  for(String material:MATERIALS)for(String kind:ITEMS){int color=color(material);event.register((stack,layer)->layer==0?color:0xffffffff,Content.get(material+"_"+kind));}
 }
 @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional event){
  for(String material:MATERIALS){for(String kind:ITEMS)event.register(model(material+"_"+kind));for(String fluid:FILLED)event.register(model(material+"_bucket_"+fluid));}
 }
 @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event){
  for(String material:MATERIALS)for(String kind:ITEMS){String name=material+"_"+kind;var fixed=event.getModels().get(model(name));if(fixed==null)throw new IllegalStateException("Missing fixed alloy model "+name);event.getModels().put(new ModelResourceLocation(Content.id(name),"inventory"),fixed);}
 }
 private ClientAlloyColors(){}
}

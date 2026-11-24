package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Source uf.b(ye,int) stage selection; original pixels retain the existing rendering transforms. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SourceBowClient826 {
 private static final String[] ARROWS={"flint","obsidian","copper","silver","rusted_iron","gold","iron","mithril","adamantium","ancient_metal"};
 @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
  for(Item item:new Item[]{Items.BOW,Content.get("mithril_bow"),Content.get("ancient_metal_bow")}){
   ItemProperties.register(item,Content.id("source_arrow"),(stack,level,actor,seed)->arrow(actor));
   ItemProperties.register(item,Content.id("source_bow_stage"),(stack,level,actor,seed)->stage(stack,actor));
  }
 });}
 public static float arrow(LivingEntity actor){
  var item=actor instanceof Player player?SourceBowNock826.get(player):null;
  if(item==null||item==Items.ARROW)return 1;
  var stack=new ItemStack(item);String material=ClassicToolCatalogue.material(stack);
  // Modern tipped/spectral arrows and the selected holy-steel extension keep their own fallback artwork.
  if(!(item instanceof Tools.MetalArrow))return 0;
  for(int i=0;i<ARROWS.length;i++)if(ARROWS[i].equals(material))return i+1;
  return 0;
 }
 public static float stage(ItemStack stack,LivingEntity actor){
  if(actor==null||!actor.isUsingItem()||actor.getUseItem()!=stack)return 0;
  float fraction=SourceBowRules815.fraction(stack.getUseDuration()-actor.getUseItemRemainingTicks(),ClassicArchery.maxPull(stack));
  return fraction>=.9f?3:fraction>=.65f?2:fraction>0?1:0;
 }
 static void accept(SourceBowNock826.State state){
  var level=Minecraft.getInstance().level;if(level!=null&&level.getEntity(state.entity()) instanceof net.minecraft.client.player.RemotePlayer player)SourceBowNock826.apply(player,state);
 }
 private SourceBowClient826(){}
}

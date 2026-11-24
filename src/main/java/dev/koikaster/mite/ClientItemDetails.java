package dev.koikaster.mite;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shift reveals actual native equipment modifiers and the same food energy used by ingestion. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientItemDetails {
 private static boolean nativeDetail(Component line,boolean modifiers){
  if(line.getContents() instanceof TranslatableContents t&&(t.getKey().equals("item.durability")||modifiers&&(t.getKey().startsWith("item.modifiers.")||t.getKey().startsWith("attribute.modifier."))))return true;
  return line.getSiblings().stream().anyMatch(s->nativeDetail(s,modifiers));
 }
 private static void add(List<Component> lines,String key,Object... values){lines.add(Component.translatable(key,values).withStyle(ChatFormatting.GRAY));}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent event){appendForModifiers(event,Screen.hasShiftDown());ClientSlotContext1533.append(event);}
 static void appendForModifiers(ItemTooltipEvent event,boolean heldShift){
  boolean shift=SourceTooltipLayout1447.extended(event,heldShift);ItemStack stack=event.getItemStack();if(stack.isEmpty())return;SourceTooltipLayout1447.apply(event,shift);ClientMapDetails1551.append(event,shift);ClientBlockDetails1566.append(event,shift);SourceShiftDetails1524.append(stack,event.getToolTip(),shift);ClientAnvilInfo1602.append(event,shift);
  if(stack.is(Items.EXPERIENCE_BOTTLE)){if(shift)event.getToolTip().add(Component.translatable("tooltip.mite_reforged.experience_value",SourceExperienceBottle854.EXPERIENCE).withStyle(ChatFormatting.BLUE));return;}
  boolean milk=stack.is(Items.MILK_BUCKET)||ClassicVessels.handled(stack)&&ClassicVessels.fluid(stack).equals("milk");
  boolean food=stack.isEdible()||stack.is(Items.CAKE)||milk;
  boolean equipment=stack.getMaxDamage()>0||stack.getItem() instanceof HorseArmorItem;
  if(!equipment&&!food)return;
  var lines=event.getToolTip();lines.removeIf(line->!SourceTooltipLore1669.authored(stack,line)&&nativeDetail(line,!shift));
  if(!shift)return;
  if(stack.getMaxDamage()>0)add(lines,"tooltip.mite_reforged.durability_detail",Math.max(0,stack.getMaxDamage()-stack.getDamageValue()),stack.getMaxDamage());
  float protection=stack.getItem() instanceof HorseArmorItem horse?horse.getProtection():OriginalArmorItems.protection(stack,event.getEntity());
  if(stack.getItem() instanceof HorseArmorItem||OriginalArmorItems.profile(stack)!=null&&!ClassicToolCatalogue.sourceItem(stack))add(lines,"tooltip.mite_reforged.protection",String.format(Locale.ROOT,protection<1?"%.2f":"%.1f",protection));
  if(food){
   var energy=milk?new SurvivalFood.Energy(Diet.MILK_BUCKET.satiation(),Diet.MILK_BUCKET.nutrition()):stack.is(Items.CAKE)?new SurvivalFood.Energy(Diet.CAKE_SLICE.satiation(),Diet.CAKE_SLICE.nutrition()):SurvivalFood.energy(stack,event.getEntity());
   if(stack.is(Items.CAKE)&&event.getEntity()!=null)energy=new SurvivalFood.Energy(Diet.read(event.getEntity()).adjustedSatiation(energy.satiation(),Diet.CAKE_SLICE.sugar()),energy.nutrition());
   if(stack.is(Items.CAKE))add(lines,"tooltip.mite_reforged.per_cake_slice");
   add(lines,"tooltip.mite_reforged.food_hunger",energy.nutrition());add(lines,"tooltip.mite_reforged.food_satiation",energy.satiation());
  }
 }
 private ClientItemDetails(){}
}

package dev.koikaster.mite;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** ItemArrow/ItemBrick/ItemGelatinousSphere/wp and ye's silver-tool detail. */
public final class SourceShiftDetails1524 {
 private static void add(List<Component> lines,String key,ChatFormatting color,Object... values){lines.add(Component.translatable("tooltip.mite_reforged."+key,values).withStyle(color));}
 public static void append(ItemStack stack,List<Component> lines,boolean extended){
  if(!extended||!ClassicToolCatalogue.sourceItem(stack))return;
  String material=ClassicToolCatalogue.material(stack);int damage=-1,recovery=-1,velocity=0;
  boolean silver=false;
  if(stack.getItem() instanceof ArrowItem&&java.util.Arrays.asList(ClassicToolCatalogue.ARROW_MATERIALS).contains(material)){
   damage=(int)ClassicToolCatalogue.materialDamage(material);recovery=(int)(ClassicToolCatalogue.arrowRecovery(material)*100);silver=material.equals("silver");
  }else if(stack.is(Items.BRICK)||stack.is(Items.NETHER_BRICK))damage=1;
  else if(stack.is(Items.SLIME_BALL))damage=SourceGelatinousImpact801.attackDamage(0);
  else if(stack.getItem() instanceof SourceGelatinousSphere.SphereItem sphere)damage=SourceGelatinousImpact801.attackDamage(sphere.subtype);
  if(stack.getItem() instanceof BowItem)velocity=Math.round((ClassicToolCatalogue.bowVelocity(material)-1)*100);
  var tool=ClassicToolWear.profile(stack);silver|=tool!=null&&tool.material().equals("silver");
  if(damage>=0||velocity>0)lines.add(Component.empty());
  if(damage>=0)add(lines,"missile_damage",ChatFormatting.BLUE,damage);
  if(velocity>0)add(lines,"bow_velocity",ChatFormatting.BLUE,velocity);
  if(silver)add(lines,"bonus_vs_undead",ChatFormatting.WHITE);
  if(recovery>=0)add(lines,"missile_recovery",ChatFormatting.GRAY,recovery);
 }
 private SourceShiftDetails1524(){}
}

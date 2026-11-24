package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
public final class PlayerState {
 private static final java.util.UUID ARMOR_WEAR=java.util.UUID.fromString("2ea40a0e-bc86-475e-b099-c847c77e110d");
 public static CompoundTag data(Player p){
  var root=p.getPersistentData();if(!root.contains(MiteReforged.ID)){
   var t=new CompoundTag();t.putBoolean("FreshFoodStores",true);root.put(MiteReforged.ID,t);
  }return root.getCompound(MiteReforged.ID);
 }
 public static int level(Player p){int l=Rules.level(data(p).getInt("Xp"));return p.isCreative()?Math.max(0,l):l;}
 public static int foodCap(Player p){return Rules.foodCap(level(p));}
 public static void addXp(ServerPlayer p,int amount){
  var t=data(p);t.putInt("Xp",Rules.addXp(t.getInt("Xp"),amount));syncVanillaXp(p);Network.sync(p);
 }
 public static void syncVanillaXp(ServerPlayer p){
  int xp=data(p).getInt("Xp"),l=level(p);p.experienceLevel=l;p.totalExperience=xp;
  p.experienceProgress=Rules.levelProgress(xp,l);
 }
 public static void tick(ServerPlayer p){
  if(p.isCreative()||p.isSpectator())return;
  var t=data(p);int lv=level(p);if(p.experienceLevel!=lv){int target=Math.max(Rules.MIN_LEVEL,Math.min(Rules.MAX_LEVEL,p.experienceLevel));t.putInt("Xp",Rules.xpForLevel(target));syncVanillaXp(p);lv=target;}
  double health=Rules.healthCap(lv,Settings.START_HEALTH);
  var max=p.getAttribute(Attributes.MAX_HEALTH);if(max!=null&&max.getBaseValue()!=health)max.setBaseValue(health);
  if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
  var speed=p.getAttribute(Attributes.ATTACK_SPEED);if(speed!=null&&speed.getBaseValue()!=100)speed.setBaseValue(100);
  int cap=foodCap(p);if(p.getFoodData().getFoodLevel()>cap)p.getFoodData().setFoodLevel(cap);
  if(p.getFoodData().getSaturationLevel()>cap)p.getFoodData().setSaturation(cap);
  if(p.tickCount%20==0){
   var protection=p.getAttribute(Attributes.ARMOR);if(protection!=null)protection.removeModifier(ARMOR_WEAR);

   for(ItemStack s:p.getInventory().items)if(WorldContentEvents.effectAllowed(p,s))convertMending(s);
   for(ItemStack s:p.getArmorSlots())if(WorldContentEvents.effectAllowed(p,s))convertMending(s);
   if(WorldContentEvents.effectAllowed(p,p.getOffhandItem()))convertMending(p.getOffhandItem());
   syncVanillaXp(p);Network.sync(p);
  }
  if(!t.getBoolean("AdamantiumUnlocked"))for(ItemStack s:p.getInventory().items)if((HarvestRules.toolLevel(s)>=5||s.is(Content.get("adamantium_ingot")))&&WorldContentEvents.effectAllowed(p,s)){t.putBoolean("AdamantiumUnlocked",true);break;}
  if(p.isFallFlying()&&!t.getBoolean("AdamantiumUnlocked"))p.stopFallFlying();
 }
 public static void convertMending(ItemStack s){
  if(s.isEmpty())return;var map=EnchantmentHelper.getEnchantments(s);
  if(map.containsKey(Enchantments.MENDING)){int level=map.remove(Enchantments.MENDING);map.put(Content.SLOW_MENDING.get(),level);EnchantmentHelper.setEnchantments(map,s);}
 }
 public static int mend(ServerPlayer p,int xp){
  if(xp<=0)return xp;
  java.util.List<ItemStack> candidates=new java.util.ArrayList<>();
  for(ItemStack s:p.getArmorSlots())if(s.isDamaged()&&WorldContentEvents.effectAllowed(p,s)&&EnchantmentHelper.getItemEnchantmentLevel(Content.SLOW_MENDING.get(),s)>0)candidates.add(s);
  for(ItemStack s:p.getHandSlots())if(s.isDamaged()&&WorldContentEvents.effectAllowed(p,s)&&EnchantmentHelper.getItemEnchantmentLevel(Content.SLOW_MENDING.get(),s)>0)candidates.add(s);
  if(candidates.isEmpty())return xp;
  ItemStack s=candidates.get(p.getRandom().nextInt(candidates.size()));double rate=2*Settings.MENDING;if(rate<=0)return xp;
  var t=data(p);double credit=t.getDouble("MendingCredit");int damage=s.getDamageValue();
  int used=Math.min(xp,(int)Math.ceil(Math.max(0,damage-credit)/rate));double value=credit+used*rate;
  int repair=Math.min(damage,(int)Math.floor(value));s.setDamageValue(damage-repair);t.putDouble("MendingCredit",Math.max(0,value-repair));
  return xp-used;
 }
 private PlayerState(){}
}

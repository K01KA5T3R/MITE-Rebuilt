package dev.koikaster.mite;

import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameRules;

/** Native equipment adapter for the tn owner. It must replace Mob's equipment
 * pickup/drop consumer, not run in parallel with it. No independent inventory
 * or drop-chance authority is stored here. */
public final class SourcePigmanGear implements SourcePigmanEquipment.Drops {
 public interface Host {
  Mob actor();float dropChance(int slot);void dropChance(int slot,float value);
  boolean massiveFall();boolean pickedUpHeld();void pickedUpHeld(boolean value);
 }
 private static final EquipmentSlot[] SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
 private final Host host;private final Mob actor;
 public SourcePigmanGear(Host host){this.host=host;actor=host.actor();}
 public static EquipmentSlot slot(int index){return SLOTS[index];}
 private static int position(ItemStack stack){
  if(stack.getItem() instanceof ArmorItem armor)return switch(armor.getType()){case BOOTS->1;case LEGGINGS->2;case CHESTPLATE->3;case HELMET->4;};
  if(stack.is(Items.PUMPKIN)||stack.is(Items.CARVED_PUMPKIN))return 4;
  return 0;
 }
 private static boolean tool(ItemStack stack){var profile=ClassicToolWear.profile(stack);return profile!=null&&profile.kind()!=LegacyToolWear.Kind.SHEARS;}
 private static boolean wearable(ItemStack stack){return stack.getItem() instanceof ArmorItem||stack.is(Items.PUMPKIN)||stack.is(Items.CARVED_PUMPKIN);}
 private static boolean bow(ItemStack stack){return stack.getItem() instanceof BowItem&&ClassicToolCatalogue.sourceItem(stack);}
 public ItemStack initialWeapon(SourcePigmanLoot.Item kind){
  Item item=switch(kind){case GOLD_SWORD->Items.GOLDEN_SWORD;case GOLD_BATTLEAXE->Content.get("gold_battleaxe");case GOLD_PICKAXE->Items.GOLDEN_PICKAXE;default->throw new IllegalArgumentException("tn weapon");};
  var stack=new ItemStack(item);SourcePigmanEquipment.wear(new Facts(stack),this);return stack;
 }
 /** og.tryPickUpItems intentionally has no ss food/valuable pickup-delay,
  * fire or ray gates. It precedes that separate consumer in og.c. */
 public void pickup(){
  if(actor.level().isClientSide||!actor.canPickUpLoot()||actor.isRemoved()||actor.getHealth()<=0||!actor.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||(actor.tickCount+actor.getId()*47)%10!=0)return;
  for(ItemEntity entity:actor.level().getEntitiesOfClass(ItemEntity.class,actor.getBoundingBox().inflate(1,.25,1))){
   if(entity.isRemoved())continue;
   var ground=entity.getItem();if(ground.isEmpty()||!tool(ground)&&!wearable(ground)&&!(bow(actor.getMainHandItem())&&bow(ground)))continue;
   int index=position(ground);var current=actor.getItemBySlot(SLOTS[index]);
   if(!SourcePigmanEquipment.replace(new Facts(ground),current.isEmpty()?null:new Facts(current),index))continue;
   actor.playSound(SoundEvents.ITEM_PICKUP,.2f,((nextFloat()-nextFloat())*.7f+1f)*2);
   if(!current.isEmpty()&&nextFloat()<host.dropChance(index))actor.spawnAtLocation(current,0);
   if(ground.getCount()==1){actor.setItemSlot(SLOTS[index],ground);actor.take(entity,1);entity.discard();}
   else{var one=ground.copy();one.setCount(1);actor.setItemSlot(SLOTS[index],one);ground.shrink(1);entity.setItem(ground);}
   host.dropChance(index,2);actor.setPersistenceRequired();if(index==0)host.pickedUpHeld(true);break;
  }
 }
 public void death(boolean recent,int looting){SourcePigmanEquipment.drops(this,recent,looting);}
 @Override public float nextFloat(){return actor.getRandom().nextFloat();}
 @Override public int slots(){return SLOTS.length;}
 @Override public SourcePigmanEquipment.Stack item(int index){var stack=actor.getItemBySlot(SLOTS[index]);return stack.isEmpty()?null:new Facts(stack);}
 @Override public float chance(int index){return host.dropChance(index);}
 @Override public boolean massiveFall(){return host.massiveFall();}
 @Override public boolean pickedUpHeld(){return host.pickedUpHeld();}
 @Override public void dropAndClear(int index){actor.spawnAtLocation(actor.getItemBySlot(SLOTS[index]),0);actor.setItemSlot(SLOTS[index],ItemStack.EMPTY);}
 private record Facts(ItemStack stack) implements SourcePigmanEquipment.Stack {
  public boolean tool(){return SourcePigmanGear.tool(stack);}public boolean bow(){return SourcePigmanGear.bow(stack);}public boolean armor(){return stack.getItem() instanceof ArmorItem;}
  public boolean enchanted(){return stack.isEnchanted();}

  /** R195 stored durability separately from the optional item tag. */
  public boolean tagged() {
   var tag = stack.getTag();
   if (tag == null) return false;
   if (tag.isEmpty()) return true;
   for (var key : tag.getAllKeys()) {
    if (key.equals("Damage") && stack.getMaxDamage() > 0 && tag.contains(key, Tag.TAG_ANY_NUMERIC)) continue;
    if (key.equals(MineralSupply.SUPPLY) && tag.contains(key, Tag.TAG_INT)) {
     int supply = tag.getInt(key);
     if (supply == MineralSupply.GENERATED || supply == MineralSupply.STOCK) continue;
    }
    if (key.equals(ClassicDefense.LAST_ARMOR_USE_WARNED) && armor()
        && tag.contains(key, Tag.TAG_BYTE) && tag.getByte(key) == 1) continue;
    return true;
   }
   return false;
  }

  public boolean damageable(){return stack.isDamageableItem();}
  public int damage(){return stack.getDamageValue();}public int maximum(){return stack.getMaxDamage();}public void damage(int value){stack.setDamageValue(value);}
  public float combinedToolDamage(){var profile=ClassicToolWear.profile(stack);return ClassicToolCatalogue.baseDamage(profile.kind())+ClassicToolCatalogue.materialDamage(profile.material());}
  public float multipliedProtection(){var profile=OriginalArmorItems.profile(stack);float protection=profile.components()*profile.materialProtection()/24f;int level=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION,stack);if(level>0)protection+=protection*(level/4f)*.5f;return protection;}
 }
}

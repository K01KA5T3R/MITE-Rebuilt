package dev.koikaster.mite;

import java.lang.reflect.Field;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** Player wp entry points and world-scoped use of native held ammunition. */
public final class SourceBowUse815 {
 private static final Field RANDOM=ObfuscationReflectionHelper.findField(Entity.class,"f_19796_");
 public static boolean accepts(ItemStack stack){return stack.is(Items.BOW)||stack.getItem() instanceof Tools.MetalBow;}
 static final class Inventory implements SourceBowRules815.Inventory {
  final net.minecraft.world.entity.player.Inventory inv;Inventory(Player player){inv=player.getInventory();}
  public int size(){return inv.items.size();}public int hotbarSize(){return net.minecraft.world.entity.player.Inventory.getSelectionSize();}
  public Object stack(int slot){var s=inv.items.get(slot);return s.isEmpty()?null:s;}public boolean arrow(Object stack){return ((ItemStack)stack).getItem() instanceof ArrowItem;}
  public int count(Object stack){return ((ItemStack)stack).getCount();}public Object item(Object stack){return ((ItemStack)stack).getItem();}
  public void decrement(Object stack){((ItemStack)stack).shrink(1);}public void set(int slot,Object stack){inv.items.set(slot,stack==null?ItemStack.EMPTY:(ItemStack)stack);}
 }
 static final class Use implements SourceBowRules815.Release {
  final Level level;final Player player;final ItemStack bow;final InteractionHand hand;final Inventory inventory;
  Use(Level level,Player player,ItemStack bow,InteractionHand hand){this.level=level;this.player=player;this.bow=bow;this.hand=hand;inventory=new Inventory(player);}
  public boolean client(){return level.isClientSide;}public boolean creative(){return player.getAbilities().instabuild;}
  private ItemStack offhandAmmo(){
   var stack=player.getOffhandItem();
   if(stack.isEmpty()||!ModernOffhand.allowed(player,stack)||!WorldContentEvents.itemActionAllowed(level,stack))return ItemStack.EMPTY;
   return stack.getItem() instanceof ArrowItem&&((BowItem)bow.getItem()).getSupportedHeldProjectiles().test(stack)?stack:ItemStack.EMPTY;
  }
  public Object readied(){var off=offhandAmmo();return off.isEmpty()?SourceBowRules815.readied(inventory):off.getItem();}
  public Object nocked(){return SourceBowNock826.get(player);}public Object flint(){return Items.ARROW;}
  public void nock(Object item){SourceBowNock826.set(player,(Item)item);}
  public void syncNock(){SourceBowNock826.sync(player);}public void start(){player.startUsingItem(hand);}public int maxPull(){return ClassicArchery.maxPull(bow);}
  public Object create(Object raw,float velocity){
   Item item=(Item)raw;var off=offhandAmmo();ItemStack ammo=off.is(item)?off.copy():null;
   if(ammo==null){int slot=SourceBowRules815.arrowSlot(inventory);ammo=slot<0?new ItemStack(item):player.getInventory().items.get(slot).copy();}
   var arrow=((BowItem)bow.getItem()).customArrow(((ArrowItem)item).createArrow(level,ammo,player));
   SourceSpecificImmunities.captureLauncher(arrow,bow);
   if(item==Items.ARROW)arrow.setBaseDamage(ClassicToolCatalogue.arrowDamage("flint"));
   final RandomSource random;try{random=(RandomSource)RANDOM.get(arrow);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
   // Original nonnegative level benefit requires archery only in skill-enabled saved worlds.
   float wander=SourceBowRules815.wander(PlayerState.level(player),SourceSkills1685.enabled(level),SourceSkills1685.has(player,SourceSkillRules1685.ARCHERY),Math.round(EnchantmentEffects.fraction("true_flight",bow)*40),ClassicArchery.suspended(player));
   var motion=SourceBowRules815.launch(player.getX(),player.getY(),player.getZ(),player.getEyeHeight(),player.getYRot(),player.getXRot(),velocity,ClassicToolCatalogue.bowVelocity(ClassicToolCatalogue.material(bow)),wander,new SourceBowRules815.Random(){public double gaussian(){return random.nextGaussian();}public boolean bool(){return random.nextBoolean();}});
   arrow.setPos(motion.x(),motion.y(),motion.z());arrow.setDeltaMovement(motion.dx(),motion.dy(),motion.dz());arrow.setYRot(motion.yaw());arrow.yRotO=motion.yaw();arrow.setXRot(motion.pitch());arrow.xRotO=motion.pitch();
   arrow.pickup=AbstractArrow.Pickup.ALLOWED;return arrow;
  }
  public void critical(Object arrow){((AbstractArrow)arrow).setCritArrow(true);}public int power(){return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,bow);}public int punch(){return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,bow);}public int flame(){return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,bow);}
  public void power(Object arrow,int level){var a=(AbstractArrow)arrow;a.setBaseDamage(a.getBaseDamage()+(double)((float)level*.5f)+.5);}
  public void punch(Object arrow,int level){((AbstractArrow)arrow).setKnockback(level);}public void flame(Object arrow){((AbstractArrow)arrow).setSecondsOnFire(100);}
  public void wear(){bow.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));}public float nextItemFloat(){return LegacyItemRandom749.RANDOM.nextFloat();}
  public void sound(float pitch){level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1f,pitch);}
  public void creativePickup(Object arrow){((AbstractArrow)arrow).pickup=AbstractArrow.Pickup.CREATIVE_ONLY;}
  public void consume(){var off=offhandAmmo();if(!off.isEmpty())off.shrink(1);else SourceBowRules815.consume(inventory);player.getInventory().setChanged();}
  public void spawn(Object arrow){level.addFreshEntity((AbstractArrow)arrow);}
 }
 public static InteractionResultHolder<ItemStack> use(Object rawLevel,Object rawPlayer,Object rawHand){
  var level=(Level)rawLevel;var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;var bow=player.getItemInHand(hand);if(!accepts(bow))return null;
  if(!ModernOffhand.canUseHand(player,hand))return InteractionResultHolder.fail(bow);
  var use=new Use(level,player,bow,hand);var event=ForgeEventFactory.onArrowNock(bow,level,player,hand,use.readied()!=null);if(event!=null)return event;
  return SourceBowRules815.use(use)?InteractionResultHolder.consume(bow):InteractionResultHolder.fail(bow);
 }
 public static boolean release(Object rawBow,Object rawLevel,Object rawActor,int remaining){
  var bow=(ItemStack)rawBow;if(!accepts(bow))return false;
  var level=(Level)rawLevel;if(level.isClientSide||!(rawActor instanceof Player player))return true;
  var hand=player.getUsedItemHand();if(!ModernOffhand.canUseHand(player,hand))return true;
  var use=new Use(level,player,bow,hand);int drawn=ForgeEventFactory.onArrowLoose(bow,level,player,bow.getUseDuration()-remaining,use.readied()!=null||use.creative());
  if(drawn>=0)SourceBowRules815.release(use,drawn);return true;
 }
 private SourceBowUse815(){}
}

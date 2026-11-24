package dev.koikaster.mite;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The user's later choice restores only Wight, Shadow and Earth Elemental.
 * Predicates are from their R195 isImmuneTo methods, nb and nc. The original
 * absolute-damage bypass remains outside the species predicates (nn).
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceSpecificImmunities {
 private static final String LAUNCH_CAPTURED="R195GateLauncherCaptured";
 private static final String LAUNCH_MAGIC="R195GateLauncherEnchanted";
 private static final String LAUNCH_FLAME="R195GateLauncherFlame";

 /** nc checks the arrow's launcher, never its shooter's hand at impact.
  * BowItem's existing release hook supplies the actual stack, including an
  * offhand bow. Native mob/crossbow arrows use the join event below.
  */
 public static void captureLauncher(AbstractArrow arrow,ItemStack launcher){
  arrow.getPersistentData().putBoolean(LAUNCH_CAPTURED,true);
  arrow.getPersistentData().putBoolean(LAUNCH_MAGIC,!launcher.isEmpty()&&launcher.isEnchanted());
  arrow.getPersistentData().putBoolean(LAUNCH_FLAME,!launcher.isEmpty()&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,launcher)>0);
 }
 @SubscribeEvent public static void joining(EntityJoinLevelEvent event){
  if(event.getLevel().isClientSide||event.loadedFromDisk()||!(event.getEntity() instanceof AbstractArrow arrow)||arrow.getPersistentData().getBoolean(LAUNCH_CAPTURED)||!(arrow.getOwner() instanceof LivingEntity owner))return;
  var launcher=owner.getUseItem();
  if(!bow(launcher))launcher=owner.getMainHandItem();
  if(!bow(launcher))launcher=owner.getOffhandItem();
  captureLauncher(arrow,bow(launcher)?launcher:ItemStack.EMPTY);
 }
 private static boolean bow(ItemStack stack){return stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem;}
 private static boolean silver(ItemStack stack){return !stack.isEmpty()&&ClassicToolCatalogue.sourceItem(stack)&&ClassicToolCatalogue.material(stack).equals("silver");}
 private static ItemStack attackedWith(DamageSource source){
  // R195 explosion.player uses the responsible living entity as nc's immediate
  // entity. Modern TNT instead exposes the primed TNT as its direct entity.
  var direct=source.is(DamageTypeTags.IS_EXPLOSION)?source.getEntity():source.getDirectEntity();
  return direct instanceof LivingEntity living?living.getMainHandItem():ItemStack.EMPTY;
 }
 private static boolean silverAspect(DamageSource source){
  if(source.getDirectEntity() instanceof AbstractArrow arrow)return silver((ItemStack)ClassicArchery.pickup(new ItemStack(Items.ARROW),arrow));
  return silver(attackedWith(source));
 }
 private static boolean magicAspect(DamageSource source){
  if(source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC)||source.is(DamageTypes.THORNS))return true;
  if(source.getDirectEntity() instanceof ThrownTrident trident)return trident.isFoil();
  if(source.getDirectEntity() instanceof AbstractArrow arrow)return arrow.getPersistentData().getBoolean(LAUNCH_MAGIC);
  // nc explicitly excludes fireballs from the responsible entity's held item.
  if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile)return false;
  return attackedWith(source).isEnchanted();
 }
 private static boolean fireAspect(DamageSource source){
  return source instanceof SourceMobStrikeNative.StrikeDamage strike?strike.fireAspect:ClassicDefense.fire(source)||source.getDirectEntity() instanceof AbstractArrow arrow&&arrow.isOnFire()
   ||source.getDirectEntity() instanceof LivingEntity&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT,attackedWith(source))>0;
 }
 public static boolean immune(Mobs.Undead mob,DamageSource source){
  if(ClassicDefense.absolute(source))return false;
  return switch(Mobs.name(mob)){
   case "wight"->!fireAspect(source)&&!source.is(DamageTypes.LAVA)&&!silverAspect(source)&&!magicAspect(source);
   case "shadow"->!silverAspect(source)&&!magicAspect(source)&&!source.is(OriginalElementalDamage.SUNLIGHT);
   case "earth_elemental"->earthImmune(mob,source);
   case "clay_golem"->mob.originalEarth().hardened()&&earthImmune(mob,source);
   case "fire_elemental"->fireImmune(source);
   default->false;
  };
 }
 private static boolean fireImmune(DamageSource source){
  if(source.is(DamageTypes.DROWN)||source.is(OriginalElementalDamage.WATER)||source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Snowball)return false;
  if(source.getDirectEntity() instanceof AbstractArrow arrow)return !arrow.getPersistentData().getBoolean(LAUNCH_MAGIC)||arrow.getPersistentData().getBoolean(LAUNCH_FLAME);
  boolean hasFire=ClassicDefense.fire(source)||source.is(DamageTypes.LAVA)||source.getDirectEntity() instanceof LivingEntity&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT,attackedWith(source))>0;
  return !magicAspect(source)||hasFire;
 }
 private static boolean earthImmune(Mobs.Undead mob,DamageSource source){
  if(source.is(DamageTypes.FALL)||source.is(DamageTypeTags.IS_EXPLOSION))return false;
  if(source.getEntity() instanceof IronGolem&&source.getDirectEntity()==source.getEntity()&&!source.is(DamageTypeTags.IS_PROJECTILE))return false;
  var stack=attackedWith(source);var block=OriginalEarthElementalLife.blockForType(mob.originalEarth().type());
  // xj's effectiveness includes the block's harvest level; an enchanted or
  // silver sword is still not a pickaxe. Warhammers share the pickaxe family.
  return block==null||HarvestRules.toolKind(stack)!=LegacyHarvest.Tool.PICKAXE||ClassicMining.npcToolStrength(stack,block.defaultBlockState())<=0;
 }
 private SourceSpecificImmunities(){}
}

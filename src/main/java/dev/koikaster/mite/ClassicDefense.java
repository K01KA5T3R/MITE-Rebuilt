package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** wh/ud/of/Damage/abg source defense, with39D's explicit50percent minimum player armor protection. */
public final class ClassicDefense {
 public static final ResourceKey<DamageType> POISON=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("poison"));
 static final ResourceKey<DamageType> HAND_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("hand_damage"));
 private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
 public static String name(DamageSource source){return source.typeHolder().unwrapKey().map(k->k.location().getPath()).orElse("");}
 public static boolean absolute(DamageSource source){return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.is(DamageTypes.IN_WALL)||source.is(SourceMagmaWorld.ABSOLUTE)||source.is(CoreProgression.HEAT);}
 public static boolean unblockable(DamageSource source){return absolute(source)||source.is(DamageTypes.DROWN)||source.is(OriginalElementalDamage.WATER)||source.is(DamageTypes.STARVE)||source.is(POISON)||source.is(HAND_DAMAGE);}
 public static boolean fire(DamageSource source){return source.is(DamageTypeTags.IS_FIRE)&&!source.is(DamageTypes.LAVA);}
 public static boolean mundaneBypass(DamageSource source){return unblockable(source)||fire(source)||source.is(DamageTypes.LAVA)||source.is(DamageTypes.FALL)||source.is(DamageTypes.GENERIC)||source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC)||source.is(DamageTypes.WITHER)||source.is(DamageTypeTags.BYPASSES_ARMOR);}
 public static boolean scoped(LivingEntity actor){var key=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(actor.getType());return actor instanceof Player||key!=null&&(key.getNamespace().equals("minecraft")||key.getNamespace().equals(MiteReforged.ID));}
 public static boolean handles(Object actor){return scoped((LivingEntity)actor);}
 public static boolean nativeShield(Object actor){return actor instanceof Player;}
 public static float postHelmet(Object actor){return handles(actor)?1f:.75f;}
 public static float exhaustion(float original,Object rawSource){var source=(DamageSource)rawSource;var key=source.typeHolder().unwrapKey();if(key.isPresent()&&!Set.of("minecraft",MiteReforged.ID).contains(key.get().location().getNamespace()))return original;return mundaneBypass(source)?0f:.3f;}
 public static float snap(float value){float nearest=Math.round(value);return Math.abs(value-nearest)<=.0001f?nearest:value;}
 public static float armor(LivingEntity actor,DamageSource source,boolean enchantments){if(source!=null&&unblockable(source))return 0;float total=0;if(source==null||!mundaneBypass(source)){for(var stack:actor.getArmorSlots())if(WorldContentEvents.effectAllowed(actor,stack))total+=OriginalArmorItems.protection(stack,actor);if(actor instanceof Horse horse&&horse.getArmor().getItem() instanceof HorseArmorItem barding)total+=barding.getProtection();}
  if(enchantments&&source!=null&&!source.is(DamageTypes.FELL_OUT_OF_WORLD))for(var stack:actor.getArmorSlots())if(OriginalArmorItems.profile(stack)!=null&&WorldContentEvents.effectAllowed(actor,stack)){float wear=OriginalArmorItems.factor(stack,actor);int fall=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FALL_PROTECTION,stack);if(source.is(DamageTypes.FALL)&&fall>0)return 15f*(fall/4f)*wear;float multiplier=fire(source)?EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION,stack)/4f:0;if(source.is(DamageTypeTags.IS_EXPLOSION))multiplier+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION,stack)/4f;if(source.is(DamageTypeTags.IS_PROJECTILE))multiplier+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION,stack)/4f;total+=OriginalArmorItems.protection(stack,actor)*multiplier;}
  return snap(total);
 }
 public static float total(LivingEntity actor,DamageSource source){if(source!=null&&absolute(source))return 0;float natural=(float)(actor.getAttribute(Attributes.ARMOR)==null?0:actor.getAttribute(Attributes.ARMOR).getBaseValue());if(actor instanceof Mobs.Undead&&Mobs.name(actor).equals("earth_elemental")&&(source==null||!mundaneBypass(source)))natural+=4;if(actor instanceof Mobs.Undead clay&&Mobs.name(clay).equals("clay_golem")&&clay.originalEarth().hardened()&&(source==null||!mundaneBypass(source)))natural+=2;if(actor instanceof SourceWitch witch)natural+=witch.naturalDefense(source);if(actor instanceof SourceLongdead longdead)natural+=longdead.naturalDefense(source);if(actor instanceof SourceCreeper creeper)natural+=creeper.naturalDefense(source);if(actor instanceof SourceMagmaCube magma)natural+=magma.naturalDefense(source);float value=natural+armor(actor,source,true);if(actor.hasEffect(MobEffects.DAMAGE_RESISTANCE))value+=(actor.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()+1)*5f;if(source!=null&&fire(source)&&actor.hasEffect(MobEffects.FIRE_RESISTANCE))value+=(actor.getEffect(MobEffects.FIRE_RESISTANCE).getAmplifier()+1)*5f;return snap(value);}
 public static int display(Object rawActor){return net.minecraft.util.Mth.ceil(total((LivingEntity)rawActor,null));}
 public static boolean fireImmunity(Object rawActor,Object rawEffect,Object rawSource){var actor=(LivingEntity)rawActor;return actor.hasEffect((net.minecraft.world.effect.MobEffect)rawEffect)&&(!scoped(actor)||!((DamageSource)rawSource).is(DamageTypes.LAVA)||AdvancedFireResistance.protectsFromLava(actor));}
 public static DamageSource poison(Object rawActor){var actor=(LivingEntity)rawActor;return !scoped(actor)?actor.damageSources().magic():new DamageSource(actor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(POISON));}
 public static DamageSource effectMagic(Object rawActor,Object effect){return effect==MobEffects.POISON?poison(rawActor):((LivingEntity)rawActor).damageSources().magic();}
 public static net.minecraft.world.effect.MobEffectInstance incomingEffect(Object rawActor,Object rawEffect){var actor=(LivingEntity)rawActor;var effect=(net.minecraft.world.effect.MobEffectInstance)rawEffect;if(actor.level().isClientSide||!scoped(actor)||effect.getEffect()!=MobEffects.POISON||effect.isInfiniteDuration())return effect;float resistance=silverCoverage(actor)*.5f;if(resistance<=0)return effect;var tag=effect.save(new net.minecraft.nbt.CompoundTag());tag.putInt("Duration",(int)(effect.getDuration()*(1f-resistance)));return net.minecraft.world.effect.MobEffectInstance.load(tag);}
 public static float preHit(Object rawActor,Object rawSource,float amount){var actor=(LivingEntity)rawActor;var source=(DamageSource)rawSource;if(!scoped(actor)||amount<=0)return amount;if(source.is(DamageTypes.FALLING_ANVIL)||source.is(DamageTypes.FALLING_BLOCK)){var head=actor.getItemBySlot(EquipmentSlot.HEAD);if(!head.isEmpty()&&WorldContentEvents.effectAllowed(actor,head)){wearItem(actor,source,head,(int)(amount*4f+actor.getRandom().nextFloat()*amount*2f));amount*=.75f;}}return Math.max(amount,1f);}
 private static float blockDecay(ItemStack stack){var p=ClassicToolWear.profile(stack);if(p==null)return 0;var vampiric=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(Content.id("vampiric"));return LegacyToolWear.attackDamage(p.kind(),vampiric!=null&&EnchantmentHelper.getItemEnchantmentLevel(vampiric,stack)>0,EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS,stack)>0);}
 public static float absorb(Object rawActor,Object rawSource,float amount){var actor=(LivingEntity)rawActor;var source=(DamageSource)rawSource;if(amount<=0)return 0;if(absolute(source))return amount;
  if(actor instanceof Player player&&!mundaneBypass(source)&&player.isUsingItem()&&WorldContentEvents.effectAllowed(player,player.getUseItem())&&ClassicToolTraits.canBlock(player.getUseItem())){amount=Math.max(amount/2f,1f);var stack=player.getUseItem();int wear=(int)(amount*blockDecay(stack));if(wear>0){int beforeDamage=stack.getDamageValue(),beforeCount=stack.getCount();stack.hurtAndBreak(wear,player,p->p.broadcastBreakEvent(player.getUsedItemHand()));boolean destroyed=stack.getCount()<beforeCount;SourceParentHurtFrame.heldDamage(player,destroyed||stack.getDamageValue()>beforeDamage,destroyed);}}
  float protection=total(actor,source);damageArmor(actor,source,Math.min(armor(actor,source,false),amount));float piercing=0;if(source.getDirectEntity() instanceof LivingEntity attacker)piercing=EnchantmentHelper.getItemEnchantmentLevel(Content.PIERCING.get(),attacker.getMainHandItem())/5f*5f;float effective=Math.max(protection-piercing,0);
  if(actor instanceof Player&&effective>=amount){int delta=(int)(effective-amount);for(int i=-1;i<delta;i++)if(actor.getRandom().nextFloat()<.2f)return 0;}
  return Math.max(amount-effective,1f);
 }
 public static void damageArmor(LivingEntity actor,DamageSource source,float amount){if(!(actor instanceof Player player)||player.isCreative()||unblockable(source))return;int remaining=Math.max((int)amount,1);while(remaining>0&&hasDamageableWorn(actor)){int index=actor.getRandom().nextInt(4);var stack=actor.getItemBySlot(SLOTS[index]);if(usableArmor(actor,stack)){int count=0;for(var slot:SLOTS)if(usableArmor(actor,actor.getItemBySlot(slot)))count++;int portion=count==1?remaining:actor.getRandom().nextInt(remaining)+1;wearItem(actor,source,stack,portion);remaining-=portion;}else remaining-=actor.getRandom().nextInt(remaining)+1;}}
 private static boolean usableArmor(LivingEntity actor,ItemStack stack){
  return stack.getItem() instanceof ArmorItem&&WorldContentEvents.effectAllowed(actor,stack);
 }
 private static boolean hasDamageableWorn(LivingEntity actor){for(var stack:actor.getArmorSlots())if(stack.isDamageableItem()&&WorldContentEvents.effectAllowed(actor,stack))return true;return false;}
 public static void wearItem(LivingEntity owner,DamageSource source,ItemStack stack,int amount){if(!stack.isDamageableItem()||amount<1||owner instanceof Player player&&player.isCreative()||!WorldContentEvents.effectAllowed(owner,stack))return;var profile=OriginalArmorItems.profile(stack);String material=profile==null?"other":profile.material();boolean leather=material.equals("leather"),lava=source.is(DamageTypes.LAVA),pepsin=name(source).equals("pepsin"),acid=name(source).equals("acid");if(acid&&Set.of("gold","mithril","adamantium","diamond","netherite").contains(material)||fire(source)&&!leather||lava&&(material.equals("adamantium")||material.equals("netherite"))||pepsin&&!leather)return;boolean prevent=!acid&&!pepsin&&!lava&&!(fire(source)&&leather);if(lava&&!leather)amount*=10;int beforeDamage=stack.getDamageValue();boolean destroyed=false;boolean broken=stack.hurt(amount,owner.getRandom(),owner instanceof ServerPlayer p?p:null);if(prevent){if(stack.getDamageValue()>=stack.getMaxDamage())stack.setDamageValue(stack.getMaxDamage()-1);}else if(broken){destroyed=true;var identity=stack.getItem();stack.shrink(1);stack.setDamageValue(0);for(var slot:SLOTS)if(owner.getItemBySlot(slot)==stack){owner.broadcastBreakEvent(slot);break;}if(owner instanceof ServerPlayer player)player.awardStat(net.minecraft.stats.Stats.ITEM_BROKEN.get(identity));}if(!destroyed)warnLastArmorUse(owner,stack,beforeDamage);SourceParentHurtFrame.armorDamage(owner,destroyed||stack.getDamageValue()>beforeDamage,destroyed);}
 private static void warnLastArmorUse(LivingEntity owner,ItemStack stack,int previousDamage){
  if(!(owner instanceof ServerPlayer player)||!(stack.getItem() instanceof ArmorItem))return;
  int lastUse=stack.getMaxDamage()-1;
  if(previousDamage>=lastUse||stack.getDamageValue()!=lastUse)return;
  boolean worn=false;
  for(var slot:SLOTS)if(owner.getItemBySlot(slot)==stack){worn=true;break;}
  if(!worn)return;
  var tag=stack.getOrCreateTag();
  if(tag.getBoolean("MiteLastArmorUseWarned"))return;
  tag.putBoolean("MiteLastArmorUseWarned",true);
  player.playNotifySound(net.minecraft.sounds.SoundEvents.ANVIL_PLACE,net.minecraft.sounds.SoundSource.PLAYERS,.3f,1.4f);
 }
 public static float silverCoverage(LivingEntity owner){float result=0;for(var stack:owner.getArmorSlots()){var p=OriginalArmorItems.profile(stack);if(p!=null&&p.material().equals("silver"))result+=p.components()/24f*(p.chain()?.5f:1)*OriginalArmorItems.factor(stack,owner);}if(owner instanceof Horse horse&&horse.getArmor().getItem() instanceof OriginalArmorItems.HorseArmor armor&&armor.material.equals("silver"))result+=1;return snap(result);}
 private ClassicDefense(){}
}

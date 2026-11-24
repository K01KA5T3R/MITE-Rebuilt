package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

/** Human-confirmed R195 rx/oq hand-taming outcome. Modern raw fish/tempt and
 * breeding adapters remain; successful old CatType1..3 becomes an owned native
 * Cat rather than a trust-only Ocelot. Existing modern Cats are untouched. */
public final class SourceOcelotTaming {
 public static boolean tempting(Object raw){return raw!=null&&((TemptGoal)raw).isRunning();}
 public static void afterTick(Object raw){var host=(SourceOcelotTamingHost)raw;int cooldown=host.mite$tamingCooldown();if(cooldown>0)host.mite$tamingCooldown(cooldown-1);}
 public static InteractionResult interact(Object raw,Object rawPlayer,Object rawHand){
  var ocelot=(Ocelot)raw;var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;var host=(SourceOcelotTamingHost)raw;var stack=player.getItemInHand(hand);
  if(!host.mite$ocelotTempting()||!ocelot.isFood(stack)||player.distanceToSqr(ocelot)>=9)return host.mite$ocelotAnimalInteract(player,hand);
  if(ocelot.level().isClientSide)return InteractionResult.SUCCESS;
  if(host.mite$tamingCooldown()!=0)return InteractionResult.CONSUME;
  if(ocelot.getRandom().nextInt(3)!=0||ForgeEventFactory.onAnimalTame(ocelot,player)){
   sourceParticles(ocelot,false);ocelot.level().broadcastEntityEvent(ocelot,(byte)40);host.mite$tamingCooldown(400);consume(player,stack);return InteractionResult.CONSUME;
  }
  int oldVariant=1+ocelot.getRandom().nextInt(3);var cat=EntityType.CAT.create(ocelot.level());if(cat==null)return InteractionResult.FAIL;
  cat.moveTo(ocelot.getX(),ocelot.getY(),ocelot.getZ(),ocelot.getYRot(),ocelot.getXRot());cat.setDeltaMovement(ocelot.getDeltaMovement());cat.setYHeadRot(ocelot.getYHeadRot());cat.yBodyRot=ocelot.yBodyRot;
  cat.setAge(ocelot.getAge());cat.setNoAi(ocelot.isNoAi());cat.setSilent(ocelot.isSilent());cat.setInvulnerable(ocelot.isInvulnerable());cat.setCustomName(ocelot.getCustomName());cat.setCustomNameVisible(ocelot.isCustomNameVisible());cat.getPersistentData().merge(ocelot.getPersistentData());ClassicCats.markOriginal(cat,oldVariant);
  var variant=ClassicCats.variant(oldVariant);
  cat.setVariant(ocelot.level().registryAccess().registryOrThrow(Registries.CAT_VARIANT).getHolderOrThrow(variant).value());cat.setTame(true);cat.setOwnerUUID(player.getUUID());cat.setHealth(Math.min(ocelot.getHealth(),cat.getMaxHealth()));cat.setOrderedToSit(true);cat.setInSittingPose(true);cat.setPersistenceRequired();
  for(var effect:ocelot.getActiveEffects())cat.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
  // Forge may reject insertion. Until it succeeds the old actor and item remain.
  if(!ocelot.level().addFreshEntity(cat))return InteractionResult.FAIL;
  if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)net.minecraft.advancements.CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer,cat);
  var origin=(SourceSpawnOrigin)ocelot;((SourceSpawnOrigin)cat).mite$setOrigin(origin.mite$originX(),origin.mite$originY(),origin.mite$originZ());
  var leash=ocelot.getLeashHolder();if(leash!=null){ocelot.dropLeash(true,false);cat.setLeashedTo(leash,true);}var vehicle=ocelot.getVehicle();if(vehicle!=null){ocelot.stopRiding();cat.startRiding(vehicle,true);}
  sourceParticles(ocelot,true);ocelot.level().broadcastEntityEvent(ocelot,(byte)41);consume(player,stack);ocelot.discard();return InteractionResult.CONSUME;
 }
 private static void consume(Player player,ItemStack stack){if(!player.getAbilities().instabuild)stack.shrink(1);}
 /** oq.i consumes three Gaussian and three float draws per particle on both
  * source sides. Native server addParticle consumes the draws without sending
  * a duplicate cloud; the existing tracked Ocelot event routes client FX. */
 private static void sourceParticles(Ocelot ocelot,boolean success){var random=ocelot.getRandom();for(int i=0;i<7;i++){double dx=random.nextGaussian()*.02,dy=random.nextGaussian()*.02,dz=random.nextGaussian()*.02;ocelot.level().addParticle(success?ParticleTypes.HEART:ParticleTypes.SMOKE,ocelot.getX()+(double)(random.nextFloat()*ocelot.getBbWidth()*2f)-(double)ocelot.getBbWidth(),ocelot.getY()+.5+(double)(random.nextFloat()*ocelot.getBbHeight()),ocelot.getZ()+(double)(random.nextFloat()*ocelot.getBbWidth()*2f)-(double)ocelot.getBbWidth(),dx,dy,dz);}}
 private SourceOcelotTaming(){}
}

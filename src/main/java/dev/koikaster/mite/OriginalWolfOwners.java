package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Actual per-Wolf bridge owner. Hellhound keeps its independently frozen0.29 constructor and lifecycle. */
public final class OriginalWolfOwners {
 public static final class Owner {
  public final Wolf wolf;public final OriginalWolfFood food;public final OriginalWolfFleeState flee;public final OriginalWolfLife life;
  public boolean installed,attacking;private LivingEntity threat;private int threatTicks;
  private Owner(Wolf wolf){this.wolf=wolf;food=new OriginalWolfFood(wolf,Network::wolfHealFx);flee=new OriginalWolfFleeState(wolf);life=new OriginalWolfLife(wolf);}
  private void attacking(boolean value){if(value==attacking)return;attacking=value;if(value)wolf.playSound(SoundEvents.WOLF_GROWL,soundVolume(wolf),wolf.getVoicePitch());}
  private void threats(){if(wolf.isTame()&&(wolf.isInSittingPose()||wolf.getTarget()==null)&&(wolf.tickCount+wolf.getId()*47)%10==0&&Math.random()<.25){var player=OriginalDespawn.closest(wolf,-1,false);if(player!=null&&wolf.distanceTo(player)<=16)for(var candidate:wolf.level().getEntitiesOfClass(Mob.class,wolf.getBoundingBox().inflate(16,8,16)))if(candidate!=wolf&&isThreat(wolf,candidate)){wolf.playSound(wolf.getRandom().nextInt(8)==0?SoundEvents.WOLF_GROWL:SoundEvents.WOLF_AMBIENT,soundVolume(wolf),wolf.getVoicePitch());threat=candidate;threatTicks=(wolf.getRandom().nextInt(10)+5)*10;break;}}if(threatTicks>0)--threatTicks;if(threatTicks==0||threat!=null&&!threat.isAlive()||wolf.getTarget()!=null)threat=null;if(threat!=null)wolf.getLookControl().setLookAt(threat,10,wolf.getMaxHeadXRot());}
 }
 public static boolean ordinary(Wolf wolf){return wolf.getType()==EntityType.WOLF;}
 public static boolean handles(Wolf wolf){return ordinary(wolf)||wolf.getType()==Mobs.WOLVES.get("dire_wolf").get();}
 public static Owner owner(Wolf wolf){var bridge=(SourceWolfOwner)wolf;var state=(Owner)bridge.mite$wolfOwner();if(state==null){state=new Owner(wolf);bridge.mite$wolfOwner(state);}return state;}
 public static boolean install(Object raw){if(!(raw instanceof Wolf wolf)||!handles(wolf))return false;var existing=(Owner)((SourceWolfOwner)wolf).mite$wolfOwner();if(existing!=null&&existing.installed)return true;
  // Forge's attribute-add event does not replace an existing supplier value. Mob has already created
  // this instance's AttributeMap before virtual registerGoals, so establish sf before any owner/goal.
  if(ordinary(wolf)){wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8);wolf.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3);wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.4f);wolf.setHealth(Math.min(wolf.getHealth(),8));}
  var state=owner(wolf);
  var leaves=new OriginalWolfParentGoals.Leaves(()->new OriginalWolfAvoidFireGoal(wolf),()->OriginalHellhoundProfile.swimGoal(wolf),()->new OriginalWolfSitGoal(wolf),()->OriginalHellhoundProfile.leapGoal(wolf),()->new BreedGoal(wolf,1),()->new OriginalWolfBegGoal(wolf),()->OriginalHellhoundProfile.watchGoal(wolf),()->OriginalHellhoundProfile.idleGoal(wolf),()->new OriginalWolfPanicGoal(wolf,state.flee),()->playerGoal(wolf),()->OriginalWolfPreyProfile.zombieFamilyNearest(wolf),()->OriginalWolfPreyProfile.ghoulFamilyNearest(wolf));
  OriginalWolfParentGoals.installIntoEmptySelectors(wolf,state.food,leaves,OriginalWolfOwners::callToOwner);state.installed=true;return true;
 }
 public static void afterAi(Object raw){if(raw instanceof Wolf wolf&&handles(wolf)&&!wolf.level().isClientSide)owner(wolf).food.tick();}
 /** Native persistent-anger ticking is replaced only for ordinary sf wolves; Dire's confirmed owner is retained. */
 public static void nativeAnger(Object raw,Object level,boolean players){if(raw instanceof Wolf wolf&&!ordinary(wolf))wolf.updatePersistentAnger((net.minecraft.server.level.ServerLevel)level,players);}
 public static void tick(Object raw){if(!(raw instanceof Wolf wolf)||!handles(wolf)||wolf.level().isClientSide)return;var state=owner(wolf);state.flee.tick();if(ordinary(wolf))state.life.tick();state.attacking(wolf.getTarget()!=null);state.threats();}
 public static boolean hostile(Wolf wolf){return ordinary(wolf)?owner(wolf).life.hostile:((Mobs.DireWolf)wolf).originalWolf().hostile;}
 private static boolean witch(Wolf wolf){return ordinary(wolf)?owner(wolf).life.witchAlly:((Mobs.DireWolf)wolf).originalWolf().witchAlly;}
 private static Goal playerGoal(Wolf wolf){Goal selected=ordinary(wolf)?new OriginalNearestTargetGoal(wolf,Player.class,0,true):((Mobs.DireWolf)wolf).originalWolf().playerGoal();return new Goal(){ {setFlags(EnumSet.of(Flag.TARGET));}public boolean canUse(){return (!ordinary(wolf)||hostile(wolf)||witch(wolf))&&selected.canUse();}public boolean canContinueToUse(){return selected.canContinueToUse();}public void start(){owner(wolf).attacking(true);selected.start();}public void stop(){selected.stop();}public void tick(){selected.tick();}public boolean requiresUpdateEveryTick(){return true;}};}
 public static boolean permitTarget(Object raw,Object target){if(raw instanceof Wolf wolf&&handles(wolf)&&target!=null){if(target==wolf.getOwner())return false;if(wolf.isBaby()){wolf.setTarget(null);return false;}}return true;}
 public static void target(Object raw){if(raw instanceof Wolf wolf&&ordinary(wolf)&&wolf.getTarget()!=null)owner(wolf).life.target();}
 public static boolean hitResult(boolean affected,Object raw,Object target){if(affected&&raw instanceof Wolf wolf&&ordinary(wolf)){if(target instanceof Entity entity&&entity.isAlive())owner(wolf).life.target();else wolf.setTarget(null);}return affected;}
 public static boolean hurtResult(boolean affected,Object raw,Object source){if(!affected||!(raw instanceof Wolf wolf)||!handles(wolf)||wolf.level().isClientSide||!(source instanceof DamageSource damage)||!(damage.getEntity() instanceof LivingEntity attacker))return affected;var state=owner(wolf);state.flee.harming(attacker);state.flee.consider();if(!wolf.isTame())for(var peer:wolf.level().getEntitiesOfClass(Wolf.class,wolf.getBoundingBox().inflate(8,4,8)))if(peer.getType()==wolf.getType()&&!peer.isTame()&&handles(peer)){var memory=owner(peer).flee;if(memory.harming()!=null)continue;memory.harming(attacker);if(memory.consider()){if(peer.isInLove())peer.resetLove();}else memory.harming(null);}return affected;}
 public static void onFleeing(Wolf wolf){if(ordinary(wolf))owner(wolf).life.hostile=false;else ((Mobs.DireWolf)wolf).originalWolf().hostile=false;wolf.setTarget(null);}
 public static boolean tame(Object raw,boolean tame){if(!(raw instanceof Wolf wolf)||!ordinary(wolf))return false;float max=wolf.getMaxHealth(),health=wolf.getHealth();((SourceWolfOwner)wolf).mite$wolfTameFlag(tame);wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tame?12:8);wolf.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(tame?32:16);wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3);wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.4f);wolf.setHealth(Math.min(wolf.getMaxHealth(),health+Math.max(0,wolf.getMaxHealth()-max)));wolf.refreshDimensions();return true;}
 public static InteractionResult bone(Object raw,Object player,Object hand){return raw instanceof Wolf wolf&&ordinary(wolf)?owner(wolf).life.bone((Player)player,(InteractionHand)hand):null;}
 public static void save(Object raw,Object tag){if(raw instanceof Wolf wolf&&handles(wolf)){var state=owner(wolf);var nbt=(CompoundTag)tag;nbt.putBoolean("Sitting",wolf.isInSittingPose());state.flee.save(nbt);if(ordinary(wolf))state.life.save(nbt);}}
 public static void load(Object raw,Object tag){if(raw instanceof Wolf wolf&&handles(wolf)){var state=owner(wolf);state.flee.load((CompoundTag)tag);if(ordinary(wolf))state.life.load((CompoundTag)tag);}}
 public static SoundEvent ambient(Object raw){if(!(raw instanceof Wolf wolf)||!ordinary(wolf))return null;var state=owner(wolf);if(state.attacking&&!wolf.isTame()||state.life.hostile)return SoundEvents.WOLF_GROWL;if(state.flee.fleeing)return SoundEvents.WOLF_WHINE;return wolf.isTame()&&wolf.getHealth()/wolf.getMaxHealth()<.5f?SoundEvents.WOLF_WHINE:SoundEvents.WOLF_PANT;}
 public static float soundVolume(Wolf wolf){return (wolf.isBaby()?.2f:.4f)*(ordinary(wolf)?1:1.5f);}
 public static float volume(float original,Object raw){return raw instanceof Wolf wolf&&ordinary(wolf)?soundVolume(wolf):original;}
 public static EntityDimensions dimensions(Object raw){return raw instanceof Wolf wolf&&ordinary(wolf)?EntityDimensions.scalable(.70000005f,.8f).scale(wolf.isBaby()?.5f:1):null;}
 public static void callToOwner(Wolf wolf){wolf.playSound(SoundEvents.WOLF_AMBIENT,soundVolume(wolf),wolf.getVoicePitch());}
 private static boolean isThreat(Wolf wolf,Mob entity){if(!entity.isAlive()||!(entity instanceof Enemy)&&!Mobs.name(entity).equals("hellhound"))return false;if(entity instanceof Wolf other&&java.util.Objects.equals(other.getOwnerUUID(),wolf.getOwnerUUID()))return false;var target=entity.getTarget();return (target==wolf||target==wolf.getOwner())&&wolf.getSensing().hasLineOfSight(entity);}
 private OriginalWolfOwners(){}
}

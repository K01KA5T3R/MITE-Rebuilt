package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Native adapters for the sealed sm combat rules. Native movement, pose,
 * synchronized head IDs and boss packets are the human73A transport. */
public final class SourceWitherBrain implements SourceWitherRules.Host,OriginalParentHarmState.Flags {
 final SourceWitherBoss actor;public final SourceWitherRules.State state=new SourceWitherRules.State();
 public final OriginalParentHarmState harm;private boolean decided,fleeing;
 SourceWitherBrain(SourceWitherBoss actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);}
 SourceWitherNativeParent parent(){return (SourceWitherNativeParent)(Object)actor;}
 public void step(){
  var ritual=actor.ritual();
  if(ritual!=null&&actor.getInvulnerableTicks()<=0&&actor.level() instanceof ServerLevel level){
   parentAi();
   ritual.combat.step(level,this,ritual.combatants(level, actor));
   SourceWitherRules.maintain(this,ritual.combat.canRegenerate(actor.tickCount));
  }else SourceWitherRules.step(this);
  parent().mite$witherBossEvent().setProgress(actor.getHealth()/actor.getMaxHealth());
 }
 public boolean hurt(DamageSource source,float amount){
  return hurt(source,amount,0);
 }
 boolean hurt(DamageSource source,float amount,int part){
  var ritual=actor.ritual();
  if(part!=0&&!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer))return false;
  float applied=ritual==null?amount:ritual.combat.protectedDamage(actor,source,amount,part);
  float before=actor.getHealth();
  Boolean accepted=SourceWitherRules.hurt(this,new SourceWitherRules.Damage<Boolean>(){
   public boolean arrow(){return source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow;}
   public boolean armored(){return ritual==null?actor.isPowered():part==0&&ritual.combat.deflectArrow(actor,source);}public boolean responsibleLiving(){return source.getEntity() instanceof LivingEntity;}public boolean responsiblePlayer(){return source.getEntity() instanceof Player;}public boolean sameCreatureType(){return source.getEntity() instanceof LivingEntity living&&living.getMobType()==actor.getMobType();}
   public Boolean parent(){
    // isHarmedByFire/isHarmedByLava belong to the source parent, after sm's
    // agitation mutation. They must not become a modern Wither tag gate.
    if(source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))return false;
    try(var frame=SourceParentHurtFrame.begin(actor,outcome->harm.afterDamage(source,outcome.negativelyAffected()))){boolean value=SourceWitherBrain.this.parent().mite$witherMonsterHurt(source,applied);var result=frame.finish();if(!result.destroyed()&&result.negativelyAffected()&&source.getEntity() instanceof LivingEntity responsible&&responsible!=actor&&responsible!=actor.getVehicle()&&responsible!=actor.getFirstPassenger())actor.setTarget(responsible);return value;}
   }
  });
  if(Boolean.TRUE.equals(accepted)&&ritual!=null&&actor.isAlive()&&actor.getHealth()<before){
   if(part!=0)ritual.combat.damageHead(actor,part,before-actor.getHealth());
   ritual.combat.onDamage(actor,source);
  }
  return Boolean.TRUE.equals(accepted);
 }
 private record Node(Entity value) implements SourceWitherRules.Target {
  public int id(){return value.getId();}public boolean alive(){return value.isAlive();}public boolean player(){return value instanceof Player;}public boolean creative(){return value instanceof Player player&&player.getAbilities().invulnerable;}
 }
 private Entity entity(SourceWitherRules.Target node){return ((Node)node).value;}
 public SourceWitherRules.State state(){return state;}public int invulnerable(){return actor.getInvulnerableTicks();}public void invulnerable(int value){actor.setInvulnerableTicks(value);}public int ticks(){return actor.tickCount;}public int difficulty(){return actor.level().getDifficulty().getId();}public double x(){return actor.getX();}public double y(){return actor.getY();}public double z(){return actor.getZ();}public int nextInt(int bound){return actor.getRandom().nextInt(bound);}public double nextDouble(){return actor.getRandom().nextDouble();}
 public void parentAi(){parent().mite$witherMonsterAi();}public void heal(float amount){actor.heal(amount);}public void health(float value){actor.setHealth(value);}public float maximumHealth(){return actor.getMaxHealth();}
 public SourceWitherRules.Target entity(int id){var value=actor.level().getEntity(id);return value==null?null:new Node(value);}public int sideTarget(int head){return actor.getAlternativeTarget(head);}public void sideTarget(int head,int id){actor.setAlternativeTarget(head,id);}public SourceWitherRules.Target mainTarget(){return actor.getTarget()==null?null:new Node(actor.getTarget());}
 public List<? extends SourceWitherRules.Target> candidates(){return actor.level().getEntitiesOfClass(LivingEntity.class,actor.getBoundingBox().inflate(20,8,20),living->living.getMobType()!=MobType.UNDEAD).stream().map(Node::new).toList();}
 public boolean self(SourceWitherRules.Target target){return entity(target)==actor;}public double distanceSquared(SourceWitherRules.Target target){return actor.distanceToSqr(entity(target));}public boolean canSee(SourceWitherRules.Target target){return actor.hasLineOfSight(entity(target));}
 public void shoot(int head,SourceWitherRules.Target target){shoot(head,(LivingEntity)entity(target));}
 void shoot(int head,LivingEntity target){shoot(head,target.getX(),target.getY()+(double)target.getEyeHeight()*.5,target.getZ(),head==0&&actor.getRandom().nextFloat()<.001f);}
 private double headX(int head){if(head<=0)return x();float yaw=(actor.yBodyRot+(float)(180*(head-1)))/180f*(float)Math.PI;return x()+(double)Mth.cos(yaw)*1.3;}
 private double headY(int head){return head<=0?y()+3:y()+2.2;}
 private double headZ(int head){if(head<=0)return z();float yaw=(actor.yBodyRot+(float)(180*(head-1)))/180f*(float)Math.PI;return z()+(double)Mth.sin(yaw)*1.3;}
 Vec3 headPosition(int head){return new Vec3(headX(head),headY(head),headZ(head));}
 void faceRitualTarget(Vec3 target,float turnLimit){
  Vec3 delta=target.subtract(actor.position());
  if(delta.horizontalDistanceSqr()<.001)return;
  float yaw=Mth.approachDegrees(actor.yBodyRot,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),turnLimit);
  actor.setYRot(yaw);actor.setYHeadRot(yaw);actor.yBodyRot=yaw;
 }
 public void shoot(int head,double x,double y,double z,boolean charged){
  var skull=skull(head,new Vec3(x,y,z));if(charged)skull.setDangerous(true);actor.level().addFreshEntity(skull);
 }
 SourceWitherSkull shootMarked(Vec3 landing,float angle){
  faceRitualShot(landing);
  var skull=skull(0,landing);
  skull.markLanding(landing,angle);
  skull.markRitualShot();
  return actor.level().addFreshEntity(skull)?skull:null;
 }
 SourceWitherSkull shootRitual(int head,Vec3 target){
  faceRitualShot(target);
  var skull=skull(head,target);
  skull.aimRitualShot(target);
  return actor.level().addFreshEntity(skull)?skull:null;
 }
 private void faceRitualShot(Vec3 shot){
  var target=actor.getTarget();
  if(target==null){faceRitualTarget(shot,180);return;}
  Vec3 victim=target.getBoundingBox().getCenter();
  Vec3 towardVictim=BossAttackGeometry.flatDirection(victim.subtract(actor.position()));
  Vec3 towardShot=BossAttackGeometry.flatDirection(shot.subtract(actor.position()));
  faceRitualTarget(towardShot.dot(towardVictim)>.35?victim:shot,180);
 }
 private SourceWitherSkull skull(int head,Vec3 target){
  actor.level().levelEvent(null,1024,eventPos(),0);
  double px=headX(head),py=headY(head),pz=headZ(head);
  var skull=new SourceWitherSkull(actor.level(),actor,target.x-px,target.y-py,target.z-pz);
  skull.setPos(px,py,pz);
  return skull;
 }
 private BlockPos eventPos(){return new BlockPos((int)x(),(int)y(),(int)z());}
 public boolean griefing(){return actor.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);}
 public boolean breakable(int x,int y,int z){var pos=new BlockPos(x,y,z);var state=actor.level().getBlockState(pos);return !state.isAir()&&!state.is(Blocks.BEDROCK)&&!state.is(Blocks.END_PORTAL)&&!state.is(Blocks.END_PORTAL_FRAME)&&!(state.getBlock() instanceof SourceMantleBlock)&&!Grave.protectedAt(actor.level(),pos);}
 public boolean destroy(int x,int y,int z){var pos=new BlockPos(x,y,z);return !Grave.protectedAt(actor.level(),pos)&&actor.level().destroyBlock(pos,true,actor);}
 public void spawnExplosion(){if(actor.level() instanceof ServerLevel level)SourceExplosion.create(level,actor,x(),y()+(double)actor.getEyeHeight(),z(),WitherRitual.BLAST_POWER,WitherRitual.BLAST_POWER,false,griefing());}
 public void spawnEvent(){if(actor.level() instanceof ServerLevel level)level.globalLevelEvent(1023,eventPos(),0);}public void breakEvent(){actor.level().levelEvent(null,1022,eventPos(),0);}
 public boolean hasDecidedToFlee(){return decided;}public void hasDecidedToFlee(boolean value){decided=value;}public boolean fleeing(){return fleeing;}public void fleeing(boolean value){fleeing=value;}
}

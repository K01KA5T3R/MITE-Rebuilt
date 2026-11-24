package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Real entity/world adapters for the frozen070 source decisions. */
public final class SourceDragonBrain implements SourceDragonRules.Host,OriginalParentHarmState.Flags {
 public final SourceDragonRules.State state=new SourceDragonRules.State();
 public final OriginalParentHarmState harm;final SourceEnderDragon actor;private boolean decided,fleeing;
 public SourceDragonBrain(SourceEnderDragon actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);}
 public record Node(Entity value) implements SourceDragonRules.Target {public double x(){return value.getX();}public double z(){return value.getZ();}}
 private record Crystal(EndCrystal value) implements SourceDragonRules.Crystal {public boolean removed(){return value.isRemoved();}}
 private record Victim(Entity value,SourceEnderDragon actor) implements SourceDragonRules.Victim {
  public boolean living(){return value instanceof LivingEntity;}public double x(){return value.getX();}public double z(){return value.getZ();}public void push(double x,double y,double z){value.push(x,y,z);}public void damage(float amount){value.hurt(actor.damageSources().mobAttack(actor),amount);}
 }
 public boolean hurt(EnderDragonPart part,DamageSource source,float amount){return hurt(part,source,amount,1f);}
 boolean hurt(EnderDragonPart part,DamageSource source,float amount,float damageScale){
  final float[] value={amount};return Boolean.TRUE.equals(SourceDragonRules.hurtPart(this,part==actor.sourceParts()[0]||part==actor.head,new SourceDragonRules.Damage<Boolean>(){
   public float amount(){return value[0];}public void scale(float factor,float floor){value[0]*=factor;if(value[0]<floor)value[0]=floor;}
   public boolean playerResponsible(){return source.getEntity() instanceof Player;}public boolean explosion(){return source.is(DamageTypeTags.IS_EXPLOSION);}
   public Boolean parent(){if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypes.LAVA))return false;try(var frame=SourceParentHurtFrame.begin(actor,result->harm.afterDamage(source,result.negativelyAffected()))){boolean accepted=actor.sourceParentHurt(source,value[0]*damageScale);frame.finish();return accepted;}}
  }));
 }
 public void knock(AABB box){var body=actor.sourceParts()[1].getBoundingBox();SourceDragonRules.knockBack((body.minX+body.maxX)/2,(body.minZ+body.maxZ)/2,victims(box));}
 public void bite(AABB box){SourceDragonRules.bite(victims(box));}
 private List<Victim> victims(AABB box){return actor.level().getEntities(actor,box).stream().map(e->new Victim(e,actor)).toList();}
 public boolean walls(AABB b){return SourceDragonRules.walls(this,new SourceDragonRules.Box(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));}
 @Override public SourceDragonRules.State state(){return state;}@Override public boolean client(){return actor.level().isClientSide;}@Override public double x(){return actor.getX();}@Override public double y(){return actor.getY();}@Override public double z(){return actor.getZ();}@Override public float yaw(){return actor.getYRot();}@Override public void yaw(float value){actor.setYRot(value);actor.yBodyRot=value;}
 @Override public int ticks(){return actor.tickCount;}@Override public int nextInt(int bound){return actor.getRandom().nextInt(bound);}@Override public float nextFloat(){return actor.getRandom().nextFloat();}@Override public float health(){return actor.getHealth();}@Override public float maximumHealth(){return actor.getMaxHealth();}@Override public void health(float value){actor.setHealth(value);}
 @Override public SourceDragonRules.Crystal crystal(){return actor.nearestCrystal==null?null:new Crystal(actor.nearestCrystal);}@Override public void crystal(SourceDragonRules.Crystal value){actor.nearestCrystal=value==null?null:((Crystal)value).value;}
 @Override public List<? extends SourceDragonRules.Crystal> crystals() {
  var nearby = actor.level().getEntitiesOfClass(EndCrystal.class, actor.getBoundingBox().inflate(32));
  if (actor.level() instanceof ServerLevel level && DragonRitual.owns(level, actor)) {
   return nearby.stream().filter(crystal -> DragonRitualCrystals.isTowerCrystal(level, crystal)).map(Crystal::new).toList();
  }
  return nearby.stream().map(Crystal::new).toList();
 }
 @Override public double distanceSquared(SourceDragonRules.Crystal value){return ((Crystal)value).value.distanceToSqr(actor);}
 @Override public void linkedCrystalDamage(float amount){actor.hurt(actor.sourceParts()[0],actor.damageSources().explosion(null,null),amount);}
 @Override public List<? extends SourceDragonRules.Target> players(){return actor.level().players().stream().map(Node::new).toList();}
 @Override public int block(int x,int y,int z){var pos=new BlockPos(x,y,z);var b=actor.level().getBlockState(pos);return b.isAir()?0:b.is(Blocks.OBSIDIAN)?49:b.is(Blocks.END_STONE)?121:b.is(Blocks.BEDROCK)||Grave.protectedAt(actor.level(),pos)?7:1;}
 @Override public boolean griefing(){return actor.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);}@Override public boolean remove(int x,int y,int z){var pos=new BlockPos(x,y,z);return !Grave.protectedAt(actor.level(),pos)&&actor.level().removeBlock(pos,false);}
 @Override public void particle(boolean huge,double x,double y,double z){actor.level().addParticle(huge?ParticleTypes.EXPLOSION_EMITTER:ParticleTypes.EXPLOSION,x,y,z,0,0,0);}
 @Override public void deathEvent(int x,int y,int z){if(actor.level() instanceof ServerLevel world)world.globalLevelEvent(1028,new BlockPos(x,y,z),0);}
 @Override public void move(double x,double y,double z){actor.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(x,y,z));}
 @Override public int orbValue(int total){return ExperienceOrb.getExperienceValue(total);}
 @Override public void orb(int value){if(actor.level() instanceof ServerLevel world&&!DragonRitual.owns(world,actor))world.addFreshEntity(new SourceXpOrb860(world,x(),y(),z(),value));}
 @Override public void portal(int x,int z){if(actor.level() instanceof ServerLevel world){if(DragonRitual.openVictoryPortal(world,actor))return;try(var blocks=new SourceDragonPortalWorld(world)){SourceDragonPortal.create(blocks,x,z);}var fight=actor.getDragonFight();if(fight!=null)((SourceDragonFightAccess)(Object)fight).mite$dragonPortal(new BlockPos(x,64,z));}}
 @Override public void discard(){var fight=actor.getDragonFight();if(fight!=null)fight.setDragonKilled(actor);if(actor.level() instanceof ServerLevel world)DragonRitual.completed(world,actor);actor.remove(Entity.RemovalReason.KILLED);actor.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);}
 public boolean hasDecidedToFlee(){return decided;}public void hasDecidedToFlee(boolean value){decided=value;}public boolean fleeing(){return fleeing;}public void fleeing(boolean value){fleeing=value;}
}

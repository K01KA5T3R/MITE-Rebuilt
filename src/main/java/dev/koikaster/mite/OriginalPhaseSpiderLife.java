package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct EntityPhaseSpider leaf control flow.
 * The required bindings expose source AI, block flags and FX transport instead
 * of inventing replacements for the unported parent owners.
 */
public final class OriginalPhaseSpiderLife {
 /** Actual WoodSpider owner exposes the same state used by hurt/aiStep/NBT. */
 public interface Owner {OriginalPhaseSpiderLife originalPhaseSpider();}
 public interface SourceDamage {
  boolean isFallDamage();boolean isFireDamage();boolean isPoison();
  Entity immediateEntity();Entity responsibleEntity();
 }
 public interface WorldAccess {
  boolean blockExists(int x,int y,int z);
  boolean solid(int x,int y,int z);boolean liquid(int x,int y,int z);
  boolean collisionFree(AABB movedBox);boolean liquidFree(AABB movedBox);
 }
 public interface Host {
  /** R195 on.bp availability; do not substitute target existence for a path. */
  boolean hasPath();Path path();boolean fleeing();
  float maximumTargetingRange();Player findPlayerToAttack(float range);
  /** Source blockFX runegate trail: destination cell, short count, old block position. */
  void teleportTrail(BlockPos destination,short particleCount,BlockPos approximateOrigin);
  void portalSoundAt(Vec3 previousPosition);
  /** Must use the entity tracker's coherent position-update path, not a bare
   * teleport packet that leaves its relative-position codec on the old base. */
  void requestImmediatePositionUpdate();
 }

 private final PathfinderMob mob;private final Host host;private final WorldAccess world;
 public int maxNumEvasions,numEvasions;

 public OriginalPhaseSpiderLife(PathfinderMob mob){this(mob,new SourcePhaseNativeHost(mob),new SourcePhaseNativeWorld(mob));}
 public OriginalPhaseSpiderLife(PathfinderMob mob,Host host,WorldAccess world){
  this.mob=Objects.requireNonNull(mob);this.host=Objects.requireNonNull(host);this.world=Objects.requireNonNull(world);
  if(!mob.level().isClientSide)maxNumEvasions=numEvasions=mob.getRandom().nextInt(3)+2;
 }
 public void save(CompoundTag tag){tag.putByte("max_num_evasions",(byte)maxNumEvasions);tag.putByte("num_evasions",(byte)numEvasions);}
 public void load(CompoundTag tag){maxNumEvasions=tag.getByte("max_num_evasions");numEvasions=tag.getByte("num_evasions");}
 private boolean dead(){return mob.isRemoved()||mob.getHealth()<=0;}
 private int offsetTicks(){return mob.tickCount+mob.getId()*47;}

 /** Call after the complete parent aiStep, once per actor update. Active path
  * blinking has its own bounded node step and never spends an evasion charge.
  */
 public void afterAi(){
  if(mob.level().isClientSide||mob.getHealth()<=0)return;
  int ticks=offsetTicks();
  if(numEvasions<maxNumEvasions&&ticks%100==0)++numEvasions;
  if(!host.hasPath()||mob.getTarget()==null&&!host.fleeing()||ticks%10!=0||mob.getRandom().nextInt(3)!=0)return;
  Path path=Objects.requireNonNull(host.path(),"Source hasPath/path binding must be coherent");
  if(path.isDone())return;int remaining=path.getNodeCount()-path.getNextNodeIndex();if(remaining<=1)return;
  int advancement=Mth.clamp(mob.getRandom().nextInt(remaining),1,4);
  var point=path.getNode(path.getNextNodeIndex()+advancement);
  double x=point.x+.5,y=point.y,z=point.z+.5;
  double dx=mob.getX()-x,dy=mob.getY()-y,dz=mob.getZ()-z;
  // ale.distanceSqTo returns float after summing doubles; the threshold is
  // squared distance >3, distinct from the separate three-block threat radius.
  if((double)(float)(dx*dx+dy*dy+dz*dz)>3.0&&tryTeleportTo(x,y,z))
   path.setNextNodeIndex(path.getNextNodeIndex()+advancement-1);
 }

 /** True cancels this incoming damage before the parent hurt implementation.
  * No material or magic immunity is introduced;57D's cancellation is retained.
  */
 public boolean beforeHurt(SourceDamage damage){
  Objects.requireNonNull(damage);if(mob.level().isClientSide)return false;
  boolean canEvade=!(damage.isFallDamage()||damage.isFireDamage()||damage.isPoison());
  if(canEvade&&numEvasions>0){
   --numEvasions; // failed searches consume the charge too, before any candidate RNG.
   Entity threat=damage.immediateEntity();if(threat==null)threat=damage.responsibleEntity();
   if(tryTeleportAwayFrom(threat,3.0))return true;
  }
  return false;
 }

 public boolean tryTeleportTo(double posX,double posY,double posZ){
  if(mob.level().isClientSide||dead())return false;
  int x=Mth.floor(posX),y=Mth.floor(posY),z=Mth.floor(posZ);
  if(y<1||!world.blockExists(x,y,z))return false;
  while(!world.solid(x,--y,z)){
   if(y<1)return false;posY-=1.0;
  }
  if(world.solid(x,++y,z)||world.liquid(x,y,z))return false;
  double dx=posX-mob.getX(),dy=posY-mob.getY(),dz=posZ-mob.getZ();
  AABB moved=mob.getBoundingBox().move(dx,dy,dz);
  if(!world.collisionFree(moved)||!world.liquidFree(moved))return false;
  // abw.getDistanceFromDeltas returns float, promoted back to double before16x.
  double distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
  Vec3 previous=mob.position();
  host.teleportTrail(new BlockPos(x,y,z),(short)(int)(16.0*distance),BlockPos.containing(previous));
  host.portalSoundAt(previous);
  mob.setPos(posX,posY,posZ);
  host.requestImmediatePositionUpdate();
  return true;
 }

 public boolean tryTeleportAwayFrom(Entity threat,double minimumDistance){
  if(mob.level().isClientSide||dead())return false;
  double minimumSquared=minimumDistance*minimumDistance;
  int x=Mth.floor(mob.getX()),y=Mth.floor(mob.getY()+(double)1.0E-4f),z=Mth.floor(mob.getZ());
  double threatX=threat==null?mob.getX():threat.getX(),threatZ=threat==null?mob.getZ():threat.getZ();
  for(int attempt=0;attempt<64;attempt++){
   int dx=mob.getRandom().nextInt(11)-5,dy=mob.getRandom().nextInt(9)-4,dz=mob.getRandom().nextInt(11)-5;
   if(Math.abs(dx)<3&&Math.abs(dz)<3)continue;
   int tryX=x+dx,tryY=y+dy,tryZ=z+dz;double posX=tryX+.5,posZ=tryZ+.5;
   double distanceX=posX-threatX,distanceZ=posZ-threatZ;
   if(distanceX*distanceX+distanceZ*distanceZ<minimumSquared||tryY<1||!world.blockExists(tryX,tryY,tryZ))continue;
   // Keep this pre-scan distinct from tryTeleportTo. In particular, this source
   // branch rejects tryY<1 after decrement even if a solid block was found at0.
   while(!world.solid(tryX,--tryY,tryZ)&&tryY>=1){}
   if(tryY<1||world.solid(tryX,++tryY,tryZ)||world.liquid(tryX,tryY,tryZ)||!tryTeleportTo(posX,tryY,posZ))continue;
   Player target=host.findPlayerToAttack(Math.min(host.maximumTargetingRange(),24.0f));
   if(target!=null&&target!=mob.getTarget())mob.setTarget(target);
   return true;
  }
  return false;
 }
}


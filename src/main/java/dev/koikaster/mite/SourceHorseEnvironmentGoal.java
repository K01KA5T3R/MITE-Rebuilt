package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Literal16 indexed-trial source algorithms; only graph/shape API is adapted.
 * Queries are fresh and only the selected graph's native metadata is committed.
 */
final class SourceHorseEnvironmentGoal extends OriginalItemMovementGoal {
 enum Kind {PREDATOR,RAIN,WATER}
 final AbstractHorse horse;final Kind kind;int index;List<LivingEntity> predators;double nearest;
 SourceHorseEnvironmentGoal(AbstractHorse horse,Kind kind){super(horse,kind==Kind.PREDATOR?1.05f:1,true);this.horse=horse;this.kind=kind;index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(horse);}
 public boolean canUse(){
  if(kind==Kind.PREDATOR){if(horse.getRandom().nextInt(10)>0)return false;predators=SourceHorsePredators.query(horse,new AABB(horse.getX()-32,horse.getY()-4,horse.getZ()-32,horse.getX()+32,horse.getY()+4,horse.getZ()+32),SourceHorseBehavior.shy(horse));nearest=SourceHorsePredators.nearest(horse.getBlockX(),horse.getBlockY(),horse.getBlockZ(),predators);return nearest<256;}
  if(kind==Kind.WATER&&horse.getFirstPassenger() instanceof Player)return false;
  if(!OriginalRandomTables.chance_in_16[++index&32767])return false;
  // rs, including its undead-looking variants, inherits of.aY()=UNDEFINED and
  // of.aA()=false: native undead MobType must not suppress these horses' exit goal.
  return kind==Kind.RAIN?OriginalWeather.rain(horse.level(),horse.blockPosition()):horse.isInWater();
 }
 protected Path movementPath(){
  int x=horse.getBlockX(),y=horse.getBlockY(),z=horse.getBlockZ(),maximum=kind==Kind.PREDATOR?32:16;double best=kind==Kind.PREDATOR?nearest:0;Path selected=null;
  for(int attempt=0;attempt<16;attempt++){
   int dx=OriginalRandomTables.int_max[++index&32767]%(maximum*2+1)-maximum,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%(maximum*2+1)-maximum;
   var trial=OriginalEnvironmentPaths.settle(horse,new BlockPos(x+dx,y+dy,z+dz));
   if(kind!=Kind.PREDATOR&&(!OriginalEnvironmentPaths.passable(horse,trial,false)||OriginalEnvironmentPaths.occupied(horse,trial)||kind==Kind.RAIN&&OriginalWeather.rain(horse.level(),trial)||kind==Kind.WATER&&OriginalEnvironmentPaths.passable(horse,trial.below(),true)))continue;
   double distance=kind==Kind.PREDATOR?SourceHorsePredators.nearest(trial.getX(),trial.getY(),trial.getZ(),predators):OriginalEnvironmentPaths.sq(dx,dy,dz);
   if(kind==Kind.PREDATOR?!(distance>best):selected!=null&&!(distance<best))continue;
   var path=SourceArachnidPathBudget.block(horse,trial,maximum);if(path==null)continue;var end=path.getEndNode();var at=new BlockPos(end.x,end.y,end.z);
   if(kind!=Kind.PREDATOR&&(OriginalEnvironmentPaths.occupied(horse,at)||kind==Kind.RAIN&&OriginalWeather.rain(horse.level(),at)||kind==Kind.WATER&&OriginalEnvironmentPaths.passable(horse,at.below(),true)))continue;
   distance=kind==Kind.PREDATOR?SourceHorsePredators.nearest(end.x,end.y,end.z,predators):OriginalEnvironmentPaths.sq(end.x-x,end.y-y,end.z-z);
   if(kind==Kind.PREDATOR?!(distance>best):selected!=null&&!(distance<best))continue;best=distance;selected=path;
  }
  if(selected==null&&kind==Kind.WATER)selected=towardOrigin();
  if(selected!=null)((SourceArachnidPathBudget)horse.getNavigation()).mite$sourceCommitPath(selected);return selected;
 }
 private static double sourceSquared(int a,int b,int c){float x=a,y=b,z=c;return x*x+y*y+z*z;}
 private Path towardOrigin(){var target=OriginalOrigin.position(horse);int x=horse.getBlockX(),y=horse.getBlockY(),z=horse.getBlockZ(),cursor=horse.level().random.nextInt();double best=sourceSquared(x-target.getX(),y-target.getY(),z-target.getZ());Path selected=null;for(int attempt=0;attempt<16;attempt++){int dx=OriginalRandomTables.int_max[++cursor&32767]%33-16,dy=OriginalRandomTables.int_7_minus_3[++cursor&32767],dz=OriginalRandomTables.int_max[++cursor&32767]%33-16;var at=OriginalEnvironmentPaths.settle(horse,new BlockPos(x+dx,y+dy,z+dz));double distance=sourceSquared(at.getX()-target.getX(),at.getY()-target.getY(),at.getZ()-target.getZ());if(!(distance<best))continue;var path=SourceArachnidPathBudget.block(horse,at,16);if(path==null)continue;var end=path.getEndNode();distance=sourceSquared(end.x-target.getX(),end.y-target.getY(),end.z-target.getZ());if(!(distance<best))continue;best=distance;selected=path;}return selected;}
 protected float movementSpeed(){if(kind!=Kind.PREDATOR||predators==null)return super.movementSpeed();for(var e:predators)if(e instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==horse)return super.movementSpeed()*1.2f;return super.movementSpeed();}
 public void stop(){super.stop();if(kind==Kind.PREDATOR)predators=null;}
}

package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
/** R195 EntityFireElemental.c and ape.java still-lava natural spawning. */
public final class OriginalFireElementalLife {
 private final Mobs.Undead mob;int fireDelay,fizzDelay;
 public OriginalFireElementalLife(Mobs.Undead mob){this.mob=mob;}
 public static boolean wet(Mobs.Undead mob){return mob.isInWater()||OriginalWeather.rain(mob.level(),mob.blockPosition())||OriginalWeather.rain(mob.level(),BlockPos.containing(mob.getX(),mob.getY()+mob.getBbHeight(),mob.getZ()));}
 public void beforeAi(){
  if(mob.level().isClientSide)return;
  // Source nn.getTicksExistedWithOffset is ticksExisted + entityId*47. The dry-world pulse is unconditional in R195.
  if((mob.tickCount+mob.getId()*47)%40==0)mob.hurt(mob.damageSources().drown(),1);
  if(wet(mob)){
   if(--fizzDelay<=0){mob.playSound(SoundEvents.FIRE_EXTINGUISH,.7f,1.6f+(mob.getRandom().nextFloat()-mob.getRandom().nextFloat())*.4f);fizzDelay=mob.getRandom().nextInt(7)+2;if(mob.getRandom().nextInt(mob.isInWater()?1:4)==0)mob.hurt(mob.damageSources().drown(),1);}
  }else if(--fireDelay<=0){mob.playSound(SoundEvents.FIRE_AMBIENT,1+mob.getRandom().nextFloat(),mob.getRandom().nextFloat()*.7f+.3f);fireDelay=mob.getRandom().nextInt(21)+30;if(mob.isInLava())mob.heal(4);}
 }
 public static void onLavaRandomTick(Object rawLevel,Object rawPos,Object rawState,Object rawRandom){
  if(!(rawLevel instanceof ServerLevel level)||level.isClientSide||!(rawPos instanceof BlockPos pos)||!(rawState instanceof FluidState state)||!(rawRandom instanceof RandomSource random))return;
  if(!state.is(FluidTags.LAVA)||!state.isSource()||level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||SourceTournament.inArena(level,pos.getX(),pos.getZ()))return;
  int rarity=Math.max(1,level.dimensionType().hasSkyLight()?16+pos.getY():16);
  boolean spawned=false;
  if(level.dimension()==Level.NETHER&&!level.getFluidState(pos.below()).is(FluidTags.LAVA)&&level.isEmptyBlock(pos.above())&&random.nextInt(4)==0&&random.nextInt(rarity)==0&&canSpawnFireElementalNear(level,pos)){
   spawned=trySpawnFireElemental(level,pos.above(),random);
  }
  if(!spawned&&random.nextInt(16)==0&&random.nextInt(rarity)==0&&level.isEmptyBlock(pos.below())&&level.isEmptyBlock(pos.below(2))&&canSpawnFireElementalNear(level,pos)){
   trySpawnFireElemental(level,pos.below(2),random);
  }
 }
 private static boolean canSpawnFireElementalNear(ServerLevel level,BlockPos pos){
  if(!level.hasNearbyAlivePlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16.0))return false;
  return level.getEntitiesOfClass(Mobs.Undead.class,new AABB(pos).inflate(16.0),e->e.isAlive()&&Mobs.name(e).equals("fire_elemental")).size()<2;
 }
 private static boolean trySpawnFireElemental(ServerLevel level,BlockPos spawnPos,RandomSource random){
  var entity=Mobs.UNDEAD.get("fire_elemental").get().create(level);
  if(entity==null)return false;
  entity.moveTo(spawnPos.getX()+.5,spawnPos.getY(),spawnPos.getZ()+.5,random.nextFloat()*360f,0f);
  if(!level.noCollision(entity))return false;
  entity.finalizeSpawn(level,level.getCurrentDifficultyAt(spawnPos),MobSpawnType.NATURAL,null,null);
  return level.addFreshEntity(entity);
 }
}

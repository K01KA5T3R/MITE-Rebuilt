package dev.koikaster.mite;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

/** V009/V117 only. Modern species/diets/navigation remain native; reproduction,
 * wellness, feeding order and growth use the generic R195 animal/livestock path. */
public final class ModernBreeding420 {
 public static final String DATA="MiteModernBreeding420";
 public static boolean usesSelectiveBreeding(Animal animal) {
  var settings = WorldContentMode.settings(animal.level());
  return !settings.classicVisuals() && settings.extras();
 }
 public static OriginalWellness419 initialize(Animal a){var r=a.getRandom();return new OriginalWellness419(.8f+r.nextFloat()*.2f,.8f+r.nextFloat()*.2f,.8f+r.nextFloat()*.2f);}
 public static OriginalWellness419 state(Animal a){return ((SelectiveBreeding420)a).sourceWellness();}
 private static boolean sourceFood(Block block){return block==Blocks.GRASS_BLOCK||block==Blocks.GRASS||block==Blocks.FERN;}
 private static boolean sourceWater(Animal a,BlockPos p){var l=a.level();var s=l.getBlockState(p);return s.is(Blocks.WATER)||s.is(Blocks.SNOW)||s.is(Blocks.SNOW_BLOCK)||s.is(Blocks.WATER_CAULDRON)&&p.getY()+1<a.getEyeY()&&s.getValue(LayeredCauldronBlock.LEVEL)>0;}
 public static boolean nearby(Animal a,BlockPos center,boolean food){int height=Mth.floor(a.getBbHeight());for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=height;dy++)for(int dz=-1;dz<=1;dz++){var p=center.offset(dx,dy,dz);if(!a.level().hasChunkAt(p))continue;if(food?sourceFood(a.level().getBlockState(p).getBlock()):sourceWater(a,p)&&!a.level().getBlockState(p.above()).isFaceSturdy(a.level(),p.above(),Direction.DOWN))return true;}return false;}
 public static boolean crowded(Animal a){return !SourceSpawnOutdoors.isOutdoors(a.level(),a.blockPosition())||a.level().getEntitiesOfClass(Mob.class,a.getBoundingBox().inflate(2,.5,2)).size()>2;}
 public static void afterAi(Animal a){if(a.level().isClientSide||a.tickCount%100!=0||a.getRandom().nextInt(10)==0)return;state(a).update(nearby(a,a.blockPosition(),true),nearby(a,a.blockPosition(),false),a.level().isRainingAt(a.blockPosition()),crowded(a));}
 public static boolean canLove(Animal a){return !a.level().isClientSide&&state(a).canLove(a.getHealth()>=a.getMaxHealth());}
 public static InteractionResult feed(Animal a,Player player,ItemStack stack){
  if(!a.isFood(stack))return InteractionResult.PASS;
  if(a.level().isClientSide)return InteractionResult.CONSUME;
  var s=state(a);if(!s.canFeed(a.getAge(),a.isInLove(),a.isAlive(),a.getHealth()>=a.getMaxHealth()))return InteractionResult.PASS;
  s.eaten();if(!stack.is(Items.ROTTEN_FLESH))a.setInLove(player);
  // rp.onFoodEaten calls bX before og.onFoodEaten heals half maximum health.
  if(a.getMobType()!=MobType.UNDEAD)a.heal(a.getMaxHealth()*.5f);if(!player.getAbilities().instabuild)stack.shrink(1);return InteractionResult.SUCCESS;
 }
 public static int growthTarget(Animal a,int next){
  if(a.level().isClientSide)return next;var current=a.getAge();if(!state(a).allowGrowth(current,next))return current;
  // Spawn/load age assignments may run on a chunk-generation worker. Only a live
  // animal becoming adult needs the source cauldron headroom check.
  if(current<0&&next>=0&&a.isAddedToWorld()){
   var p=a.blockPosition();var block=a.level().getBlockState(p).getBlock();if((block==Blocks.CAULDRON||block==Blocks.WATER_CAULDRON||block==Blocks.POWDER_SNOW_CAULDRON||block==Blocks.LAVA_CAULDRON)&&!SourceTroopPlacement.passable(a.level(),p.above(),true))return -1;
  }
  return next;
 }
 public static void save(Animal a,CompoundTag root){if(a.level().isClientSide)return;var s=state(a);var tag=new CompoundTag();tag.putFloat("food",s.food);tag.putFloat("water",s.water);tag.putFloat("freedom",s.freedom);root.put(DATA,tag);}
 public static void load(Animal a,CompoundTag root){if(a.level().isClientSide||!root.contains(DATA))return;var t=root.getCompound(DATA);state(a).set(t.getFloat("food"),t.getFloat("water"),t.getFloat("freedom"));}
 public static void breed(Animal a,ServerLevel l,Animal other){var child=a.getBreedOffspring(l,other);var event=new BabyEntitySpawnEvent(a,other,child);boolean cancelled=MinecraftForge.EVENT_BUS.post(event);child=event.getChild();if(cancelled){a.setAge(16000);other.setAge(other instanceof SelectiveBreeding420?16000:6000);a.resetLove();other.resetLove();return;}if(child!=null){child.setBaby(true);child.moveTo(a.getX(),a.getY(),a.getZ(),0,0);a.finalizeSpawnChildFromBreeding(l,other,child);l.addFreshEntityWithPassengers(child);}}
 public static void finishBirth(Animal a,ServerLevel l,Animal other,AgeableMob child){
  if(child instanceof Animal baby&&child instanceof SelectiveBreeding420){state(baby).adopt(state(a),other instanceof SelectiveBreeding420?state(other):null);baby.setAge(-64000);}
  Optional.ofNullable(a.getLoveCause()).or(()->Optional.ofNullable(other.getLoveCause())).ifPresent(p->{p.awardStat(Stats.ANIMALS_BRED);CriteriaTriggers.BRED_ANIMALS.trigger(p,a,other,child);});
  a.setAge(16000);other.setAge(other instanceof SelectiveBreeding420?16000:6000);a.resetLove();other.resetLove();l.broadcastEntityEvent(a,(byte)18);
  // Original rp breeding creates its child/hearts without experience orbs.
 }
 private ModernBreeding420(){}
}

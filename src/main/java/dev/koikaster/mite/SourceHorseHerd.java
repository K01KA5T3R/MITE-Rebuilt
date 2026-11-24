package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
/** Original unified Horse herd across modern distinct Horse/Donkey registries.
 * A replacement is made before insertion; the temporary Horse never enters a world. */
final class SourceHorseHerd {
 record Data(OriginalHorseSpawn.Group group) implements SpawnGroupData{}
 record Finished(Mob actor,SpawnGroupData data){}
 static Finished initialize(Mob original,ServerLevelAccessor region,MobSpawnType reason,SpawnGroupData data){
  var event=new MobSpawnEvent.FinalizeSpawn(original,region,original.getX(),original.getY(),original.getZ(),region.getCurrentDifficultyAt(original.blockPosition()),reason,data,null,null);
  if(MinecraftForge.EVENT_BUS.post(event))return new Finished(original,null);
  var random=original.getRandom();var input=event.getSpawnData() instanceof Data prior?prior.group:null;
  var result=OriginalHorseSpawn.initialize(new OriginalHorseSpawn.Random(){public int nextInt(int bound){return random.nextInt(bound);}public double nextDouble(){return random.nextDouble();}public double nextGaussian(){return random.nextGaussian();}},input);
  Mob actor=original;if(result.kind()==1){actor=EntityType.DONKEY.create(region.getLevel());if(actor==null)throw new IllegalStateException("Source donkey factory");actor.moveTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),original.getXRot());actor.setUUID(original.getUUID());actor.setId(original.getId());actor.setSpawnCancelled(original.isSpawnCancelled());original.discard();}
  if(actor instanceof Horse horse)((SourceHorseVariantAccess)horse).mite$sourceHorseVariant(result.variant());
  if(result.newborn())((AgeableMob)actor).setAge(-24000); // Human original questionnaire48D: modern growth/breeding.
  actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(result.health());actor.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(result.speed());actor.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(result.jump());actor.setHealth((float)result.health());SourceSpawnInitialization.apply(actor,event.getSpawnType(),result.followBonus());
  return new Finished(actor,new Data(result.group()));
 }
 private SourceHorseHerd(){}
}

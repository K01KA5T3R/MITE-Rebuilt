package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;

/** tw.a(oi): the village appearance is a 5% group flag on the ordinary zombie
 * entry, never a second modern biome entry. tw.a(boolean) explicitly forces
 * adulthood, but its old first world-RNG draw is still consumed. Modern entity
 * identities split tw into Zombie and ZombieVillager; keep that split internal.
 */
public final class SourceZombieVariants {
 record Group(boolean villager,SpawnGroupData nativeData) implements SpawnGroupData {}
 static Group group(SpawnGroupData old,net.minecraft.util.RandomSource random){
  if(old instanceof Group group)return group;
  random.nextFloat(); // R195's disabled baby flag; preserve the source RNG order.
  return new Group(random.nextFloat()<.05f,null);
 }
 static SourceHorseHerd.Finished finish(Mob original,ServerLevelAccessor region,MobSpawnType reason,SpawnGroupData data){
  var group=group(data,region.getRandom());Mob actor=original;
  if(group.villager()){
   actor=Objects.requireNonNull(EntityType.ZOMBIE_VILLAGER.create(original.level()));
   actor.moveTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),original.getXRot());
  }
  // Explicit adult/no-jockey data also avoids the modern baby RNG and chicken branch.
  var nativeData=group.nativeData()==null?(actor instanceof SourceZombieOwner?new SourceZombieBody.Group(false,group.villager()):new Zombie.ZombieGroupData(false,false)):group.nativeData();
  SpawnGroupData initialized;
  try(var ignored=SourceSpawnInitialization.begin(actor)){
   initialized=ForgeEventFactory.onFinalizeSpawn(actor,region,region.getCurrentDifficultyAt(actor.blockPosition()),reason,nativeData,null);
  }
  ((Zombie)actor).setBaby(false);
  if(actor instanceof net.minecraft.world.entity.monster.ZombieVillager villager)
   villager.setVillagerData(villager.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.FARMER).setLevel(1)); // R195 setVillager(true,0)
  return new SourceHorseHerd.Finished(actor,new Group(group.villager(),initialized));
 }
 private SourceZombieVariants(){}
}

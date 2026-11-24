package dev.koikaster.mite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
/** tw.a(of), now used by ordinary source owners. Existing tn caller remains
 * frozen; its newly-created villager now reaches the new source initializer. */
public final class SourceZombieKills {
 static boolean killed(Zombie actor,ServerLevel level,LivingEntity victim,boolean parent){
  if(level.getDifficulty().getId()<2||!(victim instanceof Villager villager)||SourcePigmanKills.tool(actor.getMainHandItem()))return parent;
  if(level.getDifficulty()==Difficulty.NORMAL&&actor.getRandom().nextBoolean())return parent;
  var zombie=java.util.Objects.requireNonNull(EntityType.ZOMBIE_VILLAGER.create(level));zombie.moveTo(victim.getX(),victim.getY(),victim.getZ(),victim.getYRot(),victim.getXRot());victim.discard();
  net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(zombie,level,level.getCurrentDifficultyAt(zombie.blockPosition()),MobSpawnType.CONVERSION,null,null);
  for(int i=0;i<5;i++)actor.setItemSlot(SourcePigmanGear.slot(i),ItemStack.EMPTY);
  zombie.setVillagerData(zombie.getVillagerData().setProfession(villager.getVillagerData().getProfession()).setLevel(1));zombie.setBaby(false);level.addFreshEntity(zombie);level.levelEvent(null,1026,actor.blockPosition(),0);return false;
 }
 private SourceZombieKills(){}
}

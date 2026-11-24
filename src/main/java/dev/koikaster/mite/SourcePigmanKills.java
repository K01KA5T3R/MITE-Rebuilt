package dev.koikaster.mite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;
/** tw.a(of) caller: source tools prevent infection. The existing native
 * ZombieVillager identity carries the victim profession from tw.a(of), without a second type roll.
 * Its ordinary Zombie initializer remains a separately tracked wider0.1 port. */
public final class SourcePigmanKills {
 static boolean tool(ItemStack stack){var value=ClassicToolWear.profile(stack);return value!=null&&value.kind()!=LegacyToolWear.Kind.SHEARS;}
 static boolean killed(SourcePigman actor,ServerLevel level,LivingEntity victim,boolean parentResult){
  if(level.getDifficulty().getId()<2||!(victim instanceof Villager villager)||tool(actor.getMainHandItem()))return parentResult;
  if(level.getDifficulty()==Difficulty.NORMAL&&actor.getRandom().nextBoolean())return parentResult;
  var zombie=java.util.Objects.requireNonNull(EntityType.ZOMBIE_VILLAGER.create(level));zombie.moveTo(victim.getX(),victim.getY(),victim.getZ(),victim.getYRot(),victim.getXRot());victim.discard();
  try(var ignored=SourceSpawnInitialization.begin(zombie)){ForgeEventFactory.onFinalizeSpawn(zombie,level,level.getCurrentDifficultyAt(zombie.blockPosition()),MobSpawnType.CONVERSION,new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(false,false),null);}
  for(int i=0;i<5;i++)actor.setItemSlot(SourcePigmanGear.slot(i),ItemStack.EMPTY);
  zombie.setVillagerData(villager.getVillagerData().setLevel(1));zombie.setBaby(false);level.addFreshEntity(zombie);
  // bfl source1016 is mob.zombie.infect. Native1016 is Ghast shooting;1026 is
  // the matching native infection event. Never transplant obsolete event IDs.
  level.levelEvent(null,1026,actor.blockPosition(),0);return false;
 }
 private SourcePigmanKills(){}
}

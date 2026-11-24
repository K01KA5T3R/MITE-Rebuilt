package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** tr/sb repair state is installed on native owners and consumed after their real Mob.aiStep. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OriginalRepairOwners {
 public static OriginalRepairPickup owner(PathfinderMob mob){
  var bridge=(SourceRepairOwner)mob;
  if(bridge.mite$repairOwner()==null){var state=new OriginalRepairPickup(mob,Network::repairFx);bridge.mite$repairOwner(state);state.install();}
  return (OriginalRepairPickup)bridge.mite$repairOwner();
 }
 public static void afterAi(Object raw){if(raw instanceof PathfinderMob mob&&!mob.level().isClientSide&&OriginalRepairPickup.handles(mob))owner(mob).tick();}
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof PathfinderMob mob&&OriginalRepairPickup.handles(mob))owner(mob);}
 /** Source leaf attributes; skeleton equipment, combat AI and SnowGolem melting remain separate ports. */
 @Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static final class Profiles {
  @SubscribeEvent public static void attributes(net.minecraftforge.event.entity.EntityAttributeModificationEvent event){
   for(var type:java.util.List.of(EntityType.SKELETON,EntityType.WITHER_SKELETON)){event.add(type,Attributes.MAX_HEALTH,6);event.add(type,Attributes.MOVEMENT_SPEED,.3f);event.add(type,Attributes.ATTACK_DAMAGE,4);}
   event.add(EntityType.SNOW_GOLEM,Attributes.MAX_HEALTH,8);event.add(EntityType.SNOW_GOLEM,Attributes.MOVEMENT_SPEED,.2f);
  }
 }
 private OriginalRepairOwners(){}
}

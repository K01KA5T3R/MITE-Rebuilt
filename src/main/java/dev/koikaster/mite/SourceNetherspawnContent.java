package dev.koikaster.mite;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceNetherspawnContent {
 public static final RegistryObject<EntityType<SourceNetherspawn>> NETHERSPAWN=Mobs.ENTITIES.register("netherspawn",()->EntityType.Builder.of(SourceNetherspawn::new,MobCategory.MONSTER).sized(.3f,.7f).build(MiteReforged.ID+":netherspawn"));
 static void register(){Content.item("netherspawn_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(NETHERSPAWN,0x493d34,0xbd5a16,new Item.Properties()));SourceCreatureFragments.registerItems();}
 static void attributes(EntityAttributeCreationEvent event){event.put(NETHERSPAWN.get(),SourceSilverfish.attributes().build());}
 static void placements(SpawnPlacementRegisterEvent event){event.register(NETHERSPAWN.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceSilverfishContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);}
 @SubscribeEvent public static void quench(PlayerInteractEvent.EntityInteract event){
  if(!(event.getTarget() instanceof SourceNetherspawn target))return;var stack=event.getItemStack();boolean bowl=OriginalBowls.water(stack),bucket=ClassicVessels.handled(stack)&&ClassicVessels.fluid(stack).equals("water");if(!bowl&&!bucket)return;
  event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));if(event.getLevel().isClientSide)return;
  target.hurt(OriginalElementalDamage.water(target),bucket?8:2);SourceChemicalFx.entity(target,SourceChemicalFx.ENTITY_STEAM);var player=event.getEntity();
  if(!player.isCreative()){var empty=bowl?new ItemStack(Items.BOWL):ClassicVessels.peer(stack,"");stack.shrink(1);if(stack.isEmpty())player.setItemInHand(event.getHand(),empty);else if(!player.getInventory().add(empty))player.drop(empty,false);}
 }
 private SourceNetherspawnContent(){}
}

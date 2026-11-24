package dev.koikaster.mite;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ModernContentEvents {
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(EntityJoinLevelEvent event){
  var entity=event.getEntity();
  if(ModernContentPolicy.removed(entity.getType())){event.setCanceled(true);return;}
  if(event.getLevel().isClientSide)return;
  var settings=WorldContentMode.settings(event.getLevel());
  if(!RebuiltContent.allows(entity,settings))return;
  DeepslateRemoval.joined(entity);
  if(entity instanceof net.minecraft.world.entity.LivingEntity living)for(var slot:EquipmentSlot.values()){
   var stack=living.getItemBySlot(slot);
   if(living instanceof MythrilPillager && slot==EquipmentSlot.MAINHAND && stack.is(Items.CROSSBOW))continue;
   // A player's saved equipment is owned stock. Mode admission applies to generated mob gear.
   if (living instanceof Player ? ModernContentPolicy.removed(stack) : disallowed(stack, settings)) {
    living.setItemSlot(slot, ItemStack.EMPTY);
   }
  }
  if (entity instanceof ItemEntity item) {
   // This may be a preexisting item, a player toss or a broken container. Loot sources enforce mode admission.
   if (ModernContentPolicy.removed(item.getItem())) event.setCanceled(true);
   else item.setItem(ModernContentPolicy.cleanEnchantments(item.getItem()));
  }
  if(settings.extras()&&!event.loadedFromDisk()&&entity instanceof Drowned drowned){
   // Join occurs after native finalization/equipment assignment, also covering conversion.
   var biome=event.getLevel().getBiome(entity.blockPosition());
   if(biome.is(BiomeTags.IS_OCEAN)){if(!drowned.getMainHandItem().is(Items.TRIDENT))drowned.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));}
   else if(biome.is(BiomeTags.IS_RIVER))for(var hand:new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND})if(drowned.getItemBySlot(hand).is(Items.TRIDENT))drowned.setItemSlot(hand,ItemStack.EMPTY);
  }
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock event){
  if(ModernContentPolicy.removed(event.getItemStack())||ModernContentPolicy.removed(event.getLevel().getBlockState(event.getPos()))){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void item(PlayerInteractEvent.RightClickItem event){if(ModernContentPolicy.removed(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event){
  for (var item : event.getDrops()) item.setItem(DeepslateRemoval.stack(item.getItem()));
  var settings = WorldContentMode.settings(event.getEntity().level());
  boolean owned = event.getEntity() instanceof Player;
  event.getDrops().removeIf(item -> owned
      ? ModernContentPolicy.removed(item.getItem()) : disallowed(item.getItem(), settings));
  for (var item : event.getDrops()) item.setItem(ModernContentPolicy.cleanEnchantments(item.getItem()));
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void inventory(TickEvent.PlayerTickEvent event){
  if(event.phase!=TickEvent.Phase.START||event.player.level().isClientSide)return;
  var player=event.player;var inventory=player.getInventory();boolean changed=false;
  DeepslateRemoval.inventory(player);
  for(int i=0;i<inventory.getContainerSize();i++){
   var before=inventory.getItem(i);var after=ModernContentPolicy.removed(before)?ItemStack.EMPTY:ModernContentPolicy.cleanEnchantments(before);
   if(before!=after){inventory.setItem(i,after);changed=true;}
  }
  for(var slot:player.containerMenu.slots)if(slot.container!=inventory&&ModernContentPolicy.removed(slot.getItem())){slot.set(ItemStack.EMPTY);changed=true;}
  var carried=player.containerMenu.getCarried();var clean=ModernContentPolicy.removed(carried)?ItemStack.EMPTY:ModernContentPolicy.cleanEnchantments(carried);
  if(clean!=carried){player.containerMenu.setCarried(clean);changed=true;}
  if(changed)player.containerMenu.broadcastChanges();
 }
 private static boolean disallowed(ItemStack stack,WorldExtraSettings settings){return ModernContentPolicy.removed(stack)||!RebuiltContent.allows(stack,settings);}
 private ModernContentEvents(){}
}

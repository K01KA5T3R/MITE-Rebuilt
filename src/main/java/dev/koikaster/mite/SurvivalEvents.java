package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SurvivalEvents {
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p){PlayerState.tick(p);InventoryCrafting.tick(p);if(p.tickCount%20==0)Grave.recover(p);}}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){SurvivalFood.initialize(p);PlayerState.syncVanillaXp(p);Network.sync(p);}}
 @SubscribeEvent public static void clone(PlayerEvent.Clone e){
  var copy=PlayerState.data(e.getOriginal()).copy();
  if(e.isWasDeath()){
   int xp=copy.getInt("Xp");
   copy.putInt("Xp",WorldContentMode.rebuilt(e.getEntity().level())?Rules.deathXp(xp):Rules.classicDeathXp(xp));
   SurvivalFood.reset(copy);Diet.reset(copy);
  }
  e.getEntity().getPersistentData().put(MiteReforged.ID,copy);
 }
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){
  if(e.getEntity() instanceof ServerPlayer p){
   SurvivalFood.initialize(p);PlayerState.syncVanillaXp(p);PlayerState.tick(p);
   if(!e.isEndConquered())p.setHealth(p.getMaxHealth());
   Network.sync(p);
  }
 }
 @SubscribeEvent public static void startEating(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p&&!SurvivalFood.canIngest(p,e.getItem()))e.setCanceled(true);}
 @SubscribeEvent public static void beforeXpPickup(PlayerXpEvent.PickupXp e){for(var s:e.getEntity().getArmorSlots())PlayerState.convertMending(s);for(var s:e.getEntity().getHandSlots())PlayerState.convertMending(s);}
 @SubscribeEvent public static void xp(PlayerXpEvent.XpChange event) {
  if (!(event.getEntity() instanceof ServerPlayer player)) return;
  int before = PlayerState.data(player).getInt("Xp");
  int amount = event.getAmount();
  if (amount != 0) PlayerState.addXp(player, PlayerState.mend(player, amount));
  if (before < 0 || PlayerState.data(player).getInt("Xp") < 0) {
   // The native grant clamps totalExperience to zero even if the event amount was set to zero.
   event.setCanceled(true);
  } else if (amount != 0) {
   event.setAmount(0);
  }
 }
 @SubscribeEvent public static void level(PlayerXpEvent.LevelChange e){
  if(e.getEntity() instanceof ServerPlayer p){
   int before=PlayerState.level(p);
   int target=(int)Math.max(Rules.MIN_LEVEL,Math.min(Rules.MAX_LEVEL,(long)before+e.getLevels()));
   PlayerState.data(p).putInt("Xp",Rules.xpForLevel(target));
   if(before<0||target<0){
    e.setCanceled(true);PlayerState.syncVanillaXp(p);Network.sync(p);
   }else e.setLevels(target-p.experienceLevel);
  }
 }
 @SubscribeEvent public static void xpDrop(LivingExperienceDropEvent e){
  if(e.getEntity() instanceof net.minecraft.world.entity.animal.AbstractFish){e.setDroppedExperience(0);return;}
  if(e.getEntity().getType()==EntityType.WOLF){e.setDroppedExperience(5);return;}
  if(e.getEntity() instanceof net.minecraft.world.entity.animal.Ocelot cat){e.setDroppedExperience(cat.isBaby()?0:5);return;}
  if(e.getEntity() instanceof Mobs.DireWolf wolf&&wolf.sourceDire())return;
  if(e.getEntity() instanceof Player||e.getEntity() instanceof net.minecraft.world.entity.animal.Animal&&e.getEntity().getType().getCategory()!=MobCategory.MONSTER)e.setDroppedExperience(0);
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void deathDrops(LivingDropsEvent e){
  if(e.getEntity() instanceof ServerPlayer p){
   GraveDeathMenus211.capture(p,e.getDrops());
   StingerBoomerang.captureDeathFlights(p,e.getDrops());
   if(WorldContentMode.rebuilt(p.level()))Grave.store(p,e.getDrops());
  }
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void graveInteraction(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||e.getUseBlock()==Event.Result.DENY||!(e.getLevel().getBlockEntity(e.getPos()) instanceof Grave.Entity))return;
  // Vanilla skips Block.use while sneaking with a held item. A grave must
  // still transfer its contents without invoking the held item's placement.
  var state=e.getLevel().getBlockState(e.getPos());var result=state.use(e.getLevel(),e.getEntity(),e.getHand(),e.getHitVec());e.setCanceled(true);e.setCancellationResult(result);
 }
 @SubscribeEvent public static void fishing(ItemFishedEvent e){
  var hook=e.getHookEntity();
  if(hook.level().isClientSide||!(e.getEntity() instanceof ServerPlayer player))return;
  if(!FishingRules.habitat(player.serverLevel(),hook.blockPosition()).viable()){
   e.setCanceled(true);e.damageRodBy(1);player.displayClientMessage(Component.translatable("message.mite_reforged.fishing_pool"),true);
  }
 }
 @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){
  if(!e.getPosition().map(pos->HarvestRules.canHarvest(e.getEntity(),e.getState(),e.getEntity().level(),pos)).orElseGet(()->HarvestRules.canHarvest(e.getEntity(),e.getState())))e.setNewSpeed(0);
  else e.setNewSpeed(e.getNewSpeed()*Rules.harvestingMultiplier(PlayerState.level(e.getEntity())));
 }
 @SubscribeEvent public static void harvestCheck(PlayerEvent.HarvestCheck e){e.setCanHarvest(HarvestRules.canHarvest(e.getEntity(),e.getTargetBlock()));}
 @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){
  var p=e.getPlayer();
  if(!p.isCreative()&&!HarvestRules.canHarvest(p,e.getState(),e.getLevel(),e.getPos())){e.setCanceled(true);return;}
  if(e.getLevel().getBlockEntity(e.getPos()) instanceof Grave.Entity grave&&!grave.contents.stream().allMatch(ItemStack::isEmpty)){e.setCanceled(true);return;}
 }
 @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e){if(e.getEntity() instanceof Player p&&!p.isCreative()&&!SurvivalFood.hasFoodEnergy(p)&&ClassicMining.hardness(e.getPlacedBlock(),e.getLevel(),e.getPos())>0)e.setCanceled(true);}
 @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e){
  var p=e.getEntity();var state=e.getLevel().getBlockState(e.getPos());
  if(p.getMainHandItem().getItem() instanceof Tools.Hoe&&net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(p.getMainHandItem().getItem()).getPath().endsWith("_scythe")&&state.getBlock() instanceof CropBlock&&p instanceof ServerPlayer sp&&SurvivalFood.hasFoodEnergy(p)){
   boolean harvested=false;
   for(BlockPos q:BlockPos.betweenClosed(e.getPos().offset(-1,0,-1),e.getPos().offset(1,0,1))){var crop=sp.level().getBlockState(q);if(crop.getBlock() instanceof CropBlock c&&c.isMaxAge(crop))harvested=sp.gameMode.destroyBlock(q)||harvested;}
   if(harvested){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;}
  }
  if(state.is(Blocks.CRAFTING_TABLE)&&!p.isShiftKeyDown()){
   e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(p instanceof ServerPlayer sp)TimedCrafting.open(sp,e.getPos());return;
  }
  if(ClassicRepair.anvil(state)&&!p.isShiftKeyDown()&&ClassicRepair.accessible(e.getLevel(),e.getPos())){
   e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(p instanceof ServerPlayer sp)ClassicRepair.open(sp,e.getPos());
  }
 }
 @SubscribeEvent public static void villager(PlayerInteractEvent.EntityInteract e){
  if(e.getTarget() instanceof net.minecraft.world.entity.animal.Cow cow&&!cow.isBaby()){
   var stack=e.getEntity().getItemInHand(e.getHand());
   if(stack.getItem() instanceof SpecialItems.MetalBucket bucket&&bucket.fluid(stack).isEmpty()){
    e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(!e.getLevel().isClientSide&&!e.getEntity().isCreative())ClassicVessels.replaceOne(e.getEntity(),e.getHand(),ClassicVessels.peer(stack,"milk"));return;
   }
  }
  // Source076 has no trade entry. Its ordinary item interaction must still
  // reach Mob's name-tag path; retain the fallback for other merchants.
  if(e.getTarget() instanceof AbstractVillager&&!(e.getTarget() instanceof SourceVillager)&&!e.getEntity().isCreative()){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);}}
 @SubscribeEvent public static void sleep(PlayerSleepInBedEvent e){
  var p=e.getEntity();if(OriginalWeather.blood(p.level())){e.setResult(Player.BedSleepingProblem.NOT_SAFE);return;}if(p.isCreative())return;
  if(p.getFoodData().getFoodLevel()<PlayerState.foodCap(p)-2||p.level().canSeeSky(e.getPos().above()))e.setResult(Player.BedSleepingProblem.NOT_SAFE);
 }
 @SubscribeEvent public static void crops(BlockEvent.CropGrowEvent.Pre e){
  if(Agriculture.supports(e.getState()))return;
  var pos=e.getPos();var l=e.getLevel();
  if(ModernPlantProduction434.limited(e.getState().getBlock())&&!WorldContentMode.rebuilt(l))return;
  if(l.getRawBrightness(pos,0)<9||l.getRandom().nextDouble()>Settings.GROWTH)e.setResult(Event.Result.DENY);
 }
 @SubscribeEvent public static void gravity(BlockEvent.NeighborNotifyEvent e){
  if(Settings.COLLAPSE&&e.getLevel() instanceof ServerLevel l){SoilGravity.enqueue(l,e.getPos());for(Direction d:Direction.values())SoilGravity.enqueue(l,e.getPos().relative(d));}
 }
 @SubscribeEvent public static void joined(EntityJoinLevelEvent e){
  var entity=e.getEntity();if(entity.getType()==EntityType.PHANTOM||entity.getType()==EntityType.WARDEN||entity.getType()==EntityType.WANDERING_TRADER)e.setCanceled(true);
  if(!e.getLevel().isClientSide&&entity instanceof Monster m&&!(m instanceof Mobs.Undead)&&SourceArachnidOwners.species(m)==null){var a=m.getAttribute(Attributes.FOLLOW_RANGE);if(a!=null&&a.getBaseValue()<32)a.setBaseValue(32);}
 }
 @SubscribeEvent public static void hurt(LivingHurtEvent e){
  if(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&e.getSource().getDirectEntity() instanceof ServerPlayer p&&!ClassicMeleeEnchantments.alreadyScaled(e.getSource()))e.setAmount(e.getAmount()*Rules.meleeMultiplier(PlayerState.level(p)));

  Mobs.onHurt(e);
 }
 @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
  var server=e.getServer();
  if(e.phase==TickEvent.Phase.END&&!EmptyServerPause.paused(server)){SoilGravity.tick(server);WorldEvents.tick(server);}
 }
}

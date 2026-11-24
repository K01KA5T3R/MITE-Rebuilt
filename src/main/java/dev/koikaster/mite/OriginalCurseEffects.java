package dev.koikaster.mite;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** js/uf/re/tv/ye world curse consumers, with modern server/client API adapters. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OriginalCurseEffects {
 public static int id(Player player){if(player.getGameProfile()==null)return 0;if(player instanceof ServerPlayer server){if(server.server==null||server.server.overworld()==null)return 0;return OriginalCurses.get(server.serverLevel()).activeId(server);}return player.getPersistentData().getCompound(MiteReforged.ID).getInt("CurseId");}
 public static boolean has(Player player,int id,boolean learn){if(id(player)!=id)return false;if(learn)learn(player);return true;}
 public static void learn(Player player){if(player instanceof ServerPlayer server){if(OriginalCurses.get(server.serverLevel()).learn(server))refresh(server,false);}else{var data=PlayerState.data(player);if(!data.getBoolean("CurseKnown")){data.putBoolean("CurseKnown",true);Network.curseLearn(id(player));}}}
 private static void refresh(ServerPlayer player,boolean force){var store=OriginalCurses.get(player.serverLevel());var curse=store.find(player);int active=curse!=null&&curse.realized()?curse.definition.id():0;boolean known=active>0&&curse.known();var data=PlayerState.data(player);int previous=data.getInt("CurseId");if(force||previous!=active||data.getBoolean("CurseKnown")!=known){data.putInt("CurseId",active);data.putBoolean("CurseKnown",known);Network.sync(player);if(previous>0&&active==0){Network.curseNotice(player,2,0);if(!player.isEyeInFluid(FluidTags.WATER))player.setAirSupply(player.getMaxAirSupply());}}}
 private static boolean dropArmor(ServerPlayer player){boolean dropped=false;for(var slot:new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD}){var stack=player.getItemBySlot(slot);if(!stack.isEmpty()){player.setItemSlot(slot,ItemStack.EMPTY);var item=new net.minecraft.world.entity.item.ItemEntity(player.level(),player.getX(),player.getEyeY()-.3,player.getZ(),stack);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(40);item.setThrower(player.getUUID());var tag=item.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putShort("Age",(short)-18000);item.load(tag);player.level().addFreshEntity(item);player.awardStat(net.minecraft.stats.Stats.DROP);dropped=true;}}return dropped;}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.START)return;for(var player:event.getServer().getPlayerList().getPlayers()){var store=OriginalCurses.get(player.serverLevel());boolean realized=store.realize(player);if(realized&&store.activeId(player)==10&&dropArmor(player))store.learn(player);refresh(player,false);if(realized)Network.curseNotice(player,1,store.activeId(player));if(store.markLearned(player))Network.curseNotice(player,3,store.activeId(player));if(store.activeId(player)==2&&player.getAirSupply()>90)player.setAirSupply(90);if(store.activeId(player)==3&&player.isSprinting()){has(player,3,true);player.setSprinting(false);}}}
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Witch witch)OriginalCurses.witchSeed(witch);}
 public static void sourceWitchDeath(Witch witch){if(witch.level() instanceof ServerLevel level&&OriginalCurses.get(level).removeForWitch(witch.getUUID()))for(var player:level.getServer().getPlayerList().getPlayers())refresh(player,false);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){if(event.isCanceled()||!(event.getEntity() instanceof Witch witch)||!(witch.level() instanceof ServerLevel level))return;var store=OriginalCurses.get(level);if(store.removeForWitch(witch.getUUID()))for(var player:level.getServer().getPlayerList().getPlayers())refresh(player,false);}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)refresh(player,true);}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)refresh(player,true);}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)refresh(player,true);}
 public static void clear(ServerPlayer player){boolean removed=OriginalCurses.get(player.serverLevel()).removeFromPlayer(player);refresh(player,true);if(removed&&!player.isEyeInFluid(FluidTags.WATER))player.setAirSupply(player.getMaxAirSupply());}
 /** re.a: placed after candidate eligibility/home/vision and before optional path cache. */
 public static void consider(Object rawMob,Object rawTarget){if(rawMob instanceof Witch witch&&rawTarget instanceof ServerPlayer player&&witch.getRandom().nextInt(4)==0)OriginalCurses.get(player.serverLevel()).tryCurse(player,witch);}
 public static boolean sprint(boolean requested,Object actor){return requested&&!(actor instanceof Player player&&has(player,3,true));}
 public static int air(int amount,Object actor){return actor instanceof Player player&&amount>90&&has(player,2,false)?90:amount;}
 public static void breathing(Object actor){if(actor instanceof Player player)has(player,2,true);}
 public static int wear(int amount,Object rawStack,Object actor){if(actor instanceof ServerPlayer player&&!player.isCreative()&&((ItemStack)rawStack).isDamageableItem()&&has(player,1,true))amount*=2;return amount;}
 public static boolean mayWear(Object actor){return !(actor instanceof Player player&&has(player,10,true));}
 public static net.minecraft.world.InteractionResultHolder<ItemStack> armorDenied(Object rawPlayer,Object rawHand){var player=(Player)rawPlayer;return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand((net.minecraft.world.InteractionHand)rawHand));}
 public static int enderman(Object rawEnderman,Object rawPlayer){if(rawEnderman instanceof EnderMan enderman&&rawPlayer instanceof Player player&&enderman.getRandom().nextInt(3)==0&&has(player,7,true))return 1;return -1;}
 public static Vec3 entangle(Object rawActor,Object rawMovement){var movement=(Vec3)rawMovement;if(!(rawActor instanceof Player player))return movement;var state=player.level().getBlockState(player.blockPosition());if((state.getBlock() instanceof BushBlock||state.getBlock() instanceof VineBlock||state.getBlock() instanceof GrowingPlantBlock||state.getBlock() instanceof SugarCaneBlock)&&has(player,9,true)){double factor=state.is(Blocks.VINE)?.2f:.4f;return movement.multiply(factor,1,factor);}return movement;}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void chest(PlayerInteractEvent.RightClickBlock event){var player=event.getEntity();if(!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ChestBlock)||id(player)!=11)return;if(!event.getLevel().getBlockState(event.getPos().above()).isSolid())has(player,11,true);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void sleep(PlayerSleepInBedEvent event){if(event.getResultStatus()==null&&has(event.getEntity(),12,true))event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);}
 /** Original client-only attack permission is one server-authoritative roll in the modern packet pipeline. */
 @SubscribeEvent public static void fear(AttackEntityEvent event){var player=event.getEntity();if(player.level().isClientSide||!(event.getTarget() instanceof LivingEntity target))return;int curse=id(player);boolean matches=curse==13&&target instanceof Spider||curse==14&&target instanceof net.minecraft.world.entity.animal.Wolf||curse==15&&target instanceof Creeper||curse==16&&target.getMobType()==MobType.UNDEAD;if(matches&&target.getRandom().nextInt(4)>0&&has(player,curse,true))event.setCanceled(true);}
 private OriginalCurseEffects(){}
}

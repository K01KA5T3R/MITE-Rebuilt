package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Actual native use/interaction entries for the original xw action kernel. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceFlintAndSteel749 {
 private static void sound(Player player){player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,1f,LegacyItemRandom749.RANDOM.nextFloat()*.4f+.8f);}
 private static void wear(Player player,InteractionHand hand){var stack=player.getItemInHand(hand);LegacyFlintDurability749.fresh(stack);stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));}
 /** Null preserves the explicitly retained modern lighting branch. */
 public static InteractionResult block(Object raw){
  var context=(UseOnContext)raw;var stack=context.getItemInHand();if(!LegacyFlintDurability749.handles(stack))return null;
  var player=context.getPlayer();if(player==null||player.isSpectator())return InteractionResult.FAIL;
  var level=context.getLevel();var clicked=context.getClickedPos();
  if(level.isOutsideBuildHeight(clicked)||!level.getWorldBorder().isWithinBounds(clicked)||!level.hasChunkAt(clicked))return InteractionResult.FAIL;
  var state=level.getBlockState(clicked);if(state.isAir()||state.getBlock() instanceof BaseFireBlock||state.getBlock() instanceof LiquidBlock)return InteractionResult.FAIL;
  if(CampfireBlock.canLight(state)||CandleBlock.canLight(state)||CandleCakeBlock.canLight(state))return null;
  if(!level.mayInteract(player,clicked))return InteractionResult.FAIL;
  var next=clicked.relative(context.getClickedFace());boolean accepted=SourceIgnitionRules749.blockUse(new SourceIgnitionRules749.BlockUse(){
   public boolean blockHit(){return true;}public boolean tnt(){return state.is(Blocks.TNT);}
   public boolean neighborAir(){return level.isEmptyBlock(next);}public boolean editNeighbor(){return !level.isOutsideBuildHeight(next)&&level.getWorldBorder().isWithinBounds(next)&&level.mayInteract(player,next)&&player.mayUseItemAt(next,context.getClickedFace(),stack);}
   public boolean server(){return !level.isClientSide;}
   public void primeTnt(){state.onCaughtFire(level,clicked,context.getClickedFace(),player);level.removeBlock(clicked,false);}
   public void spark(){level.setBlock(next,SourceSpark749.BLOCK.get().defaultBlockState(),3);}
   public void swing(){} // The native sided-success result supplies the client swing.
   public void sound(){SourceFlintAndSteel749.sound(player);}public void wear(){SourceFlintAndSteel749.wear(player,context.getHand());}
  });return accepted?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.FAIL;
 }
 /** TNT's block activation runs before Item.useOn; use the same one-action owner. */
 public static InteractionResult tnt(Object rawPlayer,Object rawHand,Object rawHit){var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;if(!LegacyFlintDurability749.handles(player.getItemInHand(hand)))return null;return block(new UseOnContext(player,hand,(BlockHitResult)rawHit));}
 private static SourceIgnitionRules749.Target kind(Entity entity){
  if(entity instanceof Chicken)return SourceIgnitionRules749.Target.CHICKEN;
  if(entity instanceof Sheep)return SourceIgnitionRules749.Target.SHEEP;
  if(entity instanceof Mobs.DireWolf wolf){if(wolf.sourceHellhound())return SourceIgnitionRules749.Target.HELLHOUND;if(wolf.sourceDire())return SourceIgnitionRules749.Target.DIRE_WOLF;}
  return entity instanceof Wolf?SourceIgnitionRules749.Target.WOLF:SourceIgnitionRules749.Target.OTHER;
 }
 private static boolean entity(Player player,InteractionHand hand,Entity target){
  if(player.isSpectator()||!LegacyFlintDurability749.handles(player.getItemInHand(hand)))return false;
  SourceIgnitionRules749.entityUse(new SourceIgnitionRules749.EntityUse(){
   public boolean server(){return !player.level().isClientSide;}public SourceIgnitionRules749.Target target(){return kind(target);}
   public boolean sheared(){return target instanceof Sheep sheep&&sheep.isSheared();}public boolean tame(){return target instanceof Wolf wolf&&wolf.isTame();}
   public void targetPlayer(){((Wolf)target).setTarget(player);}public void ignite(int seconds){target.setSecondsOnFire(seconds);}
   public void sound(){SourceFlintAndSteel749.sound(player);}public void wear(){SourceFlintAndSteel749.wear(player,hand);}
  });return true;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void interact(PlayerInteractEvent.EntityInteract event){if(entity(event.getEntity(),event.getHand(),event.getTarget())){event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));}}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void specific(PlayerInteractEvent.EntityInteractSpecific event){if(entity(event.getEntity(),event.getHand(),event.getTarget())){event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));}}
 private SourceFlintAndSteel749(){}
}

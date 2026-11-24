package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 ItemRock/xl sacrifice values plus ITE 2.0.30 ItemGemsTrans world-scoped config values & quartz cap. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class MineralExperience {
 public static int value(ItemStack stack){
  if(stack.is(Items.REDSTONE))return 5;if(stack.is(Items.LAPIS_LAZULI)||stack.is(Items.AMETHYST_SHARD))return 25;
  if(stack.is(Items.QUARTZ))return 50;if(stack.is(Items.EMERALD))return 250;if(stack.is(Items.DIAMOND))return 500;return 0;
 }
 public static int value(ItemStack stack, Level level){
  if(level==null||!WorldContentMode.ite(level))return value(stack);
  var cfg=IteConfig.effective(level);
  if(stack.is(Items.LAPIS_LAZULI))return cfg.lapisExp();
  if(stack.is(Items.QUARTZ))return cfg.quartzExp();
  if(stack.is(Items.EMERALD))return cfg.emeraldExp();
  if(stack.is(Items.DIAMOND))return cfg.diamondExp();
  return value(stack);
 }
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem e){
  var player=e.getEntity();var stack=e.getItemStack();var level=e.getLevel();
  if(!WorldContentMode.rebuilt(level)&&(stack.is(Items.REDSTONE)||stack.is(Items.AMETHYST_SHARD)))return;
  int xp=value(stack,level);
  // Per ItemGemsTrans/Config contract, values < 1 are disabled and must never cancel or deduct negative XP.
  if(e.getHand()!=InteractionHand.MAIN_HAND||xp<1||player.isSpectator())return;
  // Redstone keeps its normal placement when pointing at a block, including
  // a failed placement. Only an air click sacrifices it for experience.
  if(stack.is(Items.REDSTONE)&&player.pick(player.getBlockReach(),1,false).getType()==HitResult.Type.BLOCK)return;
  // The original injection is inside ItemRock's server branch. Keep client bobbing.
  if(!level.isClientSide&&WorldContentMode.ite(level)&&stack.is(Items.QUARTZ)){
   int maxLevel=IteConfig.effective(level).quartzMaxExpLevel();
   if(maxLevel>0&&PlayerState.level(player)>=maxLevel)return;
  }
  e.setCanceled(true);e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
  if(!level.isClientSide&&!(player instanceof ServerPlayer))return;
  var item=stack.getItem();
  SourceUseOrder1066.mineral(new SourceUseOrder1066.Mineral(){
   public int value(){return xp;}public boolean server(){return !level.isClientSide;}
   public void breakingEffect(){SourceToolBreakTransport1058.broken(player,item,player.getInventory().selected);}
   public void convert(){SourceHeldConversion1037.convert(player,InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
   public void experience(int value){var server=(ServerPlayer)player;server.increaseScore(value);server.level().playSound(null,server.getX(),server.getY(),server.getZ(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.1f,.5f*((server.getRandom().nextFloat()-server.getRandom().nextFloat())*.7f+1.8f));PlayerState.addXp(server,value);}
   public void bob(){SourceSnowballEffects.bob(InteractionHand.MAIN_HAND);}
  });
  if(player instanceof ServerPlayer server){if(!server.isCreative())server.awardStat(Stats.ITEM_USED.get(item));server.containerMenu.broadcastChanges();}
 }
 private MineralExperience(){}
}

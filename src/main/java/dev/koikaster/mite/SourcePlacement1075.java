package dev.koikaster.mite;
import java.util.*;import java.util.function.*;
import net.minecraft.stats.Stat;import net.minecraft.stats.Stats;import net.minecraft.world.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.item.context.UseOnContext;import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;import net.minecraftforge.common.util.BlockSnapshot;import net.minecraftforge.event.ForgeEventFactory;
/** R195 placement consumes at the accepted world transaction boundary. */
public final class SourcePlacement1075 {
 private static final ThreadLocal<Frame> active=new ThreadLocal<>();
 private static final class Frame {
  final Player player;final ItemStack input,before;final Item item;final Level level;final boolean captureBefore,restoreBefore;final int snapshotsBefore;
  int requests;boolean placed,accepted;float hunger;List<BlockSnapshot> snapshots;final List<Stat<?>> statistics=new ArrayList<>();
  Frame(UseOnContext context){player=context.getPlayer();input=context.getItemInHand();before=input.copy();item=input.getItem();level=context.getLevel();captureBefore=level.captureBlockSnapshots;restoreBefore=level.restoringBlockSnapshots;snapshotsBefore=level.capturedBlockSnapshots.size();}
 }
 private static Frame admit(UseOnContext context){
  var player=context.getPlayer();var input=context.getItemInHand();
  if(player==null||player.isSpectator()||context.getHand()!=InteractionHand.MAIN_HAND||player.getMainHandItem()!=input||input.isEmpty()||!(input.getItem() instanceof BlockItem)||input.getItem() instanceof SolidBucketItem||!ClassicToolCatalogue.sourceItem(input))return null;
  return new Frame(context);
 }
 public static InteractionResult server(Object rawContext){var context=(UseOnContext)rawContext;return use(context,()->ForgeHooks.onPlaceItemIntoWorld(context));}
 @SuppressWarnings("unchecked") public static InteractionResult client(Object rawStack,Object rawContext,Object rawAction){var context=(UseOnContext)rawContext;return use(context,()->((SourcePlacementAccess1075)rawStack).mite$placeNativeUse1075(context,(Function<UseOnContext,InteractionResult>)rawAction));}
 private static InteractionResult use(UseOnContext context,Supplier<InteractionResult> nativeUse){
  if(!SourceSkillWorld1820.planting(context))return InteractionResult.FAIL;
  var previous=active.get();var frame=admit(context);if(frame==null)active.remove();else active.set(frame);
  boolean entered=false;
  try{
   EnchantmentEffects.beforeUse(context);entered=true;
   var result=nativeUse.get();
   if(frame!=null){frame.accepted=result.consumesAction();if(frame.accepted){
    if(frame.placed){
     if(frame.level.isClientSide)SourcePlacementClient1075.placed();
     else if(!frame.player.isCreative()){
      if(frame.hunger!=0)frame.player.causeFoodExhaustion(frame.hunger);
      for(int i=0;i<frame.requests;i++){
       var consumed=frame.player.getMainHandItem();if(consumed.isEmpty())break;var copy=consumed.copy();
       SourceHeldConversion1037.convert(frame.player,InteractionHand.MAIN_HAND,ItemStack.EMPTY);
       if(consumed.isEmpty())ForgeEventFactory.onPlayerDestroyItem(frame.player,copy,InteractionHand.MAIN_HAND);
      }
      if(!ItemStack.matches(frame.before,frame.player.getMainHandItem()))frame.player.awardStat(Stats.ITEM_USED.get(frame.item));
     }
     if(frame.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
       && frame.item instanceof SourceSacredSandstone.Item && frame.snapshots!=null)
      SourceWonderTournament.acceptedPlacement(serverPlayer,frame.snapshots);
    }else for(var stat:frame.statistics)frame.player.awardStat(stat);
   }}return result;
  }catch(Throwable error){if(frame!=null&&!frame.accepted)rollback(frame);throw error;}
  finally{
   if(frame!=null){frame.level.captureBlockSnapshots=frame.captureBefore;frame.level.restoringBlockSnapshots=frame.restoreBefore;}
   try{if(entered)EnchantmentEffects.afterUse(InteractionResult.PASS);}finally{if(previous==null)active.remove();else active.set(previous);}
  }
 }
 /** Only the single audited BlockItem.place shrink site calls this. */
 public static void consume(Object rawStack,int amount){var stack=(ItemStack)rawStack;var frame=active.get();if(frame!=null&&frame.input==stack&&amount==1){frame.requests++;return;}stack.shrink(amount);}
 public static InteractionResult success(boolean client,Object rawContext){var context=(UseOnContext)rawContext;var frame=active.get();if(frame!=null&&frame.level==context.getLevel()&&frame.input==context.getItemInHand())frame.placed=true;return InteractionResult.sidedSuccess(client);}
 public static void awardUse(Object rawPlayer,Object rawStat){var player=(Player)rawPlayer;var stat=(Stat<?>)rawStat;var frame=active.get();if(frame!=null&&frame.player==player){frame.statistics.add(stat);return;}player.awardStat(stat);}
 public static boolean deferHunger(Player player,float amount){var frame=active.get();if(frame==null||frame.player!=player||!frame.placed||frame.level.isClientSide)return false;frame.hunger+=amount;return true;}
 @SuppressWarnings("unchecked") public static void snapshots(Object rawSnapshots){var frame=active.get();if(frame==null)return;var all=(List<BlockSnapshot>)rawSnapshots;frame.snapshots=new ArrayList<>(all.subList(Math.min(frame.snapshotsBefore,all.size()),all.size()));}
 private static void rollback(Frame frame){
  if(frame.level.isClientSide)return;
  frame.level.captureBlockSnapshots=false;frame.level.restoringBlockSnapshots=true;
  try{if(frame.snapshots!=null)for(int i=frame.snapshots.size()-1;i>=0;i--)frame.snapshots.get(i).restore(true,false);else{var all=frame.level.capturedBlockSnapshots;for(int i=all.size()-1;i>=frame.snapshotsBefore;i--){all.get(i).restore(true,false);all.remove(i);}}if(frame.player.getMainHandItem()==frame.input){frame.input.setCount(frame.before.getCount());frame.input.setTag(frame.before.hasTag()?frame.before.getTag().copy():null);}}
  finally{frame.level.captureBlockSnapshots=frame.captureBefore;frame.level.restoringBlockSnapshots=frame.restoreBefore;}
 }
 private SourcePlacement1075(){}
}

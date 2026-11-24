package dev.koikaster.mite;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

/** xm gives ingestion priority without Ctrl; its original throw remains the fallback. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceEggUse487 {
 private record Pending(InteractionHand hand,boolean ctrl,int tick){}
 private static final Map<ServerPlayer,Pending> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
 private static SimpleChannel channel;
 public static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(Control.class,id,NetworkDirection.PLAY_TO_SERVER).encoder(Control::write).decoder(Control::read).consumerMainThread(Control::handle).add();}
 public static void send(InteractionHand hand,boolean ctrl){channel.sendToServer(new Control(hand,ctrl));}
 public record Control(InteractionHand hand,boolean ctrl){
  public Control{Objects.requireNonNull(hand);}
  public static void write(Control value,FriendlyByteBuf buffer){buffer.writeEnum(value.hand);buffer.writeBoolean(value.ctrl);}
  public static Control read(FriendlyByteBuf buffer){return new Control(buffer.readEnum(InteractionHand.class),buffer.readBoolean());}
  static void handle(Control value,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null)control(player,value.hand,value.ctrl);context.get().setPacketHandled(true);}
 }
 public static void control(ServerPlayer player,InteractionHand hand,boolean ctrl){
  if(hand==InteractionHand.MAIN_HAND&&!player.isRemoved()&&!player.isSpectator()&&player.getItemInHand(hand).is(Items.EGG))PENDING.put(player,new Pending(hand,ctrl,player.tickCount));
 }
 private static boolean take(Player player,InteractionHand hand){
  var value=player instanceof ServerPlayer server?PENDING.remove(server):null;
  return value!=null&&value.hand()==hand&&player.tickCount-value.tick()>=0&&player.tickCount-value.tick()<=20&&value.ctrl();
 }
 public static boolean canIngest(Player player,ItemStack held){
  if(player.isSpectator()||!held.is(Items.EGG)||player.hasEffect(MobEffects.CONFUSION)||player.isEyeInFluid(FluidTags.WATER)&&!player.canBreatheUnderwater())return false;
  var food=player.getFoodData();var diet=Diet.read(player);var energy=SurvivalFood.energy(held,player);
  if(!diet.needsNutrients(1)&&!LegacyFood.canIngest((int)food.getSaturationLevel(),food.getFoodLevel(),PlayerState.foodCap(player),energy.satiation(),energy.nutrition()))return false;
  return !CurseIngestion.forbidden(player,held);
 }
 /** One ingestion/Ctrl decision; source throw is the fallback. Non-egg callers retain the native entry. */
 public static InteractionResultHolder<ItemStack> use(Object rawLevel,Object rawPlayer,Object rawHand){
  var level=(Level)rawLevel;var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;var held=player.getItemInHand(hand);
  if(!held.is(Items.EGG))return null;
  if(hand==InteractionHand.OFF_HAND)return InteractionResultHolder.fail(held);
  boolean ctrl=level.isClientSide?ClientEggControl487.beforeUse(hand):take(player,hand);
  if(ctrl||!canIngest(player,held))return SourceEgg887.use(level,player,hand);
  player.startUsingItem(hand);return InteractionResultHolder.consume(held);
 }
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
  if(event.phase==TickEvent.Phase.START&&event.player.isUsingItem()&&event.player.getUseItem().is(Items.EGG)&&!canIngest(event.player,event.player.getUseItem()))event.player.stopUsingItem();
 }
 private SourceEggUse487(){}
}

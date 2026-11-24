package dev.koikaster.mite;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
/** bcw connection-owned random and ud/bdc stock state. Source tool/vessel policies use the same snapshot and scorer on both sides; unresolved catalogue aliases remain staged. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientRootItems1001 {
 private static final Map<ClientPacketListener,State> states=new WeakHashMap<>();
 static final class State {final Random effectRandom=new Random();ItemStack previous=ItemStack.EMPTY;long reset,token;boolean pending;Object level;UUID player;}
 static State state(){var m=Minecraft.getInstance();var c=m.getConnection();if(c==null)return null;return states.computeIfAbsent(c,k->new State());}
 public static void broken(SourceRootTransport1001.Broken p){
  var m=Minecraft.getInstance();var level=m.level;if(level==null||(p.kind()!=1&&p.kind()!=2)||p.slot() < -1||p.slot()>8)return;
  var actor=level.getEntity(p.entity());if(!(actor instanceof net.minecraft.world.entity.player.Player player)||!actor.getUUID().equals(p.uuid()))return;
  var s=state();if(s==null)return;var item=p.kind()==1?Items.STICK:Items.BONE;
  SourceBrokenItemRules996.render(new SourceBrokenItemRules996.Host(){
   public boolean clientWorld(){return true;}public double x(){return actor.getX();}public double legacyY(){return actor.getY()+(double)1.62f;}public double z(){return actor.getZ();}public float pitch(){return actor.getXRot();}public float yaw(){return actor.getYRot();}public float legacyEyeOffset(){return actor.isShiftKeyDown()?-.1185f:.12f;}
   public void particle(double x,double y,double z,double vx,double vy,double vz){ClientSourceIcon1001.spawn(level,item,x,y,z,vx,vy,vz);}
  },s.effectRandom,Math::random);
  level.playLocalSound(actor.getX(),SourceBrokenItemRules996.soundY(actor.getY(),1.8f),actor.getZ(),SoundEvents.ITEM_BREAK,SoundSource.PLAYERS,.8f,SourceBrokenItemRules996.soundPitch(level.random.nextFloat()),false);
 }
 public static void notice(SourceRootTransport1001.Notice p){
  var m=Minecraft.getInstance();var s=state();if(s==null||m.player==null||p.slot()<0||p.slot()>8)return;
  var item=p.previous();if(item.isEmpty())return;
  s.previous=item;s.token=p.token();s.reset=System.currentTimeMillis()+3000;s.level=m.level;s.player=m.player.getUUID();if(!SourceStockIdentity1026.damageable(item))s.pending=true;
 }
 private static boolean window(State s){return SourceRestockRules997.inventoryWindow(s.reset,System.currentTimeMillis());}
 /** Called only on the native right-use fallthrough; successful/cancelled interactions return earlier. */
 public static void afterNoUse1001(){var m=Minecraft.getInstance();var s=state();if(s!=null&&m.player!=null&&m.player.getMainHandItem().isEmpty()&&!s.previous.isEmpty()&&window(s))s.pending=true;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var m=Minecraft.getInstance();var s=state();if(s==null||m.player==null||m.gameMode==null)return;
  if(s.level!=m.level||!m.player.getUUID().equals(s.player)){s.previous=ItemStack.EMPTY;s.pending=false;return;}
  var inventory=m.player.getInventory();var held=m.player.getMainHandItem();
  // A server notice can arrive before its empty-slot update. Inventory restock
  // also has the source250ms delay; neither wait may consume the pending intent.
  if(s.pending&&(!held.isEmpty()||!window(s)&&nextSlot(s,inventory)>=9)){
   if(System.currentTimeMillis()>=s.reset)s.pending=false;return;
  }
  if(!held.isEmpty()&&SourceStockIdentity1026.sameItem(held,s.previous))s.pending=false;
  if(!s.pending||m.screen!=null||m.player.containerMenu!=m.player.inventoryMenu)return;
  SourceRestockRules997.attempt(new SourceRestockRules997.Switch(){
   int oldSelected=inventory.selected;
   public boolean client(){return true;}public void wrongSide(){}public boolean previousKnown(){return !s.previous.isEmpty();}public void missingPrevious(){}public boolean immediate(){return true;}public void pending(boolean v){s.pending=v;}
   public int choose(){return SourceRestockRules997.choose(new SourceRestockRules997.Scores(){public boolean previousKnown(){return !s.previous.isEmpty();}public int selected(){return inventory.selected;}public int similarity(int slot){var stack=inventory.getItem(slot);return SourceStockIdentity1026.score(s.previous,stack);}});}
   public int selected(){return inventory.selected;}public void selectHotbar(int slot){inventory.selected=slot;}public void equipChanged(){m.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);}public void sendSelection(){((SourceRestockController1001)m.gameMode).mite$sendRestockSelection1001();}
   public boolean inventoryWindow(){return window(s);}public boolean selectedOccupied(){return !inventory.getSelected().isEmpty();}public void moveInventoryToSelected(int slot){var stack=inventory.getItem(slot);inventory.setItem(oldSelected,stack);inventory.setItem(slot,ItemStack.EMPTY);}public void sendMovedSlots(int slot){SourceRootTransport1001.requestMove(s.token,slot,oldSelected);}public void clearLastUsed(){s.previous=ItemStack.EMPTY;}public void noteLastUsed(){throw new AssertionError("Immediate stock entry");}
  });
 }
 public static void toolBroken(SourceToolBreakTransport1058.Packet p){
  var m=Minecraft.getInstance();var level=m.level;if(level==null||(p.item()==null||p.item()==Items.AIR)||p.slot() < -1||p.slot()>8)return;
  var actor=level.getEntity(p.entity());if(!(actor instanceof net.minecraft.world.entity.player.Player player)||!actor.getUUID().equals(p.uuid()))return;
  var s=state();if(s==null)return;var item=p.item();
  SourceBrokenItemRules996.render(new SourceBrokenItemRules996.Host(){
   public boolean clientWorld(){return true;}public double x(){return actor.getX();}public double legacyY(){return actor.getY()+(double)1.62f;}public double z(){return actor.getZ();}public float pitch(){return actor.getXRot();}public float yaw(){return actor.getYRot();}public float legacyEyeOffset(){return actor.isShiftKeyDown()?-.1185f:.12f;}
   public void particle(double x,double y,double z,double vx,double vy,double vz){ClientSourceIcon1001.spawn(level,item,x,y,z,vx,vy,vz);}
  },s.effectRandom,Math::random);
  level.playLocalSound(actor.getX(),SourceBrokenItemRules996.soundY(actor.getY(),1.8f),actor.getZ(),SoundEvents.ITEM_BREAK,SoundSource.PLAYERS,.8f,SourceBrokenItemRules996.soundPitch(level.random.nextFloat()),false);
 }
 private static int nextSlot(State state,net.minecraft.world.entity.player.Inventory inventory){return SourceRestockRules997.choose(new SourceRestockRules997.Scores(){public boolean previousKnown(){return !state.previous.isEmpty();}public int selected(){return inventory.selected;}public int similarity(int slot){return SourceStockIdentity1026.score(state.previous,inventory.getItem(slot));}});}
 private ClientRootItems1001(){}
}

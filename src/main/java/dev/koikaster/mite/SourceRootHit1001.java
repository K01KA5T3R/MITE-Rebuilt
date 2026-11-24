package dev.koikaster.mite;
import com.mojang.logging.LogUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
/** Root Item.hurtEnemy only; subclass tool implementations retain their own owner. */
public final class SourceRootHit1001 {
 public static SourceRootItemHitRules996.Kind kind(Item item){return item==Items.STICK?SourceRootItemHitRules996.Kind.STICK:item==Items.BONE?SourceRootItemHitRules996.Kind.BONE:SourceRootItemHitRules996.Kind.OTHER;}
 public static boolean hurtRoot1001(Object rawItem,Object rawStack,Object rawTarget,Object rawAttacker){
  Item item=(Item)rawItem;ItemStack supplied=(ItemStack)rawStack;LivingEntity target=(LivingEntity)rawTarget,attacker=(LivingEntity)rawAttacker;
  return SourceRootItemHitRules996.hit(kind(item),new SourceRootItemHitRules996.Hit(){
   Player player(){return (Player)attacker;}
   public boolean targetOnClient(){return target.level().isClientSide;}
   public void unexpectedClientCall(){LogUtils.getLogger().error("ItemTool.hitEntity: called on client?");}
   public boolean attackerIsPlayer(){return attacker instanceof Player;}
   public boolean creative(){return player().getAbilities().instabuild;}
   public boolean sameAsHeldStack(){return player().getMainHandItem()==supplied;}
   public int selectedSlot(){return player().getInventory().selected;}
   public void breakingEffect(int slot){if(player().level().isClientSide){LogUtils.getLogger().error("entityFX: only valid on server (item_breaking)");return;}SourceRootTransport1001.broken(player(),item,slot);}
   public void consumeOneHeld(){consume(player());}
  },LegacyItemRandom749.RANDOM::nextInt);
 }
 private static void consume(Player player){
  ItemStack held=player.getMainHandItem();
  SourceHeldConsumeRules997.consume(new SourceHeldConsumeRules997.Host(){
   public boolean client(){return player.level().isClientSide;}
   public void unexpectedClientCall(){LogUtils.getLogger().error("convertOneItem: only meant to be called on server");}
   public int count(){return held.getCount();}
   public int slot(){return player.getInventory().selected;}
   public int selected(){return player.getInventory().selected;}
   public void restockNotification(){SourceRootTransport1001.beforeLastHeld(player,held);}
   public int decrement(){held.shrink(1);return held.getCount();}
   public void clearSelected(){player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
   public void addNullOutput(){}
  });
 }
 private SourceRootHit1001(){}
}

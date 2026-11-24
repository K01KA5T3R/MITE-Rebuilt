package dev.koikaster.mite;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 ItemCoin/RecipesMITE/wd: XP is stored while minting and reclaimed with the metal nugget. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicCoins {
 private static final Map<Container,WeakReference<Player>> OWNERS=Collections.synchronizedMap(new WeakHashMap<>());
 public static void bind(CraftingContainer grid,Player player){OWNERS.put(grid,new WeakReference<>(player));}
 public static Player owner(Container grid){var reference=OWNERS.get(grid);return reference==null?null:reference.get();}
 public static int value(String material){try{return LegacyProduction.coinValue(LegacyProduction.Metal.valueOf(material.toUpperCase(Locale.ROOT)));}catch(IllegalArgumentException ignored){return 0;}}
 public static boolean enabled(Item item){return !(item instanceof SpecialItems.Coin coin)||coin.value>0;}
 public static int mintCost(ItemStack output){return output.getItem() instanceof SpecialItems.Coin coin?coin.value*output.getCount():0;}
 public static boolean affordable(Player player,ItemStack output){return !(output.getItem() instanceof SpecialItems.Coin)||PlayerState.data(player).getInt("Xp")>=mintCost(output);}
 public static ItemStack output(Player player,CraftingContainer grid){return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level()).map(r->r.assemble(grid,player.level().registryAccess())).orElse(ItemStack.EMPTY);}
 /** Called before native result consumption, while ingredient identities still exist after zero-count quick transfer. */
 public static void settle(Player player,CraftingContainer grid){
  if(!(player instanceof ServerPlayer server))return;var output=output(player,grid);int cost=mintCost(output),refund=0;
  for(var stack:grid.getItems())if(stack.getItem() instanceof SpecialItems.Coin coin)refund+=coin.value;
  if(cost>0&&PlayerState.data(player).getInt("Xp")<cost)throw new IllegalStateException("Coin result taken without its required stored experience");
  if(refund!=cost)PlayerState.addXp(server,refund-cost);
 }
 /** Old non-source coins retain their metal, but never introduce experience created by obsolete free recipes. */
 @SubscribeEvent public static void legacyCoins(TickEvent.PlayerTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
  for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(!(stack.getItem() instanceof SpecialItems.Coin coin)||coin.value>0)continue;var material=coin.material;var replacement=new ItemStack(Content.nugget(material),stack.getCount());player.getInventory().setItem(i,ItemStack.EMPTY);if(!player.getInventory().add(replacement))player.drop(replacement,false);}
 }
 private ClassicCoins(){}
}

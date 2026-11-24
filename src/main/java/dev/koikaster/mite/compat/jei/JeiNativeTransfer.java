package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.*;
import java.util.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Uses only vanilla inventory-click packets. No JEI server, synthetic recipe
 * IDs, custom grant packet, ResultSlot click, or client ItemStack mutation. */
public final class JeiNativeTransfer {
 public record Need(int slot,List<ItemStack> choices,int count){}
 private record Move(int from,int to,int count){}
 private static boolean same(ItemStack wanted,ItemStack held){
  if(!wanted.is(held.getItem()))return false;
  if(ClassicVessels.handled(wanted))return ClassicVessels.fluid(wanted).equals(ClassicVessels.fluid(held));
  if(wanted.getItem() instanceof PotionItem)return JeiBrewing1420.matchesPotion(wanted,held);
  return !(wanted.getItem() instanceof SourceRunestoneItem)||SourceRunestoneItem.rune(wanted)==SourceRunestoneItem.rune(held);
 }
 private static List<Move> plan(AbstractContainerMenu menu,List<Need> needs,int invStart,int invCount,int batches,boolean crafting){
  int end=invStart+invCount;
  int[] available=new int[menu.slots.size()];for(int i=invStart;i<end;i++)available[i]=menu.getSlot(i).getItem().getCount();
  var result=new ArrayList<Move>();
  for(var need:needs){
   var moves=planSlot(menu,need,invStart,end,available,need.count()*batches);
   if(moves==null)return crafting?matchCraftInputs(menu,needs,invStart,end,batches):null;
   for(var move:moves)available[move.from()]-=move.count();
   result.addAll(moves);
  }return result;
 }
 private static List<Move> planSlot(AbstractContainerMenu menu,Need need,int start,int end,int[] available,int count){
  var target=menu.getSlot(need.slot());
  for(int first=start;first<end;first++){
   var material=menu.getSlot(first).getItem();
   if(available[first]==0||!accepts(target,need,material,count))continue;
   var moves=new ArrayList<Move>();
   int remaining=count;
   for(int source=first;source<end&&remaining>0;source++){
    if(available[source]==0||!ItemStack.isSameItemSameTags(material,menu.getSlot(source).getItem()))continue;
    int moved=Math.min(remaining,available[source]);
    moves.add(new Move(source,need.slot(),moved));
    remaining-=moved;
   }
   if(remaining==0)return moves;
  }
  return null;
 }
 private static boolean accepts(Slot target,Need need,ItemStack material,int count){
  return count<=target.getMaxStackSize(material)&&target.mayPlace(material)
      &&need.choices().stream().anyMatch(choice->same(choice,material));
 }

 private static final class CraftMaterial {
  final ItemStack sample;
  final List<Integer> sources = new ArrayList<>();
  int count;

  CraftMaterial(ItemStack sample) {
   this.sample = sample;
  }
 }

 private static List<Move> matchCraftInputs(AbstractContainerMenu menu, List<Need> needs,
        int start, int end, int batches) {
  var materials = new ArrayList<CraftMaterial>();
  int[] available = new int[menu.slots.size()];
  for (int source = start; source < end; source++) {
   var stack = menu.getSlot(source).getItem();
   if (stack.isEmpty()) continue;
   var material = materials.stream()
       .filter(group -> ItemStack.isSameItemSameTags(group.sample, stack))
       .findFirst().orElse(null);
   if (material == null) {
    material = new CraftMaterial(stack);
    materials.add(material);
   }
   material.sources.add(source);
   material.count += stack.getCount();
   available[source] = stack.getCount();
  }

  int[] capacity = materials.stream().mapToInt(group -> group.count / batches).toArray();
  if (Arrays.stream(capacity).sum() < needs.size()) return null;
  var choices = new ArrayList<List<Integer>>();
  for (var need : needs) {
   var eligible = new ArrayList<Integer>();
   var target = menu.getSlot(need.slot());
   for (int group = 0; group < materials.size(); group++) {
    if (capacity[group] != 0 && accepts(target, need, materials.get(group).sample, batches)) {
     eligible.add(group);
    }
   }
   if (eligible.isEmpty()) return null;
   choices.add(eligible);
  }

  int[] assigned = new int[needs.size()];
  Arrays.fill(assigned, -1);
  for (int need = 0; need < needs.size(); need++) {
   if (!reserveCraftMaterial(need, choices, capacity, assigned, new boolean[materials.size()])) return null;
  }

  var moves = new ArrayList<Move>();
  for (int need = 0; need < needs.size(); need++) {
   int remaining = batches;
   for (int source : materials.get(assigned[need]).sources) {
    int count = Math.min(available[source], remaining);
    if (count == 0) continue;
    moves.add(new Move(source, needs.get(need).slot(), count));
    available[source] -= count;
    remaining -= count;
    if (remaining == 0) break;
   }
  }
  return moves;
 }

 private static boolean reserveCraftMaterial(int need, List<List<Integer>> choices,
        int[] capacity, int[] assigned, boolean[] visited) {
  for (int material : choices.get(need)) {
   if (visited[material]) continue;
   visited[material] = true;
   if (capacity[material] > 0) {
    capacity[material]--;
    assigned[need] = material;
    return true;
   }
   // If another slot can use a different material, take its place in this group.
   for (int previous = 0; previous < assigned.length; previous++) {
    if (assigned[previous] == material && reserveCraftMaterial(previous, choices, capacity, assigned, visited)) {
     assigned[need] = material;
     return true;
    }
   }
  }
  return false;
 }

 private static List<Move> bestPlan(AbstractContainerMenu menu,List<Need> needs,int invStart,int invCount,boolean maximum,boolean crafting){
  for(int batches=maximum?64:1;batches>=1;batches--){
   var moves=plan(menu,needs,invStart,invCount,batches,crafting);
   if(moves!=null)return moves;
  }
  return null;
 }
 private static IRecipeTransferError admission(AbstractContainerMenu menu,List<Integer> inputSlots,IRecipeTransferHandlerHelper errors){
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.gameMode==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty())return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.clear_cursor"));
  if(inputSlots.stream().anyMatch(i->menu.getSlot(i).hasItem()))return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.clear_grid"));
  return null;
 }
 private static void clickMove(AbstractContainerMenu menu,int from,int to,int count){
  var mc=Minecraft.getInstance();
  mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
  for(int n=0;n<count;n++)mc.gameMode.handleInventoryMouseClick(menu.containerId,to,1,ClickType.PICKUP,mc.player);
  if(!menu.getCarried().isEmpty())mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
 }
 public static IRecipeTransferError fill(AbstractContainerMenu menu,List<Need> needs,int invStart,int invCount,List<Integer> inputSlots,boolean maximum,boolean transfer,IRecipeTransferHandlerHelper errors){
  return fill(menu,needs,invStart,invCount,inputSlots,maximum,transfer,errors,false);
 }
 private static IRecipeTransferError fill(AbstractContainerMenu menu,List<Need> needs,int invStart,int invCount,List<Integer> inputSlots,boolean maximum,boolean transfer,IRecipeTransferHandlerHelper errors,boolean crafting){
  var error=admission(menu,inputSlots,errors);if(error!=null)return error;
  var moves=bestPlan(menu,needs,invStart,invCount,maximum,crafting);
  if(moves==null)return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.missing"));
  if(transfer)for(var move:moves)clickMove(menu,move.from(),move.to(),move.count());
  return null;
 }
 private static List<Integer> slots(int start,int count){var result=new ArrayList<Integer>();for(int i=0;i<count;i++)result.add(start+i);return result;}
 public static final class Auto implements IRecipeTransferHandler<MithrilAutoWorkbench.Menu,CraftingRecipe>{
  private final IRecipeTransferHandlerHelper errors;
  public Auto(IRecipeTransferHandlerHelper errors){this.errors=errors;}
  public Class<? extends MithrilAutoWorkbench.Menu> getContainerClass(){return MithrilAutoWorkbench.Menu.class;}
  public Optional<MenuType<MithrilAutoWorkbench.Menu>> getMenuType(){return Optional.of(MithrilAutoWorkbench.MENU.get());}
  public RecipeType<CraftingRecipe> getRecipeType(){return RecipeTypes.CRAFTING;}
  public IRecipeTransferError transferRecipe(MithrilAutoWorkbench.Menu menu,CraftingRecipe recipe,IRecipeSlotsView view,Player player,boolean max,boolean transfer){
   if(recipe.getIngredients().isEmpty()||recipe instanceof JeiDisplayCraft display&&display.actual instanceof ClassicCraftRepair)return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.manual"));
   var needs=new ArrayList<Need>();int row=recipe instanceof ShapedRecipe shaped?shaped.getWidth():3;
   for(int i=0;i<recipe.getIngredients().size();i++){var ingredient=recipe.getIngredients().get(i);if(!ingredient.isEmpty())needs.add(new Need(MithrilAutoWorkbench.Menu.INPUT_START+i/row*3+i%row,Arrays.asList(ingredient.getItems()),1));}
   var inputs=slots(MithrilAutoWorkbench.Menu.INPUT_START,9);var error=admission(menu,inputs,errors);if(error!=null)return error;
   var moves=bestPlan(menu,needs,MithrilAutoWorkbench.Menu.PLAYER_START,36,max,true);
   if(moves==null)return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.missing"));
   if(!transfer)return null;
   var mc=Minecraft.getInstance();
   var sources=new LinkedHashMap<Integer,ArrayDeque<Integer>>();
   for(var move:moves){
    var queue=sources.computeIfAbsent(move.to(),ignored->new ArrayDeque<>());
    for(int i=0;i<move.count();i++)queue.addLast(move.from());
   }
   // Copy each template from a real held inventory stack. The server verifies
   // the vanilla cursor and never grants ghost items to the player.
   for(int i=0;i<9;i++)mc.gameMode.handleInventoryMouseClick(menu.containerId,i,1,ClickType.PICKUP,mc.player);
   for(var entry:sources.entrySet()){
    int from=entry.getValue().getFirst();
    mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
    mc.gameMode.handleInventoryMouseClick(menu.containerId,entry.getKey()-MithrilAutoWorkbench.Menu.INPUT_START,0,ClickType.PICKUP,mc.player);
    mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
   }
   // The machine accepts another item only after matching input slots have caught up.
   while(sources.values().stream().anyMatch(queue->!queue.isEmpty())){
    for(var entry:sources.entrySet())if(!entry.getValue().isEmpty())clickMove(menu,entry.getValue().removeFirst(),entry.getKey(),1);
   }
   return null;
  }
 }
 public static final class Craft<C extends RecipeBookMenu<CraftingContainer>> implements IRecipeTransferHandler<C,CraftingRecipe>{
  private final Class<C> kind;private final MenuType<C> type;private final int width,invStart;private final IRecipeTransferHandlerHelper errors;
  public Craft(Class<C> kind,MenuType<C> type,int width,int invStart,IRecipeTransferHandlerHelper errors){this.kind=kind;this.type=type;this.width=width;this.invStart=invStart;this.errors=errors;}
  public Class<? extends C> getContainerClass(){return kind;}public Optional<MenuType<C>> getMenuType(){return Optional.ofNullable(type);}public RecipeType<CraftingRecipe> getRecipeType(){return RecipeTypes.CRAFTING;}
  public IRecipeTransferError transferRecipe(C menu,CraftingRecipe recipe,IRecipeSlotsView view,Player player,boolean max,boolean transfer){
   if(recipe instanceof JeiDisplayCraft display&&display.actual instanceof ClassicCraftRepair)return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.repair_manual"));
   if(!recipe.canCraftInDimensions(width,width)||recipe.getIngredients().isEmpty())return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.manual"));
   var needs=new ArrayList<Need>();int rowWidth=recipe instanceof ShapedRecipe shaped?shaped.getWidth():width;
   for(int i=0;i<recipe.getIngredients().size();i++){var ingredient=recipe.getIngredients().get(i);if(ingredient.isEmpty())continue;int target=1+(i/rowWidth)*width+i%rowWidth;needs.add(new Need(target,Arrays.asList(ingredient.getItems()),1));}
   return fill(menu,needs,invStart,36,slots(1,width*width),max,transfer,errors,true);
  }
 }
 public static final class Heat implements IRecipeTransferHandler<SourceFurnaceMenu,JeiHeatRecipes.View>{
  private final JeiHeatRecipes.Device device;private final IRecipeTransferHandlerHelper errors;
  public Heat(JeiHeatRecipes.Device device,IRecipeTransferHandlerHelper errors){this.device=device;this.errors=errors;}
  public Class<? extends SourceFurnaceMenu> getContainerClass(){return SourceFurnaceMenu.class;}public Optional<MenuType<SourceFurnaceMenu>> getMenuType(){return Optional.of(Content.FURNACE_MENU.get());}public RecipeType<JeiHeatRecipes.View> getRecipeType(){return device.type();}
  public IRecipeTransferError transferRecipe(SourceFurnaceMenu menu,JeiHeatRecipes.View view,IRecipeSlotsView slots,Player player,boolean max,boolean transfer){
   if(!device.menu(menu))return errors.createUserErrorWithTooltip(Component.translatable("jei.mite_reforged.wrong_furnace"));
   return fill(menu,List.of(new Need(0,List.of(view.input()),view.input().getCount()),new Need(1,view.fuels(),1)),3,36,List.of(0,1),false,transfer,errors);
  }
 }
 private JeiNativeTransfer(){}
}

package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.*;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.*;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;

/** JEI alone discovers this class. Common/client mod entry points never link
 * against a JEI type, so an unmodified installation or dedicated server works. */
@JeiPlugin
public final class MiteJeiPlugin implements IModPlugin {
 public static volatile IJeiRuntime runtime;
 public static Map<String,List<JeiHeatRecipes.View>> heatRecipes=Map.of();
 public static List<CraftingRecipe> displayCrafts=List.of();
 static boolean rebuilt(){var level=Minecraft.getInstance().level;return level==null||WorldContentMode.rebuilt(level);}
 static boolean allowed(ItemStack stack){var level=Minecraft.getInstance().level;return level==null||RebuiltContent.allows(stack,WorldContentMode.settings(level));}
 public ResourceLocation getPluginUid(){return Content.id("jei");}
 public void registerItemSubtypes(ISubtypeRegistration registration){
  for(var item:BucketUse342.FILLED)registration.registerSubtypeInterpreter(item,(IIngredientSubtypeInterpreter<ItemStack>)(s,c)->BucketMaterial350.subtype(s));
  for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()){
   if(item instanceof SpecialItems.MetalBucket)registration.registerSubtypeInterpreter(item,(IIngredientSubtypeInterpreter<ItemStack>)(s,c)->ClassicVessels.fluid(s)+(s.hasTag()&&s.getTag().getBoolean("NativeVessel")?":native":""));
   if(item instanceof SourceRunestoneItem)registration.registerSubtypeInterpreter(item,(IIngredientSubtypeInterpreter<ItemStack>)(s,c)->Integer.toString(SourceRunestoneItem.rune(s)));
   if(item instanceof SourceReferencedBooks.Book)registration.registerSubtypeInterpreter(item,(IIngredientSubtypeInterpreter<ItemStack>)(s,c)->Integer.toString(SourceReferencedBooks.index(s)));
  }
 }
 public void registerExtraIngredients(IExtraIngredientRegistration registration){registration.addExtraItemStacks(JeiBrewing1420.variants());registration.addExtraItemStacks(JeiHeatRecipes.variants());registration.addExtraItemStacks(BucketMaterial350.variants());var books=new ArrayList<ItemStack>();for(int i=1;i<=9;i++)books.add(SourceReferencedBooks.generate(i));registration.addExtraItemStacks(books);}
 public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new JeiDrying1905.Category(registration.getJeiHelpers().getGuiHelper()));for(var device:JeiHeatRecipes.devices())registration.addRecipeCategories(new JeiHeatCategory(device,registration.getJeiHelpers().getGuiHelper()));registration.addRecipeCategories(new JeiBrewing1420.Category(registration.getJeiHelpers().getGuiHelper()));}
 public void registerRecipes(IRecipeRegistration registration){
  registration.addRecipes(JeiBrewing1420.TYPE,JeiBrewing1420.collect());
  if(rebuilt())registration.addRecipes(JeiDrying1905.TYPE,JeiDrying1905.collect());
  var level=Minecraft.getInstance().level;if(level==null)return;
  heatRecipes=JeiHeatRecipes.resolve(level);for(var device:JeiHeatRecipes.devices())registration.addRecipes(device.type(),heatRecipes.get(device.key()));
  displayCrafts=JeiDisplayCraft.collect(level);registration.addRecipes(RecipeTypes.CRAFTING,displayCrafts);
  if(rebuilt())registration.addItemStackInfo(new ItemStack(AdvancedEnchantContent.BLOCK.get()),Component.translatable("jei.mite_reforged.advanced_enchanting"));
  com.mojang.logging.LogUtils.getLogger().info("MITE_JEI_READY devices={} heat_views={} dynamic_crafts={}",heatRecipes.size(),heatRecipes.values().stream().mapToInt(List::size).sum(),displayCrafts.size());
 }
 public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration){
  registration.getCraftingCategory().addCategoryExtension(ShapedRecipe.class,JeiTimedCraftExtension::new);
  registration.getCraftingCategory().addCategoryExtension(ShapelessRecipe.class,JeiTimedCraftExtension::new);
 }
 public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){
  if(rebuilt())registration.addRecipeCatalyst(new ItemStack(FoodPreservation1905.ITEM.get()),JeiDrying1905.TYPE);
  registration.addRecipeCatalyst(new ItemStack(Items.BREWING_STAND),JeiBrewing1420.TYPE);
  for(var device:JeiHeatRecipes.devices())for(var catalyst:device.catalysts())registration.addRecipeCatalyst(catalyst,device.type());
  registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE),RecipeTypes.CRAFTING);
  var level=Minecraft.getInstance().level;
  if(level!=null&&WorldContentMode.r196(level))for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()){
   if(item instanceof BlockItem blockItem&&blockItem.getBlock() instanceof R196Workbench)
    registration.addRecipeCatalyst(new ItemStack(item),RecipeTypes.CRAFTING);
  }
  if(rebuilt())registration.addRecipeCatalyst(new ItemStack(MithrilAutoWorkbench.ITEM.get()),RecipeTypes.CRAFTING);
 }
 public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration){
  registration.addRecipeTransferHandler(new JeiBrewing1420.Transfer(registration.getTransferHelper()),JeiBrewing1420.TYPE);
  var helper=registration.getTransferHelper();
  registration.addRecipeTransferHandler(new JeiNativeTransfer.Auto(helper),RecipeTypes.CRAFTING);
  registration.addRecipeTransferHandler(new JeiNativeTransfer.Craft<>(TimedCrafting.Menu.class,Content.CRAFT_MENU.get(),3,10,helper),RecipeTypes.CRAFTING);
  registration.addRecipeTransferHandler(new JeiNativeTransfer.Craft<>(InventoryMenu.class,null,2,9,helper),RecipeTypes.CRAFTING);
  for(var device:JeiHeatRecipes.devices())if(device.mode()>=0)registration.addRecipeTransferHandler(new JeiNativeTransfer.Heat(device,helper),device.type());
 }
 public void registerGuiHandlers(IGuiHandlerRegistration registration){
  registration.addRecipeClickArea(SourceBrewingScreen1412.class,97,16,9,28,JeiBrewing1420.TYPE);
  registration.addRecipeClickArea(SourceCraftScreen.class,90,35,23,16,RecipeTypes.CRAFTING);
  registration.addGuiContainerHandler(SourceCraftScreen.class,new IGuiContainerHandler<SourceCraftScreen>(){
   public List<Rect2i> getGuiExtraAreas(SourceCraftScreen screen){return screen.getRecipeBookComponent().isVisible()?List.of(new Rect2i(screen.getGuiLeft()-147,screen.getGuiTop(),147,166)):List.of();}
  });
  var types=JeiHeatRecipes.devices().stream().map(JeiHeatRecipes.Device::type).toArray(mezz.jei.api.recipe.RecipeType<?>[]::new);
  registration.addRecipeClickArea(ClientSetup.SourceFurnaceScreen.class,79,34,24,17,types);
 }
 public void onRuntimeAvailable(IJeiRuntime value){
  runtime=value;var ingredients=value.getIngredientManager();
  var hidden = ingredients.getAllItemStacks().stream()
      .filter(stack -> JeiHeatRecipes.hidden(stack) || !allowed(stack))
      .toList();
  if (!hidden.isEmpty()) ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
  // Vanilla JEI pages use modern timing/fuels/repair; our categories own these devices.
  var recipes=value.getRecipeManager();recipes.hideRecipeCategory(RecipeTypes.BREWING);recipes.hideRecipeCategory(RecipeTypes.SMELTING);recipes.hideRecipeCategory(RecipeTypes.SMOKING);recipes.hideRecipeCategory(RecipeTypes.BLASTING);recipes.hideRecipeCategory(RecipeTypes.CAMPFIRE_COOKING);recipes.hideRecipeCategory(RecipeTypes.ANVIL);
  if(!rebuilt())recipes.hideRecipeCategory(JeiDrying1905.TYPE);
  var level=Minecraft.getInstance().level;if(level!=null)recipes.hideRecipes(RecipeTypes.CRAFTING,level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream().filter(r->ModernRecipePolicy.removed(r,level.registryAccess())).toList());
 }
 public void onRuntimeUnavailable(){JeiBrewing1420.views=List.of();runtime=null;heatRecipes=Map.of();displayCrafts=List.of();}
}

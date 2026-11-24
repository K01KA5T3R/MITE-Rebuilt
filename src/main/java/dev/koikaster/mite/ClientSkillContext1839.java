package dev.koikaster.mite;
import java.util.*;import java.io.*;import java.nio.charset.StandardCharsets;import com.google.gson.*;
import net.minecraft.ChatFormatting;import net.minecraft.client.Minecraft;import net.minecraft.client.gui.screens.Screen;import net.minecraft.client.gui.screens.inventory.InventoryScreen;import net.minecraft.network.chat.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.inventory.*;import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.crafting.*;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.ScreenEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
/** Original awy admission warning and Shift-only axv avatar professions. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientSkillContext1839 {
 private record Skill(int bit,String key){}private static final List<Skill> SKILLS;
 static {var list=new ArrayList<Skill>();try(var in=ClientSkillContext1839.class.getResourceAsStream("/data/mite_reforged/source_skill_names1791.json")){if(in==null)throw new IOException("Missing source skill names");for(var raw:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){var row=raw.getAsJsonObject();list.add(new Skill(row.get("bit").getAsInt(),row.get("key").getAsString()));}}catch(IOException e){throw new ExceptionInInitializerError(e);}SKILLS=List.copyOf(list);}
 public static Component professions(int bits){MutableComponent text=null;for(var skill:SKILLS)if((bits&skill.bit)!=0){if(text==null)text=Component.empty();else text.append(" / ");text.append(Component.translatable(skill.key+".profession"));}return text;}
 private static Component alternatives(int[] sets){if(sets==null)return null;MutableComponent text=null;for(int mask:sets){var names=professions(mask);if(names==null)continue;if(text==null)text=Component.empty();else text.append(Component.translatable("gui.mite_reforged.skill_or"));text.append(names);}return text;}
 public static Component missingSkillNames(Player player,AbstractContainerMenu menu,ItemStack output){
  if(!SourceSkills1685.enabled(player.level()))return null;
  CraftingContainer grid;
  if(menu instanceof TimedCrafting.Menu table)grid=table.grid;
  else if(menu instanceof InventoryMenu&&menu.getSlot(1).container instanceof CraftingContainer inventory)grid=inventory;
  else return null;
  var recipe=player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level());
  if(recipe.isEmpty())return null;
  var decision=SourceSkillFacts1710.crafting(recipe.get(),grid,output);
  if(!decision.known()||SourceSkillRules1685.alternatives(SourceSkills1685.bits(player),decision.skillsets()))return null;
  return alternatives(decision.skillsets());
 }
 public static boolean appendRequirement(Player player,AbstractContainerMenu menu,ItemStack output,List<Component> lines){
  if(!SourceSkills1685.enabled(player.level()))return false;
  CraftingContainer grid;
  if(menu instanceof TimedCrafting.Menu table)grid=table.grid;
  else if(menu instanceof InventoryMenu&&menu.getSlot(1).container instanceof CraftingContainer inventory)grid=inventory;
  else return false;
  var recipe=player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level()).orElse(null);
  if(recipe==null||SourceSkillCrafting1710.allowed(player,recipe,grid,output))return false;
  var names=missingSkillNames(player,menu,output);
  if(names==null)return false;
  lines.add(Component.translatable("gui.mite_reforged.requires_skill",names).withStyle(ChatFormatting.DARK_GRAY));
  return true;
 }
 @SubscribeEvent public static void avatar(ScreenEvent.Render.Post event){if(!(event.getScreen() instanceof InventoryScreen screen)||!Screen.hasShiftDown())return;var mc=Minecraft.getInstance();if(mc.player==null||screen.getMenu()!=mc.player.containerMenu||!SourceSkills1685.enabled(mc.player.level()))return;int left=screen.getGuiLeft()+26,top=screen.getGuiTop()+8,x=event.getMouseX(),y=event.getMouseY();if(x<left||x>left+51||y<top||y>top+69)return;var names=professions(SourceSkills1685.bits(mc.player));if(names!=null)event.getGuiGraphics().renderTooltip(mc.font,names,x,y);}
 private ClientSkillContext1839(){}
}

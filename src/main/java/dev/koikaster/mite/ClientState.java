package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
public final class ClientState {
 public static void receive(CompoundTag data){var p=Minecraft.getInstance().player;if(p!=null){p.getPersistentData().put(MiteReforged.ID,data);if(data.contains("Xp")){int l=PlayerState.level(p);p.experienceLevel=l;p.totalExperience=data.getInt("Xp");p.experienceProgress=Rules.levelProgress(p.totalExperience,l);}SourceSkills1685.receive(p,data);if(data.contains("SourceRandomSeed925"))OriginalRandomTables.initialize(data.getLong("SourceRandomSeed925"));if(data.contains("FoodNutrition")){p.getFoodData().setFoodLevel(data.getInt("FoodNutrition"));p.getFoodData().setSaturation(data.getFloat("FoodSatiation"));}}IteConfig.readSync(data);}
 private ClientState(){}
 public static void crafting(String key,int progress,int total){var player=Minecraft.getInstance().player;if(player!=null){var data=PlayerState.data(player);data.putString("InventoryCraftKey",key);data.putInt("InventoryCraftProgress",Math.max(0,progress));data.putInt("InventoryCraftTotal",Math.max(1,total));}}
}

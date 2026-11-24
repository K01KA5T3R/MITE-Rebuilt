package dev.koikaster.mite;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
/** R195 wormCooked: original stack16, food1/1, protein, no sugar or XP. */
public final class SourceCookedWorm461 {
 public static final RegistryObject<Item> ITEM=Content.item("cooked_worm",()->new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(1).saturationMod(.5f).meat().build())));
 public static final RegistryObject<SoundEvent> SIZZLE=SourceCreatureSounds.SOUNDS.register("food_sizzle",()->SoundEvent.createVariableRangeEvent(Content.id("food_sizzle")));
 public static void register(){}
 private SourceCookedWorm461(){}
}

package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.RegistryObject;
public final class TorchBowContent {
 public static final RegistryObject<Item> BOW=Content.item("torch_bow",()->new TorchBow(new Item.Properties().durability(ClassicToolCatalogue.bowDurability("wood"))));
 public static final RegistryObject<EntityType<TorchBowShot>> SHOT=Mobs.ENTITIES.register("torch_shot",()->EntityType.Builder.<TorchBowShot>of(TorchBowShot::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(1).fireImmune().build(MiteReforged.ID+":torch_shot"));
 public static final RegistryObject<RecipeSerializer<TorchBowRecipe>> RECIPE=Content.SERIALIZERS.register("torch_bow",TorchBowRecipe.Serializer::new);
 public static void register(){}
 private TorchBowContent(){}
}

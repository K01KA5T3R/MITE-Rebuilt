package dev.koikaster.mite;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

/** Nine source hook identities; the historical native ID remains the iron peer. */
public final class SourceCarrotSticks {
 public static final List<String> MATERIALS=List.of("flint","obsidian","copper","silver","gold","iron","mithril","adamantium","ancient_metal");
 public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
 public static final RegistryObject<RecipeSerializer<?>> RECIPE=Content.SERIALIZERS.register("carrot_stick",SourceCarrotStickRecipe.Serializer::new);
 public static void register(){for(String m:MATERIALS)if(!m.equals("iron"))ITEMS.put(m,Content.item(m+"_carrot_on_a_stick",()->new Stick(m)));}
 public static boolean handles(ItemStack stack){return stack.is(Items.CARROT_ON_A_STICK)||stack.getItem() instanceof Stick;}
 public static String material(ItemStack stack){return stack.getItem() instanceof Stick item?item.material:stack.is(Items.CARROT_ON_A_STICK)?"iron":null;}
 public static Item item(String material){return material.equals("iron")?Items.CARROT_ON_A_STICK:ITEMS.get(material).get();}
 public static Item rod(String material){return material.equals("iron")?Items.FISHING_ROD:Content.get(material+"_fishing_rod");}
 public static ItemStack returnedRod(ItemStack original){var output=new ItemStack(rod(Objects.requireNonNull(material(original))));if(original.hasTag())output.setTag(original.getTag().copy());output.setDamageValue(0);return output;}
 static void damage(Player player,InteractionHand hand,int amount){var stack=player.getItemInHand(hand);if(!handles(stack))return;if(hand==InteractionHand.MAIN_HAND){SourceHeldBreak1058.pigNibble(player,stack,amount);return;}var rod=returnedRod(stack);stack.hurtAndBreak(amount,player,p->{});if(stack.isEmpty())player.setItemInHand(hand,rod);}
 public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  boolean accepted=SourcePigRidingRules.use(new SourcePigRidingRules.UseHost(){
   public boolean ridingPig(){return player.isPassenger()&&player.getVehicle() instanceof SourcePig;}
   public int remaining(){var stack=player.getItemInHand(hand);return stack.getMaxDamage()-stack.getDamageValue();}public boolean server(){return !level.isClientSide;}
   public boolean boostAllowed(){return ((SourcePig)player.getVehicle()).control().canBoost();}public void boost(){((SourcePig)player.getVehicle()).control().boost();}public void damage(int amount){SourceCarrotSticks.damage(player,hand,amount);}
  });return accepted?InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide):InteractionResultHolder.pass(player.getItemInHand(hand));
 }
 public static final class Stick extends Item {
  public final String material;
  public Stick(String material){super(new Item.Properties().durability(25));this.material=material;}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return SourceCarrotSticks.use(level,player,hand);}
 }
 private SourceCarrotSticks(){}
}

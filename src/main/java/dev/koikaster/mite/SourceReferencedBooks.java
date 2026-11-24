package dev.koikaster.mite;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/** ItemReferencedBook and jv's per-player index list, with original immutable pages. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceReferencedBooks {
 public static final RegistryObject<Item> BOOK=Content.item("referenced_book",Book::new);
 private static final JsonArray CONTENTS=contents();
 private static JsonArray contents(){try(var in=SourceReferencedBooks.class.getResourceAsStream("/assets/mite_reforged/referenced_books.json")){if(in==null)throw new IOException("Missing original book contents");try(var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonArray();}}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 public static ItemStack generate(int index){
  if(index<1||index>9)return ItemStack.EMPTY;
  var row=CONTENTS.get(index-1).getAsJsonObject();var stack=new ItemStack(BOOK.get());var tag=stack.getOrCreateTag();
  tag.putInt("reference_index",index);tag.putInt("xp_reward",row.get("xp_reward").getAsInt());tag.putString("author",row.get("author").getAsString());tag.putString("title",row.get("title").getAsString());tag.putString("flavor_text",row.get("flavor_text").getAsString());
  var pages=new ListTag();for(var page:row.getAsJsonArray("pages"))pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page.getAsString()))));tag.put("pages",pages);tag.putBoolean("resolved",true);return stack;
 }
 public static int index(ItemStack stack){return !stack.isEmpty()&&stack.getItem() instanceof Book&&stack.hasTag()?stack.getTag().getInt("reference_index"):0;}
 public static void read(ServerPlayer player,ItemStack stack){
  int index=index(stack);if(index<=0)return;var data=PlayerState.data(player);var read=Arrays.stream(data.getIntArray("referenced_books_read")).boxed().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  if(read.contains(index))return;read.add(index);data.putIntArray("referenced_books_read",read);int reward=stack.getTag().getInt("xp_reward");if(reward!=0)PlayerState.addXp(player,reward);
  for(int i=1;i<=9;i++)if(!read.contains(i))return;award(player,"enlightenment");
 }
 static void award(ServerPlayer player,String id){SourceAchievementService570.legacyCallback(player,id);var advancement=player.server.getAdvancements().getAdvancement(Content.id(id));if(advancement!=null)for(String criterion:player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria())player.getAdvancements().award(advancement,criterion);}
 @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){if(event.getEntity() instanceof ServerPlayer player&&event.getCrafting().is(Items.BOOKSHELF)){var state=SourceBookWorld.get(player.serverLevel());synchronized(state){state.bookcase=true;state.setDirty();}award(player,"bookcase");}}
 public static final class Book extends WrittenBookItem {
  Book(){super(new Item.Properties().stacksTo(1));}
  @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player instanceof ServerPlayer server)read(server,stack);if(world.isClientSide)ClientReferencedBooks.open(stack);return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);}
  @Override public Component getName(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("title",Tag.TAG_STRING)?Component.literal(stack.getTag().getString("title")):super.getName(stack);}
  @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag flag){if(stack.hasTag()){var tag=stack.getTag();if(tag.contains("author"))lines.add(Component.translatable("book.byAuthor",tag.getString("author")).withStyle(net.minecraft.ChatFormatting.GRAY));}}
  @Override public void onDestroyed(ItemEntity entity){if(!entity.level().isClientSide){int index=index(entity.getItem());if(index>0)SourceBookWorld.get((net.minecraft.server.level.ServerLevel)entity.level()).release(index);}super.onDestroyed(entity);}
 }
 public static void register(){}
 private SourceReferencedBooks(){}
}

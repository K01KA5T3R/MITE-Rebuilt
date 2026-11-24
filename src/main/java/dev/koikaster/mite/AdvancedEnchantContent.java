package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;

public final class AdvancedEnchantContent {
 public static final RegistryObject<Block> BLOCK=Content.block("advanced_enchanting_table",()->new Table(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(6,1200)));
 public static final RegistryObject<BlockEntityType<Book>> ENTITY=Content.BLOCK_ENTITIES.register("advanced_enchanting_table",()->BlockEntityType.Builder.of(Book::new,BLOCK.get()).build(null));
 public static final RegistryObject<MenuType<AdvancedEnchantMenu>> MENU=Content.MENUS.register("advanced_enchanting_table",()->net.minecraftforge.common.extensions.IForgeMenuType.create(AdvancedEnchantMenu::new));
 public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<AdvancedTableRecipe>> RECIPE=Content.SERIALIZERS.register("advanced_enchanting_table",AdvancedTableRecipe.Serializer::new);
 public static void register(){AdvancedEnchantConfig.register();}
 public static final class Table extends EnchantmentTableBlock {
  public Table(Properties properties){super(properties);}
  @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Book(pos,state);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?createTickerHelper(type,ENTITY.get(),EnchantmentTableBlockEntity::bookAnimationTick):null;}
  @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
   if(!WorldContentMode.rebuilt(level)||!OriginalEnchantingTables.passable(level,pos.above()))return InteractionResult.PASS;
   if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new AdvancedEnchantMenu(id,inv,pos),Component.translatable("block.mite_reforged.advanced_enchanting_table")),buffer->buffer.writeBlockPos(pos));
   return InteractionResult.sidedSuccess(level.isClientSide);
  }
 }
 /** Book animation/name only: items remain in the owner's vanilla menu lifecycle. */
 public static final class Book extends EnchantmentTableBlockEntity {
  public Book(BlockPos pos,BlockState state){super(pos,state);}
  @Override public BlockEntityType<?> getType(){return ENTITY.get();}
 }
 private AdvancedEnchantContent(){}
}

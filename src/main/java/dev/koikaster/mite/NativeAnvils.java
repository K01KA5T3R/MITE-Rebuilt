package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Global native anvil entries adapted to the source anvil implementation. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class NativeAnvils {
 public static SourceAnvil convert(Level level,BlockPos pos){var state=level.getBlockState(pos);if(state.getBlock() instanceof MetalAnvilBlock)return (SourceAnvil)level.getBlockEntity(pos);if(!(state.getBlock() instanceof AnvilBlock))return null;int stage=state.is(Blocks.DAMAGED_ANVIL)?2:state.is(Blocks.CHIPPED_ANVIL)?1:0;var mapped=Content.ALL_BLOCKS.get("iron_anvil").get().defaultBlockState().setValue(MetalAnvilBlock.FACING,state.getValue(AnvilBlock.FACING)).setValue(MetalAnvilBlock.DAMAGE,stage);level.setBlock(pos,mapped,3);var anvil=(SourceAnvil)level.getBlockEntity(pos);if(anvil!=null)anvil.setDamage(SourceAnvil.minimumStage("iron",stage));return anvil;}
 public static boolean fall(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){var state=(BlockState)rawState;if(!(state.getBlock() instanceof AnvilBlock))return false;var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;var converted=convert(level,pos);if(converted!=null)((MetalAnvilBlock)converted.getBlockState().getBlock()).tick(converted.getBlockState(),level,pos,(RandomSource)rawRandom);return true;}
 public static InteractionResult use(Object rawLevel,Object rawPos,Object rawPlayer){var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var player=(Player)rawPlayer;if(!ClassicRepair.accessible(level,pos))return InteractionResult.PASS;if(player instanceof ServerPlayer server){convert(level,pos);ClassicRepair.open(server,pos);}return InteractionResult.sidedSuccess(level.isClientSide);}
 @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event){if(!(event.getLevel() instanceof ServerLevel level)||!(event.getPlacedBlock().getBlock() instanceof AnvilBlock))return;var state=event.getPlacedBlock();var anvil=convert(level,event.getPos());if(anvil==null||!(event.getEntity() instanceof LivingEntity actor))return;for(var hand:InteractionHand.values()){var stack=actor.getItemInHand(hand);if(stack.getItem() instanceof BlockItem item&&item.getBlock()==state.getBlock()){if(stack.hasCustomHoverName())anvil.name=stack.getHoverName();anvil.setChanged();break;}}}
 private NativeAnvils(){}
}

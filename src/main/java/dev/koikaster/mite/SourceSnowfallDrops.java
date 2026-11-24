package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** The original snowed-upon crop drop cause is independent of self drops. */
public final class SourceSnowfallDrops {
 private static final ThreadLocal<ServerLevel> ACTIVE=new ThreadLocal<>();
 static boolean active(ServerLevel world){return ACTIVE.get()==world;}
 static void drop(ServerLevel world,BlockPos pos,BlockState state){var previous=ACTIVE.get();ACTIVE.set(world);try{var drops=Block.getDrops(state,world,pos,world.getBlockEntity(pos),null,ItemStack.EMPTY);int count=0;for(var stack:drops){count+=stack.getCount();Block.popResource(world,pos,stack);}if(count>0&&state.getBlock() instanceof CropBlock)world.playSound(null,pos,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.05f,((world.random.nextFloat()-world.random.nextFloat())*.7f+1)*2);}finally{if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}}
 private SourceSnowfallDrops(){}
}

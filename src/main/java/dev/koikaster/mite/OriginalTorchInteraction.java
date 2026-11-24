package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** source og nearby-light query and disable action; connected to the actual Shadow owner. */
public final class OriginalTorchInteraction {
 public static boolean light(BlockState state){return state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH)||state.is(Blocks.JACK_O_LANTERN)||((state.is(Blocks.REDSTONE_TORCH)||state.is(Blocks.REDSTONE_WALL_TORCH))&&state.getValue(RedstoneTorchBlock.LIT));}
 public static boolean near(Mob mob,BlockPos center){for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1+(int)mob.getBbHeight();dy++)for(int dz=-1;dz<=1;dz++)if(light(mob.level().getBlockState(center.offset(dx,dy,dz))))return true;return false;}
 public static boolean disable(Mob mob){
  if(mob.level().isClientSide||((SourceMobAi)mob).mite$recentPlayerHitTicks()!=0)return false;
  double nearest=Double.MAX_VALUE;for(var player:mob.level().players())if(!player.isSpectator())nearest=Math.min(nearest,mob.distanceToSqr(player));if(nearest<=16)return false;
  var level=mob.level();var center=mob.blockPosition();for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1+(int)mob.getBbHeight();dy++)for(int dz=-1;dz<=1;dz++){
   var at=center.offset(dx,dy,dz);var state=level.getBlockState(at);if(!light(state))continue;
   if(state.is(Blocks.JACK_O_LANTERN)){if(!level.setBlock(at,Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,state.getValue(HorizontalDirectionalBlock.FACING)),3))continue;var torch=new ItemEntity(level,at.getX(),at.getY(),at.getZ(),new ItemStack(Items.TORCH));torch.setPickUpDelay(10);level.addFreshEntity(torch);}
   else if(!level.destroyBlock(at,true,mob))continue;
   mob.playSound(SoundEvents.ITEM_PICKUP,.05f,((mob.getRandom().nextFloat()-mob.getRandom().nextFloat())*.7f+1)*2);return true;
  }return false;
 }
 private OriginalTorchInteraction(){}
}

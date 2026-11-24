package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
/** tg.br's original fourteen block families; modern small flowers/sand follow V001. */
public final class SourceEndermanBlocks {
 static boolean allowed(BlockState state){return state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)||state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(Blocks.GRAVEL)||state.is(BlockTags.SMALL_FLOWERS)||state.is(Blocks.BROWN_MUSHROOM)||state.is(Blocks.RED_MUSHROOM)||state.is(Blocks.TNT)||state.is(Blocks.CACTUS)||state.is(Blocks.CLAY)||state.is(Blocks.PUMPKIN)||state.is(Blocks.MELON)||state.is(Blocks.MYCELIUM);}
 static void tick(SourceEnderman actor){if(actor.level().isClientSide||!actor.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!ForgeEventFactory.getMobGriefingEvent(actor.level(),actor))return;var r=actor.getRandom();var l=actor.level();
  if(actor.getCarriedBlock()==null){if(r.nextInt(20)!=0)return;var p=BlockPos.containing(actor.getX()-2+r.nextDouble()*4,actor.getY()+r.nextDouble()*3,actor.getZ()-2+r.nextDouble()*4);var state=l.getBlockState(p);if(!allowed(state))return;actor.setCarriedBlock(state);l.setBlock(p,Blocks.AIR.defaultBlockState(),3);if(l instanceof net.minecraft.server.level.ServerLevel server)SourceSoilControl271.tryFall(server,p.above());
  }else if(r.nextInt(2000)==0){var p=BlockPos.containing(actor.getX()-1+r.nextDouble()*2,actor.getY()+r.nextDouble()*2,actor.getZ()-1+r.nextDouble()*2);if(l.isEmptyBlock(p)&&!l.isEmptyBlock(p.below())&&l.getBlockState(p.below()).isSolidRender(l,p.below())){l.setBlock(p,actor.getCarriedBlock(),3);actor.setCarriedBlock(null);}}
 }
 private SourceEndermanBlocks(){}
}

package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** pm/pj: source wooden door/trapdoor/gate search and480/1920 progress; intentionally no mutex flags. */
public final class OriginalBreakPortalGoal extends Goal {
 private final PathfinderMob mob;private BlockPos pos;private BlockState selected;private int progress,lastStage=-1;
 public OriginalBreakPortalGoal(PathfinderMob mob){this.mob=mob;}
 private boolean wooden(BlockState block){return block.is(BlockTags.WOODEN_DOORS)||block.is(BlockTags.WOODEN_TRAPDOORS)||block.getBlock() instanceof FenceGateBlock;}
 private boolean closed(BlockState block){return wooden(block)&&!block.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);}
 private boolean find(BlockPos candidate){var block=mob.level().getBlockState(candidate);if(!wooden(block))return false;pos=candidate.immutable();selected=block;return true;}
 public boolean canUse(){if(!mob.horizontalCollision||!(mob.getNavigation() instanceof GroundPathNavigation nav)||!nav.getNodeEvaluator().canOpenDoors())return false;var path=nav.getPath();if(path==null||path.isDone())return false;boolean found=false;
  for(int i=0;i<Math.min(path.getNextNodeIndex()+2,path.getNodeCount());i++){var node=path.getNode(i);var trial=new BlockPos(node.x,node.y+1,node.z);if(mob.distanceToSqr(node.x,mob.getY(),node.z)<=2.25&&(find(trial)||find(trial.below()))){found=true;break;}}
  if(!found)found=find(mob.blockPosition().above())||find(mob.blockPosition());if(!found)return false;if(selected.getBlock() instanceof FenceGateBlock&&!(mob.getTarget() instanceof Player))return false;return mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)&&closed(selected);
 }
 public int ticksToBreak(){int ticks=selected.getBlock() instanceof DoorBlock?1920:480;if(Mobs.name(mob).equals("earth_elemental"))ticks/=8;return OriginalDigging.frenzied(mob)?ticks/2:ticks;}
 public void start(){progress=0;}public boolean canContinueToUse(){return pos!=null&&progress<=ticksToBreak()&&closed(mob.level().getBlockState(pos))&&mob.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<4;}
 public void tick(){OriginalDespawn.refresh(mob,-400);var block=mob.level().getBlockState(pos);if(mob.getRandom().nextInt(20)==0){mob.level().playSound(null,pos,SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,SoundSource.HOSTILE,1,1);mob.level().levelEvent(2001,pos,Block.getId(block));}float strength=Math.max(ClassicMining.npcToolStrength(mob.getMainHandItem(),block),1);progress+=(int)(strength+.5f);int total=ticksToBreak();progress=Math.min(progress,total);int stage=(int)(progress/(float)total*10);if(stage!=lastStage){mob.level().destroyBlockProgress(mob.getId(),pos,stage);lastStage=stage;}if(progress==total&&mob.level().getDifficulty()==Difficulty.HARD){if(block.getBlock() instanceof DoorBlock&&block.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER)mob.level().setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);mob.level().removeBlock(pos,false);mob.level().playSound(null,pos,SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,SoundSource.HOSTILE,1,1);mob.level().levelEvent(2001,pos,Block.getId(selected));}}
 public void stop(){if(pos!=null)mob.level().destroyBlockProgress(mob.getId(),pos,-1);}public boolean requiresUpdateEveryTick(){return true;}
}

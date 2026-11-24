package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.tags.FluidTags;

/** pl's single server goal owns riding, including wear and travel. The native
 * client receives ordinary entity motion; it does not run a second controller. */
public final class SourcePigControl extends Goal implements SourcePigRidingRules.Host {
 private final SourcePig pig;private final SourcePigRidingRules.State state=new SourcePigRidingRules.State();private boolean running;
 SourcePigControl(SourcePig pig){this.pig=pig;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
 public boolean canUse(){return SourcePigRidingRules.canUse(this);}public boolean canContinueToUse(){return canUse();}public boolean requiresUpdateEveryTick(){return true;}
 public void start(){running=true;SourcePigRidingRules.start(state);pig.getNavigation().stop();}public void stop(){running=false;SourcePigRidingRules.stop(state);}public void tick(){SourcePigRidingRules.tick(this);}
 public boolean running(){return running;}public boolean canBoost(){return SourcePigRidingRules.canBoost(state);}public void boost(){SourcePigRidingRules.boost(this);}
 public SourcePigRidingRules.State state(){return state;}public boolean alive(){return pig.isAlive();}public boolean rider(){return pig.getFirstPassenger() instanceof Player;}private Player player(){return (Player)pig.getFirstPassenger();}public boolean stick(){return rider()&&SourceCarrotSticks.handles(player().getMainHandItem());}public boolean creative(){return player().isCreative();}public float riderYaw(){return player().getYRot();}public float yaw(){return pig.getYRot();}public void yaw(float value){pig.setYRot(value);pig.yBodyRot=pig.yHeadRot=value;}
 public double x(){return pig.getX();}public double y(){return pig.getY();}public double z(){return pig.getZ();}public boolean grounded(){return pig.onGround();}public float friction(int x,int y,int z){var at=new BlockPos(x,y,z);return pig.level().getBlockState(at).getFriction(pig.level(),at,pig);}public float aiSpeed(){return (float)pig.getAttributeValue(Attributes.MOVEMENT_SPEED);}public float width(){return pig.getBbWidth();}public float height(){return pig.getBbHeight();}public float riderHeight(){return player().getBbHeight();}
 public boolean air(int x,int y,int z){return pig.level().getBlockState(new BlockPos(x,y,z)).isAir();}public boolean stairOrBottomSlab(int x,int y,int z){var block=pig.level().getBlockState(new BlockPos(x,y,z));return block.getBlock() instanceof StairBlock||block.getBlock() instanceof SlabBlock&&block.getValue(SlabBlock.TYPE)==SlabType.BOTTOM;}
 public int path(int x,int y,int z,int width,int height,int depth){return SourcePigPathRules.query(new SourcePigPathRules.Host(){
  public SourcePigPathRules.Cell cell(int x,int y,int z){var at=new BlockPos(x,y,z);var state=pig.level().getBlockState(at);var block=state.getBlock();boolean water=state.getFluidState().is(FluidTags.WATER)&&state.getCollisionShape(pig.level(),at).isEmpty(),lava=state.getFluidState().is(FluidTags.LAVA),open=state.hasProperty(DoorBlock.OPEN)&&state.getValue(DoorBlock.OPEN);boolean portal=block instanceof DoorBlock||block instanceof TrapDoorBlock||block instanceof FenceGateBlock;return new SourcePigPathRules.Cell(state.isAir(),block instanceof TrapDoorBlock,water,block instanceof BaseRailBlock,water||portal&&open||state.isPathfindable(pig.level(),at,PathComputationType.LAND),lava,block instanceof FenceBlock||block instanceof WallBlock||block instanceof FenceGateBlock);}
  public boolean standingOnRail(){return pig.level().getBlockState(pig.blockPosition()).getBlock() instanceof BaseRailBlock||pig.level().getBlockState(pig.blockPosition().below()).getBlock() instanceof BaseRailBlock;}public boolean inLava(){return pig.isInLava();}
 },x,y,z,width,height,depth);}
 public void jump(){pig.getJumpControl().jump();}public float nextFloat(){return pig.getRandom().nextFloat();}public int nextInt(int bound){return pig.getRandom().nextInt(bound);}public void nibble(int amount){SourceCarrotSticks.damage(player(),InteractionHand.MAIN_HAND,amount);}public void move(float forward){pig.sourceRiderTravel(forward);}
}

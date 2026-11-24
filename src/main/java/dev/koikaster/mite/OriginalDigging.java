package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.gameevent.GameEvent;

/** EntityAnimalWatcher block arbitration, eligibility, progress, aftermath and source cooldown. */
public final class OriginalDigging implements OriginalMeleeGoal.Digger {
 public interface Host {OriginalDigging originalDigging();}
 public final PathfinderMob mob;public final SourceDiggingState state=new SourceDiggingState();
 public OriginalDigging(PathfinderMob mob){this.mob=mob;}
 public static boolean watcher(Entity entity){return entity instanceof SourceZombieOwner||entity instanceof Host&&switch(Mobs.name(entity)){case "ghoul","revenant","earth_elemental","clay_golem"->true;default->false;};}
 public static boolean frenzied(Mob mob){return SourceSpecialSpawns.bloodUp(mob.level());}
 public boolean preventsDigging(){var p=ClassicToolWear.profile(mob.getMainHandItem());return p!=null&&switch(p.kind()){case SWORD,DAGGER,KNIFE,CUDGEL,CLUB,SCYTHE->true;default->false;};}
 public boolean enabled(){return mob instanceof SourceZombieOwner owner?owner.sourceZombie()!=null&&owner.sourceZombie().canDig():watcher(mob)&&!preventsDigging();}
 public static boolean passable(Level level,BlockPos pos,boolean includeLiquid){if(level.isOutsideBuildHeight(pos))return true;if(!level.hasChunkAt(pos))return false;var block=level.getBlockState(pos);if(!includeLiquid&&block.getBlock() instanceof LiquidBlock)return false;if(block.getBlock() instanceof FenceGateBlock)return block.getValue(FenceGateBlock.OPEN);if(block.getBlock() instanceof DoorBlock||block.getBlock() instanceof TrapDoorBlock||block.getBlock() instanceof FenceBlock||block.getBlock() instanceof IronBarsBlock)return false;return !block.isSolid();}
 public boolean blockWillFall(BlockPos pos){var state=mob.level().getBlockState(pos);var block=state.getBlock();return SoilGravity.affected(state)||block instanceof FallingBlock||block instanceof CactusBlock||block instanceof TorchBlock||block instanceof WallTorchBlock||block instanceof SnowLayerBlock;}
 public Vec3 eye(){return mob.position().add(0,mob.getBbHeight()*.75f,0);}public Vec3 legs(){return mob.position().add(0,mob.getBbHeight()*.25f,0);}public Vec3 center(LivingEntity target){return target.position().add(0,target.getBbHeight()/2f,0);}
 public float toolStrength(BlockState block){return ClassicMining.npcToolStrength(mob.getMainHandItem(),block);}
 private int minLevel(BlockState block){return block.is(Blocks.BEDROCK)||block.is(Blocks.END_PORTAL_FRAME)?2:HarvestRules.requiredLevel(block);}
 public int elementalHarvestLevel(){int type=mob instanceof Mobs.Undead owner?owner.originalEarth().type()&255:1;return type==49?4:type==87?1:type==82?0:type==172?1:2;}
 private boolean claimed(BlockPos pos){for(var entity:mob.level().getEntities(mob,new AABB(mob.getX()-4,mob.getY()-4,mob.getZ()-4,mob.getX()+4,mob.getY()+4,mob.getZ()+4)))if(watcher(entity)){var other=((Host)entity).originalDigging().state;if(other.destroying&&other.x==pos.getX()&&other.y==pos.getY()&&other.z==pos.getZ())return true;}return false;}
 public BlockHitResult trace(Vec3 from,Vec3 to){var hit=OriginalRaycast.blocks(mob,from,to,false,true);if(hit!=null)return hit;var delta=to.subtract(from);return BlockHitResult.miss(to,net.minecraft.core.Direction.getNearest(delta.x,delta.y,delta.z),BlockPos.containing(to));}
 private boolean blocked(Vec3 from,BlockPos to){var hit=trace(from,Vec3.atCenterOf(to));return hit.getType()==HitResult.Type.BLOCK&&!hit.getBlockPos().equals(to);}
 public boolean canDestroy(BlockPos pos,boolean checkClipping){
  if(preventsDigging())return false;int foot=Mth.floor(mob.getBoundingBox().minY);if(pos.getY()<foot&&!(mob.getMainHandItem().getItem() instanceof ShovelItem))return false;if(pos.getY()>foot+1)return false;
  if(mob.position().add(0,mob.getBbHeight()*.5f,0).distanceToSqr(Vec3.atCenterOf(pos))>3.25)return false;if(checkClipping&&blocked(eye(),pos)&&blocked(legs(),pos))return false;
  var block=mob.level().getBlockState(pos);if(block.isAir()||block.getBlock() instanceof LiquidBlock)return false;
  if(mob instanceof Mobs.Undead elemental&&elemental.earthBody()&&minLevel(block)<=elementalHarvestLevel())return true;
  boolean eligible=minLevel(block)==0||frenzied(mob)&&minLevel(block)<2||toolStrength(block)>0||block.is(Blocks.SAND)||block.is(Blocks.RED_SAND)||block.is(Blocks.DIRT)||block.is(Blocks.GRASS_BLOCK)||block.is(Blocks.GRAVEL)||block.is(Blocks.SNOW_BLOCK)||block.getBlock() instanceof FarmBlock||block.is(Blocks.CLAY)||block.getBlock() instanceof LeavesBlock||block.is(BlockTags.WOOL)||block.getBlock() instanceof CactusBlock&&(mob.isInvulnerable()||mob.getMobType()==MobType.UNDEAD)||block.is(Blocks.SPONGE)||block.is(Blocks.WET_SPONGE)||block.getBlock() instanceof PumpkinBlock||block.getBlock() instanceof MelonBlock||block.is(Blocks.MYCELIUM)||block.is(Blocks.HAY_BLOCK)||block.is(Blocks.GLASS_PANE)||block.getBlock() instanceof StainedGlassPaneBlock;
  return eligible&&!claimed(pos);
 }
 public boolean setBlockToDig(BlockPos pos,boolean checkClipping){if(!canDestroy(pos,checkClipping))return false;state.destroying=true;if(state.x==pos.getX()&&state.y==pos.getY()&&state.z==pos.getZ())return true;if(pos.getY()==Mth.floor(mob.getBoundingBox().minY)+1&&mob.level().getBlockState(pos).getBlock() instanceof CactusBlock&&canDestroy(pos.below(),checkClipping))pos=pos.below();state.progress=-1;state.x=pos.getX();state.y=pos.getY();state.z=pos.getZ();return true;}
 public BlockPos position(){return new BlockPos(state.x,state.y,state.z);}
 public void cancel(){if(state.cancel()&&!mob.level().isClientSide)((ServerLevel)mob.level()).destroyBlockProgress(mob.getId(),position(),-1);}
 public int cooloff(){var pos=position();var block=mob.level().getBlockState(pos);if(block.isAir())return 40;int result=(int)(300f*ClassicMining.hardness(block,mob.level(),pos));if(frenzied(mob))result/=2;if(mob instanceof Mobs.Undead elemental&&elemental.earthBody())result/=8;if(!mob.getMainHandItem().isEmpty()&&ClassicToolWear.profile(mob.getMainHandItem())!=null)result=(int)(result/(1+toolStrength(block)*.5f));return Mobs.name(mob).equals("ghoul")?result/2:result;}
 public void beforeAi(){if(state.destroying&&state.pauseTicks==0){var pos=position();mob.getLookControl().setLookAt(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,10,mob.getMaxHeadXRot());if(!canDestroy(pos,true))cancel();}else if(!state.destroying)state.inactiveTick();}
 private void suppressNearby(){for(var entity:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(3,1,3)))for(var goal:((SourceMobAi)entity).mite$goals().getAvailableGoals())if(goal.getGoal() instanceof OriginalMeleeGoal melee){melee.ticksSuppressed=Math.max(melee.ticksSuppressed,10);melee.attackCooldown=Math.max(melee.attackCooldown,10);break;}}
 public void progress(){
  var pos=position();if(!canDestroy(pos,true)){cancel();return;}OriginalDespawn.refresh(mob,-400);var level=(ServerLevel)mob.level();var block=level.getBlockState(pos);var held=mob.getMainHandItem();if(block.getBlock() instanceof CactusBlock&&ClassicToolWear.profile(held)==null&&!held.is(Items.STICK)&&!held.is(Items.BONE))mob.hurt(mob.damageSources().cactus(),1);
  if(++state.progress<10)state.destroying=true;else{
   state.progress=-1;if(block.getBlock() instanceof AbstractGlassBlock||block.is(Blocks.GLASS_PANE))level.levelEvent(2001,pos,Block.getId(block));
   Block.dropResources(block,level,pos,level.getBlockEntity(pos),mob,held);level.removeBlock(pos,false);level.gameEvent(GameEvent.BLOCK_DESTROY,pos,GameEvent.Context.of(mob,block));if(blockWillFall(pos.above()))suppressNearby();
   if(ClassicToolWear.profile(held)!=null&&ClassicMining.hardness(block,level,pos)>0&&toolStrength(block)>1&&!HarvestRules.portable(block)){var p=ClassicToolWear.profile(held);boolean sandstone=block.is(Blocks.SANDSTONE)||block.is(Blocks.RED_SANDSTONE);boolean grass=block.is(Blocks.WHEAT)||block.is(Blocks.GRASS)||block.is(Blocks.TALL_GRASS)||block.is(Blocks.FERN)||block.is(Blocks.LARGE_FERN);boolean plant=block.is(BlockTags.WOOL)||block.getBlock() instanceof BushBlock||block.getBlock() instanceof VineBlock;held.hurtAndBreak(LegacyToolWear.blockDamage(ClassicMining.hardness(block,level,pos),LegacyToolWear.blockDecay(p.kind(),sandstone,grass,plant)),mob,e->e.broadcastBreakEvent(EquipmentSlot.MAINHAND));}
   state.destroying=false;var above=level.getBlockState(pos.above());if(above.getBlock() instanceof FallingBlock||SoilGravity.affected(above)){state.destroying=true;state.pauseTicks=10;}else if(!above.isAir()&&!blockWillFall(pos.above())){state.y=pos.getY()==Mth.floor(mob.getBoundingBox().minY)&&canDestroy(pos.above(),true)?state.y+1:state.y-1;state.destroying=true;state.pauseTicks=10;}else if(pos.getY()==Mth.floor(mob.getBoundingBox().minY)+1&&!passable(level,mob.blockPosition().above(2),false)&&canDestroy(pos.below(),true)){state.destroying=true;state.pauseTicks=10;--state.y;}
   if(SoilGravity.affected(above))SourceSoilControl271.tryFall(level,pos.above());
  }
  level.destroyBlockProgress(mob.getId(),pos,state.progress);level.levelEvent(2001,pos,Block.getId(block));
 }
}

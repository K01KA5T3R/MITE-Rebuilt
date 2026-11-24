package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.*;import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;import net.minecraftforge.registries.RegistryObject;

/** N015: a manually operated, ground-anchored repeating crossbow. No targeting AI. */
public final class MountedCrossbow1949 {
 public static final int SHOT_TICKS=6,INPUT_LEASE_TICKS=10;
 public static final RegistryObject<MountBlock> BLOCK=Content.BLOCKS.register("mounted_crossbow",MountBlock::new);
 public static final RegistryObject<Item> ITEM=Content.ITEMS.register("mounted_crossbow",()->new BlockItem(BLOCK.get(),new Item.Properties().stacksTo(1)));
 public static final RegistryObject<BlockEntityType<MountEntity>> BLOCK_ENTITY=Content.BLOCK_ENTITIES.register("mounted_crossbow",()->BlockEntityType.Builder.of(MountEntity::new,BLOCK.get()).build(null));
 public static final RegistryObject<EntityType<Seat>> SEAT=Mobs.ENTITIES.register("mounted_crossbow_seat",()->EntityType.Builder.<Seat>of(Seat::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(6).updateInterval(2).build(MiteReforged.ID+":mounted_crossbow_seat"));
 public static void register(){Content.ALL_BLOCKS.put("mounted_crossbow",(RegistryObject)BLOCK);Content.ALL_ITEMS.put("mounted_crossbow",ITEM);}
 public static boolean supported(LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
 public static Seat seat(Level level,BlockPos pos){return level.getEntitiesOfClass(Seat.class,new AABB(pos).inflate(.25),s->!s.isRemoved()&&s.origin().equals(pos)).stream().findFirst().orElse(null);}
 public static final class MountBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
  private static final VoxelShape SHAPE=Shapes.or(Block.box(1,0,1,15,3,15),Block.box(6,3,6,10,22,10));
  MountBlock(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2f).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
  public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
  public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
  public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return supported(level,pos);}
  public BlockState getStateForPlacement(BlockPlaceContext context){return supported(context.getLevel(),context.getClickedPos())&&context.getLevel().getFluidState(context.getClickedPos()).isEmpty()?defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()):null;}
  public BlockState updateShape(BlockState state,Direction side,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos other){return side==Direction.DOWN&&!canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,side,adjacent,level,pos,other);}
  public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
  public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
  public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MountEntity(pos,state);}
  public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
   if(player.isSpectator()||player.isShiftKeyDown()||player.isPassenger()||!supported(level,pos))return InteractionResult.PASS;
   if(level.isClientSide)return InteractionResult.SUCCESS;
   var mount=seat(level,pos);if(mount!=null&&!mount.getPassengers().isEmpty())return InteractionResult.CONSUME;
   boolean fresh=mount==null;if(fresh){mount=SEAT.get().create(level);if(mount==null)return InteractionResult.FAIL;mount.anchor(pos);mount.setPos(pos.getX()+.5,pos.getY()+.15,pos.getZ()+.5);mount.setYRot(state.getValue(FACING).toYRot());if(!level.addFreshEntity(mount))return InteractionResult.FAIL;}
   if(!player.startRiding(mount)){if(fresh)mount.discard();return InteractionResult.FAIL;}
   mount.clearTrigger();return InteractionResult.CONSUME;
  }
  public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))for(var mount:level.getEntitiesOfClass(Seat.class,new AABB(pos).inflate(.25),s->s.origin().equals(pos))){mount.clearTrigger();mount.ejectPassengers();mount.discard();}super.onRemove(state,level,pos,next,moving);}
  public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 20;}
  public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 5;}
 }
 public static final class MountEntity extends BlockEntity {
  MountEntity(BlockPos pos,BlockState state){super(BLOCK_ENTITY.get(),pos,state);}
  public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(.5,1,.5);}
 }
 public static final class Seat extends Entity {
  private static final EntityDataAccessor<BlockPos> ORIGIN=SynchedEntityData.defineId(Seat.class,EntityDataSerializers.BLOCK_POS);
  private long triggerUntil=Long.MIN_VALUE,nextShot;private UUID triggerPlayer;private int emptyTicks;
  public Seat(EntityType<? extends Seat> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
  protected void defineSynchedData(){entityData.define(ORIGIN,BlockPos.ZERO);}
  public BlockPos origin(){return entityData.get(ORIGIN);}
  void anchor(BlockPos pos){entityData.set(ORIGIN,pos.immutable());}
  protected void readAdditionalSaveData(CompoundTag tag){entityData.set(ORIGIN,BlockPos.of(tag.getLong("Mount")));clearTrigger();}
  protected void addAdditionalSaveData(CompoundTag tag){tag.putLong("Mount",origin().asLong());}
  public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
  protected boolean canAddPassenger(Entity passenger){return passenger instanceof Player&&!passenger.isSpectator()&&getPassengers().isEmpty();}
  public boolean isPickable(){return false;}public boolean isPushable(){return false;}
  protected void positionRider(Entity passenger,Entity.MoveFunction move){if(hasPassenger(passenger)){var back=Vec3.directionFromRotation(0,passenger.getYRot()).scale(-.5);move.accept(passenger,getX()+back.x,origin().getY()+.2,getZ()+back.z);}}
  public void clearTrigger(){triggerUntil=Long.MIN_VALUE;triggerPlayer=null;}
  public void input(ServerPlayer player,boolean held){if(getFirstPassenger()!=player||player.getVehicle()!=this||!player.isAlive()||player.isSpectator())return;if(held){triggerPlayer=player.getUUID();triggerUntil=level().getGameTime()+INPUT_LEASE_TICKS;}else clearTrigger();}
  public void tick(){
   super.tick();setDeltaMovement(Vec3.ZERO);
   if(level().isClientSide)return;BlockPos pos=origin();
   if(!level().hasChunkAt(pos)||!level().getBlockState(pos).is(BLOCK.get())||!supported(level(),pos)){clearTrigger();ejectPassengers();discard();return;}
   setPos(pos.getX()+.5,pos.getY()+.15,pos.getZ()+.5);
   if(!(getFirstPassenger() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()){clearTrigger();if(++emptyTicks>=20){ejectPassengers();discard();}return;}
   emptyTicks=0;setYRot(player.getYRot());setXRot(net.minecraft.util.Mth.clamp(player.getXRot(),-60,60));
   long now=level().getGameTime();if(now<triggerUntil&&now>=nextShot&&player.getUUID().equals(triggerPlayer)){nextShot=now+SHOT_TICKS;shoot(player);}
  }
  private void shoot(ServerPlayer player){
   ItemStack ammo=player.getOffhandItem();if(!(ammo.getItem() instanceof ArrowItem)){ammo=ItemStack.EMPTY;for(var stack:player.getInventory().items)if(stack.getItem() instanceof ArrowItem&&!stack.isEmpty()){ammo=stack;break;}}
   boolean creative=player.getAbilities().instabuild;if(ammo.isEmpty()){if(!creative)return;ammo=new ItemStack(Items.ARROW);}
   var aim=Vec3.directionFromRotation(getXRot(),getYRot());var start=new Vec3(getX(),origin().getY()+1.55,getZ());var muzzle=start.add(aim.scale(.85));
   if(level().clip(new ClipContext(start,muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS||!level().getWorldBorder().isWithinBounds(BlockPos.containing(muzzle)))return;
   var arrow=((ArrowItem)ammo.getItem()).createArrow(level(),ammo.copyWithCount(1),player);if(ammo.is(Items.ARROW))arrow.setBaseDamage(ClassicToolCatalogue.arrowDamage("flint"));SourceSpecificImmunities.captureLauncher(arrow,ItemStack.EMPTY);
   arrow.setPos(muzzle.x,muzzle.y,muzzle.z);arrow.shoot(aim.x,aim.y,aim.z,2.8f,1f);arrow.pickup=creative?AbstractArrow.Pickup.CREATIVE_ONLY:AbstractArrow.Pickup.ALLOWED;
   if(!level().addFreshEntity(arrow))return;if(!creative){ammo.shrink(1);player.getInventory().setChanged();}level().playSound(null,getX(),origin().getY()+1.55,getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1f,.9f+level().random.nextFloat()*.2f);
  }
  public Vec3 getDismountLocationForPassenger(LivingEntity passenger){for(var direction:Direction.Plane.HORIZONTAL){var location=DismountHelper.findSafeDismountLocation(passenger.getType(),level(),origin().relative(direction),false);if(location!=null)return location;}return Vec3.atBottomCenterOf(origin().above());}
 }
 private MountedCrossbow1949(){}
}

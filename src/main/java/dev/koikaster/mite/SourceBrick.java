package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** R195 ItemBrick/EntityBrick. A distinct projectile keeps snowball-only damage gates out. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceBrick extends ThrowableItemProjectile implements net.minecraftforge.entity.IEntityAdditionalSpawnData {
 public static final net.minecraftforge.registries.RegistryObject<EntityType<SourceBrick>> TYPE=Mobs.ENTITIES.register("thrown_brick",()->EntityType.Builder.<SourceBrick>of(SourceBrick::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(1).build(MiteReforged.ID+":thrown_brick"));
 public static void register(){}
 @Override protected Item getDefaultItem(){return Items.BRICK;}
 @Override protected float getGravity(){return .07f;}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
 @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf data){data.writeItem(getItem());data.writeInt(getOwner()==null?-1:getOwner().getId());}
 @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf data){setItem(data.readItem());int owner=data.readInt();if(owner>=0)setOwner(level().getEntity(owner));}

 private static final Random ITEM_RANDOM=LegacyItemRandom749.RANDOM;
 private final SourceBrickRules790.State source=new SourceBrickRules790.State();
 private final Flight host=new Flight();private int raycastOffset;private boolean legacyNativeOwner;
 public SourceBrick(EntityType<? extends ThrowableItemProjectile> type,Level level){super(type,level);}
 public SourceBrick(Level level,LivingEntity thrower,Item item){
  this(TYPE.get(),level);setItem(new ItemStack(item));setOwner(thrower);
  SourceBrickRules790.thrown(host,new SourceBrickRules790.Thrower(thrower,thrower.getX(),thrower.getY(),thrower.getZ(),thrower.getYRot(),thrower.getXRot(),thrower.getEyeHeight(),thrower instanceof Player,thrower instanceof Player player?PlayerState.level(player):0,!thrower.isPassenger()&&!thrower.onGround()&&(thrower.isInWater()||thrower.isInLava())));
 }
 public SourceBrick(Level level,double x,double y,double z){this(TYPE.get(),level);setPos(x,y,z);pull();}
 @Override public void setOwner(Entity owner){if(source!=null)source.owner=owner;super.setOwner(owner);}
 @Override public Entity getOwner(){if(source==null)return super.getOwner();if(source.owner==null&&legacyNativeOwner)source.owner=super.getOwner();return (Entity)SourceBrickRules790.owner(host);}
 @Override public boolean shouldRenderAtSqrDistance(double distance){return SourceBrickRules790.renderDistance(getBoundingBox().getSize(),distance);}
 @Override public void shoot(double x,double y,double z,float velocity,float wander){pull();SourceBrickRules790.shoot(host,x,y,z,velocity,wander);}
 @Override public void lerpMotion(double x,double y,double z){pull();SourceBrickRules790.receivedMotion(host,x,y,z);}
 @Override public void tick(){pull();SourceBrickRules790.tick(host);}
 int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 private void pull(){source.x=getX();source.y=getY();source.z=getZ();var v=getDeltaMovement();source.vx=v.x;source.vy=v.y;source.vz=v.z;source.yaw=getYRot();source.pitch=getXRot();source.oldYaw=yRotO;source.oldPitch=xRotO;}
 private void pushMotionRotation(){setDeltaMovement(source.vx,source.vy,source.vz);setYRot(source.yaw);setXRot(source.pitch);yRotO=source.oldYaw;xRotO=source.oldPitch;}
 @Override protected void onHit(HitResult hit){SourceBrickRules790.impact(new Impact(hit));}
 private final class Impact implements SourceBrickRules790.Impact {
  final HitResult hit;Impact(HitResult hit){this.hit=hit;}
  public boolean server(){return !level().isClientSide;}public boolean entity(){return hit instanceof EntityHitResult;}public boolean block(){return hit instanceof BlockHitResult;}
  public boolean flowerPot(){return level().getBlockState(((BlockHitResult)hit).getBlockPos()).getBlock() instanceof FlowerPotBlock;}
  public void damage(float amount){var target=((EntityHitResult)hit).getEntity();var damage=damageSources().thrown(SourceBrick.this,getOwner());if(target instanceof LivingEntity living){try(var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){target.hurt(damage,amount);frame.finish();}}else target.hurt(damage,amount);}
  public void breakPot(){level().destroyBlock(((BlockHitResult)hit).getBlockPos(),true,SourceBrick.this);}
  public void blockCallback(){var h=(BlockHitResult)hit;var state=level().getBlockState(h.getBlockPos());state.onProjectileHit(level(),state,h,SourceBrick.this);}
  public void poof(){level().addParticle(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,getItem()),getX(),getY(),getZ(),0,0,0);}
  public void discard(){SourceBrick.this.discard();}
 }
 private final class Flight implements SourceBrickRules790.Host {
  public SourceBrickRules790.State state(){return source;}public void pose(){moveTo(source.x,source.y,source.z,source.yaw,source.pitch);}
  public void position(){setPos(source.x,source.y,source.z);pushMotionRotation();}public void changedMotionRotation(){pushMotionRotation();}
  public void parentTick(){xOld=source.lastX;yOld=source.lastY;zOld=source.lastZ;((SourceThrowableNativeParent)(Object)SourceBrick.this).mite$throwableEntityTick();pull();}
  public double gaussian(){return random.nextGaussian();}public float nextFloat(){return random.nextFloat();}public boolean server(){return !level().isClientSide;}
  public int block(int x,int y,int z){return BuiltInRegistries.BLOCK.getId(level().getBlockState(new BlockPos(x,y,z)).getBlock());}
  public Object raycast(){pushMotionRotation();var origin=positionVector();var end=origin.add(getDeltaMovement());var block=SourceBrickRaycast790.blocks(SourceBrick.this,origin,end);HitResult hit=block;if(!level().isClientSide){var entity=SourceBrickRaycast790.entities(SourceBrick.this,origin,block==null?end:block.getLocation(),source.airTicks);if(entity!=null)hit=entity;}return hit;}
  private Vec3 positionVector(){return SourceBrick.this.position();}
  public boolean portal(Object raw){return raw instanceof BlockHitResult hit&&level().getBlockState(hit.getBlockPos()).is(Blocks.NETHER_PORTAL);}
  public void enterPortal(Object raw){handleInsidePortal(((BlockHitResult)raw).getBlockPos());}
  public void impact(Object raw){pushMotionRotation();var hit=(HitResult)raw;if(!ForgeEventFactory.onProjectileImpact(SourceBrick.this,hit))onHit(hit);pull();}
  public boolean water(){return isInWater();}public void bubble(double x,double y,double z,double vx,double vy,double vz){level().addParticle(ParticleTypes.BUBBLE,x,y,z,vx,vy,vz);}public void discard(){SourceBrick.this.discard();}
  public Object findPlayer(String name){for(var player:level().players())if(player.getGameProfile().getName().equals(name))return player;return null;}public String playerName(Object raw){return raw instanceof Player player?player.getGameProfile().getName():null;}
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){
  super.addAdditionalSaveData(tag);tag.remove("Owner");tag.remove("LeftOwner");tag.remove("HasBeenShot");SourceBrickRules790.save(host,new Tags(tag));tag.putBoolean("SourceBrickUq",true);
  tag.putInt("SourceTileX",source.tileX);tag.putInt("SourceTileY",source.tileY);tag.putInt("SourceTileZ",source.tileZ);var block=BuiltInRegistries.BLOCK.byId(source.inTile);tag.putString("SourceInTile",BuiltInRegistries.BLOCK.getKey(block).toString());
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){
  legacyNativeOwner=!tag.getBoolean("SourceBrickUq");var nativeTag=tag.copy();if(!legacyNativeOwner){nativeTag.remove("Owner");nativeTag.remove("LeftOwner");nativeTag.remove("HasBeenShot");}super.readAdditionalSaveData(nativeTag);SourceBrickRules790.load(source,new Tags(tag));
  source.tileX=tileCoordinate(tag,0);source.tileY=tileCoordinate(tag,1);source.tileZ=tileCoordinate(tag,2);if(tag.contains("SourceInTile")){var id=ResourceLocation.tryParse(tag.getString("SourceInTile"));source.inTile=BuiltInRegistries.BLOCK.getId(id==null?Blocks.AIR:BuiltInRegistries.BLOCK.get(id));}
  source.airTicks=source.groundTicks=0;raycastOffset=0;pull();
 }
 private static int tileCoordinate(CompoundTag tag,int axis){String full=new String[]{"SourceTileX","SourceTileY","SourceTileZ"}[axis],old=new String[]{"xTile","yTile","zTile"}[axis];if(tag.contains(full))return tag.getInt(full);int value=tag.getShort(old);var p=tag.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);if(tag.getByte("inGround")==1&&p.size()==3){int ref=net.minecraft.util.Mth.floor(p.getDouble(axis)),candidate=(ref&~65535)|(value&65535);if(candidate-ref>32768)candidate-=65536;else if(ref-candidate>32768)candidate+=65536;return candidate;}return value;}
 private record Tags(CompoundTag tag) implements SourceBrickRules790.Tags {public void putShort(String name,short value){tag.putShort(name,value);}public void putByte(String name,byte value){tag.putByte(name,value);}public void putString(String name,String value){tag.putString(name,value);}public short getShort(String name){return tag.getShort(name);}public byte getByte(String name){return tag.getByte(name);}public String getString(String name){return tag.getString(name);}}
 private static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);var item=stack.getItem();
  SourceBrickRules790.itemUse(new SourceBrickRules790.ItemUse(){
   public boolean client(){return level.isClientSide;}public boolean creative(){return player.getAbilities().instabuild;}
   public void bob(){SourceSnowballEffects.bob(hand);}public void consume(){SourceHeldConversion1037.convert(player,hand,net.minecraft.world.item.ItemStack.EMPTY);}public float endurance(){return EnchantmentEffects.endurance(player);}public void exhaust(float value){player.causeFoodExhaustion(value);}
   public float itemRandom(){return ITEM_RANDOM.nextFloat();}public void bow(float volume,float pitch){level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,volume,pitch);}
   public void spawn(){level.addFreshEntity(new SourceBrick(level,player,item));}
  });
  return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void rightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event){if(!event.getItemStack().is(Items.BRICK)&&!event.getItemStack().is(Items.NETHER_BRICK))return;var result=use(event.getLevel(),event.getEntity(),event.getHand());event.setCanceled(true);event.setCancellationResult(result.getResult());}
}

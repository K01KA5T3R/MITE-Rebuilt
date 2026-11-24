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

import net.minecraft.world.entity.projectile.ThrownExperienceBottle;

import net.minecraft.world.item.*;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.*;

import net.minecraft.world.phys.*;

import net.minecraftforge.event.ForgeEventFactory;



/** R195 xr/ut on the normal EXPERIENCE_BOTTLE registry owner. Source uq owns flight and owner state. */

public final class SourceXpBottle860 extends ThrownExperienceBottle {

 private static final Random ITEM_RANDOM=LegacyItemRandom749.RANDOM;

 private final SourceSnowballRules.State source=new SourceSnowballRules.State();
 private final SourceEntityGaussian845 sourceGaussian=new SourceEntityGaussian845();

 private final Flight host=new Flight();private int raycastOffset;private boolean legacyNativeOwner;

 public SourceXpBottle860(EntityType<? extends ThrownExperienceBottle> type,Level level){super(type,level);}

 public SourceXpBottle860(Level level,LivingEntity thrower){

  this(EntityType.EXPERIENCE_BOTTLE,level);setOwner(thrower);

  SourceBottleFlight854.thrown(host,new SourceSnowballRules.Thrower(thrower,thrower.getX(),thrower.getY(),thrower.getZ(),thrower.getYRot(),thrower.getXRot(),thrower.getEyeHeight(),thrower instanceof Player,thrower instanceof Player player?PlayerState.level(player):0,!thrower.isPassenger()&&!thrower.onGround()&&(thrower.isInWater()||thrower.isInLava())));

 }

 public SourceXpBottle860(Level level,double x,double y,double z){this(EntityType.EXPERIENCE_BOTTLE,level);setPos(x,y,z);pull();}

 @Override public void setOwner(Entity owner){if(source!=null)source.owner=owner;super.setOwner(owner);}

 @Override public Entity getOwner(){if(source==null)return super.getOwner();if(source.owner==null&&legacyNativeOwner)source.owner=super.getOwner();return (Entity)SourceSnowballRules.owner(host);}

 @Override public ItemStack getItem(){return new ItemStack(Items.EXPERIENCE_BOTTLE);}

 @Override public boolean shouldRenderAtSqrDistance(double distance){return SourceSnowballRules.renderDistance(getBoundingBox().getSize(),distance);}

 @Override public void shoot(double x,double y,double z,float velocity,float wander){pull();SourceSnowballRules.shoot(host,x,y,z,velocity,wander);}

 @Override public void lerpMotion(double x,double y,double z){pull();SourceSnowballRules.receivedMotion(host,x,y,z);}

 @Override public void tick(){pull();SourceBottleFlight854.tick(host);}

 int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}

 private void pull(){source.x=getX();source.y=getY();source.z=getZ();var v=getDeltaMovement();source.vx=v.x;source.vy=v.y;source.vz=v.z;source.yaw=getYRot();source.pitch=getXRot();source.oldYaw=yRotO;source.oldPitch=xRotO;}

 private void pushMotionRotation(){setDeltaMovement(source.vx,source.vy,source.vz);setYRot(source.yaw);setXRot(source.pitch);yRotO=source.oldYaw;xRotO=source.oldPitch;}

 @Override protected void onHit(HitResult hit){SourceExperienceBottle854.impact(new Impact(hit));}

 private final class Impact implements SourceExperienceBottle854.Impact {
  Impact(HitResult hit){}public boolean server(){return !level().isClientSide;}public double x(){return getX();}public double y(){return getY();}public double z(){return getZ();}
  public void event(int id,int x,int y,int z,int data){level().levelEvent(null,id,new BlockPos(x,y,z),data);}public void orb(int value,boolean bottle){level().addFreshEntity(new SourceXpOrb860(level(),x(),y(),z(),value).bottle());}public void discard(){SourceXpBottle860.this.discard();}
 }

 @Override public Entity changeDimension(net.minecraft.server.level.ServerLevel next,net.minecraftforge.common.util.ITeleporter teleporter){

  var owner=getOwner();if((source.ownerName==null||source.ownerName.isEmpty())&&owner instanceof Player player)source.ownerName=player.getGameProfile().getName();

  return super.changeDimension(next,teleporter);

 }

 private final class Flight implements SourceSnowballRules.Host {

  public SourceSnowballRules.State state(){return source;}public void pose(){moveTo(source.x,source.y,source.z,source.yaw,source.pitch);}

  public void position(){setPos(source.x,source.y,source.z);pushMotionRotation();}public void changedMotionRotation(){pushMotionRotation();}

  public void parentTick(){xOld=source.lastX;yOld=source.lastY;zOld=source.lastZ;((SourceThrowableNativeParent)(Object)SourceXpBottle860.this).mite$throwableEntityTick();pull();}

  public double gaussian(){return sourceGaussian.next(random::nextDouble);}public float nextFloat(){return random.nextFloat();}public boolean server(){return !level().isClientSide;}

  public int block(int x,int y,int z){return BuiltInRegistries.BLOCK.getId(level().getBlockState(new BlockPos(x,y,z)).getBlock());}

  public Object raycast(){pushMotionRotation();var origin=positionVector();var end=origin.add(getDeltaMovement());var block=SourceBottleRaycast860.blocks(SourceXpBottle860.this,origin,end);HitResult hit=block;if(!level().isClientSide){var entity=SourceBottleRaycast860.entities(SourceXpBottle860.this,origin,block==null?end:block.getLocation(),source.airTicks);if(entity!=null)hit=entity;}return hit;}

  private Vec3 positionVector(){return SourceXpBottle860.this.position();}

  public boolean portal(Object raw){return raw instanceof BlockHitResult hit&&level().getBlockState(hit.getBlockPos()).is(Blocks.NETHER_PORTAL);}

  public void enterPortal(Object raw){handleInsidePortal(((BlockHitResult)raw).getBlockPos());}

  public void impact(Object raw){pushMotionRotation();var hit=(HitResult)raw;if(!ForgeEventFactory.onProjectileImpact(SourceXpBottle860.this,hit))onHit(hit);pull();}

  public boolean water(){return isInWater();}public void bubble(double x,double y,double z,double vx,double vy,double vz){level().addParticle(ParticleTypes.BUBBLE,x,y,z,vx,vy,vz);}public void discard(){SourceXpBottle860.this.discard();}

  public Object findPlayer(String name){for(var player:level().players())if(player.getGameProfile().getName().equals(name))return player;return null;}public String playerName(Object raw){return raw instanceof Player player?player.getGameProfile().getName():null;}

 }

 @Override public void addAdditionalSaveData(CompoundTag tag){

  boolean legacy=legacyNativeOwner&&getOwner()==null;super.addAdditionalSaveData(tag);if(!legacy)tag.remove("Owner");tag.remove("LeftOwner");tag.remove("HasBeenShot");tag.remove("Item");tag.putBoolean("SourceXpBottleLegacyOwner860",legacy);SourceSnowballRules.save(host,new Tags(tag));tag.putBoolean("SourceXpBottleUq860",true);

  tag.putInt("SourceTileX",source.tileX);tag.putInt("SourceTileY",source.tileY);tag.putInt("SourceTileZ",source.tileZ);var block=BuiltInRegistries.BLOCK.byId(source.inTile);tag.putString("SourceInTile",BuiltInRegistries.BLOCK.getKey(block).toString());

 }

 @Override public void readAdditionalSaveData(CompoundTag tag){

  legacyNativeOwner=!tag.getBoolean("SourceXpBottleUq860")||tag.getBoolean("SourceXpBottleLegacyOwner860");var nativeTag=tag.copy();if(!legacyNativeOwner){nativeTag.remove("Owner");nativeTag.remove("LeftOwner");nativeTag.remove("HasBeenShot");}super.readAdditionalSaveData(nativeTag);SourceSnowballRules.load(source,new Tags(tag));

  source.tileX=tileCoordinate(tag,0);source.tileY=tileCoordinate(tag,1);source.tileZ=tileCoordinate(tag,2);if(tag.contains("SourceInTile")){var id=ResourceLocation.tryParse(tag.getString("SourceInTile"));source.inTile=BuiltInRegistries.BLOCK.getId(id==null?Blocks.AIR:BuiltInRegistries.BLOCK.get(id));}

  source.airTicks=source.groundTicks=0;raycastOffset=0;pull();

 }

 private static int tileCoordinate(CompoundTag tag,int axis){String full=new String[]{"SourceTileX","SourceTileY","SourceTileZ"}[axis],old=new String[]{"xTile","yTile","zTile"}[axis];if(tag.contains(full))return tag.getInt(full);int value=tag.getShort(old);var p=tag.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);if(tag.getByte("inGround")==1&&p.size()==3){int ref=net.minecraft.util.Mth.floor(p.getDouble(axis)),candidate=(ref&~65535)|(value&65535);if(candidate-ref>32768)candidate-=65536;else if(ref-candidate>32768)candidate+=65536;return candidate;}return value;}

 private record Tags(CompoundTag tag) implements SourceSnowballRules.Tags {public void putShort(String name,short value){tag.putShort(name,value);}public void putByte(String name,byte value){tag.putByte(name,value);}public void putString(String name,String value){tag.putString(name,value);}public short getShort(String name){return tag.getShort(name);}public byte getByte(String name){return tag.getByte(name);}public String getString(String name){return tag.getString(name);}}

 public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){

  var stack=player.getItemInHand(hand);SourceExperienceBottle854.use(new SourceExperienceBottle854.Use(){public boolean server(){return !level.isClientSide;}public boolean creative(){return player.getAbilities().instabuild;}public void consume(){SourceHeldConversion1037.convert(player,hand,net.minecraft.world.item.ItemStack.EMPTY);}public float itemRandom(){return ITEM_RANDOM.nextFloat();}public void bow(float volume,float pitch){level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.NEUTRAL,volume,pitch);}public void spawn(){level.addFreshEntity(new SourceXpBottle860(level,player));}});

  return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);

 }

}


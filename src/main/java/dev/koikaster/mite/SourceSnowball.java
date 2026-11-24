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
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** Actual SNOWBALL registry owner. uq owns the flight; native Snowball's
 * zero-damage impact and duplicate entity-state particle broadcast do not run. */
public final class SourceSnowball extends Snowball {
 private static final Random ITEM_RANDOM=LegacyItemRandom749.RANDOM;
 private final SourceSnowballRules.State source=new SourceSnowballRules.State();
 private final Flight host=new Flight();private int raycastOffset;private boolean legacyNativeOwner;
 public SourceSnowball(EntityType<? extends Snowball> type,Level level){super(type,level);}
 public SourceSnowball(Level level,LivingEntity thrower){
  this(EntityType.SNOWBALL,level);setOwner(thrower);
  SourceSnowballRules.thrown(host,new SourceSnowballRules.Thrower(thrower,thrower.getX(),thrower.getY(),thrower.getZ(),thrower.getYRot(),thrower.getXRot(),thrower.getEyeHeight(),thrower instanceof Player,thrower instanceof Player player?PlayerState.level(player):0,!thrower.isPassenger()&&!thrower.onGround()&&(thrower.isInWater()||thrower.isInLava())));
 }
 public SourceSnowball(Level level,double x,double y,double z){this(EntityType.SNOWBALL,level);setPos(x,y,z);pull();}
 @Override public void setOwner(Entity owner){if(source!=null)source.owner=owner;super.setOwner(owner);}
 @Override public Entity getOwner(){if(source==null)return super.getOwner();if(source.owner==null&&legacyNativeOwner)source.owner=super.getOwner();return (Entity)SourceSnowballRules.owner(host);}
 @Override public ItemStack getItem(){return new ItemStack(Items.SNOWBALL);}
 @Override public boolean shouldRenderAtSqrDistance(double distance){return SourceSnowballRules.renderDistance(getBoundingBox().getSize(),distance);}
 @Override public void shoot(double x,double y,double z,float velocity,float wander){pull();SourceSnowballRules.shoot(host,x,y,z,velocity,wander);}
 @Override public void lerpMotion(double x,double y,double z){pull();SourceSnowballRules.receivedMotion(host,x,y,z);}
 @Override public void tick(){pull();SourceSnowballRules.tick(host);}
 int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 private void pull(){source.x=getX();source.y=getY();source.z=getZ();var v=getDeltaMovement();source.vx=v.x;source.vy=v.y;source.vz=v.z;source.yaw=getYRot();source.pitch=getXRot();source.oldYaw=yRotO;source.oldPitch=xRotO;}
 private void pushMotionRotation(){setDeltaMovement(source.vx,source.vy,source.vz);setYRot(source.yaw);setXRot(source.pitch);yRotO=source.oldYaw;xRotO=source.oldPitch;}
 @Override protected void onHit(HitResult hit){SourceSnowballRules.impact(new Impact(hit));}
 private void blockCallback(BlockHitResult hit){super.onHitBlock(hit);}
 private void damage(Entity victim,float amount){
  if(getOwner() instanceof Player&&!(victim instanceof LivingEntity)&&!SourceSnowballTraits.playerDamage(victim))return;
  var damage=damageSources().thrown(this,getOwner());
  if(victim instanceof LivingEntity living){try(var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){victim.hurt(damage,amount);frame.finish();}}
  else victim.hurt(damage,amount);
 }
 private final class Impact implements SourceSnowballRules.Impact {
  final HitResult hit;final Entity victim;Impact(HitResult hit){this.hit=hit;victim=hit instanceof EntityHitResult e?e.getEntity():null;}
  public boolean server(){return !level().isClientSide;}public boolean entity(){return victim!=null;}
  public boolean blazeOrFireElemental(){return SourceSnowballTraits.fireTarget(victim);}public boolean magmaOrNetherspawn(){return victim instanceof net.minecraft.world.entity.monster.MagmaCube||victim instanceof SourceNetherspawn;}
  public void quench(){SourceChemicalFx.entity(victim,SourceChemicalFx.ENTITY_STEAM);}public void damage(float amount){SourceSnowball.this.damage(victim,amount);}
  public boolean burning(){return victim.isOnFire();}public double globalRandom(){return Math.random();}public void extinguish(){victim.clearFire();}
  public boolean magmaEarth(){return victim instanceof Mobs.Undead e&&e.earthBody()&&e.originalEarth().magma();}public void normalizeEarth(){((Mobs.Undead)victim).originalEarth().normal(true);}
  public boolean neighborFire(){var h=(BlockHitResult)hit;return level().getBlockState(h.getBlockPos().relative(h.getDirection())).is(Blocks.FIRE);}
  public void douseNeighbor(){var h=(BlockHitResult)hit;var at=h.getBlockPos().relative(h.getDirection());level().levelEvent(null,1009,at,0);level().removeBlock(at,false);}
  public void blockCallback(){SourceSnowball.this.blockCallback((BlockHitResult)hit);}public void poof(){level().addParticle(ParticleTypes.ITEM_SNOWBALL,getX(),getY(),getZ(),0,0,0);}public void discard(){SourceSnowball.this.discard();}
 }
 private final class Flight implements SourceSnowballRules.Host {
  public SourceSnowballRules.State state(){return source;}public void pose(){moveTo(source.x,source.y,source.z,source.yaw,source.pitch);}
  public void position(){setPos(source.x,source.y,source.z);pushMotionRotation();}public void changedMotionRotation(){pushMotionRotation();}
  public void parentTick(){xOld=source.lastX;yOld=source.lastY;zOld=source.lastZ;((SourceThrowableNativeParent)(Object)SourceSnowball.this).mite$throwableEntityTick();pull();}
  public double gaussian(){return random.nextGaussian();}public float nextFloat(){return random.nextFloat();}public boolean server(){return !level().isClientSide;}
  public int block(int x,int y,int z){return BuiltInRegistries.BLOCK.getId(level().getBlockState(new BlockPos(x,y,z)).getBlock());}
  public Object raycast(){pushMotionRotation();var origin=positionVector();var end=origin.add(getDeltaMovement());var block=SourceSnowballRaycast.blocks(SourceSnowball.this,origin,end);HitResult hit=block;if(!level().isClientSide){var entity=SourceSnowballRaycast.entities(SourceSnowball.this,origin,block==null?end:block.getLocation(),source.airTicks);if(entity!=null)hit=entity;}return hit;}
  private Vec3 positionVector(){return SourceSnowball.this.position();}
  public boolean portal(Object raw){return raw instanceof BlockHitResult hit&&level().getBlockState(hit.getBlockPos()).is(Blocks.NETHER_PORTAL);}
  public void enterPortal(Object raw){handleInsidePortal(((BlockHitResult)raw).getBlockPos());}
  public void impact(Object raw){pushMotionRotation();var hit=(HitResult)raw;if(!ForgeEventFactory.onProjectileImpact(SourceSnowball.this,hit))onHit(hit);pull();}
  public boolean water(){return isInWater();}public void bubble(double x,double y,double z,double vx,double vy,double vz){level().addParticle(ParticleTypes.BUBBLE,x,y,z,vx,vy,vz);}public void discard(){SourceSnowball.this.discard();}
  public Object findPlayer(String name){for(var player:level().players())if(player.getGameProfile().getName().equals(name))return player;return null;}public String playerName(Object raw){return raw instanceof Player player?player.getGameProfile().getName():null;}
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){
  super.addAdditionalSaveData(tag);tag.remove("Owner");tag.remove("LeftOwner");tag.remove("HasBeenShot");tag.remove("Item");SourceSnowballRules.save(host,new Tags(tag));tag.putBoolean("SourceSnowballUq",true);
  tag.putInt("SourceTileX",source.tileX);tag.putInt("SourceTileY",source.tileY);tag.putInt("SourceTileZ",source.tileZ);var block=BuiltInRegistries.BLOCK.byId(source.inTile);tag.putString("SourceInTile",BuiltInRegistries.BLOCK.getKey(block).toString());
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){
  legacyNativeOwner=!tag.getBoolean("SourceSnowballUq");var nativeTag=tag.copy();if(!legacyNativeOwner){nativeTag.remove("Owner");nativeTag.remove("LeftOwner");nativeTag.remove("HasBeenShot");}super.readAdditionalSaveData(nativeTag);SourceSnowballRules.load(source,new Tags(tag));
  source.tileX=tileCoordinate(tag,0);source.tileY=tileCoordinate(tag,1);source.tileZ=tileCoordinate(tag,2);if(tag.contains("SourceInTile")){var id=ResourceLocation.tryParse(tag.getString("SourceInTile"));source.inTile=BuiltInRegistries.BLOCK.getId(id==null?Blocks.AIR:BuiltInRegistries.BLOCK.get(id));}
  source.airTicks=source.groundTicks=0;raycastOffset=0;pull();
 }
 private static int tileCoordinate(CompoundTag tag,int axis){String full=new String[]{"SourceTileX","SourceTileY","SourceTileZ"}[axis],old=new String[]{"xTile","yTile","zTile"}[axis];if(tag.contains(full))return tag.getInt(full);int value=tag.getShort(old);var p=tag.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);if(tag.getByte("inGround")==1&&p.size()==3){int ref=net.minecraft.util.Mth.floor(p.getDouble(axis)),candidate=(ref&~65535)|(value&65535);if(candidate-ref>32768)candidate-=65536;else if(ref-candidate>32768)candidate+=65536;return candidate;}return value;}
 private record Tags(CompoundTag tag) implements SourceSnowballRules.Tags {public void putShort(String name,short value){tag.putShort(name,value);}public void putByte(String name,byte value){tag.putByte(name,value);}public void putString(String name,String value){tag.putString(name,value);}public short getShort(String name){return tag.getShort(name);}public byte getByte(String name){return tag.getByte(name);}public String getString(String name){return tag.getString(name);}}
 public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);SourceSnowballRules.itemUse(new SourceSnowballRules.ItemUse(){public boolean client(){return level.isClientSide;}public boolean creative(){return player.getAbilities().instabuild;}public void bob(){SourceSnowballEffects.bob(hand);}public void consume(){SourceHeldConversion1037.convert(player,hand,net.minecraft.world.item.ItemStack.EMPTY);}public float itemRandom(){return ITEM_RANDOM.nextFloat();}public void bow(float volume,float pitch){level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.NEUTRAL,volume,pitch);}public void spawn(){level.addFreshEntity(new SourceSnowball(level,player));}});
  return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
}

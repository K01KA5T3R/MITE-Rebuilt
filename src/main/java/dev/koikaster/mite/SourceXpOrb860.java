package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

/** Normal experience_orb ID. Source balls stay independent; pre-port native Count saves keep their native branch. */
public final class SourceXpOrb860 extends ExperienceOrb {
 private final SourceExperienceOrb854.State source=new SourceExperienceOrb854.State();
 private final Flight host=new Flight();private boolean sourceActive=true;
 public SourceXpOrb860(EntityType<? extends ExperienceOrb> type,Level level){super(type,level);refreshDimensions();setPos(getX(),getY(),getZ());}
 public SourceXpOrb860(Level level,double x,double y,double z,int value){
  this(EntityType.EXPERIENCE_ORB,level);SourceExperienceOrb854.init(new SourceExperienceOrb854.Constructor(){
   public SourceExperienceOrb854.State state(){return source;}public void size(float width,float height){refreshDimensions();}public void yOffset(float value){}
   public void position(double x,double y,double z){setPos(x,y,z);pull();}public boolean server(){return !level.isClientSide;}public double globalRandom(){return Math.random();}
  },x,y,z,value);push();
 }
 public static boolean active(Object actor){return actor instanceof SourceXpOrb860 orb&&orb.sourceActive;}
 public static void awardSource860(ServerLevel level,Vec3 at,int experience){for(int remaining=experience;remaining>0;){int part=SourceExperienceOrb854.split(remaining);remaining-=part;level.addFreshEntity(new SourceXpOrb860(level,at.x,at.y,at.z,part));}}
 public SourceXpOrb860 bottle(){source.bottle=true;return this;}
 public void owner(String name){SourceExperienceOrb854.owner(source,name);}
 public boolean fromBottle(){return source.bottle;}
 @Override public EntityDimensions getDimensions(Pose pose){return sourceActive?EntityDimensions.fixed(.25f,.25f):super.getDimensions(pose);}
 @Override public void setPos(double x,double y,double z){super.setPos(x,y,z);if(sourceActive)setBoundingBox(getBoundingBox().move(0,-.125,0));}
 private void pull(){source.x=getX();source.y=getY();source.z=getZ();var motion=getDeltaMovement();source.vx=motion.x;source.vy=motion.y;source.vz=motion.z;}
 private void push(){setDeltaMovement(source.vx,source.vy,source.vz);value=source.value;}
 @Override public void tick(){if(!sourceActive){super.tick();return;}pull();SourceExperienceOrb854.tick(host);push();}
 private final class Flight implements SourceExperienceOrb854.Host {
  public SourceExperienceOrb854.State state(){return source;}public void parentTick(){SourceXpOrb860.super.baseTick();pull();}public boolean server(){return !level().isClientSide;}public boolean fullLava(){return SourceOrbGeometry860.fullLava(SourceXpOrb860.this);}
  public void pushOut(){push();SourceOrbGeometry860.escape(SourceXpOrb860.this);pull();}public int entityId(){return getId();}public Iterable<?> players(){return level().players();}public String name(Object p){return ((Player)p).getGameProfile().getName();}
  public double distanceSquared(Object p){return ((Player)p).distanceToSqr(SourceXpOrb860.this);}public Object closest(double range){Player best=null;double distance=-1;for(var p:level().players()){if(p.isSpectator()||p.isRemoved()||p.getHealth()<=0)continue;double next=p.distanceToSqr(SourceXpOrb860.this);if(next<range*range&&(distance<0||next<distance)){distance=next;best=p;}}return best;}
  public double playerX(Object p){return ((Player)p).getX();}public double playerY(Object p){return ((Player)p).getY();}public double playerZ(Object p){return ((Player)p).getZ();}public float playerEye(Object p){return ((Player)p).getEyeHeight();}
  public void move(double x,double y,double z){push();SourceXpOrb860.this.move(MoverType.SELF,new Vec3(x,y,z));pull();}public boolean onGround(){return SourceXpOrb860.this.onGround();}
  public int belowBlock(){var at=new BlockPos(net.minecraft.util.Mth.floor(source.x),net.minecraft.util.Mth.floor(getBoundingBox().minY)-1,net.minecraft.util.Mth.floor(source.z));return BuiltInRegistries.BLOCK.getId(level().getBlockState(at).getBlock());}
  public float slipperiness(int id){return BuiltInRegistries.BLOCK.byId(id).getFriction();}public void discard(){SourceXpOrb860.this.discard();}
 }
 @Override public int getValue(){return sourceActive?source.value:super.getValue();}
 @Override public int getIcon(){return sourceActive?SourceExperienceOrb854.icon(source.value):super.getIcon();}
 @Override public boolean fireImmune(){return sourceActive||super.fireImmune();}
 @Override public boolean isAttackable(){return sourceActive?false:super.isAttackable();}
 private boolean admitted(DamageSource damage,float amount){
  if(level().isClientSide)return true;if(amount<=0||isRemoved()||isInvulnerable()||damage instanceof SourceKnockbackOnly838)return false;
  if(damage.getDirectEntity() instanceof SourceSnowball&&damage.getEntity() instanceof Player)return false;
  boolean absolute=damage.is(DamageTypes.IN_WALL)||damage.is(DamageTypes.FELL_OUT_OF_WORLD)||damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
  if(absolute)return true;
  if(damage.is(DamageTypeTags.IS_FIRE)||damage.is(DamageTypes.LAVA)||damage.is(DamageTypes.DROWN)||damage.is(DamageTypes.CACTUS)||damage.is(DamageTypes.FALLING_ANVIL)||damage.is(DamageTypes.FALLING_BLOCK)||damage.is(DamageTypes.WITHER))return false;
  if(damage.getDirectEntity() instanceof SourceGelatinousSphere sphere&&sphere.subtype()!=3&&sphere.subtype()!=4)return false;
  return true;
 }
 private static final class Result implements SourceExperienceOrb854.Result {boolean destroyed;public boolean destroyed(){return destroyed;}public void start(int health){}public void finish(int health){}public void destroyed(boolean value){destroyed=value;}}
 @Override public boolean hurt(DamageSource damage,float amount){
  if(!sourceActive)return super.hurt(damage,amount);
  var result=SourceExperienceOrb854.hurt(new SourceExperienceOrb854.Hurt(){public SourceExperienceOrb854.State state(){return source;}public SourceExperienceOrb854.Result parent(){return admitted(damage,amount)?new Result():null;}public float amount(){return amount;}public void attacked(){markHurt();}public void discard(){SourceXpOrb860.this.discard();}});return result!=null;
 }
 @Override public void playerTouch(Player player){
  if(!sourceActive){super.playerTouch(player);return;}pull();if(level().isClientSide||!SourceOrbGeometry860.reachable(this,player)||source.pickupDelay!=0||player.takeXpDelay!=0||source.ownerName!=null&&!source.ownerName.equals(player.getGameProfile().getName()))return;
  if(MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(player,this)))return;
  SourceExperienceOrb854.pickup(new SourceExperienceOrb854.Pickup(){
   public SourceExperienceOrb854.State state(){return source;}public boolean server(){return true;}public boolean reachable(){return true;}public int playerDelay(){return player.takeXpDelay;}public String playerName(){return player.getGameProfile().getName();}public void playerDelay(int value){player.takeXpDelay=value;}public void take(int count){player.take(SourceXpOrb860.this,count);}
   public void experience(int value,boolean createdByBottle){
    if(value>0){player.increaseScore(value);level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.1f,.5f*((player.getRandom().nextFloat()-player.getRandom().nextFloat())*.7f+1.8f));}
    player.giveExperiencePoints(value);
   }
   public void discard(){SourceXpOrb860.this.discard();}
  });
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){if(!sourceActive){super.addAdditionalSaveData(tag);return;}SourceExperienceOrb854.save(source,new Tags(tag));tag.putBoolean("SourceOrb860",true);}
 @Override public void readAdditionalSaveData(CompoundTag tag){sourceActive=tag.getBoolean("SourceOrb860");if(!sourceActive){super.readAdditionalSaveData(tag);refreshDimensions();return;}SourceExperienceOrb854.load(source,new Tags(tag));refreshDimensions();setPos(getX(),getY(),getZ());push();}
 @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return sourceActive?new ClientboundAddEntityPacket(this,source.value):super.getAddEntityPacket();}
 @Override public void recreateFromPacket(ClientboundAddEntityPacket packet){sourceActive=true;source.value=packet.getData();value=source.value;refreshDimensions();super.recreateFromPacket(packet);}
 private record Tags(CompoundTag tag) implements SourceExperienceOrb854.Tags {public void putShort(String n,short v){tag.putShort(n,v);}public void putString(String n,String v){tag.putString(n,v);}public void putBoolean(String n,boolean v){tag.putBoolean(n,v);}public short getShort(String n){return tag.getShort(n);}public String getString(String n){return tag.getString(n);}public boolean getBoolean(String n){return tag.getBoolean(n);}public boolean contains(String n){return tag.contains(n);}}
}

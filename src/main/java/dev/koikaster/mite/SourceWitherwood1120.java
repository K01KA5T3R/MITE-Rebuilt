package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Runtime adapters only for the new flattened R195 deadbush subtype1. */
public final class SourceWitherwood1120 {
 private static final ThreadLocal<Integer> dropReason=new ThreadLocal<>(); // 0 none, 1 source self, absent normal
 private static final ThreadLocal<MobEffectInstance> adding=new ThreadLocal<>();
 private static final EffectValues VALUES=new EffectValues();
 static boolean ground(BlockState state){return state.getBlock() instanceof SourceWitherwoodContent1120.Bush;}
 static boolean potted(BlockState state){return state.getBlock() instanceof SourceWitherwoodContent1120.Pot;}
 public static boolean dropResources(Object rawState,Object rawLevel,Object rawPos){
  var state=(BlockState)rawState;if(!ground(state)&&!potted(state))return false;
  if(rawLevel instanceof ServerLevel level){var pos=(BlockPos)rawPos;if(potted(state)){emit(level,pos,state,new ItemStack(Items.FLOWER_POT),1);emit(level,pos,state,new ItemStack(SourceWitherwoodContent1120.WITHERWOOD.get()),1);}else drop(level,pos,state,false);}
  return true;
 }
 public static int soilDrop(Object rawLevel,Object rawPos,Object rawState){var state=(BlockState)rawState;return ground(state)?drop((ServerLevel)rawLevel,(BlockPos)rawPos,state,false):-1;}
 static int drop(ServerLevel level,BlockPos pos,BlockState state,boolean exploded){Integer reason=dropReason.get();if(reason!=null&&reason==0)return 0;int choice=SourceWitherwoodRules1120.drop(false,reason!=null&&reason==1,exploded);return choice==0?0:emit(level,pos,state,choice==1?new ItemStack(SourceWitherwoodContent1120.WITHERWOOD.get()):new ItemStack(Items.STICK),choice==1?1:.5f);}
 static int emit(ServerLevel level,BlockPos pos,BlockState state,ItemStack stack,float chance){
  if(stack.isEmpty()||!(level.random.nextFloat()<chance))return 0;
  if(level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){
   var shape=state.getShape(level,pos);var bounds=shape.isEmpty()?new AABB(0,0,0,1,1,1):shape.bounds();
   double cx=(bounds.minX+bounds.maxX)*.5,cy=(bounds.minY+bounds.maxY)*.5,cz=(bounds.minZ+bounds.maxZ)*.5;
   double minX=cx+(bounds.minX-cx)*.5,minY=cy+(bounds.minY-cy)*.5,minZ=cz+(bounds.minZ-cz)*.5,maxX=cx+(bounds.maxX-cx)*.5,maxY=cy+(bounds.maxY-cy)*.5,maxZ=cz+(bounds.maxZ-cz)*.5;
   var item=SourcePotDrop1110.create(level,pos.getX()+level.random.nextFloat()*(maxX-minX)+minX,pos.getY()+level.random.nextFloat()*(maxY-minY)+minY,pos.getZ()+level.random.nextFloat()*(maxZ-minZ)+minZ,stack.copy());item.setDefaultPickUpDelay();level.addFreshEntity(item);
  }return 1;
 }
 static List<ItemStack> queriedDrops(ServerLevel level,boolean exploded){if(exploded)return List.of();Integer reason=dropReason.get();if(reason!=null)return reason==0?List.of():List.of(new ItemStack(SourceWitherwoodContent1120.WITHERWOOD.get()));return level.random.nextFloat()<.5f?List.of(new ItemStack(Items.STICK)):List.of();}
 static void illegal(ServerLevel level,BlockPos pos){boolean near=level.getNearestPlayer((float)pos.getX()+.5f,(float)pos.getY()+.5f,(float)pos.getZ()+.5f,16,false)!=null;var previous=dropReason.get();dropReason.set(near?1:0);try{level.destroyBlock(pos,near);}finally{if(previous==null)dropReason.remove();else dropReason.set(previous);}}
 public static void contact(Level level,Entity entity,boolean potted){var h=new Effects(level,entity);if(potted)SourceWitherwoodRules1120.pottedContact(h);else SourceWitherwoodRules1120.contact(h);}
 public static Boolean merge(Object rawCurrent,Object rawIncoming){if(adding.get()!=rawIncoming)return null;var current=(MobEffectInstance)rawCurrent;var incoming=(MobEffectInstance)rawIncoming;if(current.getEffect()!=MobEffects.WITHER||incoming.getEffect()!=MobEffects.WITHER)return null;SourceWitherwoodRules1120.merge(VALUES,current,incoming);return Boolean.TRUE;}
 private static class EffectValues implements SourceWitherwoodRules1120.Merge<MobEffectInstance> {
  public int amplifier(MobEffectInstance e){return e.getAmplifier();}public int duration(MobEffectInstance e){return e.getDuration();}public boolean ambient(MobEffectInstance e){return e.isAmbient();}
  public void duration(MobEffectInstance e,int value){((SourceWitherEffectAccess1120)(Object)e).mite$witherDuration1120(value);}public void amplifier(MobEffectInstance e,int value){((SourceWitherEffectAccess1120)(Object)e).mite$witherAmplifier1120(value);}public void ambient(MobEffectInstance e,boolean value){((SourceWitherEffectAccess1120)(Object)e).mite$witherAmbient1120(value);}
 }
 private static final class Effects extends EffectValues implements SourceWitherwoodRules1120.Effects<MobEffectInstance> {
  final Level level;final Entity entity;Effects(Level l,Entity e){level=l;entity=e;}
  public boolean server(){return !level.isClientSide;}public boolean living(){return entity instanceof LivingEntity;}
  public MobEffectInstance current(){return ((LivingEntity)entity).getEffect(MobEffects.WITHER);}public int amplifier(MobEffectInstance e){return e.getAmplifier();}public int duration(MobEffectInstance e){return e.getDuration();}public int interval(MobEffectInstance e){return Math.max(1,40>>e.getAmplifier());}
  public void duration(MobEffectInstance e,int value){((SourceWitherEffectAccess1120)(Object)e).mite$witherDuration1120(value);}public void amplifier(MobEffectInstance e,int value){((SourceWitherEffectAccess1120)(Object)e).mite$witherAmplifier1120(value);}public boolean player(){return entity instanceof net.minecraft.world.entity.player.Player;}public void sync(MobEffectInstance e){if(entity instanceof ServerPlayer p)p.connection.send(new ClientboundUpdateMobEffectPacket(p.getId(),e));}
  public void add(int duration,int amplifier){var actor=(LivingEntity)entity;if(actor.getMobType()==MobType.UNDEAD||actor instanceof Mobs.DireWolf wolf&&wolf.sourceHellhound())return;var incoming=new MobEffectInstance(MobEffects.WITHER,duration,amplifier);var previous=adding.get();adding.set(incoming);try{actor.addEffect(incoming);}finally{if(previous==null)adding.remove();else adding.set(previous);}}
 }
 public static void particles(Level level,BlockPos pos){if(!level.isClientSide)return;SourceWitherwoodRules1120.particles(new SourceWitherwoodRules1120.Particles(){public int nextInt(int bound){return level.random.nextInt(bound);}public float nextFloat(){return level.random.nextFloat();}public void particle(double x,double y,double z){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientSacredParticles1120.spawn(level,x,y,z));}},pos.getX(),pos.getY(),pos.getZ());}
 public static ClassicFuel.Fuel fuel(Object raw){var stack=(ItemStack)raw;return !stack.isEmpty()&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof SourceWitherwoodContent1120.Bush?new ClassicFuel.Fuel(200,1,"R195 deadbush"):null;}
 private SourceWitherwood1120(){}
}

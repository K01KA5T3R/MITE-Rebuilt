package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Server owner for abr. Native Explosion is only the callback/damage-source
 * carrier; neither native raycasting nor native explosion damage is invoked.
 * Netherspawn uses this owner; the source Creeper factory remains staged. */
public final class SourceExplosion extends Explosion implements SourceExplosionKernel.Host {
 private final ServerLevel world;private final Entity exploder;private final double x,y,z;
 private final float blockSize;private final Random fireRandom=new Random();private final SourceExplosionKernel kernel;
 private final List<ItemEntity> pendingDrops=new ArrayList<>();
 public SourceExplosion(ServerLevel level,Entity source,double x,double y,double z,float blocks,float living,boolean flaming,boolean smoking){
  super(level,source,x,y,z,blocks,flaming,smoking?BlockInteraction.DESTROY:BlockInteraction.KEEP);
  world=level;exploder=source;this.x=x;this.y=y;this.z=z;blockSize=blocks;
  boolean forced=source!=null&&Set.of("infernal_creeper","netherspawn").contains(Mobs.name(source));
  kernel=new SourceExplosionKernel(this,x,y,z,blocks,living,flaming,smoking,source instanceof SourceWitherSkull skull&&skull.isDangerous(),forced);
 }
 public static SourceExplosion create(ServerLevel world,Entity source,double x,double y,double z,float blocks,float living,boolean flaming,boolean smoking){
  var result=new SourceExplosion(world,source,x,y,z,blocks,living,flaming,smoking);
  if(net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world,result))return result;
  result.explode();result.finalizeExplosion(true);return result;
 }
 @Override public void explode(){kernel.phaseA();getToBlow().clear();for(var pos:kernel.affected())getToBlow().add(pos(pos));getHitPlayers().clear();for(var row:kernel.playerKnockback().entrySet())getHitPlayers().put((Player)row.getKey(),vec(row.getValue()));}
 @Override public void finalizeExplosion(boolean particles){
  kernel.phaseB(particles);
  for(var row:kernel.playerKnockback().entrySet())if(row.getKey() instanceof ServerPlayer player)SourceExplosionImpulse.send(player,vec(row.getValue()));
  // aqz.dropBlockAsItem_do queues explosion drops for total_time+1, so they do
  // not participate in the same explosion's entity pass or recursive drops.
  for(var drop:pendingDrops)SourceExplosionDrops.pending(world,drop);pendingDrops.clear();
 }
 SourceExplosionKernel sourceKernel(){return kernel;}
 ServerLevel world(){return world;}
 Entity exploder(){return exploder;}
 float blockSize(){return blockSize;}
 Vec3 origin(){return new Vec3(x,y,z);}
 void pending(ItemEntity item){pendingDrops.add(item);}
 private static BlockPos pos(SourceExplosionKernel.Pos value){return new BlockPos(value.x(),value.y(),value.z());}
 private static Vec3 vec(SourceExplosionKernel.Vector value){return new Vec3(value.x(),value.y(),value.z());}
 @Override public float nextWorldFloat(){return world.random.nextFloat();}
 @Override public int nextWorldInt(int bound){return world.random.nextInt(bound);}
 @Override public int nextFireInt(int bound){return fireRandom.nextInt(bound);}
 @Override public long totalTime(){return world.getGameTime();}
 private final class SourceBlock implements SourceExplosionKernel.BlockView {
  final BlockPos position;final BlockState state;
  SourceBlock(BlockPos position,BlockState state){this.position=position;this.state=state;}
  public float resistance(SourceExplosionKernel explosion){return SourceExplosionDrops.hardness(state,world);}
  public boolean chargedWitherProtected(){return state.is(Blocks.BEDROCK)||state.is(Blocks.END_PORTAL)||state.is(Blocks.END_PORTAL_FRAME)||state.getBlock() instanceof SourceMantleBlock;}
  public boolean mycelium(){return state.is(Blocks.MYCELIUM);}
  public boolean solidStandardForm(){return state.isSolidRender(world,position);}
 }
 @Override public SourceExplosionKernel.BlockView block(SourceExplosionKernel.Pos value){var at=pos(value);var state=world.getBlockState(at);return state.isAir()?null:new SourceBlock(at,state);}
 @Override public boolean exploderCanDestroy(SourceExplosionKernel explosion,SourceExplosionKernel.Pos position,SourceExplosionKernel.BlockView block,float force){
  if(exploder==null)return true;var at=pos(position);var state=block==null?Blocks.AIR.defaultBlockState():((SourceBlock)block).state;
  return exploder.shouldBlockExplode(this,world,at,state,force);
 }
 @Override public void breakFragile(SourceExplosionKernel explosion,SourceExplosionKernel.Pos position,SourceExplosionKernel.BlockView block){
  var captured=(SourceBlock)block;SourceExplosionDrops.drop(this,captured.position,captured.state,false);world.setBlock(captured.position,Blocks.AIR.defaultBlockState(),3);
 }
 @Override public void setAir(SourceExplosionKernel.Pos position){world.setBlock(pos(position),Blocks.AIR.defaultBlockState(),3);}
 @Override public void clearParticleBlock(SourceExplosionKernel.Pos position){world.setBlock(pos(position),Blocks.AIR.defaultBlockState(),2);}
 @Override public void myceliumToDirtAndUndermine(SourceExplosionKernel.Pos position){var at=pos(position);world.setBlock(at,Blocks.DIRT.defaultBlockState(),3);world.scheduleTick(at,Blocks.DIRT,1);}
 private final class SourceEntity implements SourceExplosionKernel.EntityView {
  final Entity entity;SourceEntity(Entity entity){this.entity=entity;}
  public Object identity(){return entity;}
  public boolean handleExplosion(SourceExplosionKernel explosion){return entity instanceof ItemEntity item&&SourceExplosionItems.handle(SourceExplosion.this,item);}
  public double x(){return entity.getX();}public double y(){return entity.getY();}public double z(){return entity.getZ();}
  public double eyeHeight(){return entity.getEyeHeight();}
  public SourceExplosionKernel.Box box(){var b=entity.getBoundingBox();return new SourceExplosionKernel.Box(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);}
  public boolean item(){return entity instanceof ItemEntity;}public boolean player(){return entity instanceof Player;}
  public void damage(SourceExplosionKernel explosion,float damage){entity.hurt(getDamageSource(),damage);}
  public double dampenedKnockback(double impact){
   int protection=entity instanceof LivingEntity living?EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION,living):0;
   return protection>0?impact-(double)(int)Math.floor(impact*(double)((float)protection*.15f)):impact;
  }
  public void addMotion(double dx,double dy,double dz){
   if(BossCombatTuning.knockbackImmune(entity))return;
   entity.setDeltaMovement(entity.getDeltaMovement().add(dx,dy,dz));if(!(entity instanceof Player))entity.hasImpulse=true;
  }
 }
 @Override public List<? extends SourceExplosionKernel.EntityView> entities(SourceExplosionKernel.Box bounds){return world.getEntities(exploder,new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ())).stream().map(SourceEntity::new).toList();}
 @Override public List<? extends SourceExplosionKernel.Livestock> livestock(SourceExplosionKernel.Box bounds){
  return world.getEntitiesOfClass(Animal.class,new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()),SourceLivestockSpook::handles).stream().map(animal->new SourceExplosionKernel.Livestock(){public boolean removed(){return animal.isRemoved();}public void spookedUntil(long until){SourceLivestockSpook.set(animal,until);}}).toList();
 }
 @Override public boolean physicalLine(SourceExplosionKernel.Vector from,SourceExplosionKernel.Vector to){
  return OriginalRaycast.explosionLine(world,exploder,vec(from),vec(to));
 }
 @Override public void centerEffect(double x,double y,double z,float pitch,boolean huge){
  // Explicit seed avoids modern playSound's extra world RNG draw.
  world.playSeededSound(null,x,y,z,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,4,pitch,0L);
  world.sendParticles(huge?ParticleTypes.EXPLOSION_EMITTER:ParticleTypes.EXPLOSION,x,y,z,0,1,0,0,1);
 }
 @Override public void blockParticles(SourceExplosionKernel.Vector midpoint,SourceExplosionKernel.Vector point,SourceExplosionKernel.Vector motion){
  world.sendParticles(ParticleTypes.POOF,midpoint.x(),midpoint.y(),midpoint.z(),0,motion.x(),motion.y(),motion.z(),1);
  world.sendParticles(ParticleTypes.SMOKE,point.x(),point.y(),point.z(),0,motion.x(),motion.y(),motion.z(),1);
 }
 @Override public void breakPhaseB(SourceExplosionKernel explosion,SourceExplosionKernel.Pos position,SourceExplosionKernel.BlockView captured){
  var block=(SourceBlock)captured;boolean portedCallback=block.state.getBlock() instanceof SourceRunestoneBlock||block.state.getBlock() instanceof SourceGiantBrownMushroomBlock;
  if(portedCallback||block.state.getBlock().dropFromExplosion(this))SourceExplosionDrops.drop(this,block.position,block.state,true);
  if(portedCallback)world.setBlock(block.position,Blocks.AIR.defaultBlockState(),3);else block.state.onBlockExploded(world,block.position,this);
 }
 @Override public void setFire(SourceExplosionKernel.Pos position){world.setBlock(pos(position),BaseFireBlock.getState(world,pos(position)),3);}
}

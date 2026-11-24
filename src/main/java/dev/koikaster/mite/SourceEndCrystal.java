package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** sj owner. Native beam/bottom data remain ritual transport; native tick and
 * native crystal hurt/explosion bodies are never run. */
public final class SourceEndCrystal extends EndCrystal implements SourceEndCrystalRules.Host<SourceEndCrystal.Result> {
 private static final EntityDataAccessor<Integer> SOURCE_HEALTH=SynchedEntityData.defineId(SourceEndCrystal.class,EntityDataSerializers.INT);
 private DamageSource incoming;private float incomingAmount;
 public SourceEndCrystal(EntityType<? extends EndCrystal> type,Level level){super(type,level);}
 public SourceEndCrystal(Level level,double x,double y,double z){this(EntityType.END_CRYSTAL,level);setPos(x,y,z);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SOURCE_HEALTH,5);}
 private boolean absolute(DamageSource source){return source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.GENERIC_KILL);}
 private boolean parentImmune(DamageSource source){return isRemoved()||isInvulnerable()||source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypes.LAVA)||source.is(DamageTypes.DROWN)||source.is(DamageTypes.FALLING_ANVIL)||source.is(DamageTypes.FALLING_BLOCK);}
 private Entity immediate(DamageSource source){return source.is(DamageTypeTags.IS_EXPLOSION)?source.getEntity() instanceof LivingEntity?source.getEntity():null:source.getDirectEntity();}
 private ItemStack weapon(DamageSource source){return immediate(source) instanceof LivingEntity living?living.getMainHandItem():ItemStack.EMPTY;}
 private boolean immune(DamageSource source){return SourceEndCrystalRules.immune(new SourceEndCrystalRules.Defense(){
  public boolean parentImmune(){return SourceEndCrystal.this.parentImmune(source);}
  public boolean melee(){var immediate=immediate(source);boolean projectile=immediate instanceof Projectile||source.is(DamageTypeTags.IS_PROJECTILE);boolean indirect=source.getEntity()!=null&&source.getEntity()!=immediate&&!projectile;return !projectile&&!indirect;}
  public boolean item(){return !weapon(source).isEmpty();}public boolean tool(){return weapon(source).getItem() instanceof TieredItem||weapon(source).getItem() instanceof ShearsItem;}
  public boolean effectiveAgainstMithrilBlock(){var stack=weapon(source);return stack.getItem() instanceof PickaxeItem&&HarvestRules.harvestLevel(stack)>=5;}
 });}
 @Override public boolean isInvulnerableTo(DamageSource source){return isRemoved()||isInvulnerable()||!absolute(source)&&immune(source);}
 @Override public boolean hurt(DamageSource source,float amount){incoming=source;incomingAmount=amount;return SourceEndCrystalRules.hurt(this)!=null;}
 @Override public Result parentDamage(){if(level().isClientSide||incomingAmount<=0||isRemoved()||isInvulnerable()||!absolute(incoming)&&immune(incoming))return null;return new Result();}
 public static final class Result implements SourceEndCrystalRules.Result {boolean destroyed;public void destroyed(){destroyed=true;}}
 @Override public int health(){return entityData.get(SOURCE_HEALTH);}@Override public void health(int value){entityData.set(SOURCE_HEALTH,value);}
 @Override public void explode(){if(level() instanceof ServerLevel world){SourceExplosion.create(world,null,getX(),getY(),getZ(),6,6,false,true);var fight=world.getDragonFight();if(fight!=null)fight.onCrystalDestroyed(this,incoming);}}
 @Override public void tick(){SourceEndCrystalRules.tick(this);}
 @Override public void previousPosition(){xo=getX();yo=getY();zo=getZ();xOld=getX();yOld=getY();zOld=getZ();}
 @Override public int animation(){return time;}@Override public void animation(int value){time=value;}@Override public void synchronizeHealth(){}
 @Override public double x(){return getX();}@Override public double y(){return getY();}@Override public double z(){return getZ();}
 @Override public boolean fire(int x,int y,int z){return level().getBlockState(new BlockPos(x,y,z)).is(Blocks.FIRE);}
 @Override public void fireAt(int x,int y,int z){level().setBlock(new BlockPos(x,y,z),Blocks.FIRE.defaultBlockState(),3);}
 @Override public boolean fireImmune(){return true;}
 // Explicit mod-owned boundary avoids remapping an inherited Entity method as an interface method.
 @Override public void miteDiscardCrystal(){super.discard();}
}

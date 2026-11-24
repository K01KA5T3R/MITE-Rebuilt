package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
/** ang contact rules and the three low sand metadata kill bits. */
public final class SourceCactusHarm {
 public static final IntegerProperty KILLS=IntegerProperty.create("source_cactus_kills",0,7);
 private record Contact(Level world,BlockPos pos){}
 private static final ThreadLocal<Contact> CONTACT=new ThreadLocal<>();
 public static void sandDefinition(Object builder){((StateDefinition.Builder<?,?>)builder).add(KILLS);}
 private static BlockPos sand(Level world,BlockPos cactus){var pos=cactus.below();while(world.getBlockState(pos).is(Blocks.CACTUS))pos=pos.below();return world.getBlockState(pos).getBlock() instanceof SandBlock?pos:null;}
 public static int kills(Level world,BlockPos cactus){var pos=sand(world,cactus);if(pos==null)return 0;var state=world.getBlockState(pos);return state.hasProperty(KILLS)?state.getValue(KILLS):0;}
 public static int activeKills(){var contact=CONTACT.get();return contact==null?0:kills(contact.world,contact.pos);}
 public static void removed(Object rawLevel,Object rawPos,Object rawNext){var world=(Level)rawLevel;var pos=(BlockPos)rawPos;if(world.isClientSide||((BlockState)rawNext).is(Blocks.CACTUS))return;var below=pos.below();var state=world.getBlockState(below);if(state.getBlock() instanceof SandBlock&&state.hasProperty(KILLS))world.setBlock(below,state.setValue(KILLS,0),4);}
 public static boolean hurt(Object rawActor,Object rawDamage,float amount,Object rawLevel,Object rawPos){
  var actor=(Entity)rawActor;var world=(Level)rawLevel;var pos=(BlockPos)rawPos;if(!(world instanceof ServerLevel))return false;
  if(actor instanceof Mob mob&&mob.getDeltaMovement().length()<=(double).2f){
   var support=BlockPos.containing(mob.getX(),mob.getBoundingBox().minY-.01,mob.getZ());boolean standing=world.getBlockState(support).is(Blocks.CACTUS);
   boolean entities=!world.getEntities(actor,actor.getBoundingBox(),Entity::isPushable).isEmpty();boolean blocks=world.getBlockCollisions(actor,actor.getBoundingBox()).iterator().hasNext();if(!standing&&!entities&&!blocks)return false;
  }
  boolean alive=actor.isAlive();boolean[] negative={false};var previous=CONTACT.get();CONTACT.set(new Contact(world,pos.immutable()));
  try{
   boolean accepted;if(actor instanceof LivingEntity living){try(var frame=SourceParentHurtFrame.beginObserver(living,result->negative[0]=result.negativelyAffected())){accepted=actor.hurt((DamageSource)rawDamage,amount);frame.finish();}}else accepted=actor.hurt((DamageSource)rawDamage,amount);boolean destroyed=alive&&!actor.isAlive();
   if(destroyed){var base=sand(world,pos);if(base!=null){var state=world.getBlockState(base);if(state.hasProperty(KILLS)&&state.getValue(KILLS)<7)world.setBlock(base,state.setValue(KILLS,state.getValue(KILLS)+1),4);}}
   if(actor instanceof Mob mob&&negative[0])mob.getPersistentData().putLong("MiteLastCactusHarm",world.getGameTime());return accepted;
  }finally{if(previous==null)CONTACT.remove();else CONTACT.set(previous);}
 }
 private SourceCactusHarm(){}
}

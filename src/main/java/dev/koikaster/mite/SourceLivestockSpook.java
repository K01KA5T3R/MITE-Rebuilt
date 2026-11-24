package dev.koikaster.mite;

import net.minecraft.world.entity.animal.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** abr's livestock spook and its propagation. Only source livestock participate;
 * the user's modern breeding/feeding choice remains in its existing owner. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceLivestockSpook {
 private static final String UNTIL="MiteSourceSpookedUntil",PEER="MiteSourceSpookedByPeer";
 static boolean handles(Animal animal){return animal instanceof Chicken||animal instanceof Cow||animal instanceof Pig||animal instanceof Sheep;}
 static boolean spooked(Animal animal){long until=animal.getPersistentData().getLong(UNTIL);if(until>animal.level().getGameTime())return true;if(until!=0)animal.getPersistentData().remove(UNTIL);return false;}
 static void set(Animal animal,long until){animal.getPersistentData().putLong(UNTIL,until);install(animal);}
 private static void install(Animal animal){SourceLivestockOwners.install(animal);}
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Animal animal&&handles(animal)&&spooked(animal))install(animal);}
 // EntityLivestock.c propagates AFTER its parent's AI, once per source offset.
 static void propagate(Animal animal){
  var data=animal.getPersistentData();long time=animal.level().getGameTime();if(data.getBoolean(PEER)&&time%4000==0)data.remove(PEER);
  if(!(SourceLivestockOwners.fleeingOrDecided(animal)||spooked(animal))||(animal.tickCount+animal.getId()*47)%20!=0)return;
  for(var other:animal.level().getEntitiesOfClass(Animal.class,animal.getBoundingBox().inflate(8,4,8),SourceLivestockSpook::handles)){
   if(other==animal||SourceLivestockOwners.fleeingOrDecided(other)||spooked(other)||other.getPersistentData().getBoolean(PEER)||other.isRemoved()||!other.getSensing().hasLineOfSight(animal))continue;
   var target=animal.blockPosition();var path=SourceArachnidPathBudget.block(other,target,8);var end=path==null?null:path.getEndNode();if(end==null||end.x!=target.getX()||end.y!=target.getY()||end.z!=target.getZ())continue;
   set(other,time+400+animal.level().random.nextInt(400));other.getPersistentData().putBoolean(PEER,true);
  }
 }
 private SourceLivestockSpook(){}
}

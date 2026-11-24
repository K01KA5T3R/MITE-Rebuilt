package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** nn/abw signed state and og/rp/tm removal branches. Native species absent from R195 keep their policy. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OriginalDespawn {
 private static final Set<String> ORIGINAL_TYPES=Set.of("bat","blaze","cave_spider","chicken","cow","creeper","ender_dragon","enderman","ghast","horse","donkey","mule","skeleton_horse","zombie_horse","iron_golem","magma_cube","mooshroom","ocelot","pig","sheep","silverfish","skeleton","slime","snow_golem","spider","squid","villager","witch","wither","wither_skeleton","wolf","zombie","zombie_villager","zombified_piglin");
 public static int counter(Entity entity){return ((SourceDespawnState)entity).mite$getDespawnCounter();}
 public static void set(Entity entity,int value){((SourceDespawnState)entity).mite$setDespawnCounter(value);}
 public static void refresh(Entity entity,int value){if(counter(entity)>value)set(entity,value);}
 /** The world increments immediately before each actual passenger/non-passenger entity update. */
 public static void tick(Object actor){var entity=(Entity)actor;set(entity,counter(entity)+1);}
 public static void save(Object actor,Object rawTag){((CompoundTag)rawTag).putInt("despawn_counter",counter((Entity)actor));}
 public static void load(Object actor,Object rawTag){set((Entity)actor,((CompoundTag)rawTag).getInt("despawn_counter"));}
 public static boolean handled(Mob mob){var id=ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());return id!=null&&(id.getNamespace().equals(MiteReforged.ID)||id.getNamespace().equals("minecraft")&&ORIGINAL_TYPES.contains(id.getPath()));}
 /** Leave native peaceful removal in its native entry; normal removal belongs inside server AI. */
 public static boolean suppressNative(Object actor){var mob=(Mob)actor;return handled(mob)&&mob.level().getDifficulty()!=Difficulty.PEACEFUL;}
 public static int renderedCap(Mob mob){return mob instanceof WaterAnimal?-9600:-400;}
 /** abw.getClosestPlayer: creative counts, spectators adapt ghost/tournament exclusions; radius is strict. */
 public static Player closest(Mob mob,double radius,boolean alive){Player result=null;double best=-1;for(var player:mob.level().players()){if(player.isSpectator()||alive&&!player.isAlive())continue;double distance=player.distanceToSqr(mob);if(radius>=0&&distance>=radius*radius||best!=-1&&distance>=best)continue;best=distance;result=player;}return result;}
 private static boolean renderCandidate(Mob mob,double squared){var box=mob.getBoundingBox();double edge=(box.getXsize()+box.getYsize()+box.getZsize())/3;double distance=edge*64*1.25;return squared<distance*distance;}
 public static boolean inPlayerView(Mob mob,Player player){if(player.isSpectator()||mob.level()!=player.level())return false;double squared=player.distanceToSqr(mob);if(squared>4096&&!renderCandidate(mob,squared))return false;var eye=player.getEyePosition();var feet=mob.position();var upper=feet.add(0,mob.getBbHeight()+.5,0);return OriginalVision.line(player,eye,feet.add(0,mob.getBbHeight()*.75f,0),false)||OriginalVision.line(player,eye,feet.add(0,.1,0),false)||OriginalVision.line(player,eye,upper,false)||OriginalVision.line(player,eye.add(0,1,0),upper,false);}
 private static boolean baseMayDespawn(Mob mob){for(var player:mob.level().players())if(inPlayerView(mob,player)){refresh(mob,renderedCap(mob));return false;}return true;}
 private static boolean fenceNearby(Mob mob){var center=mob.blockPosition();for(var pos:BlockPos.betweenClosed(center.offset(-16,-1,-16),center.offset(16,1,16)))if(mob.level().getBlockState(pos).is(Blocks.OAK_FENCE))return true;return false;}
 /** og.canPathTo requires the final node to equal all three coordinates, not merely a non-null partial path. */
 private static boolean canPathTo(Mob mob,Player player){var target=player.blockPosition();var path=mob.getNavigation().createPath(target,0,48);if(path==null)return false;var end=path.getEndNode();return end!=null&&end.x==target.getX()&&end.y==target.getY()&&end.z==target.getZ();}
 public static boolean mayDespawn(Mob mob){
  if(mob instanceof AbstractGolem||mob instanceof Villager||mob instanceof Witch)return false;
  if(mob instanceof SourceZombieOwner owner&&owner.sourceConverting())return false;
  if(mob instanceof ZombieVillager zombie&&(zombie.isConverting()||zombie.getPersistentData().getBoolean("was_villager_that_was_generated_with_village")))return false;
  boolean hellhound=Mobs.name(mob).equals("hellhound");
  if(mob instanceof Wolf wolf&&(mob.getPersistentData().getBoolean("is_witch_ally")||mob.tickCount<=2400&&!hellhound||wolf.isTame()))return false;
  if(mob instanceof Ocelot cat&&(((SourceOcelotState)cat).mite$isTrusting()||mob.tickCount<=2400))return false;
  if(mob instanceof Animal&&!hellhound){if(counter(mob)<800)return false;if(closest(mob,1024,false)!=null||fenceNearby(mob)){set(mob,0);return false;}if(counter(mob)<=2400)return false;}
  if(!baseMayDespawn(mob))return false;
  if(mob instanceof Monster){var player=closest(mob,48,true);if(player!=null&&canPathTo(mob,player)){refresh(mob,-1200);return false;}}
  return true;
 }
 /** Preserve source branch order, including the random reset after a protected t() returned false. */
 public static void tryDespawn(Object actor){if(actor instanceof SourceGelatinousCube cube&&cube.deferSourceDespawn())return;if(actor instanceof SourceMagmaCube magma&&magma.deferSourceDespawn())return;var mob=(Mob)actor;if(!handled(mob)||mob.level().isClientSide||mob.isRemoved()||mob instanceof EnderDragon)return;if(mob instanceof WitherBoss||mob.isPersistenceRequired()){set(mob,0);return;}if(counter(mob)<200)return;var player=closest(mob,-1,false);if(player==null)return;double dx=player.getX()-mob.getX(),dy=player.getY()-mob.getY(),dz=player.getZ()-mob.getZ();int hour=(int)(Math.floorMod(mob.level().getDayTime(),24000)+6000)%24000/1000;if(hour!=5)dy*=2;boolean contained=mob instanceof SourceEnderman enderman&&enderman.valuables().hasItems()||mob instanceof SourcePigman pigman&&(pigman.sourcePickup().size()>0||pigman.sourceLegacyOffhand().hasItems())||mob instanceof SourceZombieOwner zombie&&zombie.sourceZombie().legacy.hasItems()||!mob.getPersistentData().getList("MiteContainedArrows",net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty();if(dx*dx+dy*dy+dz*dz>256&&!contained&&mayDespawn(mob))mob.discard();else set(mob,(int)(Math.random()*100)-50);}
 /** abw inactive branch: loaded hostile mobs still age, except vehicles and their passengers. */
 @SubscribeEvent public static void inactive(TickEvent.LevelTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;for(var entity:level.getAllEntities())if(entity instanceof Mob mob&&handled(mob)&&(mob instanceof Enemy||Mobs.name(mob).equals("hellhound"))&&!mob.isRemoved()&&!mob.isPassenger()&&!mob.isVehicle()&&!level.isPositionEntityTicking(mob.blockPosition())){tick(mob);tryDespawn(mob);}}
 private OriginalDespawn(){}
}

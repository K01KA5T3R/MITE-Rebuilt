package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** rj population, aggressors, name reputation and golem production. Door
 * geometry stays in its already-qualified registry. V060 breeding is absent. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceVillageLife {
 public static final class Aggressor {
  public final LivingEntity actor;public int seen;
  Aggressor(LivingEntity actor,int seen){this.actor=actor;this.seen=seen;}
 }
 public static final class State {
  public int population,golems,stable,tick,matingTick;
  public final TreeMap<String,Integer> reputation=new TreeMap<>();
  public final List<Aggressor> aggressors=new ArrayList<>();
 }
 private static final Map<Villager,SourceVillageDoors.Village> MEMBERSHIP=new WeakHashMap<>();
 public static boolean suppressModernGolem(Object actor){return actor instanceof Villager v&&v.getType()==EntityType.VILLAGER;}
 public static void bind(Villager actor,SourceVillageDoors.Village village){
  if(village==null){MEMBERSHIP.remove(actor);actor.clearRestriction();}
  else{MEMBERSHIP.put(actor,village);actor.restrictTo(village.center,(int)((float)village.radius*.6f));}
 }
 static AABB populationBox(SourceVillageDoors.Village v){return new AABB(v.center.getX()-v.radius,v.center.getY()-4,v.center.getZ()-v.radius,v.center.getX()+v.radius,v.center.getY()+4,v.center.getZ()+v.radius);}
 static void tick(ServerLevel level,SourceVillageDoors.Village v,int tick){
  var s=v.life;s.tick=tick;s.aggressors.removeIf(a->!a.actor.isAlive()||Math.abs(s.tick-a.seen)>300);
  if(tick%20==0){s.population=level.getEntitiesOfClass(Villager.class,populationBox(v)).size();if(s.population==0)s.reputation.clear();}
  if(tick%30==0)s.golems=level.getEntitiesOfClass(IronGolem.class,populationBox(v)).size();
  if(s.golems<s.population/10&&v.doors.size()>20&&level.random.nextInt(7000)==0){
   var site=spawnSite(level,v);if(site!=null){var golem=EntityType.IRON_GOLEM.create(level);if(golem!=null){golem.moveTo(site.getX(),site.getY(),site.getZ(),0,0);level.addFreshEntity(golem);s.golems++;}}
  }
 }
 static BlockPos spawnSite(ServerLevel level,SourceVillageDoors.Village v){
  for(int i=0;i<10;i++){var pos=v.center.offset(level.random.nextInt(16)-8,level.random.nextInt(6)-3,level.random.nextInt(16)-8);int dx=v.center.getX()-pos.getX(),dy=v.center.getY()-pos.getY(),dz=v.center.getZ()-pos.getZ();
   if((float)(dx*dx+dy*dy+dz*dz)>=(float)(v.radius*v.radius)||!space(level,pos))continue;return pos;
  }return null;
 }
 static boolean space(ServerLevel level,BlockPos pos){
  if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))return false;
  for(int x=pos.getX()-1;x<pos.getX()+1;x++)for(int y=pos.getY();y<pos.getY()+4;y++)for(int z=pos.getZ()-1;z<pos.getZ()+1;z++){var at=new BlockPos(x,y,z);var state=level.getBlockState(at);if(state.isSolidRender(level,at)&&state.isCollisionShapeFullBlock(level,at))return false;}
  return true;
 }
 public static int reputation(SourceVillageDoors.Village v,String name){return v.life.reputation.getOrDefault(name,0);}
 public static int change(ServerLevel level,SourceVillageDoors.Village v,String name,int delta){int value=Math.max(-30,Math.min(10,reputation(v,name)+delta));v.life.reputation.put(name,value);SourceVillageDoors.get(level).setDirty();return value;}
 public static void aggressor(SourceVillageDoors.Village v,LivingEntity actor){for(var a:v.life.aggressors)if(a.actor==actor){a.seen=v.life.tick;return;}v.life.aggressors.add(new Aggressor(actor,v.life.tick));}
 public static LivingEntity nearestAggressor(SourceVillageDoors.Village v,LivingEntity guard){LivingEntity best=null;double distance=Double.MAX_VALUE;for(var a:v.life.aggressors){double next=a.actor.distanceToSqr(guard);if(next<=distance){best=a.actor;distance=next;}}return best;}
 public static Player nearestHostile(ServerLevel level,SourceVillageDoors.Village v,LivingEntity guard){Player best=null;double distance=Double.MAX_VALUE;for(var entry:v.life.reputation.entrySet()){if(entry.getValue()>-15)continue;for(var player:level.players())if(player.getGameProfile().getName().equals(entry.getKey())){double next=player.distanceToSqr(guard);if(next<=distance){best=player;distance=next;}break;}}return best;}
 /** Called after the actual native revenge setter, matching ub.b's parent-first order. */
 public static void revenge(Object raw,Object attacker){if(!(raw instanceof Villager actor)||!(actor.level() instanceof ServerLevel level)||!(attacker instanceof LivingEntity living))return;var v=MEMBERSHIP.get(actor);if(v==null)return;aggressor(v,living);if(living instanceof Player p)change(level,v,p.getGameProfile().getName(),actor.isBaby()?-3:-1);}
 public static void villagerDied(Object raw,Object damage){if(!(raw instanceof Villager actor)||!(actor.level() instanceof ServerLevel level))return;var v=MEMBERSHIP.get(actor);if(v==null)return;var responsible=((DamageSource)damage).getEntity();if(responsible instanceof Player p)change(level,v,p.getGameProfile().getName(),-2);else if(responsible instanceof Enemy||responsible==null&&OriginalDespawn.closest(actor,16,false)!=null){v.life.matingTick=v.life.tick;SourceVillageDoors.get(level).setDirty();}}
 public static void golemDied(ServerLevel level,SourceVillageDoors.Village village,boolean playerCreated,Player killer){if(!playerCreated&&village!=null&&killer!=null)change(level,village,killer.getGameProfile().getName(),-5);}
 static void save(SourceVillageDoors.Village v,CompoundTag tag){var s=v.life;tag.putInt("PopSize",s.population);tag.putInt("Golems",s.golems);tag.putInt("Stable",s.stable);tag.putInt("Tick",s.tick);tag.putInt("MTick",s.matingTick);tag.putInt("Radius",v.radius);tag.putInt("CX",v.center.getX());tag.putInt("CY",v.center.getY());tag.putInt("CZ",v.center.getZ());int x=0,y=0,z=0;for(var d:v.doors){x+=d.pos.getX();y+=d.pos.getY();z+=d.pos.getZ();}tag.putInt("ACX",x);tag.putInt("ACY",y);tag.putInt("ACZ",z);var players=new ListTag();s.reputation.forEach((name,score)->{var n=new CompoundTag();n.putString("Name",name);n.putInt("S",score);players.add(n);});tag.put("Players",players);}
 static void load(SourceVillageDoors.Village v,CompoundTag tag){var s=v.life;s.population=tag.getInt("PopSize");s.golems=tag.getInt("Golems");s.stable=tag.getInt("Stable");s.tick=tag.getInt("Tick");s.matingTick=tag.getInt("MTick");var players=tag.getList("Players",Tag.TAG_COMPOUND);for(int i=0;i<players.size();i++){var p=players.getCompound(i);s.reputation.put(p.getString("Name"),p.getInt("S"));}}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel)MEMBERSHIP.keySet().removeIf(v->v.level()==event.getLevel());}
 private SourceVillageLife(){}
}

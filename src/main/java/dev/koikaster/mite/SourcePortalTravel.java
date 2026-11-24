package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import static dev.koikaster.mite.SourcePortalState.Destination;

/** Only the native nether-portal entry is retargeted; commands and End portals
 * keep their own dimension entry. Placement is prepared before ServerPlayer's
 * native respawn/removal path, which cannot safely receive a null PortalInfo. */
public final class SourcePortalTravel {
 private static final Map<Entity,ResourceKey<Level>> WARMUPS=new WeakHashMap<>();
 private static final int CORE_ENTRY_MIN_PROTECTION = 60 * 20;
 public static void contact(Object rawState,Object rawLevel,Object rawPos,Object rawActor){
  var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var actor=(Entity)rawActor;
  if(!actor.canChangeDimensions()||!SourcePortalState.supported(level.dimension())||BossArenaPortals.blockedNetherEntrance(level,pos))return;
  var state=(BlockState)rawState;var role=state.getValue(SourcePortalState.DESTINATION);
  if(state.getValue(SourcePortalState.RUNEGATE)){
   if(!level.isClientSide&&actor instanceof ServerPlayer player)SourceRuneGate.contact(player,pos);
   return;
  }
  if(level.isClientSide){if(mayEnter(role,actor)&&!(level.dimension()==Level.OVERWORLD&&role==Destination.OVERWORLD))actor.handleInsidePortal(pos);return;}
  if(actor instanceof ServerPlayer player&&SourcePortalTransition.pending(player))return;
  role=SourcePortalFrames.resolve(level,pos);if(role==Destination.UNSET||!mayEnter(role,actor))return;
  if(level.dimension()==Level.OVERWORLD&&role==Destination.OVERWORLD){
   if(actor instanceof ServerPlayer player&&!player.isOnPortalCooldown()&&!SourceBiomePortal.contact(player,pos))
    SourcePortalTransition.beginSpawn(player,pos);
   return;
  }
  actor.handleInsidePortal(pos);
 }
 /** Invoked only when native contact sets isInsidePortal, including projectile contact. */
 public static void trackWarmup(Entity actor){
  if(!(actor.level() instanceof ServerLevel level))return;
  var access=(SourcePortalEntityAccess)actor;
  if(BossArenaPortals.blockedNetherEntrance(level,access.mite$portalEntrance())){
   clearWarmup(actor,access);
   return;
  }
  WARMUPS.put(actor,level.dimension());
 }
 public static boolean allowContact(boolean inside,Entity actor){
  if(!inside){
   // The native no-contact branch subtracts four and clamps the counter to zero.
   if(!WARMUPS.isEmpty()&&((SourcePortalEntityAccess)actor).mite$portalWarmup()<=4)WARMUPS.remove(actor);
   return false;
  }
  var access=(SourcePortalEntityAccess)actor;
  if(!BossArenaPortals.blockedNetherEntrance(actor.level(),access.mite$portalEntrance()))return true;
  clearWarmup(actor,access);
  return false;
 }
 static void cancelLocked(ServerLevel level){
  for(var entries=WARMUPS.entrySet().iterator();entries.hasNext();){
   var entry=entries.next();
   var actor=entry.getKey();
   if(actor==null||actor.isRemoved()||actor.level().dimension()!=entry.getValue()){
    entries.remove();
    continue;
   }
   if(actor.level()!=level)continue;
   var access=(SourcePortalEntityAccess)actor;
   if(BossArenaPortals.blockedNetherEntrance(level,access.mite$portalEntrance())){
    access.mite$clearPortalContact();
    entries.remove();
   }
  }
 }
 private static void clearWarmup(Entity actor,SourcePortalEntityAccess access){
  access.mite$clearPortalContact();
  WARMUPS.remove(actor);
 }
 public static Entity change(Entity actor,ServerLevel ignoredVanillaTarget){
  if(!(actor.level() instanceof ServerLevel source)||!actor.canChangeDimensions())return null;
  var entrance=((SourcePortalEntityAccess)actor).mite$portalEntrance();
  if(entrance==null)return null;
  if(BossArenaPortals.blocked(source,entrance))return denyLocked(actor);
  var role=SourcePortalFrames.resolve(source,entrance);if(role==Destination.UNSET||!mayEnter(role,actor))return null;
  var target=source.getServer().getLevel(SourcePortalState.dimension(role));
  if(target==null||target==source)return null;
  var sourceFrame=SourcePortalFrames.complete(source,entrance);if(sourceFrame.isEmpty())return null;
  var frame=sourceFrame.get();
  if(BossArenaPortals.blockedPortal(source,frame.rectangle(),frame.axis()))return denyLocked(actor);
  if(role==Destination.CORE&&!SourcePortalFrames.touchesCore(source,frame))return null;
  boolean coreRoute=role==Destination.CORE||source.dimension()==CoreProgression.WORLD;
  double scale=coreRoute?1:source.dimension()==Level.NETHER?8:target.dimension()==Level.NETHER?.125:1;
  var border=target.getWorldBorder();if(border.getMaxX()-border.getMinX()<8||border.getMaxZ()-border.getMinZ()<8)return null;
  double x=Math.max(border.getMinX()+4,Math.min(border.getMaxX()-4,actor.getX()*scale));
  double z=Math.max(border.getMinZ()+4,Math.min(border.getMaxZ()-4,actor.getZ()*scale));
  var desired=new Vec3(x,actor.getY(),z);var returningTo=SourcePortalState.of(source.dimension());
  var forcer=SourcePortalForcer.in(target);var exit=forcer.find(target,desired,returningTo);
  if(exit.isEmpty())exit=forcer.create(target,desired,returningTo,()->actor.setPortalCooldown(0));
  if(exit.isEmpty())return null;
  var destination=exit.get();
  var exitAxis=target.getBlockState(destination.minCorner).getValue(NetherPortalBlock.AXIS);
  if(BossArenaPortals.blockedPortal(target,destination,exitAxis))return denyLocked(actor);
  var relative=PortalShape.getRelativePosition(frame.rectangle(),frame.axis(),actor.position(),actor.getDimensions(actor.getPose()));
  var info=PortalShape.createPortalInfo(target,destination,frame.axis(),relative,actor,actor.getDeltaMovement(),actor.getYRot(),actor.getXRot());
  if(BossArenaPortals.blocked(target,BlockPos.containing(info.pos)))return denyLocked(actor);
  // Native orientation/collision adaptation retains the user's modern movement choice.
  var moved=actor.changeDimension(target,new Prepared(info));
  if(Boolean.getBoolean("mite.sourcePortValidation")){
   var trace=new java.util.LinkedHashMap<String,Object>();trace.put("source",source.dimension().location().toString());trace.put("target",target.dimension().location().toString());trace.put("requested_portal_info",info.pos.toString());trace.put("result_exists",moved!=null);
   if(moved!=null){trace.put("result_dimension",moved.level().dimension().location().toString());trace.put("actual_position",moved.position().toString());trace.put("uuid_retained",moved.getUUID().equals(actor.getUUID()));trace.put("added_to_world",moved.isAddedToWorld());trace.put("removed",moved.isRemoved());trace.put("removal_reason",String.valueOf(moved.getRemovalReason()));trace.put("public_uuid_lookup_immediate",target.getEntity(moved.getUUID())==moved);}
   SourcePortTestReporter.recordDetails("native-transfer-"+actor.getUUID(),trace);
  }
  return moved;
 }
 private static Entity denyLocked(Entity actor){
  // The native timer sets the cooldown before it calls this travel adapter.
  clearWarmup(actor,(SourcePortalEntityAccess)actor);
  actor.setPortalCooldown(0);
  return null;
 }
 private static boolean mayEnter(Destination role,Entity actor){
  return role!=Destination.CORE||!(actor instanceof Player player)||player.isCreative()||player.isSpectator()
   ||AdvancedFireResistance.protectsFromLavaFor(player,CORE_ENTRY_MIN_PROTECTION);
 }
 private record Prepared(PortalInfo info) implements ITeleporter {
  public PortalInfo getPortalInfo(Entity actor,ServerLevel target,Function<ServerLevel,PortalInfo> unused){return info;}
  public Entity placeEntity(Entity actor,ServerLevel source,ServerLevel target,float yaw,Function<Boolean,Entity> reposition){
   var moved=reposition.apply(false);if(moved!=null){moved.setDeltaMovement(info.speed);moved.fallDistance=0;}return moved;
  }
 }
 private SourcePortalTravel(){}
}

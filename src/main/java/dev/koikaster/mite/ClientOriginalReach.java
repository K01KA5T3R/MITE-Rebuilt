package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;

/** Keep source16-block entity selection separate from source melee and interaction click permission. */
public final class ClientOriginalReach {
 public static boolean pick(Object renderer,float partial){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getCameraEntity()!=mc.player||mc.player.isSpectator())return false;mc.getProfiler().push("pick");mc.crosshairPickEntity=null;mc.hitResult=OriginalRaycast.select(mc.player,partial,false,false,null);if(mc.hitResult instanceof EntityHitResult hit)mc.crosshairPickEntity=hit.getEntity();mc.getProfiler().pop();return true;}
 public static boolean mayAttack(Object raw){var mc=(Minecraft)raw;return mc.player==null||!(mc.hitResult instanceof EntityHitResult hit)||OriginalReach.canReachHit(mc.player,hit.getEntity(),hit.getLocation(),LegacyReach.Context.MELEE,1);}
 private static Vec3 hit(Player player,Entity target){var selected=Minecraft.getInstance().hitResult;if(selected instanceof EntityHitResult result&&result.getEntity()==target)return result.getLocation();var origin=player.getEyePosition();var end=origin.add(OriginalRaycast.look(player,1).scale(16));var box=OriginalRaycast.targetBox(player,target);return box.contains(origin)?origin:box.clip(origin,end).orElse(null);}
 public static boolean attack(Object actor,Object victim){var player=(Player)actor;var target=(Entity)victim;var hit=hit(player,target);return hit!=null&&OriginalReach.canReachHit(player,target,hit,LegacyReach.Context.MELEE,1);}
 private static boolean interact(Object actor,Object victim,Object suppliedHit){var player=(Player)actor;var target=(Entity)victim;var hit=suppliedHit instanceof EntityHitResult result?result.getLocation():hit(player,target);return hit!=null&&OriginalReach.canReachHit(player,target,hit,LegacyReach.Context.INTERACTION,1);}
 public static net.minecraft.world.InteractionResult interaction(Object actor,Object victim,Object hit){return interact(actor,victim,hit)?null:net.minecraft.world.InteractionResult.PASS;}
 public static net.minecraft.world.InteractionResult blockInteraction(Object actor,Object hit){return useBlock(actor,hit)?null:net.minecraft.world.InteractionResult.PASS;}
 public static boolean block(Object rawPos){var player=Minecraft.getInstance().player;return player!=null&&OriginalReach.canSelectBlock(player,(net.minecraft.core.BlockPos)rawPos,1);}
 public static boolean useBlock(Object rawPlayer,Object rawHit){return OriginalReach.canSelectBlock((Player)rawPlayer,((BlockHitResult)rawHit).getBlockPos(),1);}
 private ClientOriginalReach(){}
}

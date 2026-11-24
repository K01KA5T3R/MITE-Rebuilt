package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** uf/yc/Raycast ranges used by native selection, actions and melee goals; runtime verification pending. */
public final class OriginalReach {
 public static float heldBonus(ItemStack stack){var profile=ClassicToolWear.profile(stack);if(profile!=null)return ClassicToolCatalogue.reachBonus(profile.kind());return stack.is(Items.STICK)||stack.is(Items.BONE)?.5f:0;}
 /** xj effectiveness includes its material harvest floor; existing mapped tool/block classification is reused. */
 public static float block(Player player,BlockState state){if(player.isCreative())return 5f;var held=player.getMainHandItem();float bonus=ClassicToolWear.profile(held)!=null&&ClassicMining.baseStrength(player,state)>1f?heldBonus(held):0;return LegacyReach.block(player.isCreative(),bonus);}
 public static float entity(Player player,Entity target,LegacyReach.Context context){if(player.isCreative())return 5f;var held=player.getMainHandItem();float extra=context==LegacyReach.Context.MELEE&&CrabulonContent.fungalSpear(held)?CrabulonContent.SPEAR_REACH:0;return LegacyReach.entity(false,context,player.getY(),target.getY(),heldBonus(held)+extra,target instanceof ThrowableProjectile||target instanceof SourceLargeFireball,target instanceof Boat);}
 public static boolean canSelectBlock(Player player,BlockPos pos,float partial){var eye=player.getEyePosition(partial);return LegacyReach.blockCenterReachable(eye.x,eye.y,eye.z,pos.getX(),pos.getY(),pos.getZ(),block(player,player.level().getBlockState(pos)));}
 public static boolean canReachHit(Player player,Entity target,Vec3 hit,LegacyReach.Context context,float partial){return player.getEyePosition(partial).distanceTo(hit)<=entity(player,target,context);}
 public static float npc(Mob actor){return LegacyReach.npc(heldBonus(actor.getMainHandItem()),actor instanceof IronGolem);}
 public static double npcSquared(Object rawActor,Object rawTarget){var actor=(Mob)rawActor;var target=(LivingEntity)rawTarget;if(actor instanceof net.minecraft.world.entity.animal.Animal)return LegacyReach.animalSquaredThreshold(actor.getBbWidth(),target.getBbWidth(),heldBonus(actor.getMainHandItem()));double radius=npc(actor);return radius*radius;}
 /** og/of: primary point at75% of height; any clear50%/90%/10% target point permits a strike. */
 public static boolean npcStrike(Object rawActor,Object rawTarget){var actor=(Mob)rawActor;var target=(LivingEntity)rawTarget;double squared=actor.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());if(actor instanceof net.minecraft.world.entity.animal.Animal){if(squared>npcSquared(actor,target))return false;}else if(Math.sqrt(squared)>npc(actor))return false;var origin=actor.position().add(0,actor.getBbHeight()*.75f,0);for(float fraction:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,target.getBbHeight()*fraction,0)))return true;return false;}
 public static boolean npcPermission(Object actor,Object target,double nativeSquared){return nativeSquared>npcSquared(actor,target)||npcStrike(actor,target);}
 /** ka's active left-click signal has a16-block protocol envelope; actual melee range stays client-side. */
 public static boolean serverEnvelope(Object rawPlayer,Object rawTarget,double nativePadding,Object rawPacket){var player=(Player)rawPlayer;var target=(Entity)rawTarget;boolean[] attack={false};((net.minecraft.network.protocol.game.ServerboundInteractPacket)rawPacket).dispatch(new net.minecraft.network.protocol.game.ServerboundInteractPacket.Handler(){public void onAttack(){attack[0]=true;}public void onInteraction(net.minecraft.world.InteractionHand hand){}public void onInteraction(net.minecraft.world.InteractionHand hand,Vec3 location){}});return attack[0]?player.distanceToSqr(target)<256:player.canReachRaw(target,nativePadding);}
 private OriginalReach(){}
}

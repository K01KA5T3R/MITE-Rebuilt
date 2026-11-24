package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
/** Ordinary sf state only. Dire-wolf thresholds/countdown/hostility remain in its confirmed0.28 owner. */
public final class OriginalWolfLife {
 private final Wolf wolf;public int targetCountdown,tamingCooldown;public boolean hostile,witchAlly;
 public OriginalWolfLife(Wolf wolf){this.wolf=wolf;}
 public void target(){targetCountdown=Math.min(800,Math.max(0,800+wolf.getRandom().nextInt(100)-wolf.getRandom().nextInt(100)));if(wolf.getTarget() instanceof Player&&!wolf.isTame())hostile=true;}
 public void tick(){if(tamingCooldown>0)--tamingCooldown;if(targetCountdown>0)--targetCountdown;if(targetCountdown==0){if(wolf.getTarget()!=null)wolf.setTarget(null);hostile=false;}if(!wolf.isTame()&&SourceSpecialSpawns.bloodUp(wolf.level()))hostile=true;wolf.setRemainingPersistentAngerTime(hostile||wolf.getTarget()!=null&&!wolf.isTame()?800:0);}
 public int tamingOutcome(Player player){float roll=wolf.getRandom().nextFloat();if(roll<.05f)return -1;if(roll<.1f)return 0;if(roll>.9f)return 1;roll+=wolf.getRandom().nextFloat()*PlayerState.level(player)*.02f;return roll<.2f?-1:roll<.8f?0:1;}
 public InteractionResult bone(Player player,InteractionHand hand){if(wolf.isTame()||!player.getItemInHand(hand).is(Items.BONE))return null;if(hostile||wolf.getTarget()!=null)return InteractionResult.PASS;if(!wolf.level().isClientSide&&tamingCooldown==0){int outcome=tamingOutcome(player);if(outcome<=0){wolf.level().broadcastEntityEvent(wolf,(byte)6);tamingCooldown=100;if(outcome<0&&!SourceSpecialSpawns.blueUp(wolf.level()))wolf.setTarget(player);}else{wolf.tame(player);wolf.getNavigation().stop();wolf.setTarget(null);wolf.setOrderedToSit(true);wolf.level().broadcastEntityEvent(wolf,(byte)7);wolf.playSound(wolf.getRandom().nextInt(2)==0?SoundEvents.WOLF_AMBIENT:SoundEvents.WOLF_PANT,OriginalWolfOwners.soundVolume(wolf),wolf.getVoicePitch());}if(!player.getAbilities().instabuild)player.getItemInHand(hand).shrink(1);}return InteractionResult.sidedSuccess(wolf.level().isClientSide);}
 public void save(CompoundTag tag){tag.putInt("target_countdown",targetCountdown);tag.putBoolean("hostile_to_players",hostile);tag.putBoolean("is_witch_ally",witchAlly);}
 public void load(CompoundTag tag){targetCountdown=Math.min(800,Math.max(0,tag.getInt("target_countdown")));hostile=tag.getBoolean("hostile_to_players");witchAlly=tag.getBoolean("is_witch_ally");}
}

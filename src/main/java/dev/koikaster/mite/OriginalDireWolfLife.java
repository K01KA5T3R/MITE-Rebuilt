package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
/** Direct control flow from EntityDireWolf and sf/oq. Native APIs carry the state.
 * The user's modern feeding/breeding rule stays in Wolf; only wild bone taming is adapted here. */
public final class OriginalDireWolfLife {
 private final Mobs.DireWolf wolf;
 public int targetCountdown,tamingCooldown;public boolean hostile,witchAlly;
 public OriginalDireWolfLife(Mobs.DireWolf wolf){this.wolf=wolf;}
 public Player nearby(){Player closest=null;double best=16;for(var p:wolf.level().players()){if(!p.isAlive()||p.isSpectator()||p.getAbilities().invulnerable)continue;double distance=wolf.distanceToSqr(p);if(distance<best){closest=p;best=distance;}}return closest;}
 public void target(){targetCountdown=Math.min(800,Math.max(0,800+wolf.getRandom().nextInt(100)-wolf.getRandom().nextInt(100)));if(wolf.getTarget() instanceof Player&&!wolf.isTame())hostile=true;}
 public void tick(){
  if(tamingCooldown>0)tamingCooldown--;
  if(targetCountdown>0)targetCountdown--;
  if(targetCountdown==0){if(wolf.getTarget()!=null)wolf.setTarget(null);hostile=false;}
  if(!wolf.isTame()&&SourceSpecialSpawns.bloodUp(wolf.level())||witchAlly)hostile=true;
  if(!wolf.isBaby()&&!wolf.isTame()&&!wolf.isInLove()&&!SourceSpecialSpawns.blueUp(wolf.level())&&wolf.getRandom().nextFloat()<.004f){var player=nearby();if(player!=null)wolf.setTarget(player);}
  wolf.setRemainingPersistentAngerTime(hostile||wolf.getTarget()!=null&&!wolf.isTame()?800:0);
 }
 public int tamingOutcome(Player player){float roll=wolf.getRandom().nextFloat();if(roll<.2f)return -1;if(roll<.4f)return 0;if(roll>.95f)return 1;roll+=wolf.getRandom().nextFloat()*PlayerState.level(player)*.02f;return roll<.5f?-1:roll<1?0:1;}
 public InteractionResult bone(Player player,InteractionHand hand){
  if(wolf.isTame()||!player.getItemInHand(hand).is(Items.BONE))return null;
  if(hostile||wolf.getTarget()!=null)return InteractionResult.PASS;
  if(!wolf.level().isClientSide&&tamingCooldown==0){
   int outcome=tamingOutcome(player);
   if(outcome<=0){wolf.level().broadcastEntityEvent(wolf,(byte)6);tamingCooldown=100;if(outcome<0&&!SourceSpecialSpawns.blueUp(wolf.level()))wolf.setTarget(player);}
   else{wolf.tame(player);wolf.getNavigation().stop();wolf.setTarget(null);wolf.setOrderedToSit(true);wolf.level().broadcastEntityEvent(wolf,(byte)7);wolf.playSound(wolf.getRandom().nextInt(2)==0?net.minecraft.sounds.SoundEvents.WOLF_AMBIENT:net.minecraft.sounds.SoundEvents.WOLF_PANT);}
   if(!player.getAbilities().instabuild)player.getItemInHand(hand).shrink(1);
  }
  return InteractionResult.sidedSuccess(wolf.level().isClientSide);
 }
 public void save(CompoundTag tag){tag.putInt("target_countdown",targetCountdown);tag.putBoolean("hostile_to_players",hostile);tag.putBoolean("is_witch_ally",witchAlly);}
 public void load(CompoundTag tag){targetCountdown=Math.min(800,Math.max(0,tag.getInt("target_countdown")));hostile=tag.getBoolean("hostile_to_players");witchAlly=tag.getBoolean("is_witch_ally");}
 public Goal playerGoal(){var wrapped=new OriginalNearestTargetGoal(wolf,Player.class,0,true);return new Goal(){ {setFlags(EnumSet.of(Flag.TARGET));}public boolean canUse(){return (hostile||witchAlly)&&wrapped.canUse();}public boolean canContinueToUse(){return wrapped.canContinueToUse();}public void start(){wrapped.start();}public void stop(){wrapped.stop();}public void tick(){wrapped.tick();}public boolean requiresUpdateEveryTick(){return true;}};}
}

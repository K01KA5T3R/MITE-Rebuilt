package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Remaining rs constructor pp/px/ql leaves. Existing movement, damage,
 * feeding and breeding owners are kept verbatim. No global float replacement.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceHorseAmbient {
 public static float eyeHeight(float nativeHeight,Object rawHorse,Object rawDimensions){return SourceHorseOwners.handles((AbstractHorse)rawHorse)?((EntityDimensions)rawDimensions).height*.85f:nativeHeight;}
 static void install(AbstractHorse horse){
  var goals=((SourceMobAi)horse).mite$goals();if(goals.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof Swim))return;
  for(var g:List.copyOf(goals.getAvailableGoals()))if(g.getGoal() instanceof FloatGoal||g.getGoal() instanceof LookAtPlayerGoal||g.getGoal() instanceof RandomLookAroundGoal)goals.removeGoal(g.getGoal());
  goals.addGoal(0,new Swim(horse));goals.addGoal(7,new Watch(horse));goals.addGoal(8,new Idle(horse));
 }
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof AbstractHorse horse&&SourceHorseOwners.handles(horse))install(horse);}
 /** R195 pp, specialized to rs (which can never be an EarthElemental). */
 static final class Swim extends Goal {
  final AbstractHorse horse;
  Swim(AbstractHorse horse){this.horse=horse;setFlags(EnumSet.of(Flag.JUMP));horse.getNavigation().setCanFloat(true);}
  boolean buoyant(){
   if(horse.horizontalCollision)return true;
   var slow=horse.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var haste=horse.getEffect(MobEffects.MOVEMENT_SPEED);
   float slowing=slow==null?0:(slow.getAmplifier()+1)*-.2f;
   if(horse.level().getBlockStates(horse.getBoundingBox()).anyMatch(s->s.is(Blocks.COBWEB)))slowing-=.75f;
   float speeding=haste==null?0:(haste.getAmplifier()+1)*.2f;double combined=(double)(slowing+speeding);
   if(combined<0)combined*=(double)(1-EnchantmentEffects.paralysisResistance(horse));
   return (float)combined>=-.5f;
  }
  /** nn.isInsideOfMaterial(WATER,-eyeHeight): source liquid surfaces are
   * (Y+1)-((metadata+1)/9-.11111111), including falling-water metadata>=8.
   * Waterlogged modern blocks use their actual fluid shape as the new-terrain adapter.
   */
  boolean footInWater(){var at=horse.blockPosition();var state=horse.level().getBlockState(at);var fluid=horse.level().getFluidState(at);if(!fluid.is(FluidTags.WATER))return false;
   double surface;if(state.getBlock() instanceof LiquidBlock){int metadata=state.getValue(LiquidBlock.LEVEL);if(metadata>=8)metadata=0;float offset=(metadata+1)/9.0f-.11111111f;surface=(float)(at.getY()+1)-offset;}else surface=at.getY()+fluid.getHeight(horse.level(),at);
   return horse.getY()<surface;
  }
  boolean fullWater(){var state=horse.level().getBlockState(horse.blockPosition());return state.is(Blocks.WATER)&&state.getValue(LiquidBlock.LEVEL)==0;}
  public boolean canUse(){if(!buoyant())return false;if(horse.isInWater()&&!footInWater()&&!fullWater())return horse.isInLava();return horse.isInWater()||horse.isInLava();}
  public boolean canContinueToUse(){return canUse();}
  public void tick(){if(!buoyant())return;if(horse.level().getFluidState(BlockPos.containing(horse.getX(),horse.getEyeY()+.5,horse.getZ())).is(FluidTags.WATER)){var v=horse.getDeltaMovement();horse.setDeltaMovement(v.x,.1f,v.z);return;}if(horse.getRandom().nextFloat()<.8f)horse.getJumpControl().jump();}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** rs px watches players within6, 40..79 complete ticks; creative counts. */
 static final class Watch extends Goal {
  final AbstractHorse horse;Player target;int time;
  Watch(AbstractHorse horse){this.horse=horse;setFlags(EnumSet.of(Flag.LOOK));}
  public boolean canUse(){if(horse.getRandom().nextFloat()>=.02f)return false;target=OriginalDespawn.closest(horse,6,false);return target!=null;}
  public boolean canContinueToUse(){return target!=null&&target.isAlive()&&horse.distanceToSqr(target)<=36&&time>0;}
  public void start(){time=40+horse.getRandom().nextInt(40);}public void stop(){target=null;}
  public void tick(){horse.getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,40);--time;}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** Unpatched base1.6.4 ql: MOVE+LOOK, 20..39 and >=0 continuation. */
 static final class Idle extends Goal {
  final AbstractHorse horse;double dx,dz;int time;
  Idle(AbstractHorse horse){this.horse=horse;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){return horse.getRandom().nextFloat()<.02f;}public boolean canContinueToUse(){return time>=0;}
  public void start(){double angle=Math.PI*2*horse.getRandom().nextDouble();dx=Math.cos(angle);dz=Math.sin(angle);time=20+horse.getRandom().nextInt(20);}
  public void tick(){--time;horse.getLookControl().setLookAt(horse.getX()+dx,horse.getEyeY(),horse.getZ()+dz,10,40);}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceHorseAmbient(){}
}

package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;

/** Binding to the actual existing parent state. Constructor draws no
 * RNG; source Phase's web/evasion constructor order is unchanged on adoption.
 */
public final class SourceParentArachnidLife {
 private final PathfinderMob actor;
 public final OriginalParentHarmState harm;
 public final OriginalParentFleeTransition flee;
 public SourceParentArachnidLife(PathfinderMob actor,SourceArachnidState state){
  this.actor=Objects.requireNonNull(actor);Objects.requireNonNull(state);
  harm=new OriginalParentHarmState(actor,new OriginalParentHarmState.Flags(){public boolean hasDecidedToFlee(){return state.hasDecidedToFlee;}public void hasDecidedToFlee(boolean value){state.hasDecidedToFlee=value;}public boolean fleeing(){return state.fleeing;}public void fleeing(boolean value){state.fleeing=value;}});
  flee=new OriginalParentFleeTransition(actor,harm,OriginalParentFleeTransition.nativePath(actor,1));
 }
 public void beforeTick(){harm.beforeTick();}
 public boolean beforeDecisions(){return flee.beforeDecisions();}
 /** of memory is before tm retaliation. A lethal negative result still writes
  * memory/considers fleeing, while tm does not replace the attack target.
  */
 public void afterDamage(DamageSource source,SourceParentHurtFrame.Outcome outcome){
  if(actor.level().isClientSide)return;
  harm.afterDamage(source,outcome.negativelyAffected());
  Entity responsible=source.getEntity();
  if(!outcome.destroyed()&&outcome.negativelyAffected()&&responsible!=actor&&responsible!=actor.getVehicle()&&responsible!=actor.getFirstPassenger()&&(responsible==null||responsible instanceof LivingEntity))actor.setTarget((LivingEntity)responsible);
 }
 public void save(CompoundTag tag){harm.save(tag);}
 public void load(CompoundTag tag){harm.load(tag);}
}

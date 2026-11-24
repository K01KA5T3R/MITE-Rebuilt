package dev.koikaster.mite;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;

/** New scoped outcome observer for the real native hurt caller. Source
 * EntityDamageResult.negative is an OR of seven flags, not the native boolean.
 * The integration patch supplies source wrapper/gear event marker sites.
 */
public final class SourceParentHurtFrame implements AutoCloseable {
 public record Outcome(boolean knockedBack,boolean lostHealth,boolean destroyed,boolean heldDurabilityLost,boolean heldDestroyed,boolean armorDurabilityLost,boolean armorDestroyed){
  public boolean negativelyAffected(){return knockedBack||lostHealth||destroyed||heldDurabilityLost||heldDestroyed||armorDurabilityLost||armorDestroyed;}
 }
 private static final ThreadLocal<ArrayDeque<SourceParentHurtFrame>> ACTIVE=new ThreadLocal<>();
 private final LivingEntity actor;private final float healthBefore;private final boolean removedBefore;
 private final Consumer<Outcome> apply;
 private boolean knockedBack,heldDurabilityLost,heldDestroyed,armorDurabilityLost,armorDestroyed,closed,dispatched,observeChildren,childHealthLost,childDestroyed;
 private SourceParentHurtFrame(LivingEntity actor,Consumer<Outcome> apply){this.actor=Objects.requireNonNull(actor);this.apply=Objects.requireNonNull(apply);healthBefore=actor.getHealth();removedBefore=actor.isRemoved();var stack=ACTIVE.get();if(stack==null){stack=new ArrayDeque<>();ACTIVE.set(stack);}stack.push(this);}
 public static SourceParentHurtFrame begin(LivingEntity actor,Consumer<Outcome> apply){return new SourceParentHurtFrame(actor,apply);}
 /** A contact owner can observe a species' nested hurt frame without replacing
  * that species' harm callback or interpreting native acceptance as damage. */
 public static SourceParentHurtFrame beginObserver(LivingEntity actor,Consumer<Outcome> apply){var frame=begin(actor,apply);frame.observeChildren=true;return frame;}
 private static SourceParentHurtFrame active(Object actor){var stack=ACTIVE.get();if(stack!=null)for(var frame:stack)if(frame.actor==actor)return frame;return null;}
 /** of.knockBack returned true. This source result remains true if its
  * attribute roll suppresses all motion; admission cancellation never marks. */
 public static void knockedBack(Object actor){var frame=active(actor);if(frame!=null)frame.knockedBack=true;}
 public static boolean wasKnockedBack(Object actor){var frame=active(actor);return frame!=null&&frame.knockedBack;}
 public static void heldDamage(Object actor,boolean lostDurability,boolean destroyed){var frame=active(actor);if(frame!=null){frame.heldDurabilityLost|=lostDurability;frame.heldDestroyed|=destroyed;}}
 public static void armorDamage(Object actor,boolean lostDurability,boolean destroyed){var frame=active(actor);if(frame!=null){frame.armorDurabilityLost|=lostDurability;frame.armorDestroyed|=destroyed;}}
 private Outcome outcome(){float healthAfter=actor.getHealth();return new Outcome(knockedBack,childHealthLost||healthAfter<healthBefore,childDestroyed||healthBefore>0&&(healthAfter<=0||!removedBefore&&actor.isRemoved()),heldDurabilityLost,heldDestroyed,armorDurabilityLost,armorDestroyed);}
 private void dispatch(Outcome outcome){if(!dispatched){dispatched=true;var stack=ACTIVE.get();if(stack!=null)for(var observer:stack)if(observer!=this&&observer.actor==actor&&observer.observeChildren){observer.knockedBack|=outcome.knockedBack;observer.childHealthLost|=outcome.lostHealth;observer.childDestroyed|=outcome.destroyed;observer.heldDurabilityLost|=outcome.heldDurabilityLost;observer.heldDestroyed|=outcome.heldDestroyed;observer.armorDurabilityLost|=outcome.armorDurabilityLost;observer.armorDestroyed|=outcome.armorDestroyed;}apply.accept(outcome);}}
 /** Inject before the actual native hurt caller invokes die. Source of updates harm
  * memory and consumes its RNG before it invokes death; post-hurt alone would
  * incorrectly move those random draws after native loot generation.
  */
 public static void beforeDeath(Object actor){var frame=active(actor);if(frame!=null)frame.dispatch(frame.outcome());}
 public Outcome finish(){if(closed)throw new IllegalStateException("Hurt frame is closed");var outcome=outcome();dispatch(outcome);return outcome;}
 @Override public void close(){if(closed)return;var stack=ACTIVE.get();if(stack==null||stack.peek()!=this)throw new IllegalStateException("Native hurt frames must close in call order");stack.pop();closed=true;if(stack.isEmpty())ACTIVE.remove();}
}

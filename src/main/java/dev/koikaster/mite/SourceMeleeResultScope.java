package dev.koikaster.mite;

import java.util.ArrayDeque;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/** A synchronous observer of the real hurt admission, independent of its
 * return boolean and damage outcome. Target-specific rejections before the
 * living parent never admit; native iframe rejection happens after admission. */
public final class SourceMeleeResultScope implements AutoCloseable {
 private static final ThreadLocal<ArrayDeque<SourceMeleeResultScope>> ACTIVE=new ThreadLocal<>();
 private final Entity target;private final DamageSource source;private boolean admitted,closed;
 private SourceMeleeResultScope(Entity target,DamageSource source){this.target=target;this.source=source;var stack=ACTIVE.get();if(stack==null){stack=new ArrayDeque<>();ACTIVE.set(stack);}stack.push(this);}
 public static SourceMeleeResultScope begin(Entity target,DamageSource source){return new SourceMeleeResultScope(target,source);}
 /** Inject after LivingEntity's invulnerability/death/fire/Forge gates, at
  * its first isSleeping call; the original branch result remains on stack. */
 public static void admitted(Object actor,Object damage){var stack=ACTIVE.get();if(stack!=null)for(var scope:stack)if(scope.target==actor&&scope.source==damage){scope.admitted=true;return;}}
 public boolean admitted(){return admitted;}
 public SourceIronGolemRules.Hit result(boolean accepted,SourceParentHurtFrame.Outcome outcome){return new SourceIronGolemRules.Hit(admitted||outcome.negativelyAffected(),outcome.knockedBack(),accepted);}
 @Override public void close(){if(closed)return;var stack=ACTIVE.get();if(stack==null||stack.peek()!=this)throw new IllegalStateException("Melee result scopes must close in call order");stack.pop();closed=true;if(stack.isEmpty())ACTIVE.remove();}
}

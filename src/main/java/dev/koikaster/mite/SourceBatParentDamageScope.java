package dev.koikaster.mite;
import java.util.ArrayDeque;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
/** ro wakes for a non-null parent result, including nil/fully resisted damage.
 * Native hurt=false also means an iframe rejected damage, so it cannot represent
 * source null. Explicit immunity/death/fire-resistance checks stay in the caller;
 * a canceled Forge attack is the extra modern null boundary. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceBatParentDamageScope implements AutoCloseable {
 private static final ThreadLocal<ArrayDeque<SourceBatParentDamageScope>> ACTIVE=new ThreadLocal<>();
 private final SourceBat actor;private final DamageSource source;private boolean canceled;
 private SourceBatParentDamageScope(SourceBat actor,DamageSource source){this.actor=actor;this.source=source;var stack=ACTIVE.get();if(stack==null){stack=new ArrayDeque<>();ACTIVE.set(stack);}stack.push(this);}
 static SourceBatParentDamageScope begin(SourceBat actor,DamageSource source){return new SourceBatParentDamageScope(actor,source);}
 boolean hasParentResult(){return !canceled;}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void attack(LivingAttackEvent event){if(!event.isCanceled())return;var stack=ACTIVE.get();if(stack!=null)for(var scope:stack)if(scope.actor==event.getEntity()&&scope.source==event.getSource()){scope.canceled=true;break;}}
 @Override public void close(){var stack=ACTIVE.get();if(stack==null||stack.peek()!=this)throw new IllegalStateException("Bat damage scopes must close in call order");stack.pop();if(stack.isEmpty())ACTIVE.remove();}
}

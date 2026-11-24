package dev.koikaster.mite;
import java.util.ArrayDeque;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
/** og.a(oi). The actor scope replaces only the source owner's parent call;
 * other native initializers keep their own path. */
public final class SourceSpawnInitialization implements AutoCloseable {
 private static final ThreadLocal<ArrayDeque<Mob>> ACTIVE=new ThreadLocal<>();private final Mob actor;
 private SourceSpawnInitialization(Mob actor){this.actor=actor;var stack=ACTIVE.get();if(stack==null){stack=new ArrayDeque<>();ACTIVE.set(stack);}stack.push(actor);}
 static SourceSpawnInitialization begin(Mob actor){return new SourceSpawnInitialization(actor);}
 static SourceHorseHerd.Finished finish(Mob actor,net.minecraft.world.level.ServerLevelAccessor region,MobSpawnType reason,SpawnGroupData data){if(actor.getType()==EntityType.HORSE)return SourceHorseHerd.initialize(actor,region,reason,data);if(actor.getType()==EntityType.ZOMBIE)return SourceZombieVariants.finish(actor,region,reason,data);try(var ignored=begin(actor)){return new SourceHorseHerd.Finished(actor,net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(actor,region,region.getCurrentDifficultyAt(actor.blockPosition()),reason,data,null));}}
 public static boolean active(Object raw){var stack=ACTIVE.get();return stack!=null&&stack.contains(raw)&&SourceNaturalSpawning.sourceType(((Mob)raw).getType());}
 public static SpawnGroupData base(Object rawActor,Object rawReason,Object rawData){var actor=(Mob)rawActor;apply(actor,(MobSpawnType)rawReason,actor.getRandom().nextGaussian()*.05);return (SpawnGroupData)rawData;}
 static void apply(Mob actor,MobSpawnType reason,double followBonus){actor.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",followBonus,AttributeModifier.Operation.MULTIPLY_BASE));((SourceSpawnBaseAccess)actor).mite$sourceSpawnType(reason);}
 @Override public void close(){var stack=ACTIVE.get();if(stack==null||stack.peek()!=actor)throw new IllegalStateException("Source finalization scopes must close in order");stack.pop();if(stack.isEmpty())ACTIVE.remove();}
}

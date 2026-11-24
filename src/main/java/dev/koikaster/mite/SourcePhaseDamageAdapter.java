package dev.koikaster.mite;
import java.util.Objects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

/** Damage/nb's phase exclusions, independently audited from armor rules.
 * Fire is nb.s: inFire/onFire/fireballs set it; lava only sets is_lava_damage.
 * Fall and poison are exact source identities. Generic magic is not poison.
 */
public final class SourcePhaseDamageAdapter {
 public record View(boolean isFallDamage,boolean isFireDamage,boolean isPoison,Entity immediateEntity,Entity responsibleEntity) implements OriginalPhaseSpiderLife.SourceDamage {}
 public static View of(DamageSource source){Objects.requireNonNull(source);return new View(source.is(DamageTypes.FALL),source.is(DamageTypeTags.IS_FIRE)&&!source.is(DamageTypes.LAVA),source.is(ClassicDefense.POISON),source.getDirectEntity(),source.getEntity());}
 private SourcePhaseDamageAdapter(){}
}

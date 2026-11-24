package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;
/** Source-owned registration. Original jm tracks EntityWeb at 64 blocks / 10 ticks / velocity=true. */
public final class SourceArachnidContent {
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MiteReforged.ID);
 public static final RegistryObject<EntityType<SourceWebProjectile>> WEB=ENTITIES.register("source_thrown_web",()->EntityType.Builder.<SourceWebProjectile>of(SourceWebProjectile::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).setShouldReceiveVelocityUpdates(true).build(MiteReforged.ID+":source_thrown_web"));
 private SourceArachnidContent(){}
}

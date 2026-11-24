package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

final class RedstoneGolemContent {
    static final RegistryObject<EntityType<SourceRedstoneGolem>> GOLEM = Mobs.ENTITIES.register("redstone_iron_golem",
        () -> EntityType.Builder.of(SourceRedstoneGolem::new,MobCategory.MONSTER).sized(1.4f,2.7f)
            .clientTrackingRange(12).updateInterval(2).build(MiteReforged.ID+":redstone_iron_golem"));
    static final RegistryObject<Item> STAR = Content.item("shock_star",ShockStarItem::new);

    private RedstoneGolemContent() {}

    static void register(IEventBus bus) { bus.addListener(RedstoneGolemContent::attributes); }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GOLEM.get(),SourceRedstoneGolem.attributes().build());
    }
}

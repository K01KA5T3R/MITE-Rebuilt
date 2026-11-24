package dev.koikaster.mite;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;
/** Original imported creature OGGs are copied unchanged; sound event names use the modern registry. */
public final class SourceCreatureSounds {
 public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,MiteReforged.ID);
 public static final RegistryObject<SoundEvent> SHADOW_SAY=register("shadow.say"),SHADOW_HURT=register("shadow.hurt"),SHADOW_DEATH=register("shadow.death");
 public static final RegistryObject<SoundEvent> HELLHOUND_SAY=register("hellhound.say"),HELLHOUND_HURT=register("hellhound.hurt"),HELLHOUND_DEATH=register("hellhound.death"),HELLHOUND_BREATH=register("hellhound.breath");
 public static final RegistryObject<SoundEvent> STALKER_SAY=register("invisible_stalker.say"),STALKER_HURT=register("invisible_stalker.hurt"),STALKER_DEATH=register("invisible_stalker.death");
 public static final RegistryObject<SoundEvent> GHOUL_SAY=register("ghoul.say"),GHOUL_HURT=register("ghoul.hurt"),GHOUL_DEATH=register("ghoul.death");
 public static final RegistryObject<SoundEvent> WIGHT_SAY=register("wight.say"),WIGHT_HURT=register("wight.hurt"),WIGHT_DEATH=register("wight.death");
 public static final RegistryObject<SoundEvent> DEMON_SPIDER_SAY=register("demon_spider.say"),DEMON_SPIDER_HURT=register("demon_spider.hurt"),DEMON_SPIDER_DEATH=register("demon_spider.death");
 public static final RegistryObject<SoundEvent> BLOCK_SPLASH=register("falling_block.splash");
 public static final RegistryObject<SoundEvent> WOLF_HOWL=register("wolf.howl"),BOOK_OPEN=register("book.open"),BOOK_CLOSE=register("book.close"),BOOK_PAGE=register("book.page"),BOIL=register("brewing.boil");
 public static final RegistryObject<SoundEvent> SKILL_DRAIN=register("skill.drain");
 private static RegistryObject<SoundEvent> register(String key){return SOUNDS.register(key,()->SoundEvent.createVariableRangeEvent(Content.id(key)));}
 private SourceCreatureSounds(){}
}

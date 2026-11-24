package dev.koikaster.mite;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraftforge.registries.*;

/** Native registry adapters for tv's exact legacy potion effects and uu identity.
 * A distinct projectile type preserves this owner when saved/reloaded, while
 * player and third-party native thrown potions retain their own factory. */
public final class SourceWitchContent {
 public static final DeferredRegister<Potion> POTIONS=DeferredRegister.create(ForgeRegistries.POTIONS,MiteReforged.ID);
 public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,MiteReforged.ID);
 public static final RegistryObject<Potion> SLOW=POTIONS.register("r195_slowness",()->new Potion("slowness",new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,1351)));
 public static final RegistryObject<Potion> WEAK=POTIONS.register("r195_weakness",()->new Potion("weakness",new MobEffectInstance(MobEffects.WEAKNESS,1351)));
 public static final RegistryObject<Potion> POISON=POTIONS.register("r195_poison",()->new Potion("poison",new MobEffectInstance(MobEffects.POISON,676)));
 public static final RegistryObject<SoundEvent> HURT=sound("r195_witch_hurt"),DEATH=sound("r195_witch_death"),CACKLE=sound("r195_witch_cackle");
 public static final RegistryObject<EntityType<SourceWitchPotion>> PROJECTILE=Mobs.ENTITIES.register("witch_potion",()->EntityType.Builder.<SourceWitchPotion>of(SourceWitchPotion::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build(MiteReforged.ID+":witch_potion"));
 private static RegistryObject<SoundEvent> sound(String id){return SOUNDS.register(id,()->SoundEvent.createVariableRangeEvent(Content.id(id)));}
 static void register(){}
 static ItemStack potion(int subtype){
  var definition=SourceWitchPotionData.effect(subtype);
  var potion=switch(definition.id()){case 1->Potions.SWIFTNESS;case 2->SLOW.get();case 6->Potions.HEALING;case 7->Potions.HARMING;case 12->Potions.FIRE_RESISTANCE;case 18->WEAK.get();case 19->POISON.get();default->throw new IllegalArgumentException("Witch effect");};
  var stack=PotionUtils.setPotion(new ItemStack(definition.splash()?Items.SPLASH_POTION:Items.POTION),potion);
  stack.getOrCreateTag().putInt("MiteR195WitchPotion",subtype);return stack;
 }
 static int subtype(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("MiteR195WitchPotion")?stack.getTag().getInt("MiteR195WitchPotion"):32732;}
 private SourceWitchContent(){}
}

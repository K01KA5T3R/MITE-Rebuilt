package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
/** Actual native Spider state/AI/save/loot owner, inherited by the three custom spiders. */
public final class SourceArachnidOwners {
 public static SourceArachnidState.Species species(Mob mob){
  var type=mob.getType();if(type==EntityType.SPIDER)return SourceArachnidState.Species.NORMAL;if(type==EntityType.CAVE_SPIDER)return SourceArachnidState.Species.CAVE;
  var id=ForgeRegistries.ENTITY_TYPES.getKey(type);if(id!=null&&id.getNamespace().equals(MiteReforged.ID))return switch(id.getPath()){case "wood_spider"->SourceArachnidState.Species.WOOD;case "demon_spider"->SourceArachnidState.Species.DEMON;case "phase_spider"->SourceArachnidState.Species.PHASE;case "black_widow_spider"->SourceArachnidState.Species.BLACK_WIDOW;default->null;};return null;
 }
 public static SourceArachnidState owner(Mob mob){var access=(SourceArachnidOwner)mob;var state=(SourceArachnidState)access.mite$arachnidState();if(state==null){var kind=species(mob);if(kind==null)throw new IllegalArgumentException("Not a ported source Arachnid");state=new SourceArachnidState(mob,kind);access.mite$arachnidState(state);}return state;}
 public static void construct(Object raw){var mob=(Mob)raw;var kind=species(mob);if(kind!=null){if(kind==SourceArachnidState.Species.NORMAL||kind==SourceArachnidState.Species.CAVE){mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind==SourceArachnidState.Species.NORMAL?12:16);mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(28);mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(1);mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);mob.setHealth(mob.getMaxHealth());}SourceArachnidAI.install(mob,owner(mob));mob.refreshDimensions();}}
 public static void beforeTick(Object raw){var mob=(Mob)raw;if(species(mob)!=null&&!mob.level().isClientSide)owner(mob).parentLife.beforeTick();}
 public static Object parentLife(Object raw){var mob=(Mob)raw;return species(mob)==null||mob.level().isClientSide?null:owner(mob).parentLife;}
 public static void tick(Object raw){var mob=(Mob)raw;if(species(mob)==null||mob.level().isClientSide)return;SourceCaveWaypoint.afterNativeTick(mob);var state=owner(mob);if(state.attackCooldown>0)--state.attackCooldown;SourceArachnidPeaceful.afterTick(mob,state.parentLife.harm.harming(),()->SourceArachnidWorld.outdoors(mob));state.afterTick();}
 public static void save(Object raw,Object tag){var mob=(Mob)raw;if(species(mob)!=null){owner(mob).save((CompoundTag)tag);SourceArachnidWorld.save(mob,(CompoundTag)tag);}}
 public static void load(Object raw,Object tag){var mob=(Mob)raw;if(species(mob)!=null){owner(mob).load((CompoundTag)tag);SourceArachnidWorld.load(mob,(CompoundTag)tag);}}
 public static boolean drops(Object raw,Object rawDamage,boolean recent){var mob=(Mob)raw;if(species(mob)==null)return false;var source=(DamageSource)rawDamage;int butchering=source.getEntity() instanceof LivingEntity responsible?EnchantmentEffects.level("butchering",responsible.getMainHandItem()):0;owner(mob).dropFewItems(recent,butchering);return true;}
 public static EntityDimensions dimensions(Object raw){var kind=species((Mob)raw);return kind==null?null:EntityDimensions.scalable(SourceArachnidProfiles.width(kind),SourceArachnidProfiles.height(kind));}
 public static int experience(Object raw){var kind=species((Mob)raw);return kind==null?-1:SourceArachnidProfiles.experience(kind);}
 public static float eyeHeight(Object raw,Object dimensions,float inherited){return species((Mob)raw)==null?inherited:((EntityDimensions)dimensions).height*.85f;}
 public static float snowballDamage(Object raw,Object rawDamage,float amount){var mob=(Mob)raw;var kind=species(mob);var source=(DamageSource)rawDamage;if(kind==null||!(source.getDirectEntity() instanceof Snowball)||SourceSnowballTraits.proper(source))return amount;if(source.getEntity() instanceof net.minecraft.world.entity.player.Player&&!SourceArachnidProfiles.woodFamily(kind))return -1;return 1;}
 private SourceArachnidOwners(){}
}

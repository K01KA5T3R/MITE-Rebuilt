package dev.koikaster.mite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
/** aci.getSubstituteClassToSpawn, after selecting the group's suitable species
 * and before constructing each individual. The selected group entry is retained. */
public final class SourceSpawnBody {
 public static EntityType<?> substitute(Object rawWorld,Object rawType){var world=(ServerLevel)rawWorld;var type=(EntityType<?>)rawType;
  if(type==Mobs.BAT.get()&&world.dimension()==WorldEvents.UNDERWORLD&&world.random.nextInt(6)==0)return SourceBatContent.GIANT.get();
  if(type==SourceLongdeadContent.LONGDEAD.get()&&world.random.nextInt(6)==0)return SourceLongdeadContent.GUARDIAN.get();return type;
 }
 private SourceSpawnBody(){}
}

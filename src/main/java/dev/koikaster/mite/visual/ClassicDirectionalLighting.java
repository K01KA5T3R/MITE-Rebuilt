package dev.koikaster.mite.visual;

import com.mojang.blaze3d.platform.Lighting;
import dev.koikaster.mite.MiteReforged;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/** 1.6.4 att.b uses the same two upward diffuse directions in every dimension.
 * The modern Nether-only downward second light is a client presentation change.
 */
public final class ClassicDirectionalLighting {
 public static boolean setupLegacyNether(Matrix4f matrix){
  if(!ClassicVisuals.lighting())return false;
  var level=Minecraft.getInstance().level;if(level==null)return false;var id=level.dimension().location();
  if(level.dimension()!=Level.NETHER&&!(id.getNamespace().equals(MiteReforged.ID)&&id.getPath().equals("underworld")))return false;
  Lighting.setupLevel(matrix);return true;
 }
 private ClassicDirectionalLighting(){}
}

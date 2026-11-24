package dev.koikaster.mite;
import java.util.function.*;
/** R195 aoz resource branches, in their original order; no Fortune multiplier or pity counter. */
public final class LegacyLeaves {
 public enum Drop {NONE,SAPLING,STICK,APPLE,ORANGE,BANANA}
 public static Drop broken(boolean placed,boolean swamp,Drop fruit,IntUnaryOperator random,DoubleSupplier draw){
  if(placed)return Drop.NONE;
  if(swamp&&random.applyAsInt(2)==0)return Drop.NONE;
  if(draw.getAsDouble()<.01f)return Drop.SAPLING;
  if(draw.getAsDouble()<.05f)return Drop.STICK;
  return fruit!=Drop.NONE&&draw.getAsDouble()<.005f?fruit:Drop.NONE;
 }
 public static Drop windfall(boolean placed,boolean swamp,Drop fruit,boolean fastForward,IntUnaryOperator random){
  // The original global chance_in_100/32 lookup is mapped to the modern tick RNG.
  if(placed||random.applyAsInt(100)!=0||fastForward&&random.applyAsInt(32)!=0||random.applyAsInt(500)!=0)return Drop.NONE;
  if(swamp&&random.applyAsInt(2)==0)return Drop.NONE;
  return fruit!=Drop.NONE&&random.applyAsInt(3)>0?fruit:Drop.STICK;
 }
 private LegacyLeaves(){}
}

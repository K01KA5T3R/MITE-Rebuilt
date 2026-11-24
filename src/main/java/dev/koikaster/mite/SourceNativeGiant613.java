package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
/** V103 keeps modern build heights. Source afl remains byte-identical: outside
 * its original safe band, translate only the adapter coordinate origin. */
public final class SourceNativeGiant613 {
 public static boolean generate(SourceGiantMushroom587.World world,SourceGiantMushroom587.Rng random,int x,int y,int z,int variant){
  if(!(world instanceof SourceMushroomWorld599 nativeWorld)||y>=1&&y<=248)return SourceGiantMushroom587.generate(world,random,x,y,z,variant);
  int offset=y-64;
  var translated=new SourceGiantMushroom587.World(){
   public int block(int a,int b,int c){int actual=b+offset;return nativeWorld.reader.isOutsideBuildHeight(new BlockPos(a,actual,c))?1:world.block(a,actual,c);}
   public boolean opaque(int token){return world.opaque(token);}
   public boolean set(int a,int b,int c,int token,int meta,int flags){int actual=b+offset;return !nativeWorld.reader.isOutsideBuildHeight(new BlockPos(a,actual,c))&&world.set(a,actual,c,token,meta,flags);}
  };
  return SourceGiantMushroom587.generate(translated,random,x,64,z,variant);
 }
 private SourceNativeGiant613(){}
}

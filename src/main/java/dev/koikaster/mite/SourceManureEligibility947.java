package dev.koikaster.mite;
/** Client side of ItemManure.tryFertilizeBlock: no mutation, FX, random draw or consumption. */
public final class SourceManureEligibility947 {
 public static boolean accept(SourceMyceliumKernel587.World w,int x,int y,int z,boolean top){
  int block=w.block(x,y,z);
  if(block==39||block==40){int below=w.block(x,y-1,z);if(below==60)return accept(w,x,y-1,z,true);if(!SourceSmallMushroom587.legal(w,block,x,y,z,w.metadata(x,y,z)))return false;return block==40?below==2&&w.outdoors(x,y,z):below==110&&!w.outdoors(x,y,z);}
  if(w.crop(block)||w.stem(block))return accept(w,x,y-1,z,true);
  if(block==2&&top&&w.block(x,y+1,z)==40)return accept(w,x,y+1,z,true);
  if(block==110&&top)return w.block(x,y+1,z)==39;
  return block==60&&top&&(w.metadata(x,y,z)&8)==0;
 }
 private SourceManureEligibility947(){}
}

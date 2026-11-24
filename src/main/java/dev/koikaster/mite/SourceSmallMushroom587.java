package dev.koikaster.mite;
/** Complete apj control kernel; World owns native geometry/light/storage and item routing remains explicit. */
public final class SourceSmallMushroom587 {
 public static final int BROWN=39,RED=40,FARMLAND=60,MYCELIUM=110;
 public interface Rng extends SourceGiantMushroom587.Rng{float nextFloat();}
 @FunctionalInterface public interface Player{void achievement(String name);}
 public interface World extends SourceGiantMushroom587.World {
  int metadata(int x,int y,int z);boolean noSky();boolean openSky(int x,int y,int z);boolean outdoors(int x,int y,int z);int light(int x,int y,int z);
  default boolean logSupport(int x,int y,int z){return false;}
  boolean passable(int x,int y,int z);boolean replaceable(int type,int metadata,int x,int y,int z);boolean nearbyPlayer16(int x,int y,int z);
  void dropSmall(int x,int y,int z,int type,int metadata);void manureFx(int x,int y,int z);
  boolean crop(int type);boolean stem(int type);boolean fertilizeSoil(int x,int y,int z,boolean top,Player player);
 }
 public static boolean lightSuitable(World world,int type,int x,int y,int z){if(type!=BROWN)return true;int light=world.light(x,y,z);return light>=0&&light<=13;}
 public static boolean legal(World world,int type,int x,int y,int z,int metadata){
  if(!world.noSky()&&world.openSky(x,y,z))return false;
  if(world.logSupport(x,y-1,z))return !world.outdoors(x,y,z)&&lightSuitable(world,type,x,y,z);
  int below=world.block(x,y-1,z);if(below==0||!world.opaque(below)&&below!=FARMLAND||below==12||below==82)return false;
  if(type==RED&&below!=2)return false;if(type==BROWN&&below!=1&&below!=13&&below!=3&&below!=FARMLAND&&below!=MYCELIUM)return false;
  return lightSuitable(world,type,x,y,z);
 }
 public static boolean canPlace(World world,int type,int x,int y,int z,int metadata){
  if(world.logSupport(x,y-1,z))return world.replaceable(type,metadata,x,y,z)&&legal(world,type,x,y,z,metadata);
  if(type==RED){if(!world.outdoors(x,y,z))return false;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(world.passable(x+dx,y,z+dz)&&world.openSky(x+dx,y,z+dz))return false;}
  else if(type==BROWN&&world.outdoors(x,y,z))return false;
  return world.replaceable(type,metadata,x,y,z)&&legal(world,type,x,y,z,metadata);
 }
 public static boolean convertsBelow(World world,int x,int y,int z){if(world.block(x,y,z)!=BROWN||world.block(x,y-1,z)!=FARMLAND)return false;int soil=world.metadata(x,y-1,z);return (soil&7)!=0&&(soil&8)!=0&&!world.outdoors(x,y,z)&&lightSuitable(world,BROWN,x,y,z);}
 public static boolean checkSoil(World world,int x,int y,int z,Player player){if(!convertsBelow(world,x,y+1,z))return false;if(player!=null)player.achievement("makeMycelium");return true;}
 public static boolean placed(World world,int x,int y,int z,Player player,boolean testOnly,boolean parentAccepted){if(parentAccepted&&!testOnly&&player!=null)checkSoil(world,x,y-1,z,player);return parentAccepted;}
 public static int increment(int metadata){return (metadata&3)<3?metadata+1:metadata;}
 public static boolean giant(World world,int type,int x,int y,int z,Player player,Rng random){
  int metadata=world.metadata(x,y,z);if(!legal(world,type,x,y,z,metadata))return false;int below=world.block(x,y-1,z);
  if(type==RED){if(below!=2||!world.outdoors(x,y,z))return false;}else if(type==BROWN){if(below!=MYCELIUM||world.outdoors(x,y,z))return false;}
  metadata=increment(metadata);if((metadata&3)!=3){world.set(x,y,z,type,metadata,6);return true;}
  world.set(x,y,z,0,0,3);int variant=type==BROWN?0:type==RED?1:-2;
  if(variant>=0&&SourceNativeGiant613.generate(world,random,x,y,z,variant)){if(player!=null)player.achievement("supersizeMe");return true;}
  world.set(x,y,z,type,metadata,3);return false;
 }
 public static boolean tick(World world,int type,int x,int y,int z,Rng random){
  if(!legal(world,type,x,y,z,world.metadata(x,y,z))){if(world.nearbyPlayer16(x,y,z))world.dropSmall(x,y,z,type,world.metadata(x,y,z));world.set(x,y,z,0,0,3);return true;}
  int roll=random.nextInt(100);
  if(roll==0){int remaining=5;for(int dx=x-4;dx<=x+4;dx++)for(int dz=z-4;dz<=z+4;dz++)for(int dy=y-1;dy<=y+1;dy++)if(world.block(dx,dy,dz)==type&&--remaining<=0)return false;
   int tx=x+random.nextInt(3)-1,ty=y+random.nextInt(2)-random.nextInt(2),tz=z+random.nextInt(3)-1;
   for(int i=0;i<4;i++){if(world.block(tx,ty,tz)==0&&legal(world,type,tx,ty,tz,0)){x=tx;y=ty;z=tz;}tx=x+random.nextInt(3)-1;ty=y+random.nextInt(2)-random.nextInt(2);tz=z+random.nextInt(3)-1;}
   if(world.block(tx,ty,tz)==0&&legal(world,type,tx,ty,tz,0)){if(type==BROWN&&world.block(tx,ty-1,tz)==MYCELIUM&&random.nextInt(4)>0)return false;world.set(tx,ty,tz,type,0,2);}
  }else if(roll==1||roll==2){if(type==BROWN)giant(world,type,x,y,z,null,random);}
  else if(type==BROWN&&roll<10&&convertsBelow(world,x,y,z))world.set(x,y-1,z,MYCELIUM,0,3);
  return false;
 }
 /** ItemManure.tryFertilizeBlock: direct mushroom is50%; the mycelium-top path is unconditional. */
 public static boolean manure(SourceMyceliumKernel587.World world,int x,int y,int z,boolean top,Player player,Rng random){
  int block=world.block(x,y,z);
  if(block==BROWN||block==RED){int below=world.block(x,y-1,z);if(below==FARMLAND)return manure(world,x,y-1,z,true,player,random);if(!legal(world,block,x,y,z,world.metadata(x,y,z)))return false;
   if(block==RED){if(below!=2||!world.outdoors(x,y,z))return false;}else if(below!=MYCELIUM||world.outdoors(x,y,z))return false;
   if(random.nextFloat()<.5f)giant(world,block,x,y,z,player,random);world.manureFx(x,y,z);return true;
  }
  if(world.crop(block)||world.stem(block))return manure(world,x,y-1,z,true,player,random);
  if(block==2&&top&&world.block(x,y+1,z)==RED)return manure(world,x,y+1,z,true,player,random);
  if(block==MYCELIUM&&top)return SourceMyceliumKernel587.fertilize(world,x,y,z,player,random);
  return block==FARMLAND&&top&&world.fertilizeSoil(x,y,z,true,player);
 }
 private SourceSmallMushroom587(){}
}

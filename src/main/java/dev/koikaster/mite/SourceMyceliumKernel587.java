package dev.koikaster.mite;
/** apk-specific server controls. Native weather/face adapters and shared aqz neighbor-parent delegation remain pending. */
public final class SourceMyceliumKernel587 {
 public interface World extends SourceSmallMushroom587.World {
  int fullLight(int x,int y,int z);boolean day();boolean precipitating();boolean freezing(int x,int z);boolean flatSolidBottom(int x,int y,int z);void lavaFx(int x,int y,int z);
 }
 public static boolean legal(World world,int x,int y,int z){int above=world.block(x,y+1,z);return above==0||above==99||above==100||!world.flatSolidBottom(x,y+1,z);}
 public static boolean tick(World world,int x,int y,int z,SourceSmallMushroom587.Rng random){
  if(!legal(world,x,y,z))return world.set(x,y,z,3,0,3);int above=world.block(x,y+1,z);if(above==8||above==9)return world.set(x,y,z,3,0,3);if(random.nextInt(4)>0)return false;
  if(world.fullLight(x,y+1,z)>13||world.outdoors(x,y,z)){if(world.day()&&(!world.precipitating()||world.freezing(x,z)))return world.set(x,y,z,3,0,3);}
  else {
   if(random.nextInt(256)==0&&world.block(x,y+1,z)==0){int count=0;for(int dx=-4;dx<=4;dx++)for(int dy=-2;dy<=2;dy++)for(int dz=-4;dz<=4;dz++){int block=world.block(x+dx,y+dy,z+dz);if((block==39||block==40)&&++count>2)return false;}world.set(x,y+1,z,39,0,3);return false;}
   int dx=0,dy=0,dz=0;
   for(int tries=0;tries<8;tries++){dx=random.nextInt(3)-1;dy=random.nextInt(5)-2;dz=random.nextInt(3)-1;if(dy!=-1&&dy!=0&&dy!=1||dx==0||dz==0){int block=world.block(x+dx,y+dy,z+dz);if((block==3||block==2||block==60)&&world.fullLight(x+dx,y+dy+1,z+dz)<=13&&legal(world,x+dx,y+dy,z+dz))break;}}
   x+=dx;y+=dy;z+=dz;if(!legal(world,x,y,z))return false;above=world.block(x,y+1,z);if(above==8||above==9||above==10||above==11)return false;int block=world.block(x,y,z);if((block==3||block==2||block==60)&&world.fullLight(x,y+1,z)<=13&&!world.outdoors(x,y,z))world.set(x,y,z,110,0,3);
  }return false;
 }
 public static boolean fertilize(World world,int x,int y,int z,SourceSmallMushroom587.Player player,SourceSmallMushroom587.Rng random){if(world.block(x,y+1,z)!=39)return false;SourceSmallMushroom587.giant(world,39,x,y+1,z,player,random);world.manureFx(x,y+1,z);return true;}
 public static boolean neighbor(World world,int x,int y,int z,boolean parentChanged){if(parentChanged&&world.block(x,y,z)!=110)return true;int above=world.block(x,y+1,z);if(above==10||above==11){world.lavaFx(x,y,z);return world.set(x,y,z,3,0,3)||parentChanged;}return parentChanged;}
 private SourceMyceliumKernel587(){}
}

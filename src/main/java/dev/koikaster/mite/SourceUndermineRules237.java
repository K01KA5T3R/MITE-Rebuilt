package dev.koikaster.mite;

/** Activated BlockUnderminable/BlockFalling control flow. Version-specific
 * block metadata, entity transport and scheduling remain in Host. Native
 * clients do not own physics. R195's default experimental flag is false. */
public final class SourceUndermineRules237 {
 public interface Actor {boolean player();boolean creative();}
 public interface Block {
  boolean undermines();int bit();int delay(Host world);boolean newSandPhysics();
  boolean solid(int metadata);
 }
 public interface Host {
  Block block(int x,int y,int z);int metadata(int x,int y,int z);
  boolean client();boolean instant();boolean loaded(int minX,int minY,int minZ,int maxX,int maxY,int maxZ);int floor();boolean experimental();
  boolean dislodged(Block falling,int metadata,int x,int y,int z);
  Object createFalling(Block block,int x,int y,int z,int metadata);void onStartFalling(Block block,int x,int y,int z,Object entity);void spawn(Object entity);
  void remove(int x,int y,int z);void write(Block block,int x,int y,int z,int metadata,int flags);void schedule(Block block,int x,int y,int z,int delay);
  void checkNotLegal(Block block,int x,int y,int z);boolean parentPlaced(Block block,int x,int y,int z,int metadata,Actor placer,boolean testOnly);
 }
 public static boolean validMetadata(int metadata){return metadata>=0&&metadata<2;}
 public static void added(Host world,Block block,int x,int y,int z){}
 public static boolean neighbor(Host world,Block block,int x,int y,int z){
  if(world.experimental()){var above=world.block(x,y+1,z);if(above!=null&&above.newSandPhysics())world.checkNotLegal(above,x,y+1,z);}return false;
 }
 public static boolean tick(Host world,Block block,int x,int y,int z){
  int metadata=world.metadata(x,y,z),bit=block.bit();
  if((metadata&bit)!=0){undermined(world,block,null,x,y,z);world.write(block,x,y,z,metadata^bit,0);return true;}return false;
 }
 public static void undermined(Host world,Block block,Actor player,int x,int y,int z){
  if(player!=null&&player.creative())return;
  if(tryFall(world,block,x,y,z)){
   int[] dx={0,1,0,-1},dz={-1,0,1,0};
   for(int i=0;i<dx.length;i++){var neighbor=world.block(x+dx[i],y,z+dz[i]);if(neighbor!=null&&neighbor.undermines())schedule(world,block,x+dx[i],y,z+dz[i]);}
  }
 }
 public static void schedule(Host world,Block caller,int x,int y,int z){
  var block=world.block(x,y,z);if(block==null||!block.undermines())return;
  int metadata=world.metadata(x,y,z)|block.bit();world.write(block,x,y,z,metadata,0);world.schedule(block,x,y,z,caller.delay(world));
 }
 public static boolean placed(Host world,Block block,int x,int y,int z,int metadata,Actor placer,boolean testOnly){
  if(!testOnly&&placer!=null&&placer.player()&&tryFall(world,block,x,y,z))return true;
  return world.parentPlaced(block,x,y,z,metadata,placer,testOnly);
 }
 public static void walked(Host world,Block block,int x,int y,int z){undermined(world,block,null,x,y,z);if(!world.client())tryFall(world,block,x,y,z);}
 public static void collided(Host world,Block block,int x,int y,int z){undermined(world,block,null,x,y,z);}
 public static boolean canFallDown(Host world,Block falling,int x,int y,int z,int metadata){var below=world.block(x,y,z);int belowMetadata=world.metadata(x,y,z);return below==null||!below.solid(belowMetadata)||world.dislodged(falling,metadata,x,y,z);}
 public static boolean tryFall(Host world,Block block,int x,int y,int z){
  int metadata=world.metadata(x,y,z);
  if(canFallDown(world,block,x,y-1,z,metadata)&&y>=world.floor()){
   int range=32;
   if(!world.instant()&&world.loaded(x-range,y-range,z-range,x+range,y+range,z+range)){
    if(!world.client()){
     var entity=world.createFalling(block,x,y,z,metadata);world.onStartFalling(block,x,y,z,entity);world.spawn(entity);
     var above=world.block(x,y+1,z);if(above!=null&&above.undermines())schedule(world,above,x,y+1,z);return true;
    }
   }else{
    world.remove(x,y,z);while(canFallDown(world,block,x,y-1,z,metadata)&&y>world.floor())--y;
    if(y>world.floor())world.write(block,x,y,z,metadata,2);return true;
   }
  }
  return false;
 }
 public static void finished(Host world,Block block,int x,int y,int z){if(block.newSandPhysics())world.checkNotLegal(block,x,y,z);}
 private SourceUndermineRules237(){}
}

package dev.koikaster.mite;

import java.util.Random;

/** Direct BiomeGenUnderworld.placeMycelium owner; staged, not registered. */
public final class OriginalMyceliumPopulation {
 public interface World {
  int underworldYOffset();
  boolean air(int x,int y,int z);
  boolean alwaysSolidOpaqueCube(int x,int y,int z);
  boolean hugeMushroomBlock(int x,int y,int z);
  boolean setMycelium(int x,int y,int z);
  void markNaturalMycelium(int x,int z);
  boolean giantBrownMushroom(Random random,int x,int y,int z);
  void setSmallBrownMushroom(int x,int y,int z);
 }
 public static void populate(World world,long hashedWorldSeed,int chunkOriginX,int chunkOriginZ){
  // The original cache is mutable but affects only performance. Own it per decoration
  // to keep native generation workers from sharing or mutating one returned list.
  var field=new OriginalChunkPostField(1,hashedWorldSeed,24,.0625f);
  var random=new Random();
  for(int x=chunkOriginX;x<chunkOriginX+16;x++)for(int z=chunkOriginZ;z<chunkOriginZ+16;z++){
   var posts=field.getNearbyPostsForBlockCoords(x,z);
   nextPost:for(int i=0;i<posts.size();i++){
    var post=posts.get(i);
    if(post.getDistanceSqFromBlockCoords(x,z)>(double)(field.getPostMaxRadiusOfEffectSq()+4))continue;
    random.setSeed(post.getSeed());random.nextInt();
    int y=random.nextInt(random.nextBoolean()?16:72)+24+world.underworldYOffset();
    int height=random.nextInt(5)+1;
    for(int dy=0;dy<height;dy++){
     if(world.air(x,y+1,z)){
      if(!world.alwaysSolidOpaqueCube(x,y,z)||world.hugeMushroomBlock(x,y,z)||!world.alwaysSolidOpaqueCube(x,y-1,z)||!world.setMycelium(x,y,z))continue nextPost;
      world.markNaturalMycelium(x,z);
      random.setSeed(post.getSeed()+(long)OriginalPostMath.hash(x,z));random.nextInt();
      if(random.nextInt(16)!=0||world.giantBrownMushroom(random,x,y+1,z))continue nextPost;
      world.setSmallBrownMushroom(x,y+1,z);continue nextPost;
     }
     ++y;
    }
   }
  }
 }
 private OriginalMyceliumPopulation(){}
}

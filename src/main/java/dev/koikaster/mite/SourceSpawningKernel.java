package dev.koikaster.mite;

import java.util.*;

/** Full aci control flow. Native world adapters own block/entity operations; source random-spawn
 * control flow owns cadence, eligibility and independent deep/upper caps.
 * World-generation entry is retained for the next region adapter. */
public final class SourceSpawningKernel {
 public enum Group {HOSTILE(50),ANIMAL(10),AMBIENT(5),AQUATIC(5);final int maximum;Group(int n){maximum=n;}}
 public enum Material {AIR,WATER,LAVA,OTHER}
 public interface Rng {int nextInt(int n);float nextFloat();boolean nextBoolean();}
 /** Unmodified164 abp hashes its packed x/z by XOR. Do not use modern ChunkPos.hashCode. */
 public record Chunk(int x,int z){@Override public int hashCode(){return x^z;}}
 public record Point(int x,int y,int z){}
 public record Player(int x,int footY,int z,boolean eligible){}
 public record Entry(Object type,int weight,int min,int max){}
 public static final class Counters {public int decreased,increased,none;}
 public interface Block {
  Material material();boolean fullStandardCube();boolean solidAt();double top();boolean topFlatSolid();boolean slabOrStairs();
  boolean bedrockOrMantle();boolean myceliumOrLeaves();boolean snowLayer();boolean cactus();boolean grass();boolean ice();
 }
 public interface Actor {
  float width();float height();boolean bat();boolean phaseSpider();boolean ghast();boolean burnsInSun();int maximumPack();
  float initialHang(int x,int y,int z,float px,float py,float pz);void move(float x,float y,float z,float yaw,float pitch);boolean canSpawnHere(boolean light);
  int blockX();int eyeBlockY();int blockZ();void aquaticInitialDespawn(int value);
 }
 public interface World {
  Rng random();Counters counters();List<Player> players();boolean overworld();boolean underworld();boolean daytime();boolean newMoon();boolean fullMoon();boolean bloodMoon(boolean nightOnly);boolean blueMoon(boolean nightOnly);boolean blueMoonNight();boolean thunderingForSpawn();
  boolean hostileAllowed();boolean peacefulAllowed();boolean spawningDisabled();long timeOfDay();
  default boolean tournamentScore(){return false;}default boolean tournamentAllowsAnimals(){return false;}default boolean tournamentArena(int x,int z){return true;}default boolean tournamentActive(){return false;}
  int nearbyHostiles(Player player);float strongholdProximity(int x,int z);int mobCount(boolean below60,boolean atLeast60);int count(Group group);void hostileLimit(boolean deep,int value);
  /** Old world floor0. A modern adapter must explicitly account for the retained negative terrain. */
  default int minimumY(){return 0;}default boolean chunkReady(Chunk chunk){return true;}int sampleHeightExclusive(Chunk chunk);Block block(int x,int y,int z);boolean air(int x,int y,int z);int totalLight(int x,int y,int z);int skyLight(int x,int y,int z);
  boolean playerWithin(double x,double y,double z,double radius,boolean ignoreCreative);Point spawnPoint();boolean outdoors(int x,int y,int z);
  Object suitable(Group group,int x,int y,int z);Object substitute(Object type);boolean vampireType(Object type);boolean ghastType(Object type);Actor construct(Object type)throws Exception;Object finish(Actor actor,Object data);void spawn(Actor actor);void constructionError(Exception error);
  List<Entry> worldgenRows(Object biome,Group group);float worldgenProbability(Object biome);boolean horseType(Object type);boolean ordinaryWolfType(Object type);boolean animalTypeExceptHellhound(Object type);int initialY(int x,int z);int highestWaterY(int x,int z);int chunkAnimals(int x,int z);void chunkAnimals(int x,int z,int value);
  /** The group already chose this type at the original sampled location. */
  default boolean canPlaceSelected(Actor actor,Object selected,boolean light){return actor.canSpawnHere(light);}
  // Region population still uses spawn; only the live world can report native admission.
  default boolean spawnNaturally(Actor actor){spawn(actor);return true;}
  default double globalChance(){return Math.random();}
 }
 private final HashMap<Chunk,Boolean> eligible=new HashMap<>();private final Random random;
 public SourceSpawningKernel(){this(new Random());}public SourceSpawningKernel(Random independentSpawnerRandom){random=Objects.requireNonNull(independentSpawnerRandom);}
 public Map<Chunk,Boolean> eligibleSnapshot(){return Collections.unmodifiableMap(new HashMap<>(eligible));}
 public void eligible(World w,boolean hostile){
  eligible.clear();boolean day=w.daytime(),full=w.fullMoon(),blood=w.bloodMoon(false);
  for(var p:w.players()){
   if(!p.eligible)continue;if(hostile){int limit;if(w.overworld()){limit=(int)(8f*(1f+(float)(64-p.footY)/32f));int minimum=8;if(!day){if(blood)minimum=minimum*3/2;else if(full)minimum=minimum*5/4;}if(limit<minimum)limit=minimum;}else limit=8;limit=(int)((float)limit+(float)limit*w.strongholdProximity(p.x,p.z));if(w.nearbyHostiles(p)>limit)continue;}
   int cx=p.x>>4,cz=p.z>>4;for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){boolean edge=dx==-8||dx==8||dz==-8||dz==8;var c=new Chunk(cx+dx,cz+dz);if(!edge)eligible.put(c,false);else if(!eligible.containsKey(c))eligible.put(c,true);}
  }
 }
 private Point chunkPoint(World w,Chunk chunk){int x=chunk.x*16+w.random().nextInt(16),z=chunk.z*16+w.random().nextInt(16);int minimum=w.minimumY(),y=minimum+w.random().nextInt(w.sampleHeightExclusive(chunk)-minimum);return new Point(x,y,z);}
 private static float distanceSpawn(World w,float x,float y,float z){var spawn=w.spawnPoint();float dx=x-(float)spawn.x,dy=y-(float)spawn.y,dz=z-(float)spawn.z;return dx*dx+dy*dy+dz*dz;}
 private Actor construct(World w,Object selected)throws Exception{return w.construct(w.substitute(selected));}
 public int hostile(World w,boolean deep){
  boolean overworld=w.overworld(),newMoon=w.newMoon(),full=w.fullMoon(),blood=w.bloodMoon(false),blue=w.blueMoon(false),day=w.daytime();int limit=Group.HOSTILE.maximum*eligible.size()/256;if(deep)limit*=2;w.hostileLimit(deep,limit);if(w.mobCount(deep,!deep)>=limit)return 0;
  boolean hang=w.random().nextBoolean();int total=0;
  chunks:for(var chunk:eligible.keySet()){
   if(eligible.get(chunk)||!w.chunkReady(chunk))continue;var point=chunkPoint(w,chunk);if(deep&&point.y>=60)point=chunkPoint(w,chunk);int x=point.x,y=point.y,z=point.z;
   if(overworld&&y==63&&w.random().nextInt(4)>0&&w.block(x,y-1,z).ice()||!w.playerWithin(x,y,z,48,true)&&w.random().nextInt(2)==0||(!deep?y<60||w.mobCount(false,true)>=limit:y>=60||w.mobCount(true,false)>=limit))continue;
   if(!canSpawnAt(w,Group.HOSTILE,x,y,z,false,null))continue;int below=0,above=0;
   groups:for(int group=0;group<3;group++){
    int px=x,pz=z;Object selected=null,data=null;int attempts=0,maximum=4;
    while(attempts<maximum){double[] result=new double[1];px+=w.random().nextInt(6)-w.random().nextInt(6);pz+=w.random().nextInt(6)-w.random().nextInt(6);if(!canSpawnAt(w,Group.HOSTILE,px,y,pz,false,result)){attempts++;continue;}
     float fx=(float)px+.5f,fy=(float)result[0],fz=(float)pz+.5f;boolean close=(w.overworld()||w.underworld())&&w.playerWithin(fx,fy,fz,24,true)&&w.totalLight(px,(int)Math.floor(fy),pz)==0&&w.totalLight(px,(int)Math.floor(fy)+1,pz)==0;
     if(close?w.playerWithin(fx,fy,fz,8,false):w.playerWithin(fx,fy,fz,24,false)){attempts++;continue;}if(distanceSpawn(w,fx,fy,fz)<576f){attempts++;continue;}
     if(selected==null&&(selected=w.suitable(Group.HOSTILE,px,y,pz))==null)continue groups;
     if(w.ghastType(selected)&&w.playerWithin(fx,fy,fz,48,false)){attempts++;continue;}
     Actor actor;try{actor=construct(w,selected);}catch(Exception error){w.constructionError(error);return total;}
     if(actor.bat()&&hang)fy=actor.initialHang(px,y,pz,fx,fy,fz);maximum=overworld&&actor.phaseSpider()?64:4;actor.move(fx,fy,fz,w.random().nextFloat()*360f,0);
     if(overworld&&w.blueMoonNight()&&w.skyLight(actor.blockX(),actor.eyeBlockY(),actor.blockZ())>0){attempts++;continue;}
     boolean allowed=w.canPlaceSelected(actor,selected,true);
     if(actor.width()>=1f&&!allowed){
      actor.move(fx-.5f,fy,fz,w.random().nextFloat()*360f,0);
      allowed=w.canPlaceSelected(actor,selected,true);
      if(!allowed){
       actor.move(fx+.5f,fy,fz,w.random().nextFloat()*360f,0);
       allowed=w.canPlaceSelected(actor,selected,true);
      }
      if(!allowed){
       actor.move(fx,fy,fz-.5f,w.random().nextFloat()*360f,0);
       allowed=w.canPlaceSelected(actor,selected,true);
      }
      if(!allowed){
       actor.move(fx,fy,fz+.5f,w.random().nextFloat()*360f,0);
       allowed=w.canPlaceSelected(actor,selected,true);
      }
     }
     if(!allowed){attempts++;continue;}
     if(day){if(actor.burnsInSun()&&w.outdoors((int)Math.floor(fx),(int)Math.floor(fy),(int)Math.floor(fz)))continue chunks;}else{int skip=blue?54:blood?2:full?3:newMoon?6:4;if(w.random().nextInt(skip)!=0&&w.outdoors((int)Math.floor(fx),(int)Math.floor(fy),(int)Math.floor(fz)))continue chunks;}
     var candidateData=w.finish(actor,data);
     attempts++;
     if(!w.spawnNaturally(actor))continue;
     data=candidateData;
     if(y<60)below++;else above++;
     total++;
     if(y<60?below>=actor.maximumPack():above>=actor.maximumPack())continue chunks;
    }
   }
  }return total;
 }
 public int peaceful(World w,Group kind){
  boolean blueAnimals=w.blueMoon(true);int limit=kind.maximum*eligible.size()/256;if(kind==Group.ANIMAL&&w.tournamentAllowsAnimals())limit*=2;if(w.count(kind)>=limit)return 0;boolean hang=w.random().nextBoolean();int total=0;
  chunks:for(var chunk:eligible.keySet()){
   if(eligible.get(chunk)||!w.chunkReady(chunk))continue;var point=chunkPoint(w,chunk);int x=point.x,y=point.y,z=point.z;if(kind==Group.ANIMAL&&w.tournamentActive()&&!w.tournamentArena(x,z)||!canSpawnAt(w,kind,x,y,z,false,null))continue;int spawned=0;
   groups:for(int group=0;group<3;group++){int px=x,pz=z;Object selected=null,data=null;int attempts=0,maximum=4;
    while(attempts<maximum){double[] result=new double[1];px+=w.random().nextInt(6)-w.random().nextInt(6);pz+=w.random().nextInt(6)-w.random().nextInt(6);if(!canSpawnAt(w,kind,px,y,pz,false,result)){attempts++;continue;}float fx=(float)px+.5f,fy=(float)result[0],fz=(float)pz+.5f;
     if(w.playerWithin(fx,fy,fz,24,false)||distanceSpawn(w,fx,fy,fz)<576f){attempts++;continue;}
     if(selected==null){selected=w.suitable(kind,px,y,pz);if(selected==null)continue groups;if(w.vampireType(selected)&&w.random().nextInt(4)==0)maximum=4+w.random().nextInt(5);}
     Actor actor;try{actor=construct(w,selected);}catch(Exception error){w.constructionError(error);return total;}if(actor.bat()&&hang)fy=actor.initialHang(px,y,pz,fx,fy,fz);actor.move(fx,fy,fz,w.random().nextFloat()*360f,0);if(!w.canPlaceSelected(actor,selected,kind!=Group.ANIMAL||!blueAnimals)){attempts++;continue;}
     var candidateData=w.finish(actor,data);
     attempts++;
     if(!w.spawnNaturally(actor))continue;
     data=candidateData;
     total++;
     if(++spawned>=actor.maximumPack())continue chunks;
    }
   }
  }return total;
 }
 public float rate(World w){
  if(!w.overworld())return .25f;float modifier=1;var c=w.counters();
  if(c.decreased>0){--c.decreased;modifier*=.5f;}else if(random.nextInt(24000)==0)c.decreased=random.nextInt(4000)+1;
  if(c.increased>0){--c.increased;modifier*=2;}else if(random.nextInt(24000)==0)c.increased=random.nextInt(2000);
  if(c.none>0){--c.none;modifier=0;}else if(random.nextInt(24000)==0)c.none=random.nextInt(2000)+random.nextInt(2000);
  if(modifier<1&&(w.bloodMoon(false)||w.thunderingForSpawn()))modifier=1;return modifier;
 }
 public void perform(World w){
  if(w.spawningDisabled()||w.players().isEmpty())return;float modifier=rate(w);boolean below=w.hostileAllowed()&&w.globalChance()<(double)(.1f*modifier),above=w.hostileAllowed()&&w.globalChance()<(double)(.17f*modifier),peaceful=w.peacefulAllowed()&&w.timeOfDay()%400==0,animals=w.tournamentScore()&&w.timeOfDay()%400==0;if(!animals)animals=w.blueMoon(true)&&w.timeOfDay()%400==0;if(!below&&!above&&!peaceful&&!animals)return;
  eligible(w,true);if(below)hostile(w,true);if(above)hostile(w,false);eligible(w,false);if(peaceful){peaceful(w,Group.AMBIENT);peaceful(w,Group.AQUATIC);}if(animals)peaceful(w,Group.ANIMAL);
 }
 public static boolean canSpawnOn(Group kind,Block block,boolean initial){
  if(kind==Group.AMBIENT)return true;if(block==null)return false;if(kind==Group.AQUATIC)return block.material()==Material.WATER;if(block.bedrockOrMantle()||block.myceliumOrLeaves())return false;if(kind==Group.ANIMAL)return block.grass();return block.snowLayer()||block.cactus()||block.slabOrStairs()||block.topFlatSolid();
 }
 public static boolean canSpawnAt(World w,Group kind,int x,int y,int z,boolean initial,double[] result){
  if(result!=null)result[0]=y;if(y<w.minimumY()+1)return false;var block=w.block(x,y,z);if(block.fullStandardCube()||block.material()==Material.LAVA)return false;
  if(kind==Group.AQUATIC){if(block.material()!=Material.WATER)return false;return result==null||w.block(x,y-1,z).material()==Material.WATER;}if(kind==Group.AMBIENT)return block.material()==Material.AIR;
  if(block.solidAt()||block.snowLayer()||block.cactus()){if(result==null)return true;result[0]=(double)y+block.top()+.005;if(block.snowLayer())return canSpawnOn(kind,w.block(x,y-1,z),initial);return canSpawnOn(kind,block,initial);}
  var below=w.block(x,y-1,z);if(!below.solidAt()&&!below.snowLayer()&&!below.cactus())return false;if(result==null)return true;result[0]=(double)(y-1)+below.top()+.005;return canSpawnOn(kind,below,initial);
 }
 private static Entry weighted(List<Entry> rows,Rng random){int total=0;for(var row:rows)total+=row.weight;int value=random.nextInt(total);for(var row:rows){value-=row.weight;if(value<0)return row;}throw new IllegalStateException("Invalid source weights");}
 public static void worldgen(World w,Object biome,Group kind,int minX,int minZ,int rangeX,int rangeZ,Rng random){
  var rows=w.worldgenRows(biome,kind);if(rows.isEmpty())return;
  while(random.nextFloat()<w.worldgenProbability(biome)){
   var entry=weighted(rows,w.random());if(w.horseType(entry.type)?random.nextFloat()<.8f:w.ordinaryWolfType(entry.type)&&random.nextFloat()<.5f)continue;Object data=null;int group=entry.min+random.nextInt(1+entry.max-entry.min),x=minX+random.nextInt(rangeX),z=minZ+random.nextInt(rangeZ),initialX=x,initialZ=z;boolean animal=w.animalTypeExceptHellhound(entry.type);int groupSpawned=0;
   for(int i=0;i<group;i++){boolean success=false;for(int attempt=0;!success&&attempt<4;attempt++){
    double[] result=new double[1];int y=w.initialY(x,z);if(kind==Group.AQUATIC&&w.block(x,y,z).material()==Material.WATER){int highest=w.highestWaterY(x,z);if(highest-y>3)y=random.nextInt(highest-y-3)+y+2;}
    if(canSpawnAt(w,kind,x,y,z,true,result)){
     if(animal){int count=w.chunkAnimals(x,z);if(count>=4)continue;w.chunkAnimals(x,z,count+1);}
     float fx=(float)x+.5f,fy=(float)result[0],fz=(float)z+.5f;Actor actor;try{actor=w.construct(entry.type);}catch(Exception error){w.constructionError(error);continue;}
     int height=(int)actor.height()+1;boolean prevented=false;for(int dy=1;dy<height;dy++)if(w.block(x,y+dy,z).fullStandardCube()){prevented=true;break;}
     if(!prevented){
      actor.move(fx,fy,fz,random.nextFloat()*360f,0);
      data=w.finish(actor,data);
      if(kind==Group.AQUATIC)actor.aquaticInitialDespawn(-9600);
      w.spawn(actor);success=true;groupSpawned++;
     }
    }
    x+=random.nextInt(5)-random.nextInt(5);z+=random.nextInt(5)-random.nextInt(5);
    // Preserve aci's rangeX comparison in the z bound, even for rectangular requests.
    while(x<minX||x>=minX+rangeX||z<minZ||z>=minZ+rangeX){x=initialX+random.nextInt(5)-random.nextInt(5);z=initialZ+random.nextInt(5)-random.nextInt(5);}
   }}
  }
 }
}

package dev.koikaster.mite;

/** Literal js chunk catch-up and load-throttled outer loop. Host supplies the
 * version-specific world, chunk persistence, section and weather APIs. */
public final class SourceFastForwardRules {
 public interface Position {int x();int z();}
 public interface Player extends Position {}
 public interface Chunk extends Position {long lastTime();void lastTime(long value);void saveOnCompletion();Section[] sections();}
 public interface Section {boolean randomTicks();int baseY();Block block(int x,int y,int z);}
 public interface Block {boolean randomTicks();void tick(Host world,int x,int y,int z);void fillRain(Host world,int x,int y,int z);}
 public interface Weather {boolean storming(long time);}
 public interface Host {
  boolean tournamentPrevents();boolean skip();int dimension();long time();void time(long value);
  Object environment();void restoreEnvironment(Object value);Weather weather();void rain(float previous,float current);void thunder(float previous,float current);void skyDarken(int value);int computeSkyDarken();void updateSkylight(Chunk chunk);
  Iterable<? extends Player> players();boolean chance16();int samplePosition();int height(int x,int z);boolean freeze(int x,int y,int z);void ice(int x,int y,int z);boolean precipitating();boolean snowfall(int x,int y,int z);void placeSnow(int x,int y,int z);boolean biomeRain(int x,int z);Block block(int x,int y,int z);
  float serverLoad();int timeOfDay();Iterable<? extends Position> activeChunks();Chunk chunk(Position position);void forwarding(boolean value);int earliestRelease();void flushLegacy();void ahead(long chunkTime,long worldTime);
 }
 public static void chunk(Host world,Chunk chunk){
  long current=world.time();
  if(chunk.lastTime()==0||chunk.lastTime()>=current||world.skip()){chunk.lastTime(current);return;}
  if(current-chunk.lastTime()>768000L)chunk.lastTime(current-768000L);
  int delta=(int)(chunk.lastTime()-current);if(delta<=-600)chunk.saveOnCompletion();
  boolean simulate=world.dimension()==0&&delta<=-2000;Object environment=world.environment();
  int baseX=chunk.x()*16,baseZ=chunk.z()*16,nearest=100;
  for(var player:world.players()){int dx=chunk.x()-player.x(),dz=chunk.z()-player.z(),distance=dx*dx+dz*dz;if(distance<nearest)nearest=distance;}
  int maximum=nearest<=2?8000:nearest<=5?4000:nearest<=10?2000:nearest<=20?1000:nearest<=29?500:250;
  try{
   for(int remaining=Math.min(-delta,maximum),run;remaining>0;remaining-=run){
    run=simulate?Math.min(remaining,4000):remaining;
    if(simulate){world.time(chunk.lastTime());var weather=world.weather();if(weather==null){world.rain(0,0);world.thunder(0,0);}else{world.rain(1,1);if(weather.storming(chunk.lastTime()))world.thunder(1,1);}world.skyDarken(world.computeSkyDarken());world.updateSkylight(chunk);}
    var sections=chunk.sections();
    for(int tick=0;tick<run;tick++){
     if(world.chance16()){
      int sample=world.samplePosition(),x=sample&15,z=sample>>8&15,y=world.height(x+baseX,z+baseZ);
      if(world.freeze(x+baseX,y-1,z+baseZ))world.ice(x+baseX,y-1,z+baseZ);
      if(world.precipitating()){
       if(world.snowfall(x+baseX,y,z+baseZ))world.placeSnow(x+baseX,y,z+baseZ);
       if(world.biomeRain(x+baseX,z+baseZ)){var block=world.block(x+baseX,y-1,z+baseZ);if(block!=null)block.fillRain(world,x+baseX,y-1,z+baseZ);}
      }
     }
     for(var section:sections){if(section==null||!section.randomTicks())continue;int yBase=section.baseY();for(int draw=0;draw<3;draw++){int sample=world.samplePosition(),x=sample&15,y=sample>>16&15,z=sample>>8&15;var block=section.block(x,y,z);if(block!=null&&block.randomTicks())block.tick(world,x+baseX,y+yBase,z+baseZ);}}
    }
    chunk.lastTime(chunk.lastTime()+run);
   }
   if(chunk.lastTime()>current)world.ahead(chunk.lastTime(),current);
  }finally{if(simulate){world.time(current);world.restoreEnvironment(environment);}}
 }
 public static void active(Host world){
  if(world.tournamentPrevents())return;float load=world.serverLoad();world.forwarding(true);
  try{if(load<.5f){for(var position:world.activeChunks())chunk(world,world.chunk(position));}else{int time=world.timeOfDay(),modulus=(int)((load-.29f)*10);for(var position:world.activeChunks())if(++time%modulus==0)chunk(world,world.chunk(position));}if(world.earliestRelease()<105)world.flushLegacy();}finally{world.forwarding(false);}
 }
 private SourceFastForwardRules(){}
}

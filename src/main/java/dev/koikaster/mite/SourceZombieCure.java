package dev.koikaster.mite;

/** xy.tryEntityInteraction and tw's conversion clock. The future native
 * owner must supply one authoritative timer, not run a second modern clock. */
public final class SourceZombieCure {
 public interface Interaction {
  boolean zombie();boolean villager();boolean converting();boolean client();boolean server();boolean weakness();int subtype();int nextInt(int bound);boolean creative();void begin(int ticks);void consume();
 }
 public interface Clock {boolean client();boolean converting();int time();void time(int ticks);int progress();void finish();}
 public interface Start {int difficulty();void time(int ticks);void markConverting();void removeWeakness();void strength(int ticks,int amplifier);void conversionEvent();}
 public interface Blocks {float nextFloat();int x();int y();int z();boolean specialBlock(int x,int y,int z);}
 public static boolean interact(Interaction h){
  if(h.zombie()){
   if(h.client())return h.villager()&&!h.converting();
   if(h.villager()&&!h.converting()&&h.weakness()){
    if(h.server()){h.begin(h.subtype()==1?1:h.nextInt(2401)+3600);if(!h.creative())h.consume();}
    return true;
   }
  }
  return false;
 }
 public static void beforeParent(Clock h){
  if(!h.client()&&h.converting()){h.time(h.time()-h.progress());if(h.time()<=0)h.finish();}
 }
 public static void begin(Start h,int ticks){h.time(ticks);h.markConverting();h.removeWeakness();h.strength(ticks,Math.min(h.difficulty()-1,0));h.conversionEvent();}
 public static int progress(Blocks h){
  int amount=1;
  if(h.nextFloat()<.01f){
   int count=0;
   for(int x=h.x()-4;x<h.x()+4&&count<14;x++)
    for(int y=h.y()-4;y<h.y()+4&&count<14;y++)
     for(int z=h.z()-4;z<h.z()+4&&count<14;z++){
      if(!h.specialBlock(x,y,z))continue;
      if(h.nextFloat()<.3f)amount++;count++;
     }
  }
  return amount;
 }
 private SourceZombieCure(){}
}

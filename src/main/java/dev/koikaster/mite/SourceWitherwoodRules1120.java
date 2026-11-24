package dev.koikaster.mite;

/** ant, aoj's witherwood consumers and afc. World/effect/drop APIs are explicit transports. */
public final class SourceWitherwoodRules1120 {
 public interface Effects<E> {
  boolean server();boolean living();E current();int amplifier(E effect);int duration(E effect);int interval(E effect);
  void duration(E effect,int value);void amplifier(E effect,int value);boolean player();void sync(E effect);void add(int duration,int amplifier);
 }
 public static <E> void contact(Effects<E> h){
  if(h.server()&&h.living()){
   E effect=h.current();if(effect==null)h.add(200,0);
   else if(h.amplifier(effect)<1){
    if(h.duration(effect)+h.interval(effect)<=200)h.duration(effect,h.duration(effect)+h.interval(effect));
    h.amplifier(effect,Math.max(h.amplifier(effect),0));if(h.player())h.sync(effect);
   }
  }
 }
 public static <E> void pottedContact(Effects<E> h){if(h.server()&&h.living())h.add(200,0);}
 public interface Merge<E> {int amplifier(E e);int duration(E e);boolean ambient(E e);void amplifier(E e,int value);void duration(E e,int value);void ambient(E e,boolean value);}
 public static <E> void merge(Merge<E> h,E existing,E incoming){if(h.amplifier(incoming)>h.amplifier(existing)){h.amplifier(existing,h.amplifier(incoming));h.duration(existing,h.duration(incoming));}else if(h.amplifier(incoming)==h.amplifier(existing)&&h.duration(existing)<h.duration(incoming))h.duration(existing,h.duration(incoming));else if(!h.ambient(incoming)&&h.ambient(existing))h.ambient(existing,h.ambient(incoming));}
 public static int drop(boolean illegal,boolean self,boolean exploded){return exploded?0:illegal||self?1:2;}
 public interface World {
  boolean nether();boolean airOrLeaves(int x,int y,int z);boolean empty(int x,int y,int z);boolean occurs(int x,int y,int z,int metadata);void place(int x,int y,int z,int metadata,int flags);
 }
 public interface Random {int nextInt(int bound);}
 public static boolean patch(World world,Random random,int x,int y,int z){
  int metadata=world.nether()?1:0;while(world.airOrLeaves(x,y,z)&&y>0)--y;
  for(int i=0;i<4;i++){
   int px=x+random.nextInt(8)-random.nextInt(8),py=y+random.nextInt(4)-random.nextInt(4),pz=z+random.nextInt(8)-random.nextInt(8);
   if(world.empty(px,py,pz)&&world.occurs(px,py,pz,metadata))world.place(px,py,pz,metadata,2);
  }
  return true;
 }
 public interface Particles {int nextInt(int bound);float nextFloat();void particle(double x,double y,double z);}
 public static void particles(Particles h,int x,int y,int z){int count=h.nextInt(3)+1;for(int i=0;i<count;i++)h.particle((float)x+h.nextFloat(),(float)y+h.nextFloat(),(float)z+h.nextFloat());}
 private SourceWitherwoodRules1120(){}
}

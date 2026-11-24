package dev.koikaster.mite;

/** xq/us control flow. Entity/world/connection/particle and damage transports stay typed at the edge. */
public final class SourcePearlRules838 {
 public interface Use {boolean client();boolean creative();void bob();void consume();float itemRandom();void bow(float volume,float pitch);void spawn();}
 public static boolean use(Use h){if(h.client())h.bob();else{if(!h.creative())h.consume();h.bow(.5f,.4f/(h.itemRandom()*.4f+.8f));h.spawn();}return true;}
 public interface Impact {
  boolean entity();void knockbackOnly();double x();double y();double z();double nextDouble();double gaussian();
  void particle(double x,double y,double z,double vx,double vy,double vz);boolean server();Object owner();boolean serverPlayer(Object actor);boolean disconnected(Object actor);boolean sameWorld(Object actor);
  boolean mounted(Object actor);void dismount(Object actor);void teleport(Object actor,double x,double y,double z);void clearFall(Object actor);void portalSound();void discard();
 }
 public static void impact(Impact h){
  if(h.entity())h.knockbackOnly();
  for(int i=0;i<32;i++)h.particle(h.x(),h.y()+h.nextDouble()*2,h.z(),h.gaussian(),0,h.gaussian());
  if(h.server()){
   if(h.owner()!=null&&h.serverPlayer(h.owner())){
    Object actor=h.owner();if(!h.disconnected(actor)&&h.sameWorld(actor)){
     if(h.mounted(h.owner()))h.dismount(h.owner());
     h.teleport(h.owner(),h.x(),h.y(),h.z());h.clearFall(h.owner());h.portalSound();
    }
   }
   h.discard();
  }
 }
 private SourcePearlRules838(){}
}

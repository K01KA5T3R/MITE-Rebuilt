package dev.koikaster.mite;

/** R195 ur.onImpact. Chicken construction/newborn/geometry are explicit native transports. */
public final class SourceEggRules887 {
 public interface Impact {
  boolean entity();void damage(float amount);boolean server();int nextInt(int bound);
  void chick();void poof();void discard();
 }
 public static void impact(Impact h){
  if(h.entity())h.damage(1f);
  if(h.server()&&h.nextInt(8)==0){int count=1;if(h.nextInt(32)==0)count=4;for(int i=0;i<count;i++)h.chick();}
  for(int i=0;i<8;i++)h.poof();if(h.server())h.discard();
 }
 private SourceEggRules887(){}
}

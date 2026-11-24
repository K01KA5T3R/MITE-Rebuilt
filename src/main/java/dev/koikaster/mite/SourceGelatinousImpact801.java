package dev.koikaster.mite;

/** EntityGelatinousSphere.onImpact. Entity result and captured block receivers are transports. */
public final class SourceGelatinousImpact801 {
 public interface Hit {
  boolean entity();boolean damage(float amount);void steam();
  boolean server();boolean block();Object hitBlock();Object neighbor();
  boolean pepsin();boolean acidic();boolean contact(Object block,boolean neighbor,boolean acid);void callback(Object block);
  int subtype();void poof(int subtype);void discard();
 }
 public static int attackDamage(int subtype){return subtype<3?subtype+1:subtype;}
 public static void impact(Hit h){
  if(h.entity()&&h.damage(1+(float)attackDamage(h.subtype())))h.steam();
  if(h.server()&&h.block()){
   Object block=h.hitBlock();
   if(h.pepsin()){
    Object neighbor=h.neighbor();
    if(neighbor!=null&&h.contact(neighbor,true,false))block=h.hitBlock();
    if(block!=null&&h.contact(block,false,false))block=h.hitBlock();
   }
   if(h.acidic()){
    Object neighbor=h.neighbor();
    if(neighbor!=null&&h.contact(neighbor,true,true))block=h.hitBlock();
    if(block!=null&&h.contact(block,false,true))block=h.hitBlock();
   }
   if(block!=null)h.callback(block);
  }
  int subtype=h.subtype();for(int i=0;i<8;i++)h.poof(subtype);
  if(h.server())h.discard();
 }
 private SourceGelatinousImpact801(){}
}

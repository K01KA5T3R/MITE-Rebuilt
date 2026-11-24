package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;

/** sm's combat clock and gates. World/entity operations are supplied by the
 * native owner; this rule class does not create a second target or AI loop. */
public final class SourceWitherRules {
 public static final class State {
  public final int[] nextHead=new int[2],idle=new int[2];
  public int breaking;
 }
 public interface Target {int id();boolean alive();boolean player();boolean creative();} // Legacy bG.a: damage invulnerability, not instabuild.
 public interface Host {
  State state();int invulnerable();void invulnerable(int ticks);int ticks();int difficulty();
  double x();double y();double z();int nextInt(int bound);double nextDouble();
  void parentAi();void heal(float amount);void health(float value);float maximumHealth();
  Target entity(int id);int sideTarget(int head);void sideTarget(int head,int id);
  Target mainTarget();List<? extends Target> candidates();boolean self(Target target);
  double distanceSquared(Target target);boolean canSee(Target target);
  void shoot(int head,Target target);void shoot(int head,double x,double y,double z,boolean charged);
  boolean griefing();boolean breakable(int x,int y,int z);boolean destroy(int x,int y,int z);
  void spawnExplosion();void spawnEvent();void breakEvent();
 }
 public interface Damage<R> {
  boolean arrow();boolean armored();boolean responsibleLiving();boolean responsiblePlayer();boolean sameCreatureType();
  R parent();
 }
 public static void start(Host h){h.invulnerable(220);h.health(h.maximumHealth()/3f);}
 public static <R> R hurt(Host h,Damage<R> damage){
  if(h.invulnerable()>0)return null;
  if(damage.armored()&&damage.arrow())return null;
  if(damage.responsibleLiving()&&!damage.responsiblePlayer()&&damage.sameCreatureType())return null;
  var s=h.state();if(s.breaking<=0)s.breaking=20;
  for(int i=0;i<s.idle.length;i++)s.idle[i]+=3;
  return damage.parent();
 }
 public static void step(Host h){
  var s=h.state();
  if(h.invulnerable()>0){
   int next=h.invulnerable()-1;
   if(next<=0){h.spawnExplosion();h.spawnEvent();}
   h.invulnerable(next);if(h.ticks()%10==0)h.heal(10f);
  }else{
   h.parentAi();
   for(int head=1;head<3;head++){
    if(h.ticks()<s.nextHead[head-1])continue;
    s.nextHead[head-1]=h.ticks()+10+h.nextInt(10);
    if(h.difficulty()>=2){
     int previous=s.idle[head-1]++;
     if(previous>15){
      double x=h.x()-10+h.nextDouble()*20,y=h.y()-5+h.nextDouble()*10,z=h.z()-10+h.nextDouble()*20;
      h.shoot(head+1,x,y,z,true);s.idle[head-1]=0;
     }
    }
    int id=h.sideTarget(head);
    if(id>0){
     var target=h.entity(id);
     if(target!=null&&target.alive()&&h.distanceSquared(target)<=900&&h.canSee(target)){
      h.shoot(head+1,target);s.nextHead[head-1]=h.ticks()+40+h.nextInt(20);s.idle[head-1]=0;continue;
     }
     h.sideTarget(head,0);continue;
    }
    var targets=new ArrayList<Target>(h.candidates());
    for(int attempt=0;attempt<10&&!targets.isEmpty();attempt++){
     var target=targets.get(h.nextInt(targets.size()));
     if(!h.self(target)&&target.alive()&&h.canSee(target)){
      // The original labeled continue abandons this head on a creative
      // player; it does not retry a different candidate from the same list.
      if(target.player()&&target.creative())break;
      h.sideTarget(head,target.id());break;
     }
     targets.remove(target);
    }
   }
   var main=h.mainTarget();h.sideTarget(0,main==null?0:main.id());
   maintain(h);
  }
 }
 static void maintain(Host h){maintain(h,true);}
 static void maintain(Host h,boolean regenerate){
   var s=h.state();
   if(s.breaking>0){
    --s.breaking;
    if(s.breaking==0&&h.griefing()){
     int y=(int)Math.floor(h.y()),x=(int)Math.floor(h.x()),z=(int)Math.floor(h.z());boolean broken=false;
     for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=3;dy++){
      int bx=x+dx,by=y+dy,bz=z+dz;if(!h.breakable(bx,by,bz))continue;broken=h.destroy(bx,by,bz)||broken;
     }
     if(broken)h.breakEvent();
    }
   }
   if(regenerate&&h.ticks()%20==0)h.heal(1f);
 }
 private SourceWitherRules(){}
}

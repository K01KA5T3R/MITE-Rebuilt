package dev.koikaster.mite;

/** EntityCubic.bl movement/target state, wired to the new039 native owner.
 * Native runtime verification is still pending.
 * Keeps source evaluation order, cached item/prey targets and early return.
 * The adapter must implement source vision/player admission and actual boxes;
 * it must not substitute modern Slime goals or add material kill gates. */
public final class OriginalCubicMotion {
 public interface Target {
  boolean removed();
  float health();
  boolean player();
  boolean item();
 }
 public interface Host {
  boolean ooze();boolean gelatinous();boolean acidic();boolean magmaCube();
  int sourceTicksWithOffset();int size();boolean onGround();boolean horizontalCollision();
  int blockFeedingCountdown();boolean feeding();boolean seeksItems();
  void blockFeedingCountdown(int value);void itemFeedingCountdown(int value);
  void tryDespawn();Target findPlayerToAttack(float range);
  Target closestVulnerablePlayer(double range,boolean requiresSight);
  Target closestDissolvableItem(double range,boolean requiresSight,boolean requiresPath);
  Target closestPrey(double range,boolean requiresSight,boolean requiresPath);
  boolean intersectsTargetCenterScaledXZ(Target target,double factor);
  boolean intersectsOwnCenterScaledXZ(Target target,double factor);
  boolean intersectsTarget(Target target);
  boolean collidesAfterMovingUp(double amount);
  void lookAt(Target target,float yawLimit,float pitchLimit);
  int jumpDelay(Target target);boolean makesSoundOnJump();
  void playSourceJumpSound();float nextFloat();
  void strafe(float value);void forward(float value);void jumping(boolean value);
  /** Original getBlocksBelow + top-face contact, gap <= .01, excluding fire above. */
  void contactSupportingBlocksWithAcid();
 }
 private int jumpDelay;
 private Target lastItem,lastPrey;
 public OriginalCubicMotion(int initialJumpDelay){jumpDelay=initialJumpDelay;}
 public void step(Host host){
  host.tryDespawn();
  Target target=host.findPlayerToAttack(host.ooze()?32:16);
  if(target==null)target=host.closestVulnerablePlayer(8,false);
  if(target==null&&host.gelatinous()&&host.blockFeedingCountdown()==0){
   if(host.seeksItems()){
    if(host.sourceTicksWithOffset()%20==0)lastItem=host.closestDissolvableItem(8,true,false);
    if(lastItem!=null){if(lastItem.removed()||lastItem.health()<=0)lastItem=null;else target=lastItem;}
   }
   if(target==null&&!host.feeding()){
    if(host.sourceTicksWithOffset()%20==0)lastPrey=host.closestPrey(8,false,false);
    if(lastPrey!=null){if(lastPrey.removed()||lastPrey.health()<=0)lastPrey=null;else target=lastPrey;}
   }
  }
  boolean crawl=host.ooze()||host.gelatinous()&&target!=null&&target.item()&&host.intersectsTargetCenterScaledXZ(target,4);
  boolean slowly=false;
  if(!crawl&&host.gelatinous()&&host.collidesAfterMovingUp(.25)){crawl=true;slowly=true;}
  if(target!=null)host.lookAt(target,10,20);
  if(host.gelatinous()){
   if(target!=null&&target.player()){host.blockFeedingCountdown(0);host.itemFeedingCountdown(0);}
   else{
    boolean touchingItem=target!=null&&target.item()&&host.intersectsTarget(target);
    if(touchingItem||host.feeding()){
     if(host.onGround()){
      host.strafe(0);
      if(!touchingItem||host.horizontalCollision()||host.intersectsOwnCenterScaledXZ(target,.5))host.forward(0);
     }
     return;
    }
   }
  }
  if(target!=null&&jumpDelay>host.jumpDelay(target))jumpDelay=host.jumpDelay(target);
  if(crawl){host.strafe(0);float forward=target==null?.15f:.25f;if(slowly)forward*=.65f;host.forward(forward);}
  else if(host.onGround()&&jumpDelay--<=0){
   jumpDelay=host.jumpDelay(target);host.jumping(true);
   if(host.makesSoundOnJump())host.playSourceJumpSound();
   host.strafe(1-host.nextFloat()*2);host.forward(host.size());
  }else{
   host.jumping(false);
   if(host.onGround()){host.forward(0);host.strafe(0);}
  }
  if(host.onGround()&&(host.acidic()||host.magmaCube()))host.contactSupportingBlocksWithAcid();
 }
}

package dev.koikaster.mite;

/** R195 uf/Raycast arithmetic shared by the native adapters. */
public final class LegacyReach {
 public enum Context {MELEE,INTERACTION}
 public static float block(boolean extended,float effectiveHeldBonus){return extended?5f:2.75f+effectiveHeldBonus;}
 public static float heightAdvantage(double observerFeet,double targetFeet){float difference=(float)(observerFeet-targetFeet);float advantage;if(difference<-.5f){advantage=(difference+.5f)*.5f;if(advantage<-1)advantage=-1;}else if(difference>.5f){advantage=(difference-.5f)*.5f;if(advantage>1)advantage=1;}else advantage=0;return advantage;}
 public static float entity(boolean extended,Context context,double observerFeet,double targetFeet,float heldBonus,boolean throwableTarget,boolean boatTarget){if(extended)return 5f;float height=heightAdvantage(observerFeet,targetFeet);if(context==Context.MELEE)return 1.5f+height+heldBonus+(throwableTarget?2f:0f);if(context==Context.INTERACTION){if(boatTarget)return 2.5f;return 2.5f+height;}return 0;}
 /** uf.getBlockReachFromPoints: eye-.25-.375 and eye-.25; Raycast compares to block center. */
 public static boolean blockCenterReachable(double eyeX,double eyeY,double eyeZ,int x,int y,int z,float reach){double dx=x+.5-eyeX,dz=z+.5-eyeZ;for(double offset:new double[]{-.625,-.25}){double dy=y+.5-(eyeY+offset);if(Math.sqrt(dx*dx+dy*dy+dz*dz)<=reach)return true;}return false;}
 public static float npc(float heldBonus,boolean ironGolem){return 1.5f+heldBonus*.6f+(ironGolem?.5f:0);}
 /** og special rp/EntityAnimal branch compares distance squared to this source threshold. */
 public static double animalSquaredThreshold(float width,float targetWidth,float heldBonus){double squared=width*1.75f*width*1.75f+targetWidth;return squared+heldBonus;}
 private LegacyReach(){}
}

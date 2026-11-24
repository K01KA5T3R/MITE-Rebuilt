package dev.koikaster.mite;

/** R195 bex periods, at average quality (the user explicitly disabled quality). */
public final class LegacyCrafting {
 public static int unmodified(float difficulty){
  if(difficulty<25f)return 25;
  if(difficulty>100f)return (int)Math.round(Math.pow(difficulty-100f,0.8f))+100;
  return Math.round(difficulty);
 }
 public static int period(float difficulty,int level,boolean table,boolean clumsy){
  return period(difficulty,level,table?.25f:0f,clumsy);
 }
 static int period(float difficulty,int level,float workbenchModifier,boolean clumsy){
  int period=unmodified(difficulty);if(clumsy)period*=2;
  return (int)Math.max(period/(1f+level*.02f+workbenchModifier),25f);
 }
 private LegacyCrafting(){}
}

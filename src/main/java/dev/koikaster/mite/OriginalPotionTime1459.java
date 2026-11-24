package dev.koikaster.mite;
/** R195 ma.a(int), including its unpadded leading minutes/hours. */
public final class OriginalPotionTime1459{
 public static String format(int par0){
  int var1=par0/20;
  int var2=var1/60;
  var1%=60;
  int hours=var2/60;
  if(hours>0)return hours+":"+((var2%=60)<10?"0":"")+(var1<10?var2+":0"+var1:var2+":"+var1);
  return var1<10?var2+":0"+var1:var2+":"+var1;
 }
 private OriginalPotionTime1459(){}
}

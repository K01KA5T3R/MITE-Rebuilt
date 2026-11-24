package dev.koikaster.mite;
/** Server-side EntityLivestock scalar rules; environmental reads belong to the
 * native owner. Original values/order are retained, with no player settings. */
public final class OriginalWellness419 {
 public float food,water,freedom;
 public OriginalWellness419(float food,float water,float freedom){set(food,water,freedom);}
 private static float clamp(float f){return Math.max(0f,Math.min(1f,f));}
 public void set(float food,float water,float freedom){this.food=clamp(food);this.water=clamp(water);this.freedom=clamp(freedom);}
 public boolean well(){return Math.min(freedom,Math.min(food,water))>=.25f;}
 public boolean hungry(){return food<.5f;}
 public boolean desperate(){return food<.05f||water<.05f;}
 public boolean canFeed(int age,boolean inLove,boolean alive,boolean fullHealth){return age==0&&!inLove&&alive&&!(fullHealth&&!well()&&!hungry());}
 public boolean canLove(boolean fullHealth){return well()&&fullHealth;}
 public void eaten(){food=clamp(food+.5f);}
 public boolean update(boolean foodNearby,boolean waterNearby,boolean rain,boolean crowded){float benefit=.1f,penalty=-.005f;food=clamp(food+(foodNearby?benefit:penalty));water=clamp(water+(waterNearby?benefit:rain?benefit/10f:penalty));freedom=clamp(freedom+(!crowded?benefit:penalty));return well();}
 public boolean allowGrowth(int current,int next){return !(current<0&&next>current&&desperate());}
 public void adopt(OriginalWellness419 a,OriginalWellness419 b){set(Math.min(a==null?1f:a.food,b==null?1f:b.food),Math.min(a==null?1f:a.water,b==null?1f:b.water),Math.min(a==null?1f:a.freedom,b==null?1f:b.freedom));}
}

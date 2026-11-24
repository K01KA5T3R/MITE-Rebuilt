package dev.koikaster.mite;

/** Literal abw/aei calendar and bfl moon/halo values. */
public final class SourceMoonCycle {
 public record Tint(float red,float green,float blue){}
 public static int phase(long time){return (int)(time/24000L+1L)%8;}
 public static int adjustedTime(long time){return ((int)(time%24000L)+6000)%24000;}
 public static int dayOfWorld(long time){return (int)((time+6000L)/24000L)+1;}
 public static boolean daytime(long time){int t=adjustedTime(time);return t>5000&&t<19000;}
 public static boolean blood(long time,boolean night){return (!night||!daytime(time))&&(time/24000L+1L)%32L==0&&!blue(time,night);}
 public static boolean blue(long time,boolean night){return (!night||!daytime(time))&&(time/24000L+1L)%128L==0;}
 public static boolean harvest(long time,boolean night){return (!night||!daytime(time))&&blood(time+192000L,night);}
 public static boolean moonDog(long time,boolean night){return (!night||!daytime(time))&&blue(time+192000L,night);}
 public static Rules.Moon kind(long time){return blue(time,false)?Rules.Moon.BLUE:blood(time,false)?Rules.Moon.BLOOD:harvest(time,false)?Rules.Moon.HARVEST:moonDog(time,false)?Rules.Moon.MOON_DOG:Rules.Moon.NORMAL;}
 public static Tint tint(long time){return blood(time,false)?new Tint(.6f,.2f,.1f):harvest(time,false)?new Tint(1,.8f,.45f):blue(time,false)?new Tint(.66f,.74f,1):new Tint(1,1,1);}
 public static float haloAlpha(float celestial,float rain){float factor=.25f;if(celestial>.77f)factor*=1-(celestial-.77f)/(.785f-.77f);return (1-rain)*factor;}
 public static float ringAlpha(float celestial,float rain){float factor=(1-rain)*.5f;if(celestial<.18f||celestial>.785f)factor=0;else if(celestial<.28f)factor*=(celestial-.18f)/(.28f-.18f);else if(celestial>.6f)factor*=1-(celestial-.6f)/(.785f-.6f);return factor;}
 private SourceMoonCycle(){}
}

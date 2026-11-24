package dev.koikaster.mite;
/** E08/E23/E30 regional weather. This independent schedule consumes no R195 random stream. */
public final class LocalWeatherPlan1981 {
 public static final int CELL=512,DAY=24000,FADE=300;
 public record Window(int start,int duration){}
 public static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
 private static int bounded(long value,int bound){return (int)Long.remainderUnsigned(value,bound);}
 public static Window window(long seed,int cellX,int cellZ,long day){long hash=mix(seed^((long)cellX*0x632be59bd9b4e019L)^((long)cellZ*0x9e3779b97f4a7c15L)^(day*0xd1b54a32d192ed03L));if(bounded(hash,8)!=0)return null;int duration=3600+bounded(mix(hash+1),2401),start=bounded(mix(hash+2),DAY-duration+1);return new Window(start,duration);}
 private static float smooth(float value){value=Math.max(0,Math.min(1,value));return value*value*(3-2*value);}
 public static float strength(long seed,long time,int x,int z){int cx=Math.floorDiv(x,CELL),cz=Math.floorDiv(z,CELL);var window=window(seed,cx,cz,Math.floorDiv(time,DAY));if(window==null)return 0;long tick=Math.floorMod(time,DAY)-window.start();if(tick<0||tick>=window.duration())return 0;float temporal=smooth(Math.min(tick,window.duration()-tick)/(float)FADE);float edge=Math.min(Math.min(Math.floorMod(x,CELL)+.5f,CELL-.5f-Math.floorMod(x,CELL)),Math.min(Math.floorMod(z,CELL)+.5f,CELL-.5f-Math.floorMod(z,CELL)))/32f;return temporal*smooth(edge);}
 private LocalWeatherPlan1981(){}
}

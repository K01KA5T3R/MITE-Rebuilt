package dev.koikaster.mite;

import java.util.function.LongUnaryOperator;

/** Historical reads belong to one world on the current server thread. The
 * actual shared live clock and weather fields are never overwritten. */
public final class SourceHistoricalTicks {
 private static final ThreadLocal<Frame> ACTIVE=new ThreadLocal<>();
 public static final class Frame implements AutoCloseable {
  final Object world;final Frame previous;final LongUnaryOperator calendarAt;long elapsed;
  float previousRain,rain,previousThunder,thunder;int darken;boolean forwarding;
  Frame(Object world,long elapsed,LongUnaryOperator calendarAt,float oldRain,float rain,float oldThunder,float thunder,int darken){this.world=world;previous=ACTIVE.get();this.elapsed=elapsed;this.calendarAt=calendarAt;previousRain=oldRain;this.rain=rain;previousThunder=oldThunder;this.thunder=thunder;this.darken=darken;}
  public long time(){return elapsed;}public void time(long value){elapsed=value;}
  public long calendar(){return calendarAt.applyAsLong(elapsed);}
  public long calendarAt(long value){return calendarAt.applyAsLong(value);}
  public Object environment(){return new Environment(previousRain,rain,previousThunder,thunder,darken);}
  public void restore(Object raw){var value=(Environment)raw;previousRain=value.previousRain;rain=value.rain;previousThunder=value.previousThunder;thunder=value.thunder;darken=value.darken;}
  public void rain(float previous,float current){previousRain=previous;rain=current;}public void thunder(float previous,float current){previousThunder=previous;thunder=current;}public void darken(int value){darken=value;}
  public void forwarding(boolean value){forwarding=value;}
  @Override public void close(){if(ACTIVE.get()!=this)throw new IllegalStateException("Historical tick scopes closed out of order");if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
 }
 private record Environment(float previousRain,float rain,float previousThunder,float thunder,int darken){}
 public static Frame open(Object world,long calendar,long game,float oldRain,float rain,float oldThunder,float thunder,int darken){return open(world,calendar,game,oldRain,rain,oldThunder,thunder,darken,elapsed->calendar+(elapsed-game));}
 public static Frame open(Object world,long calendar,long game,float oldRain,float rain,float oldThunder,float thunder,int darken,LongUnaryOperator calendarAt){if(calendarAt.applyAsLong(game)!=calendar)throw new IllegalArgumentException("Historical calendar anchor mismatch");var frame=new Frame(world,game,calendarAt,oldRain,rain,oldThunder,thunder,darken);ACTIVE.set(frame);return frame;}
 private static Frame frame(Object world){for(var value=ACTIVE.get();value!=null;value=value.previous)if(value.world==world)return value;return null;}
 public static boolean forwarding(Object world){var value=frame(world);return value!=null&&value.forwarding;}
 public static long day(long original,Object world){var value=frame(world);return value==null?original:value.calendar();}
 public static long game(long original,Object world){var value=frame(world);return value==null?original:value.elapsed;}
 private static float mix(float partial,float old,float now){return old+partial*(now-old);}
 public static float rain(float original,Object world,float partial){var value=frame(world);return value==null?original:mix(partial,value.previousRain,value.rain);}
 public static float thunder(float original,Object world,float partial){var value=frame(world);return value==null?original:mix(partial,value.previousThunder,value.thunder)*mix(partial,value.previousRain,value.rain);}
 public static int sky(int original,Object world){var value=frame(world);return value==null?original:value.darken;}
 private SourceHistoricalTicks(){}
}

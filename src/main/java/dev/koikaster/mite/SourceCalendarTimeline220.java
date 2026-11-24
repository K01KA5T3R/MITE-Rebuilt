package dev.koikaster.mite;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Elapsed time owns random-tick debt. Calendar segments describe the native
 * daylight rule and explicit calendar jumps without inventing elapsed ticks.
 * Only the source kernel's 768000-tick history, plus its pre-normal boundary,
 * is needed. This class has no access to live world clocks. */
public final class SourceCalendarTimeline220 {
 public static final long RETAIN=768002;
 public record Segment(long elapsed,long calendar,boolean running) {}
 private final NavigableMap<Long,Segment> segments=new TreeMap<>();
 private long lastElapsed,lastCalendar;
 private boolean lastRunning;
 public boolean empty(){return segments.isEmpty();}
 public long knownFrom(){if(empty())throw new IllegalStateException("Calendar timeline not initialized");return segments.firstKey();}
 public long lastElapsed(){return lastElapsed;}
 public long lastCalendar(){return lastCalendar;}
 public boolean lastRunning(){return lastRunning;}
 public List<Segment> segments(){return List.copyOf(segments.values());}
 public long calendar(long elapsed){
  if(empty())throw new IllegalStateException("Calendar timeline not initialized");
  var entry=segments.floorEntry(elapsed);
  if(entry==null)return segments.firstEntry().getValue().calendar();
  var segment=entry.getValue();
  return segment.running()?Math.addExact(segment.calendar(),elapsed-segment.elapsed()):segment.calendar();
 }
 private void anchor(long elapsed,long calendar,boolean running){
  var previous=segments.lowerEntry(elapsed);
  if(previous!=null){var before=previous.getValue();long projected=before.running()?Math.addExact(before.calendar(),elapsed-before.elapsed()):before.calendar();if(before.running()==running&&projected==calendar){segments.remove(elapsed);return;}}
  segments.put(elapsed,new Segment(elapsed,calendar,running));
 }
 /** Observe a completed native setter/tick. A same-instant command changes the
  * calendar at that instant. An otherwise unobserved discontinuity is located
  * at its first observed endpoint, without assigning extra elapsed ticks. */
 public boolean observe(long elapsed,long calendar,boolean running){
  if(elapsed<0||calendar<0)throw new IllegalArgumentException("Negative native time");
  if(!empty()&&elapsed==lastElapsed&&calendar==lastCalendar&&running==lastRunning)return false;
  if(!empty()&&elapsed<lastElapsed)segments.clear();
  if(!empty()&&elapsed>lastElapsed){long distance=elapsed-lastElapsed,calendarDistance=calendar-lastCalendar;if(calendarDistance==distance)anchor(lastElapsed,lastCalendar,true);else if(calendarDistance==0)anchor(lastElapsed,lastCalendar,false);}
  anchor(elapsed,calendar,running);lastElapsed=elapsed;lastCalendar=calendar;lastRunning=running;
  long oldest=Math.max(0,elapsed-RETAIN);
  if(segments.firstKey()<oldest){long firstCalendar=calendar(oldest);boolean firstRunning=segments.floorEntry(oldest).getValue().running();segments.headMap(oldest,false).clear();segments.put(oldest,new Segment(oldest,firstCalendar,firstRunning));}
  return true;
 }
 public static SourceCalendarTimeline220 restore(List<Segment> saved,long elapsed,long calendar,boolean running){
  if(saved.isEmpty()||elapsed<0||calendar<0)throw new IllegalArgumentException("Empty or negative calendar history");
  var result=new SourceCalendarTimeline220();long previous=-1;
  for(var segment:saved){if(segment.elapsed()<=previous||segment.elapsed()>elapsed||segment.calendar()<0)throw new IllegalArgumentException("Invalid calendar history order");result.segments.put(segment.elapsed(),segment);previous=segment.elapsed();}
  if(result.calendar(elapsed)!=calendar)throw new IllegalArgumentException("Calendar history endpoint mismatch");
  result.lastElapsed=elapsed;result.lastCalendar=calendar;result.lastRunning=running;
  if(result.segments.lastEntry().getValue().running()!=running)throw new IllegalArgumentException("Calendar history endpoint rate mismatch");return result;
 }
}

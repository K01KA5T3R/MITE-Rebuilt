package dev.koikaster.mite;

import java.util.*;

/** abw's event generation/filter/merge and WeatherEvent, without game/debug UI.
 * Creation time supplies the same independent java.util.Random seed input. */
public final class SourceMoonWeatherRules {
 public static final class Event {
  public int type;public long start,end,stormStart,stormEnd;public int duration,stormDuration;public boolean removed;
  public Event(long start,int duration){this.start=start;this.duration=duration;end=start+(long)duration;}
  public void bounds(long start,long end){this.start=start;this.end=end;duration=(int)(end-start);}
  public boolean hasStorm(){return stormStart>0;}
  public void storm(long start,long end){stormStart=start;stormEnd=end;stormDuration=(int)(end-start);if(hasStorm())type=3;}
  public void randomType(){if(hasStorm()){type=3;return;}var random=new Random(start);random.nextInt();type=random.nextInt(3);}
  public void addStorm(){if(hasStorm())return;var random=new Random(start);random.nextInt();if(random.nextInt(4)>0)return;stormDuration=Math.min(random.nextInt(4000)+2000,duration);stormStart=random.nextInt(4)==0?(random.nextBoolean()?start:end-(long)stormDuration):(long)random.nextInt(duration-stormDuration+1)+start;stormEnd=stormStart+(long)stormDuration;type=3;}
  public boolean raining(long time){return start<=time&&end>time;}
  public boolean storming(long time){return hasStorm()&&stormStart<=time&&stormEnd>time;}
 }
 public static List<Event> generate(long creation,int dimension,int day){
  var events=new ArrayList<Event>();if(day<2)return events;
  long first=(day-1)*24000-6000;
  var random=new Random(creation+(long)(dimension*938473)+(long)day);random.nextInt();
  for(int i=0;i<3&&random.nextInt(4)<=0;i++){
   var event=new Event(first+(long)random.nextInt(24000),random.nextInt(12000)+6000);
   if(SourceMoonCycle.harvest(event.start,true)||SourceMoonCycle.harvest(event.end,true)||SourceMoonCycle.harvest(event.start+6000L,true)||SourceMoonCycle.harvest(event.end-6000L,true)||SourceMoonCycle.blood(event.start,false)||SourceMoonCycle.blood(event.end,false)||SourceMoonCycle.blue(event.start,false)||SourceMoonCycle.blue(event.end,false))continue;
   events.add(event);
  }
  if(SourceMoonCycle.blood(first+6000L,false)){var event=new Event(first+6000L,13000);event.storm(event.start,event.end);events.add(event);}
  return events;
 }
 public static List<Event> today(long creation,int dimension,int day){
  var events=new ArrayList<Event>();for(int d=day-1;d<=day;d++)events.addAll(generate(creation,dimension,d));
  for(var a:events){if(a.removed)continue;for(var b:events){if(b==a||b.removed||a.end+6000L<b.start||a.start>b.end+6000L)continue;a.bounds(Math.min(a.start,b.start),Math.max(a.end,b.end));if(b.hasStorm()&&!a.hasStorm())a.storm(b.stormStart,b.stormEnd);b.removed=true;}}
  events.removeIf(event->event.removed);events.sort(Comparator.comparingLong(event->event.start));
  for(var event:events){event.randomType();event.addStorm();}return events;
 }
 public static Event at(List<Event> events,long time){for(var event:events)if(event.raining(time))return event;return null;}
 private SourceMoonWeatherRules(){}
}

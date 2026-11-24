package dev.koikaster.mite;

/** R195 xr/ut. The 200 XP amount is bound to the executed original enchantment cost(2). */
public final class SourceExperienceBottle854 {
 public static final int EXPERIENCE=200;
 public interface Use {boolean server();boolean creative();void consume();float itemRandom();void bow(float volume,float pitch);void spawn();}
 public interface Impact {boolean server();double x();double y();double z();void event(int id,int x,int y,int z,int data);void orb(int value,boolean bottle);void discard();}
 public static boolean use(Use h){if(h.server()){if(!h.creative())h.consume();h.bow(.5f,.4f/(h.itemRandom()*.4f+.8f));h.spawn();}return true;}
 public static void impact(Impact h){if(h.server()){h.event(2002,(int)Math.round(h.x()),(int)Math.round(h.y()),(int)Math.round(h.z()),0);for(int remaining=EXPERIENCE;remaining>0;){int part=SourceExperienceOrb854.split(remaining);h.orb(part,true);remaining-=part;}h.discard();}}
 private SourceExperienceBottle854(){}
}

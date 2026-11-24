package dev.koikaster.mite;

/** Exact EntityLivestock overrides, kept separate from of/wolf wounded gates.
 * This source-only stage has no native Animal owner yet. */
public final class OriginalLivestockFleeDecision {
 public interface Host {
  Object lastHarmingEntity();float distanceTo(Object entity);
  void decided(boolean value);void fleeing(boolean value);
 }
 public static boolean consider(Host host){var attacker=host.lastHarmingEntity();boolean value=attacker!=null&&host.distanceTo(attacker)<32f;host.decided(value);return value;}
 public static boolean stop(Host host){var attacker=host.lastHarmingEntity();if(attacker==null){host.decided(false);host.fleeing(false);return true;}if(host.distanceTo(attacker)>40f){host.fleeing(false);return true;}return false;}
 private OriginalLivestockFleeDecision(){}
}

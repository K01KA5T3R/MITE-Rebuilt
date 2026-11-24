package dev.koikaster.mite;
import java.util.ArrayDeque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
/** of.a's single ordinary-loot roll. Native inventory, equipment, XP and Forge
 * capture/event boundaries remain outside this decision. */
public final class SourceFallLoot1194 {
 private record Frame(LivingEntity actor,boolean ordinary){}
 private static final ThreadLocal<ArrayDeque<Frame>> active=new ThreadLocal<>();
 private static LivingEntity actor(Object raw){return raw instanceof SourceZombieBody body?body.actor():(LivingEntity)raw;}
 private static boolean scoped(LivingEntity actor){return !(actor instanceof Player)&&ClassicDefense.scoped(actor);}
 public static boolean massive(Object raw){return ((SourceFallAccess1187)(Object)actor(raw)).mite$massiveFall1187();}
 private static boolean roll(LivingEntity actor){return !scoped(actor)||!massive(actor)||actor.getRandom().nextFloat()<.1f;}
 public static boolean normalPart(Object raw){
  var actor=actor(raw);var frames=active.get();if(frames!=null)for(var frame:frames)if(frame.actor()==actor)return frame.ordinary();
  return roll(actor);
 }
 public static void dispatch(Object raw,Object damage,int looting,boolean recent){
  var actor=actor(raw);
  // The legacy pigman loot transformer replaces the entity's custom-drop body.
  if(actor instanceof SourcePigman pigman && pigman.iteElite()){
   IteEliteSpawns.dropConsumables(pigman,recent);
   return;
  }
  var frame=new Frame(actor,roll(actor));var frames=active.get();if(frames==null){frames=new ArrayDeque<>();active.set(frames);}frames.push(frame);
  try{
   var access=(SourceFallLootAccess1194)(Object)actor;var source=(DamageSource)damage;
   // Both callbacks always run. Their normal-drop portions use normalPart;
   // Mob's table reset, native equipment and each mixed owner's contents survive.
   access.mite$fallLootTable1194(source,recent);
   access.mite$fallLootCustom1194(source,looting,recent);
  }finally{if(frames.pop()!=frame)throw new IllegalStateException("Death loot frames out of order");if(frames.isEmpty())active.remove();}
 }
 public static Block earthBlock(Block block,Object actor){return normalPart(actor)?block:null;}
 /** Original of never serializes its massive-fall flag. Old private keys are
  * tolerated as input, but do not turn a rebuilt entity into a trap kill. */
 public static void ignoreLegacy(Object raw,boolean ignored){}
 public static void skipPersistent(Object raw,String key,boolean ignored){((CompoundTag)raw).remove(key);}
 private SourceFallLoot1194(){}
}

package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;

/** Direct of harming-memory and flee decisions.
 * Flags belong to the actual parent owner: this class does not duplicate its
 * attack target, path, has_decided_to_flee or fleeing fields.
 */
public final class OriginalParentHarmState {
 public interface Flags {
  boolean hasDecidedToFlee();void hasDecidedToFlee(boolean value);
  boolean fleeing();void fleeing(boolean value);
 }
 public interface Traits {
  boolean undead();boolean fleesWhenBadlyWounded();
 }
 private final Mob actor;private final Flags flags;private final Traits traits;
 private Entity harming;private String pendingUniqueId;public int memory;
 public OriginalParentHarmState(Mob actor,Flags flags){this(actor,flags,new Traits(){public boolean undead(){return actor.getMobType()==MobType.UNDEAD;}public boolean fleesWhenBadlyWounded(){return true;}});}
 public OriginalParentHarmState(Mob actor,Flags flags,Traits traits){this.actor=Objects.requireNonNull(actor);this.flags=Objects.requireNonNull(flags);this.traits=Objects.requireNonNull(traits);}
 public Flags flags(){return flags;}
 public Entity harming(){return harming;}
 public String pendingUniqueId(){return pendingUniqueId;}
 /** of2175: do not add a pending-ID clear here; expiry clears it in tick. */
 public void harming(Entity entity){harming=entity;memory=entity==null?0:900+actor.getRandom().nextInt(200);}
 public boolean badlyWounded(){return actor.getHealth()/actor.getMaxHealth()<.2f;}
 public boolean considerFleeing(){
  boolean decided=!(harming==null||traits.undead()||!traits.fleesWhenBadlyWounded()||!badlyWounded()||actor.distanceTo(harming)>32f)&&actor.getRandom().nextInt(2)==0;
  flags.hasDecidedToFlee(decided);return decided;
 }
 public boolean considerStopFleeing(){
  if(harming==null||!traits.fleesWhenBadlyWounded()||!badlyWounded()){flags.hasDecidedToFlee(false);flags.fleeing(false);return true;}
  if(actor.distanceTo(harming)>40f){flags.fleeing(false);return true;}return false;
 }
 /** Call at the start of actual actor tick, before parent AI executes. These
  * clocks are unoffset tickCount, unlike Phase's offset recharge/blink clocks.
  */
 public void beforeTick(){
  if(memory>0)--memory;
  if(harming!=null&&(harming.isRemoved()||memory==0))harming(null);
  if(memory==0)pendingUniqueId=null;else reinstateFromDisk();
  if(flags.fleeing()&&actor.tickCount%100==0)considerStopFleeing();
 }
 private void reinstateFromDisk(){
  if(actor.level().isClientSide||pendingUniqueId==null||harming!=null||actor.tickCount%10!=0)return;
  // nn2233 searches nearby loaded entities, not a global UUID lookup. Keep the
  // string verbatim; malformed strings simply fail matching until expiry.
  for(var entity:actor.level().getEntities(actor,actor.getBoundingBox().inflate(32,8,32)))if(entity.getUUID().toString().equals(pendingUniqueId)){harming=entity;pendingUniqueId=null;return;}
 }
 /** The caller supplies the complete result flag. A false native hurt return
  * is not a substitute for the source armor/held/knockback outcome. Lethal
  * negative outcomes still update this of state; tm retaliation gates death.
  */
 public void afterDamage(DamageSource source,boolean negativelyAffected){
  if(negativelyAffected&&source.getEntity() instanceof LivingEntity responsible){actor.setLastHurtByMob(responsible);harming(responsible);considerFleeing();}
 }
 public void save(CompoundTag tag){
  tag.putString("last_harming_entity_unique_id",harming==null?"":harming.getUUID().toString());tag.putInt("last_harming_entity_memory_countdown",memory);
  tag.putBoolean("has_decided_to_flee",flags.hasDecidedToFlee());tag.putBoolean("fleeing",flags.fleeing());
 }
 public void load(CompoundTag tag){
  // Source does not clear an already-resolved harming entity when loading into
  // the same instance. New actor construction naturally starts it as null.
  if(tag.contains("last_harming_entity_unique_id")){pendingUniqueId=tag.getString("last_harming_entity_unique_id");if(pendingUniqueId.isEmpty())pendingUniqueId=null;}
  memory=tag.getInt("last_harming_entity_memory_countdown");flags.hasDecidedToFlee(tag.getBoolean("has_decided_to_flee"));flags.fleeing(tag.getBoolean("fleeing"));
 }
}

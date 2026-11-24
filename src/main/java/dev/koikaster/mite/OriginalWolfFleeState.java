package dev.koikaster.mite;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
/** of harming memory/wounded decision and og spooked state, carried by each actual wolf. */
public final class OriginalWolfFleeState {
 private final Wolf wolf;private Entity harming;private UUID pending;public int memory;public boolean decided,fleeing;public long spookedUntil;
 public OriginalWolfFleeState(Wolf wolf){this.wolf=wolf;}
 public Entity harming(){return harming;}
 public void harming(Entity entity){harming=entity;memory=entity==null?0:900+wolf.getRandom().nextInt(200);if(entity==null)pending=null;}
 public boolean wounded(){return wolf.getHealth()/wolf.getMaxHealth()<.2f;}
 public boolean consider(){decided=harming!=null&&wounded()&&wolf.distanceTo(harming)<=32&&wolf.getRandom().nextInt(2)==0;return decided;}
 public boolean stopConsidering(){if(harming==null||!wounded()){decided=fleeing=false;return true;}if(wolf.distanceTo(harming)>40){fleeing=false;return true;}return false;}
 public boolean spooked(){if(spookedUntil>wolf.level().getGameTime())return true;spookedUntil=0;return false;}
 public void tick(){if(memory>0)--memory;if(harming!=null&&(harming.isRemoved()||memory==0))harming(null);if(memory==0)pending=null;else if(harming==null&&pending!=null&&wolf.tickCount%10==0)for(var entity:wolf.level().getEntities(wolf,wolf.getBoundingBox().inflate(32,8,32)))if(entity.getUUID().equals(pending)){harming=entity;pending=null;break;}if(fleeing&&wolf.tickCount%100==0)stopConsidering();}
 public void save(CompoundTag tag){tag.putString("last_harming_entity_unique_id",harming==null?"":harming.getUUID().toString());tag.putInt("last_harming_entity_memory_countdown",memory);tag.putBoolean("has_decided_to_flee",decided);tag.putBoolean("fleeing",fleeing);tag.putLong("spooked_until",spookedUntil);}
 public void load(CompoundTag tag){harming=null;String id=tag.getString("last_harming_entity_unique_id");try{pending=id.isEmpty()?null:UUID.fromString(id);}catch(IllegalArgumentException ignored){pending=null;}memory=tag.getInt("last_harming_entity_memory_countdown");decided=tag.getBoolean("has_decided_to_flee");fleeing=tag.getBoolean("fleeing");spookedUntil=tag.getLong("spooked_until");}
}

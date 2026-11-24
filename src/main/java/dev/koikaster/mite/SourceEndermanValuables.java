package dev.koikaster.mite;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
/** tg's pearl/eye identity-only storage with the existing og pickup consumer. */
public final class SourceEndermanValuables implements OriginalPickupState.Policy {
 private static final String KEY="R195EndermanContainedItems";
 private final SourceEnderman actor;private final List<Item> contained=new ArrayList<>();
 public final OriginalPickupState pickup;
 SourceEndermanValuables(SourceEnderman actor){this.actor=actor;pickup=new OriginalPickupState(actor,this);}
 static boolean ender(ItemStack stack){return stack.is(Items.ENDER_PEARL)||stack.is(Items.ENDER_EYE);}
 static boolean valuable(ItemStack stack){return !stack.isEmpty()&&stack.getDamageValue()==0&&ender(stack);}
 public boolean hasItems(){return !contained.isEmpty();}
 public int size(){return contained.size();}
 public boolean allowed(ItemEntity item){if(item.hasPickUpDelay()||item.isOnFire())return false;return ray(item);}
 boolean ray(ItemEntity item){for(float f:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,item.getBoundingBox().getCenter(),actor.position().add(0,actor.getBbHeight()*f,0)))return true;return false;}
 public boolean willEat(ItemStack stack){return false;}public void ate(ItemStack stack){}
 public boolean willRepair(ItemStack stack){return false;}public void repaired(ItemStack stack){}
 public boolean willTakeAsValuable(ItemStack stack){return valuable(stack);}
 public void contain(Item item){contained.add(item);}
 public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 void tick(){if(!actor.sourceDecoy())pickup.tick();}
 ItemEntity first(float range){for(var item:actor.level().getEntitiesOfClass(ItemEntity.class,actor.getBoundingBox().inflate(range,range*.25f,range)))if(!item.isRemoved()&&!item.isInWaterRainOrBubble()&&!item.isOnFire()&&valuable(item.getItem()))return item;return null;}
 ItemEntity nearest(){ItemEntity best=null;double shortest=0;for(var item:actor.level().getEntitiesOfClass(ItemEntity.class,actor.getBoundingBox().inflate(16,8,16))){if(item.isRemoved()||item.isInWaterRainOrBubble()||item.isOnFire()||!valuable(item.getItem())||!SourceEndermanGeometry.passable(actor,item.blockPosition().above())||!SourceEndermanGeometry.passable(actor,item.blockPosition().above(2)))continue;double d=actor.distanceToSqr(item);if(best==null||d<shortest){best=item;shortest=d;}}return best;}
 void drop(){for(var item:contained)actor.spawnAtLocation(item);}
 void save(CompoundTag tag){var list=new ListTag();for(var item:contained)list.add(StringTag.valueOf(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString()));tag.put(KEY,list);}
 void load(CompoundTag tag){contained.clear();var list=tag.getList(KEY,Tag.TAG_STRING);/* of.obtainContainedItemsFromString reads from the end. */for(int i=list.size()-1;i>=0;i--){var id=ResourceLocation.tryParse(list.getString(i));if(id==null)continue;var item=ForgeRegistries.ITEMS.getValue(id);if(item!=null&&item!=Items.AIR)contained.add(item);}}
}

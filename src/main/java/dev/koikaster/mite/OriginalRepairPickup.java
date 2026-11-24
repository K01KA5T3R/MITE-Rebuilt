package dev.koikaster.mite;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** R195 tr/sb -> og repair owner. Source FX callback must send the dedicated repair event. Connected to native owners in0.29. */
public final class OriginalRepairPickup implements OriginalMoveToRepairGoal.Owner,OriginalPickupState.Policy {
 private final PathfinderMob mob;private final Consumer<Mob> repairFx;private boolean installed;
 public final OriginalPickupState state;
 public OriginalRepairPickup(PathfinderMob mob,Consumer<Mob> repairFx){if(!handles(mob))throw new IllegalArgumentException("Not an R195 bone/snow repair owner");this.mob=mob;this.repairFx=Objects.requireNonNull(repairFx);state=new OriginalPickupState(mob,this);}
 public static boolean handles(Mob mob){return mob instanceof SourceLongdead||mob instanceof SourceSkeleton||mob.getType()==EntityType.SKELETON||mob.getType()==EntityType.WITHER_SKELETON||mob.getType()==EntityType.SNOW_GOLEM;}
 public int pickupCooldown(){return state.cooldown;}
 public boolean isRepairItem(ItemStack stack){return stack.is(mob.getType()==EntityType.SNOW_GOLEM?Items.SNOWBALL:Items.BONE);}
 public boolean willRepair(ItemStack stack){return mob.getHealth()/mob.getMaxHealth()<1&&isRepairItem(stack);}
 public void repaired(ItemStack stack){int before=(int)mob.getHealth();mob.heal(mob.getMaxHealth()*.5f);if((int)mob.getHealth()>before&&!(mob instanceof SourceSnowGolem))repairFx.accept(mob);}
 public boolean allowed(ItemEntity item){if(!isRepairItem(item.getItem())||item.hasPickUpDelay()||item.isOnFire()&&!mob.fireImmune())return false;var origin=item.getBoundingBox().getCenter();for(float fraction:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,origin,mob.position().add(0,mob.getBbHeight()*fraction,0)))return true;return false;}
 public boolean willEat(ItemStack stack){return false;}public void ate(ItemStack stack){throw new IllegalStateException("Repair-only owner");}
 public boolean willTakeAsValuable(ItemStack stack){return false;}public void contain(Item item){throw new IllegalStateException("Repair-only owner");}
 public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 /** Call after the owner's super.aiStep(), once per server tick; cooldown must not be persisted. */
 public void tick(){state.tick();}
 /** Add to complete native goals; no skeleton ranged/melee/sun or snow ranged tasks are removed here. */
 public void install(){if(installed)return;((SourceMobAi)mob).mite$goals().addGoal(mob.getType()==EntityType.SNOW_GOLEM?2:4,new OriginalMoveToRepairGoal(mob,this,1,true));installed=true;}
}

package dev.koikaster.mite;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** sf ItemMeat + rp canEat/onFoodEaten + og pickup, owned by the real Wolf bridge. */
public final class OriginalWolfFood implements OriginalMoveToFoodGoal.Owner,OriginalPickupState.Policy {
 private final Wolf wolf;private final Consumer<Mob> healFx;private boolean installed;
 public final OriginalPickupState state;
 public OriginalWolfFood(Wolf wolf,Consumer<Mob> healFx){if(Mobs.name(wolf).equals("hellhound"))throw new IllegalArgumentException("Source hellhound cannot eat");this.wolf=wolf;this.healFx=Objects.requireNonNull(healFx);state=new OriginalPickupState(wolf,this);}
 public int pickupCooldown(){return state.cooldown;}
 public boolean canEat(){return !wolf.isInLove()&&wolf.getAge()==0&&!wolf.isBaby()&&!wolf.isRemoved()&&wolf.getHealth()>0;}
 public boolean isFood(ItemStack stack){return OriginalMeatItems.includes(stack);}
 public boolean willEat(ItemStack stack){return isFood(stack)&&canEat();}
 public void ate(ItemStack stack){
  // rp.bX runs BEFORE og's healing: only already-full, non-rotten meat starts source love.
  // This applies to dropped-item consumption. Native manual feeding/breeding is deliberately untouched.
  if(!stack.is(Items.ROTTEN_FLESH)&&wolf.getHealth()>=wolf.getMaxHealth())wolf.setInLove(null);
  int before=(int)wolf.getHealth();wolf.heal(wolf.getMaxHealth()*.5f);if((int)wolf.getHealth()>before)healFx.accept(wolf);
 }
 public boolean allowed(ItemEntity item){if(!isFood(item.getItem())||item.hasPickUpDelay()||item.isOnFire())return false;var center=item.getBoundingBox().getCenter();for(float fraction:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,center,wolf.position().add(0,wolf.getBbHeight()*fraction,0)))return true;return false;}
 public boolean willRepair(ItemStack stack){return false;}public void repaired(ItemStack stack){throw new IllegalStateException("Wolf has no repair material");}
 public boolean willTakeAsValuable(ItemStack stack){return false;}public void contain(Item item){throw new IllegalStateException("Wolf has no valuable-item owner");}
 public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 /** Installed once at the source constructor position before later parent goals. */
 public void install(){if(installed)return;((SourceMobAi)wolf).mite$goals().addGoal(1,new OriginalMoveToFoodGoal(wolf,this,1,true));installed=true;}
 /** Called once after native Mob.aiStep on server; the cooldown remains transient. */
 public void tick(){state.tick();}
}

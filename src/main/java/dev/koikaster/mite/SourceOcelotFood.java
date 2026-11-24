package dev.koikaster.mite;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** rx fish + rp/og dropped food. Modern hand feeding/trust/breeding is retained. */
final class SourceOcelotFood implements OriginalMoveToFoodGoal.Owner {
 final Ocelot cat;int cooldown;
 SourceOcelotFood(Ocelot cat){this.cat=cat;}
 public int pickupCooldown(){return cooldown;}
 public boolean canEat(){return !cat.isInLove()&&cat.getAge()==0&&!cat.isBaby()&&!cat.isRemoved()&&cat.getHealth()>0;}
 public boolean isFood(ItemStack stack){return stack.is(Items.COD)||stack.is(Items.SALMON);}
 public boolean willEat(ItemStack stack){return isFood(stack)&&canEat();}
 private boolean allowed(ItemEntity item){if(!isFood(item.getItem())||item.hasPickUpDelay()||item.isOnFire())return false;var center=item.getBoundingBox().getCenter();for(float fraction:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,center,cat.position().add(0,cat.getBbHeight()*fraction,0)))return true;return false;}
 void tick(){
  if(cat.level().isClientSide||cat.isRemoved()||cat.getHealth()<=0)return;
  if(cooldown>0){--cooldown;return;}
  if(SourceOcelotOwners.trusting(cat)&&cat.getHealth()/cat.getMaxHealth()>.5f)return;
  for(var item:cat.level().getEntitiesOfClass(ItemEntity.class,cat.getBoundingBox().inflate(1,.25,1))){
   if(item.isRemoved()||!allowed(item)||!willEat(item.getItem()))continue;
   if(cat.getHealth()>=cat.getMaxHealth()){cat.setTarget(null);cat.setInLove(null);}
   int before=(int)cat.getHealth();cat.heal(cat.getMaxHealth()*.5f);if((int)cat.getHealth()>before)Network.wolfHealFx(cat);
   cooldown=400;var remaining=item.getItem().copy();remaining.shrink(1);cat.playSound(SoundEvents.ITEM_PICKUP,.2f,((cat.getRandom().nextFloat()-cat.getRandom().nextFloat())*.7f+1)*2);
   if(remaining.isEmpty())item.discard();else{item.setItem(remaining);((SourceItemAge)item).mite$itemAge(-18000);}
   OriginalDespawn.refresh(cat,-2400);break;
  }
 }
 /** Source order keeps the trusted >half-health gate AFTER its1/40 draw. */
 static final class Move extends OriginalItemMovementGoal {
  private final SourceOcelotFood food;private java.util.List<ItemEntity> items;private ItemEntity selected;
  Move(Ocelot cat,SourceOcelotFood food){super(cat,1,true);this.food=food;}
  public boolean canUse(){if(food.cooldown>0||mob.getRandom().nextInt(40)>0||!food.canEat())return false;if(SourceOcelotOwners.trusting(food.cat)&&food.cat.getHealth()/food.cat.getMaxHealth()>.5f)return false;items=mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(16,4,16),item->food.isFood(item.getItem()));return !items.isEmpty();}
  protected net.minecraft.world.level.pathfinder.Path movementPath(){net.minecraft.world.level.pathfinder.Path best=null;int shortest=Integer.MAX_VALUE;for(var item:items){if(!food.willEat(item.getItem()))continue;var at=item.blockPosition();var path=SourceArachnidPathBudget.block(mob,at,16);if(path==null||path.getNodeCount()>=shortest)continue;var end=path.getEndNode();if(end==null||end.x!=at.getX()||end.y!=at.getY()||end.z!=at.getZ())continue;shortest=path.getNodeCount();selected=item;best=path;}if(best!=null)((SourceArachnidPathBudget)mob.getNavigation()).mite$sourceCommitPath(best);return best;}
  public boolean canContinueToUse(){return food.cooldown<=0&&selected!=null&&!selected.isRemoved()&&super.canContinueToUse();}
  public void stop(){super.stop();items=null;selected=null;}
 }
}

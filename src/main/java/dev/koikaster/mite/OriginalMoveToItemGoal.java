package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

/** EntityAIMoveToItem source enumeration, shortest complete path, cleanup.
 * Deliberately no pickup-delay or raycast filter here: source applies those at actual pickup.
 * Concrete food consumption is connected; other repair/valuable owners require their species port.
 */
public abstract class OriginalMoveToItemGoal extends OriginalItemMovementGoal {
 public int maxPathLength=16;private List<ItemEntity> items;private ItemEntity selected;
 protected OriginalMoveToItemGoal(PathfinderMob mob,float speed,boolean swim){super(mob,speed,swim);}
 protected abstract boolean interestedIn(ItemStack stack);
 protected abstract boolean willPickUp(ItemStack stack);
 public boolean canUse(){items=mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(maxPathLength,maxPathLength/4,maxPathLength),item->interestedIn(item.getItem()));return !items.isEmpty();}
 protected Path movementPath(){Path best=null;int shortest=Integer.MAX_VALUE;for(var item:items){if(!willPickUp(item.getItem()))continue;var position=item.blockPosition();var path=mob.getNavigation().createPath(position,0,maxPathLength);if(path==null||path.getNodeCount()>=shortest)continue;var end=path.getEndNode();if(end.x!=position.getX()||end.y!=position.getY()||end.z!=position.getZ())continue;shortest=path.getNodeCount();selected=item;best=path;}return best;}
 public boolean canContinueToUse(){return selected!=null&&!selected.isRemoved()&&super.canContinueToUse();}
 public void stop(){super.stop();items=null;selected=null;}
}

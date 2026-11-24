package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

/** qy: six-block vertical scan, source digger visibility exemption, mounts and40tick retarget chance. */
public class OriginalNearestTargetGoal extends OriginalTargetGoal {
 private final Class<? extends LivingEntity> targetClass;private final int chance;private LivingEntity selected;
 public OriginalNearestTargetGoal(PathfinderMob mob,Class<? extends LivingEntity> type,int chance,boolean sight){super(mob,sight,false);targetClass=type;this.chance=chance;}
 private LivingEntity mount(LivingEntity target){return target!=null&&target.getVehicle() instanceof LivingEntity vehicle&&!vehicle.isInvulnerable()?vehicle:target;}
 private LivingEntity nearest(){double range=range(mob);float vertical=6;if((mob instanceof SourceZombieOwner||Mobs.name(mob).equals("revenant"))&&targetClass==Animal.class){range/=2;vertical/=2;}List<LivingEntity> list=new ArrayList<>();for(var entity:mob.level().getEntitiesOfClass(targetClass,mob.getBoundingBox().inflate(range,vertical,range)))if(KingSlimeRewards.canDetect(mob,entity,range,vertical)&&suitable(entity,false))list.add(entity);list.sort(Comparator.comparingDouble(mob::distanceToSqr));
  if(list.isEmpty()&&targetClass==Player.class&&OriginalDigging.watcher(mob)&&((OriginalDigging.Host)mob).originalDigging().enabled()&&mob.level().canSeeSky(mob.blockPosition())){for(var player:mob.level().players()){if(player.isSpectator()||player.getAbilities().invulnerable)continue;double dx=player.getX()-mob.getX(),dy=player.getY()-mob.getY(),dz=player.getZ()-mob.getZ();if(dy>=vertical&&dy<=14&&dx*dx+dz*dz<16)list.add(player);}list.sort(Comparator.comparingDouble(mob::distanceToSqr));}
  for(var target:list){if(target instanceof Player player&&player.isSpectator()||targetClass==Animal.class&&Mobs.name(target).equals("hellhound"))continue;if(target instanceof net.minecraft.world.entity.animal.Wolf&&(mob instanceof SourceZombieOwner||Mobs.name(mob).equals("revenant"))&&mob.distanceTo(target)>8)continue;return mount(target);}return null;
 }
 public boolean canUse(){if(mob instanceof Mobs.Undead actor){var forced=actor.consumeOriginalRetarget();if(forced!=null){if(!forced.isRemoved()){selected=forced;return true;}}}if(chance>0&&mob.getRandom().nextInt(chance)!=0)return false;selected=nearest();return selected!=null;}
 public void tick(){if(mob.getRandom().nextInt(40)==0){var next=nearest();if(next!=null&&next!=selected&&sees(next,false)){stop();selected=next;start();return;}}selected=mount(selected);}
 public void start(){selected=mount(selected);mob.setTarget(selected);super.start();}
}

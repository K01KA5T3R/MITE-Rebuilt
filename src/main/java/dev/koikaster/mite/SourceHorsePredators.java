package dev.koikaster.mite;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** abw.getPredatorsWithinAABBForEntity: relationship, not hunger or current target.
 * Only source families that can prey on rs are included. Spiders prey on rq,
 * Wight/other generic monsters inherit false. All rs forms were true animals.
 */
final class SourceHorsePredators {
 static boolean relation(Mob predator,AbstractHorse prey){
  if(predator instanceof Wolf wolf)return OriginalWolfPreyProfile.preysUpon(wolf,prey);
  var type=predator.getType();
  if(type==EntityType.ZOMBIE||type==EntityType.ZOMBIE_VILLAGER||type==EntityType.ZOMBIFIED_PIGLIN)return true;
  if(predator instanceof Mobs.Undead)return Set.of("ghoul","revenant").contains(Mobs.name(predator));
  if(predator instanceof SourcePredatoryBat bat)return bat.sourcePreysUpon(prey);
  if(predator instanceof Silverfish)return true;
  if(type==EntityType.SQUID){var level=prey.level();boolean blue=SourceSpecialSpawns.blueUp(level);return !blue;}
  return false;
 }
 static List<LivingEntity> query(AbstractHorse horse,AABB box,boolean shyPlayers){var result=new ArrayList<LivingEntity>();for(var e:horse.level().getEntities(horse,box))if(e instanceof Mob mob&&relation(mob,horse))result.add(mob);if(shyPlayers)result.addAll(horse.level().getEntitiesOfClass(Player.class,box,p->true));return result;}
 static double nearest(int x,int y,int z,List<? extends LivingEntity> list){double best=Double.MAX_VALUE;for(var e:list){float dx=(float)x-(float)e.getX(),dy=(float)y-(float)e.getY(),dz=(float)z-(float)e.getZ();double sq=dx*dx+dy*dy+dz*dz;if(sq<best)best=sq;}return best;}
 private SourceHorsePredators(){}
}

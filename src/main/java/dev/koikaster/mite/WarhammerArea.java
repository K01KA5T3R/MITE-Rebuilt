package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** X002 separate heavy-weapon extension. Ordinary R195 hit arithmetic stays in its owner. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class WarhammerArea {
 public static final int INTERVAL=20,MAX_EXTRA=4;public static final double RADIUS=2;public static final float FRACTION=.4f;
 private static final Map<Player,Long> LAST=new WeakHashMap<>();private static final ThreadLocal<Boolean> ACTIVE=new ThreadLocal<>();
 public static boolean hammer(ItemStack s){var p=ClassicToolWear.profile(s);return p!=null&&p.kind()==LegacyToolWear.Kind.WARHAMMER;}
 static long now(Player p){return Integer.toUnsignedLong(p.tickCount);}
 public static boolean cooling(Player p){var last=LAST.get(p);return last!=null&&((now(p)-last)&0xffffffffL)<INTERVAL;}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent e){if(!e.getEntity().level().isClientSide&&WorldContentMode.rebuilt(e.getEntity().level())&&!Boolean.TRUE.equals(ACTIVE.get())&&hammer(e.getEntity().getMainHandItem())&&cooling(e.getEntity()))e.setCanceled(true);}
 static boolean protectedTarget(Player p,LivingEntity v){return v instanceof Player||v instanceof ArmorStand||p.isAlliedTo(v)||v.isAlliedTo(p)||v instanceof TamableAnimal pet&&p.getUUID().equals(pet.getOwnerUUID())||v instanceof AbstractHorse horse&&p.getUUID().equals(horse.getOwnerUUID());}
 static void afterHit(Player p,LivingEntity primary){
  if(p.level().isClientSide||!WorldContentMode.rebuilt(p.level())||Boolean.TRUE.equals(ACTIVE.get())||!hammer(p.getMainHandItem()))return;
  LAST.put(p,now(p));var weapon=p.getMainHandItem();var nearby=p.level().getEntitiesOfClass(LivingEntity.class,primary.getBoundingBox().inflate(RADIUS,1.5,RADIUS),v->v!=p&&v!=primary&&v.isAlive()&&!protectedTarget(p,v)&&v.distanceToSqr(primary)<=RADIUS*RADIUS&&Math.abs(v.getY()-primary.getY())<=1.5);
  nearby.sort(Comparator.comparingDouble((LivingEntity v)->primary.distanceToSqr(v)).thenComparing(v->v.getUUID().toString()));int hits=0;
  ACTIVE.set(true);try{
   for(var v:nearby){if(hits>=MAX_EXTRA||p.getMainHandItem()!=weapon||weapon.isEmpty())break;
    if(!p.hasLineOfSight(v)||!ForgeHooks.onPlayerAttackTarget(p,v))continue;
    if(ClassicMeleeEnchantments.secondaryHit(p,v,FRACTION)){ClassicToolWear.hurtEnemy(weapon,v,p);hits++;}
   }
  }finally{ACTIVE.remove();}
  if(hits>0){var l=(ServerLevel)p.level();l.sendParticles(ParticleTypes.SWEEP_ATTACK,primary.getX(),primary.getY()+.2,primary.getZ(),1,0,0,0,0);l.playSound(null,primary.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.6f,.65f);}
 }
 private WarhammerArea(){}
}

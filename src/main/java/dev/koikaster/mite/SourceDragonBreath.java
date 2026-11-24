package dev.koikaster.mite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** V023 retained native strafe attack leaf: range64, five visible ticks,
 * angle<10, vanilla projectile/cloud/bottle interaction. Source flight owns
 * targeting and motion; no native phase server tick runs alongside it. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceDragonBreath {
 private int charge;private SourceDragonRules.Target firedAt;
 public void tick(SourceEnderDragon actor){
  if(!WorldContentMode.rebuiltBosses(actor.level()))return;
  if(actor.sourceBrain().state.target==firedAt)return;
  if(!(actor.sourceBrain().state.target instanceof SourceDragonBrain.Node node)||!(node.value() instanceof Player target)||target.getAbilities().invulnerable){if(charge>0)--charge;return;}
  if(target.distanceToSqr(actor)<4096&&actor.hasLineOfSight(target)){
   ++charge;var to=new Vec3(target.getX()-actor.getX(),0,target.getZ()-actor.getZ()).normalize();float yaw=actor.getYRot()*((float)Math.PI/180);var facing=new Vec3(SourceDragonTrig.sin(yaw),0,-SourceDragonTrig.cos(yaw)).normalize();float dot=(float)facing.dot(to),angle=(float)(Math.acos(dot)*57.2957763671875);angle+=.5f;
   if(charge>=5&&angle>=0&&angle<10){var view=actor.getViewVector(1);var head=actor.sourceParts()[0];double x=head.getX()-view.x,y=head.getY(.5)+.5,z=head.getZ()-view.z;var projectile=new SourceDragonFireball(actor.level(),actor,target.getX()-x,target.getY(.5)-y,target.getZ()-z);projectile.moveTo(x,y,z,0,0);if(!actor.isSilent())actor.level().levelEvent(null,1017,actor.blockPosition(),0);actor.level().addFreshEntity(projectile);charge=0;firedAt=actor.sourceBrain().state.target;}
  }else if(charge>0)--charge;
 }

 @SubscribeEvent
 public static void rejectClassicDragonHazard(EntityJoinLevelEvent event) {
  if (!(event.getLevel() instanceof ServerLevel end) || end.dimension() != Level.END) return;
  var entity = event.getEntity();
  if (!(entity instanceof DragonFireball)
      && !(entity instanceof AreaEffectCloud cloud && cloud.getParticle() == ParticleTypes.DRAGON_BREATH)) return;
  if (WorldContentMode.rebuiltBosses(end)) return;

  var fight = end.getDragonFight();
  var dragon = fight == null ? null : fight.getDragonUUID();
  if (dragon == null) return;
  // The native owner UUID survives reload even when the dragon is dead or has not loaded yet.
  var saved = entity.saveWithoutId(new CompoundTag());
  if (saved.hasUUID("Owner") && dragon.equals(saved.getUUID("Owner"))) event.setCanceled(true);
 }
}

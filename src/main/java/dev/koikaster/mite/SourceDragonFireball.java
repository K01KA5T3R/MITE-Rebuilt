package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;

/** The retained vanilla breath projectile ignores its owner's wider source
 * multipart hitboxes; all native flight/cloud/duration/effect code is kept. */
public final class SourceDragonFireball extends DragonFireball {
 public SourceDragonFireball(Level level,LivingEntity owner,double x,double y,double z){super(level,owner,x,y,z);}
 @Override protected boolean canHitEntity(Entity entity){return !(entity instanceof PartEntity<?> part&&part.getParent()==getOwner())&&super.canHitEntity(entity);}
}

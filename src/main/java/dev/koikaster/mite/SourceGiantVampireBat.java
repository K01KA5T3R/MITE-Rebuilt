package dev.koikaster.mite;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
public final class SourceGiantVampireBat extends SourceVampireBat {
 public SourceGiantVampireBat(EntityType<? extends Bat> type,Level level){super(type,level);xpReward=10;}
 @Override protected float sourceScale(){return 2;}
 @Override protected boolean sourceGiant(){return true;}
}

package dev.koikaster.mite;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
public class SourceVampireBat extends SourcePredatoryBat {
 public SourceVampireBat(EntityType<? extends Bat> type,Level level){super(type,level);xpReward=5;}
}

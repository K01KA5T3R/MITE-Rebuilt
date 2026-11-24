package dev.koikaster.mite;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
/** R195 EntityAncientBoneLord: equipment, attributes, XP and exact Longdead troop class. */
public final class SourceAncientBoneLord extends SourceBoneLord {
 public SourceAncientBoneLord(EntityType<? extends SourceAncientBoneLord> type,Level level){super(type,level);}
 @Override public boolean ancient(){return true;}
}

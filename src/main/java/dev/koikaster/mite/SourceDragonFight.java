package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;

/** Two guarded native kill leaves: source owner already made its portal/egg.
 * Bossbar, saved killed flags and the modern outer-island gateway stay native. */
public final class SourceDragonFight {
 public static void exit(EndDragonFight fight,boolean active,EnderDragon dragon){if(!(dragon instanceof SourceEnderDragon))((SourceDragonFightAccess)(Object)fight).mite$dragonExit(active);}
 public static boolean egg(ServerLevel level,BlockPos position,BlockState state,EnderDragon dragon){return !(dragon instanceof SourceEnderDragon)&&level.setBlockAndUpdate(position,state);}
 private SourceDragonFight(){}
}

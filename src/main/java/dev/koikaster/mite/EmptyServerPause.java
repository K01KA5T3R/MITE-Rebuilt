package dev.koikaster.mite;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
/** Pauses level simulation while the server loop, network and login handling remain live. */
public final class EmptyServerPause {
 public static boolean paused(MinecraftServer server){
  // Automated GameTest servers must tick without real players to run the test suite.
  return !(server instanceof net.minecraft.gametest.framework.GameTestServer)&&server.getPlayerCount()==0;
 }
 /** Descriptor deliberately uses Object so the bytecode hook consumes the original two stack values. */
 public static void tick(Object world,Object timeBudget){
  var level=(ServerLevel)world;
  if(!paused(level.getServer()))level.tick((BooleanSupplier)timeBudget);
 }
 private EmptyServerPause(){}
}

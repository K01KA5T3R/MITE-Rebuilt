package dev.koikaster.mite;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** aps metadata destination bits. UNSET admits old vanilla portal states without
 * inventing a saved destination; they are classified once on first use. */
public final class SourcePortalState {
 public enum Destination implements StringRepresentable {
  UNSET("unset"), OVERWORLD("overworld"), UNDERWORLD("underworld"), NETHER("nether"), CORE("core");
  private final String name;
  Destination(String name){this.name=name;}
  public String getSerializedName(){return name;}
 }
 // Bootstrap must depend only on the enum and native property API, not Content/Blocks.
 public static final EnumProperty<Destination> DESTINATION=EnumProperty.create("mite_destination",Destination.class);
 public static final BooleanProperty RUNEGATE=BooleanProperty.create("mite_runegate");
 public static void addProperty(Object raw){((StateDefinition.Builder<Block,BlockState>)raw).add(DESTINATION,RUNEGATE);}
 public static boolean allowIgnition(Object level){return supported(((Level)level).dimension());}
 public static Destination of(ResourceKey<Level> dimension){
  if(dimension==Level.OVERWORLD)return Destination.OVERWORLD;
  if(dimension==WorldEvents.UNDERWORLD)return Destination.UNDERWORLD;
  if(dimension==Level.NETHER)return Destination.NETHER;
  if(dimension==CoreProgression.WORLD)return Destination.CORE;
  throw new IllegalArgumentException("No ordinary portal route for "+dimension.location());
 }
 public static boolean supported(ResourceKey<Level> dimension){return dimension==Level.OVERWORLD||dimension==WorldEvents.UNDERWORLD||dimension==Level.NETHER||dimension==CoreProgression.WORLD;}
 public static ResourceKey<Level> dimension(Destination destination){return switch(destination){case OVERWORLD->Level.OVERWORLD;case UNDERWORLD->WorldEvents.UNDERWORLD;case NETHER->Level.NETHER;case CORE->CoreProgression.WORLD;default->throw new IllegalArgumentException("Unclassified portal");};}
 private SourcePortalState(){}
}

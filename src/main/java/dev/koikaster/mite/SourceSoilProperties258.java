package dev.koikaster.mite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
/** Source metadata and transport scheduling are deliberately separate. */
public final class SourceSoilProperties258 {
 public static final BooleanProperty UNDERMINED=BooleanProperty.create("mite_undermined");
 public static final BooleanProperty PENDING=BooleanProperty.create("mite_undermine_pending");
 public static int metadata(BlockState state){return state.hasProperty(UNDERMINED)&&state.getValue(UNDERMINED)?1:0;}
 public static BlockState carried(BlockState state){return state.hasProperty(PENDING)?state.setValue(PENDING,false):state;}
 private SourceSoilProperties258(){}
}

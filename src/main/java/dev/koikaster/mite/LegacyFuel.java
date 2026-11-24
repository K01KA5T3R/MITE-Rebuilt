package dev.koikaster.mite;
/** R195 yc/zh/wt/wr/xj/ItemBowl/wm branch values. No modern fuel fallback in this arithmetic. */
public final class LegacyFuel {
 public enum Kind {NONE,PAPER,MANURE,STICK_OR_ARROW,BOOK,WOOD_DOOR,BLAZE_ROD,WOOD_ITEM,OTHER_PAPER,WOOD_TOOL,HANDLED_TOOL,HANDLELESS_TOOL,COAL,CHARCOAL,LAVA_BUCKET,EMPTY_BOWL,FILLED_BOWL,BOAT,LOG,PLANK_OR_DOUBLE_SLAB_OR_BUTTON,SAPLING_OR_SLAB_OR_DEAD_BUSH,TORCH,WOOD_BLOCK,COAL_BLOCK}
 public static int duration(Kind kind){return switch(kind){case NONE,HANDLELESS_TOOL,FILLED_BOWL->0;case PAPER->25;case OTHER_PAPER->50;case MANURE,STICK_OR_ARROW,BOOK,HANDLED_TOOL->100;case WOOD_ITEM,WOOD_TOOL,EMPTY_BOWL,SAPLING_OR_SLAB_OR_DEAD_BUSH->200;case WOOD_DOOR,PLANK_OR_DOUBLE_SLAB_OR_BUTTON,WOOD_BLOCK->400;case TORCH->800;case BOAT->1200;case LOG,COAL,CHARCOAL->1600;case BLAZE_ROD->2400;case LAVA_BUCKET->3200;case COAL_BLOCK->16000;};}
 public static int heat(Kind kind){return switch(kind){case COAL,COAL_BLOCK->2;case LAVA_BUCKET->3;case BLAZE_ROD->4;default->duration(kind)>0?1:0;};}
 private LegacyFuel(){}
}

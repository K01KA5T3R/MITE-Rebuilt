package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
/** jv per-world zero-or-one source statistic values; native player clone/save owns persistence. */
public final class SourceAchievementState561 {
 public static final String KEY="R195Achievements";
 public static boolean has(CompoundTag playerData,int id){return playerData.getCompound(KEY).getByte(Integer.toString(SourceAchievementCatalog561.node(id).statId()))==1;}
 public static boolean unlock(CompoundTag playerData,int id){var node=SourceAchievementCatalog561.node(id);if(has(playerData,id))return false;if(!playerData.contains(KEY,10))playerData.put(KEY,new CompoundTag());playerData.getCompound(KEY).putByte(Integer.toString(node.statId()),(byte)1);return true;}
 public static int[] completed(CompoundTag playerData){return SourceAchievementCatalog561.nodes(true).stream().filter(n->has(playerData,n.id())).mapToInt(SourceAchievementCatalog561.Node::id).toArray();}
 private SourceAchievementState561(){}
}

package dev.koikaster.mite;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** xw.e(16); old0.1 stacks used64. A serialization epoch preserves old remaining wear. */
public final class LegacyFlintDurability749 {
 public static final int MAX=16;
 private static final ResourceLocation ITEM=new ResourceLocation("minecraft","flint_and_steel");
 private static final String EPOCH="MiteFlintWear749";
 public static boolean handles(ItemStack stack){return !stack.isEmpty()&&ITEM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));}
 public static int maximum(int original,Object raw){return handles((ItemStack)raw)?MAX:original;}
 public static void fresh(Object raw){var stack=(ItemStack)raw;if(handles(stack)&&stack.getOrCreateTag().getInt(EPOCH)<1)stack.getOrCreateTag().putByte(EPOCH,(byte)1);}
 public static Object loaded(Object raw,Object saved){
  var stack=(ItemStack)raw;var tag=(CompoundTag)saved;if(!handles(stack))return stack;
  // Read the serialized epoch, not a marker a delegating fresh constructor may create.
  int epoch=tag.getCompound("tag").getInt(EPOCH),oldDamage=Math.max(0,tag.getCompound("tag").getInt("Damage"));
  // Native NBT construction can retain the input tag object. Never alter a journal/snapshot input.
  stack.setTag(stack.hasTag()?stack.getTag().copy():new CompoundTag());
  if(epoch<1)stack.setDamageValue((int)Math.min(MAX-1,(long)oldDamage*MAX/64));
  fresh(stack);return stack;
 }
 public static Object saved(Object raw,Object output){var stack=(ItemStack)raw;var tag=(CompoundTag)output;if(handles(stack)){var itemTag=tag.contains("tag",10)?tag.getCompound("tag"):new CompoundTag();if(itemTag.getInt(EPOCH)<1)itemTag.putByte(EPOCH,(byte)1);tag.put("tag",itemTag);}return tag;}
 /** Preserve strict recovery matching for old tokenless graves; unrelated/unknown entries stay exact. */
 public static ListTag recoveryPayload(ListTag original){var result=original.copy();for(var value:result){var entry=(CompoundTag)value;if(!ITEM.toString().equals(entry.getString("id"))||entry.getByte("Count")<=0)continue;var tag=entry.contains("tag",10)?entry.getCompound("tag"):new CompoundTag();if(tag.getInt(EPOCH)<1){int oldDamage=Math.max(0,tag.getInt("Damage"));tag.putInt("Damage",(int)Math.min(MAX-1,(long)oldDamage*MAX/64));tag.putByte(EPOCH,(byte)1);entry.put("tag",tag);}}return result;}
 private LegacyFlintDurability749(){}
}

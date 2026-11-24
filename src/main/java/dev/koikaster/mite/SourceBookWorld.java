package dev.koikaster.mite;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** WorldInfoShared's bookcase achievement and unique reference signatures, shared by dimensions. */
final class SourceBookWorld extends SavedData {
 boolean bookcase;final Set<Integer> uniques=new LinkedHashSet<>();
 static SourceBookWorld get(ServerLevel level){var storage=level.getServer().overworld().getDataStorage();synchronized(storage){return storage.computeIfAbsent(SourceBookWorld::load,SourceBookWorld::new,"mite_reforged_referenced_books");}}
 static SourceBookWorld load(CompoundTag tag){var data=new SourceBookWorld();data.bookcase=tag.getBoolean("bookcase");for(int i:tag.getIntArray("uniques"))if(i>0)data.uniques.add(i);return data;}
 @Override public synchronized CompoundTag save(CompoundTag tag){tag.putBoolean("bookcase",bookcase);tag.putIntArray("uniques",new ArrayList<>(uniques));return tag;}
 synchronized boolean contains(int index){return uniques.contains(index);}
 synchronized void add(int index){if(uniques.add(index))setDirty();}
 synchronized void release(int index){if(uniques.remove(index))setDirty();}
}

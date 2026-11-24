package dev.koikaster.mite;
/** R195 nn spawn_x/y/z, independent of current position and retained when loading chunk entities. */
public interface SourceSpawnOrigin {
 int mite$originX();int mite$originY();int mite$originZ();void mite$setOrigin(int x,int y,int z);
}

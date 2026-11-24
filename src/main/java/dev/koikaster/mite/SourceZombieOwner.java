package dev.koikaster.mite;
/** One source tw owner exposed through the two existing native identities. */
public interface SourceZombieOwner extends OriginalDigging.Host {
 SourceZombieBody sourceZombie();
 boolean sourceVillager();void sourceVillager(boolean value);
 int sourceProfession();void sourceProfession(int value);
 boolean sourceConverting();void sourceConverting(boolean value);
 float sourceDropChance(int slot);
 @Override default OriginalDigging originalDigging(){return sourceZombie().digging;}
}

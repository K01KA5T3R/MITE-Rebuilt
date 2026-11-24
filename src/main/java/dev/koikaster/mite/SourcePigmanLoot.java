package dev.koikaster.mite;

/** tn.dropFewItems and its four-slot equipment choice. Quality remains disabled
 * by the human specification; randomizeForMob needs its own native adapter. */
public final class SourcePigmanLoot {
 public enum Item {ROTTEN_FLESH,GOLD_NUGGET,GOLD_INGOT,GOLD_SWORD,GOLD_BATTLEAXE,GOLD_PICKAXE}
 public interface Host {float nextFloat();int nextInt(int bound);int rareDropBound();void drop(Item item);}
 private static final Item[] WEAPONS={Item.GOLD_SWORD,Item.GOLD_SWORD,Item.GOLD_BATTLEAXE,Item.GOLD_PICKAXE};
 public static void drop(Host host,boolean recent,int looting,boolean massiveFall){
  float roll=host.nextFloat(),threshold=recent?.5f:.25f;if(roll<threshold)host.drop(Item.ROTTEN_FLESH);
  int count=host.nextInt(2+looting);for(int i=0;i<count;i++)host.drop(Item.GOLD_NUGGET);
  if(recent&&!massiveFall&&host.nextInt(host.rareDropBound())<5+looting*2)host.drop(Item.GOLD_INGOT);
 }
 public static Item weapon(Host host){return WEAPONS[host.nextInt(WEAPONS.length)];}
 private SourcePigmanLoot(){}
}

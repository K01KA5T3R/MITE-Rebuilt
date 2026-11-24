package dev.koikaster.mite;

/** tw.dropFewItems. N018's mineral-origin filter remains a native drop
 * consumer; source rare-pool selection must not silently become an ingot. */
public final class SourceZombieLoot {
 public enum Item {ROTTEN_FLESH,COPPER_NUGGET,SILVER_NUGGET,GOLD_NUGGET,IRON_NUGGET,WHEAT_SEEDS,PUMPKIN_SEEDS,MELON_SEEDS,CARROT,POTATO,ONION}
 public interface Host {boolean villager();boolean revenant();float nextFloat();int nextInt(int bound);void drop(Item item);}
 private static final Item[] NORMAL={Item.COPPER_NUGGET,Item.SILVER_NUGGET,Item.GOLD_NUGGET,Item.IRON_NUGGET};
 private static final Item[] VILLAGER={Item.WHEAT_SEEDS,Item.PUMPKIN_SEEDS,Item.MELON_SEEDS,Item.CARROT,Item.POTATO,Item.ONION};
 public static void drop(Host h,boolean recent,int looting,boolean massiveFall){
  if(h.nextFloat()<(recent?.5f:.25f))h.drop(Item.ROTTEN_FLESH);
  if(recent&&!massiveFall&&h.nextInt(h.villager()||h.revenant()?50:200)<5+looting*2){
   Item[] pool=h.villager()?VILLAGER:NORMAL;h.drop(pool[h.nextInt(pool.length)]);
  }
 }
 private SourceZombieLoot(){}
}

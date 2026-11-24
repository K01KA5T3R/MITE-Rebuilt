package dev.koikaster.mite;

/** tv.dropFewItems, preserving the18-entry weighted table and potion draws.
 * The native owner maps these resolved yc/aqz identities to registry items.
 */
public final class SourceWitchLoot {
 public enum Item {GLOWSTONE_DUST,SUGAR,REDSTONE,SPIDER_EYE,GLASS_BOTTLE,GUNPOWDER,STICK,FLINT_KNIFE,IRON_NUGGET,WHEAT_SEEDS,PUMPKIN_SEEDS,CARROT,POTATO,ONION,DANDELION,ALLIUM,POTION}
 public interface Host {int nextInt(int bound);void drop(Item item,int subtype);}
 private static final Item[] TABLE={Item.GLOWSTONE_DUST,Item.SUGAR,Item.REDSTONE,Item.SPIDER_EYE,Item.GLASS_BOTTLE,Item.GUNPOWDER,Item.STICK,Item.STICK,Item.FLINT_KNIFE,Item.IRON_NUGGET,Item.WHEAT_SEEDS,Item.PUMPKIN_SEEDS,Item.CARROT,Item.POTATO,Item.ONION,Item.DANDELION,Item.ALLIUM,Item.POTION};
 public static void drop(Host host,int looting){
  int count=host.nextInt(5+looting)+1;
  for(int i=0;i<count;i++){
   var item=TABLE[host.nextInt(TABLE.length)];
   if(item==Item.ALLIUM){host.drop(item,2);continue;}
   if(item==Item.POTION){int subtype=switch(host.nextInt(6)){case 0->8227;case 1->8261;case 2->16388;case 3->16424;case 4->16426;case 5->16460;default->throw new IllegalStateException("Source RNG outside bound6");};host.drop(item,subtype);continue;}
   host.drop(item,0);
  }
 }
 private SourceWitchLoot(){}
}

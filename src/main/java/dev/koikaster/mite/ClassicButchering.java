package dev.koikaster.mite;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** rr/ry/rz/rs/EntityArachnid;48D keeps modern feeding/breeding, without source wellness gates. */
public final class ClassicButchering {
 public static void apply(ObjectArrayList<ItemStack> loot,LootContext context){
  if(!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity actor)||!ClassicDefense.scoped(actor))return;
  var damage=context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);int butchering=damage!=null&&damage.getEntity() instanceof LivingEntity responsible?EnchantmentEffects.level("butchering",responsible.getMainHandItem()):0;var random=context.getRandom();boolean burning=actor.isOnFire();
  if(EndLifeContent.butcher(actor,loot,random,burning,butchering))return;
  if(actor instanceof Cow cow){remove(loot,Items.LEATHER,Items.BEEF,Items.COOKED_BEEF);if(cow.isBaby())return;add(loot,Items.LEATHER,random.nextInt(3)+1);int quantity=random.nextInt(3)+1+random.nextInt(1+butchering);if(quantity==1&&random.nextInt(2)==0)quantity++;add(loot,burning?Items.COOKED_BEEF:Items.BEEF,quantity);}
  else if(actor instanceof Pig pig){remove(loot,Items.PORKCHOP,Items.COOKED_PORKCHOP);if(!pig.isBaby())add(loot,burning?Items.COOKED_PORKCHOP:Items.PORKCHOP,random.nextInt(3)+1+random.nextInt(1+butchering));}
  else if(actor instanceof Sheep sheep){remove(loot,Items.MUTTON,Items.COOKED_MUTTON,Items.LEATHER);loot.removeIf(stack->stack.is(ItemTags.WOOL));if(sheep.isBaby())return;if(!sheep.isSheared()&&!burning&&random.nextInt(2)==0)add(loot,net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("minecraft",sheep.getColor().getName()+"_wool")),1);int quantity=Math.max(1,random.nextInt(2)+random.nextInt(1+butchering));add(loot,burning?Items.COOKED_MUTTON:Items.MUTTON,quantity);if(random.nextInt(2)==0)add(loot,Items.LEATHER,1);}
  else if(actor instanceof AbstractHorse horse&&(horse instanceof Horse||horse instanceof Donkey||horse instanceof Mule||horse instanceof SkeletonHorse||horse instanceof ZombieHorse)){remove(loot,Items.LEATHER,Items.BEEF,Items.COOKED_BEEF,Items.BONE,Items.ROTTEN_FLESH);if(horse.isBaby())return;int quantity=random.nextInt(3)+1;for(int i=0;i<quantity;i++){boolean none=random.nextInt(4)==0;add(loot,horse instanceof SkeletonHorse?Items.BONE:horse instanceof ZombieHorse?none?Items.AIR:Items.ROTTEN_FLESH:Items.LEATHER,1);}if(horse instanceof SkeletonHorse||horse instanceof ZombieHorse)return;quantity=1+random.nextInt(1+butchering);if(horse instanceof Horse)quantity+=random.nextInt(2);add(loot,burning?Items.COOKED_BEEF:Items.BEEF,quantity);}
  else if(actor instanceof Spider){remove(loot,Items.SPIDER_EYE);if(context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)&&(random.nextInt(3)==0||random.nextInt(1+butchering)>0))add(loot,Items.SPIDER_EYE,1);}
 }
 private static void remove(ObjectArrayList<ItemStack> loot,Item...items){var removed=java.util.Set.of(items);loot.removeIf(stack->removed.contains(stack.getItem()));}
 private static void add(ObjectArrayList<ItemStack> loot,Item item,int count){if(item!=Items.AIR&&count>0)loot.add(new ItemStack(item,count));}
 private ClassicButchering(){}
}

package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.block.entity.*;

/** Experience belongs to completed furnace work, independent of output extraction. */
public final class FurnaceExperience {
 private static final String KEY="MiteStoredSmeltingExperience",MIGRATED="MiteSmeltingExperienceMigrated";
 private static final String CARRIED="MiteCarriedSmeltingExperience";
 private static boolean managed(BlockEntity be){return be instanceof ThermalFurnace.Entity||ModernCooking.managed(be);}
 public static int perItem(ClassicFurnaces.Plan plan){
  int source=ClassicFurnaces.reward(plan.output());if(source>0)return source;
  float recipe=plan.xp();return Float.isFinite(recipe)&&recipe>0?(int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.ceil(recipe))):1;
 }
 public static int stored(BlockEntity be){
  if(!managed(be))return 0;var data=be.getPersistentData();
  if(be.getLevel()!=null&&!be.getLevel().isClientSide&&!data.getBoolean(MIGRATED)){
   data.putBoolean(MIGRATED,true);
   if(be instanceof AbstractFurnaceBlockEntity){var old=be.saveWithoutMetadata().getCompound("RecipesUsed");long extra=0;for(String key:old.getAllKeys()){
    var id=net.minecraft.resources.ResourceLocation.tryParse(key);if(id==null)continue;var recipe=be.getLevel().getRecipeManager().byKey(id).orElse(null);
    if(recipe instanceof AbstractCookingRecipe cooking){var out=cooking.getResultItem(be.getLevel().registryAccess());var plan=new ClassicFurnaces.Plan(out,1,1,cooking.getExperience());extra=Math.min(Integer.MAX_VALUE,extra+(long)Math.max(0,old.getInt(key))*perItem(plan)*out.getCount());}
    else if(id.getNamespace().equals("minecraft")){
     // The base mod replaces some vanilla cooking recipe IDs. Their old output
     // identity is still explicit in the vanilla ID; no removed addon IDs are revived.
     String path=id.getPath();int from=path.indexOf("_from_");if(from>=0)path=path.substring(0,from);
     var output=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("minecraft",path));
     if(output!=null&&output!=net.minecraft.world.item.Items.AIR){var plan=new ClassicFurnaces.Plan(new net.minecraft.world.item.ItemStack(output),1,1,0);extra=Math.min(Integer.MAX_VALUE,extra+(long)Math.max(0,old.getInt(key))*perItem(plan));}
    }
   }data.putInt(KEY,(int)Math.min(Integer.MAX_VALUE,(long)Math.max(0,data.getInt(KEY))+extra));}
   be.setChanged();
  }
  return Math.max(0,data.getInt(KEY));
 }
 public static void completed(BlockEntity be,ClassicFurnaces.Plan plan,int outputCount){
  if(be.getLevel()==null||be.getLevel().isClientSide||!WorldContentMode.rebuilt(be.getLevel())||!managed(be)||!plan.exists()||outputCount<=0)return;
  int before=stored(be);be.getPersistentData().putInt(KEY,(int)Math.min(Integer.MAX_VALUE,(long)before+(long)perItem(plan)*outputCount));be.setChanged();
 }
 public static int claim(BlockEntity be,ServerPlayer player){
  if(!WorldContentMode.rebuilt(player.level())||be.getLevel()!=player.level()||be.getLevel().getBlockEntity(be.getBlockPos())!=be||!managed(be)||!player.isAlive()||player.isSpectator()||player.distanceToSqr(be.getBlockPos().getX()+.5,be.getBlockPos().getY()+.5,be.getBlockPos().getZ()+.5)>64)return 0;
  int balance=stored(be);if(balance==0)return 0;int afterMending=PlayerState.mend(player,balance),mending=balance-afterMending;long room=Math.max(0L,(long)Rules.MAX_XP-PlayerState.data(player).getInt("Xp"));int awarded=(int)Math.min(afterMending,room),paid=mending+awarded;if(paid<=0)return 0;
  be.getPersistentData().putInt(KEY,balance-paid);be.setChanged();PlayerState.addXp(player,awarded);player.inventoryMenu.broadcastChanges();return paid;
 }

 /** Retain the balance on the one dropped item representing this furnace. */
 static void carry(List<ItemStack> drops, BlockState state, BlockEntity furnace){
  if(!managed(furnace)||!furnace.getBlockState().is(state.getBlock()))return;
  for(int i=0;i<drops.size();i++){
   var drop=drops.get(i);
   if(drop.isEmpty()||!drop.is(state.getBlock().asItem()))continue;
   int balance=stored(furnace);
   if(balance==0)return;
   var carried=drop.copyWithCount(1);
   carried.getOrCreateTag().putInt(CARRIED,balance);
   drops.set(i,carried);
   if(drop.getCount()>1)drops.add(i+1,drop.copyWithCount(drop.getCount()-1));
   return;
  }
 }

 /** Called at the native block-item placement NBT step, inside Forge's snapshot transaction. */
 public static void restorePlaced(BlockPos pos,Level level,Player player,ItemStack stack){
  if(level.isClientSide||player!=null&&player.getAbilities().instabuild||!stack.hasTag()
   ||!stack.getTag().contains(CARRIED,Tag.TAG_INT))return;
  int balance=stack.getTag().getInt(CARRIED);
  if(balance<=0)return;
  var furnace=level.getBlockEntity(pos);
  if(!managed(furnace)||!stack.is(furnace.getBlockState().getBlock().asItem()))return;
  var data=furnace.getPersistentData();
  data.putInt(KEY,balance);
  data.putBoolean(MIGRATED,true);
  furnace.setChanged();
 }

 public static ContainerData data(BlockEntity be){return new ContainerData(){public int getCount(){return 2;}public int get(int index){int xp=stored(be);return index==0?xp&65535:xp>>>16;}public void set(int index,int value){}};}
 /** Native legacy recipe bookkeeping may not emit orbs through any automatic path. */
 public static List<?> automaticPayout(Object raw){return raw instanceof BlockEntity be&&managed(be)?List.of():null;}
 private FurnaceExperience(){}
}

package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;

/** 0.4.17: Adds quality cycling, quality XP cost, and pure-vanilla overflow/close behavior. */
public final class TimedCrafting {
 public static class Menu extends RecipeBookMenu<CraftingContainer> implements SourceCraftBatch1722.Access {
  public boolean mite$moveOneCraft1722(ItemStack stack,int start,int end,boolean reverse){return moveItemStackTo(stack,start,end,reverse);}
  public final CraftingContainer grid=new TransientCraftingContainer(this,3,3);
  private final ResultContainer result=new ResultContainer();
  private final ContainerLevelAccess access;
  private final Player owner;
  private final Block openedWorkbench;
  private R196Workbench.Tier clientTier=R196Workbench.Tier.FLINT;
  private String signature="";
  private int progress,total=1;
  private boolean closing,requested,beaconRound;
  private int runeSelection;
  private int qualitySelection=-1;
  private String resumedSignature;
  private CraftingRecipe currentRecipe;
  private RecipeManager recipeManager;
  private int resumedProgress;
  private long lastTick=Long.MIN_VALUE;
  private net.minecraft.world.level.Level lastWorld;
  private final ContainerData data=new ContainerData(){
   public int get(int i){return i==4?workbenchTier().ordinal():((i<2?progress:total)>>>((i&1)*16))&65535;}
   public void set(int i,int v){if(i==4)clientTier=R196Workbench.Tier.fromNetwork(v);else if(i<2)progress=(i==0?(progress&0xffff0000)|v&65535:(progress&65535)|(v&65535)<<16);else total=(i==2?(total&0xffff0000)|v&65535:(total&65535)|(v&65535)<<16);}
   public int getCount(){return 5;}
  };
  public Menu(int id,Inventory inv){this(id,inv,ContainerLevelAccess.NULL);}
  public Menu(int id,Inventory inv,ContainerLevelAccess a){
   super(Content.CRAFT_MENU.get(),id);owner=inv.player;access=a;ClassicCoins.bind(grid,owner);
   openedWorkbench=access.evaluate((level,pos)->level.getBlockState(pos).getBlock(),null);
   AdvancedCraftingFacility.bind(grid,a);
   if(!pureVanilla()){
    var previous=PlayerState.data(owner);resumedSignature=previous.getString("CraftKey");resumedProgress=previous.getInt("CraftProgress");
   }else{
    resumedSignature="";resumedProgress=0;
   }
   addSlot(new ResultSlot(owner,grid,result,0,124,35){
    public boolean mayPickup(Player p){return p==owner&&ready();}
    public ItemStack remove(int amount){return ready()?super.remove(amount):ItemStack.EMPTY;}
    public void onTake(Player p,ItemStack stack){
     int qCost=qualityXpCost(stack);
     ClassicCoins.settle(owner,grid);
     if(p instanceof ServerPlayer server&&!server.isCreative()&&qCost>0){
      if(ClassicQualityCrafting.availableXp(p)<qCost)throw new IllegalStateException("Quality result taken without required experience");
      PlayerState.addXp(server,-qCost);
      Network.sync(server);
     }
     if(!pureVanilla())CraftingCheckpoints.consume(owner,"table",signature);
     requested=false;resumedSignature="";resumedProgress=0;progress=0;clearBeaconRound();ClassicCraftRepair.settle(owner,grid);super.onTake(p,stack);p.causeFoodExhaustion(.15f);save();
    }
   });
   for(int y=0;y<3;y++)for(int x=0;x<3;x++)addSlot(new Slot(grid,x+y*3,30+x*18,17+y*18));
   for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,8+x*18,84+y*18));
   for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));
   addDataSlots(data);
  }
  boolean pureVanilla(){return !WorldContentMode.rebuilt(owner.level());}
  public void fillCraftSlotsStackedContents(StackedContents contents){grid.fillStackedContents(contents);}
  public void clearCraftingContent(){grid.clearContent();result.clearContent();}
  public boolean recipeMatches(Recipe<? super CraftingContainer> recipe){return recipe.matches(grid,owner.level());}
  public int getResultSlotIndex(){return 0;}public int getGridWidth(){return 3;}public int getGridHeight(){return 3;}public int getSize(){return 10;}
  public RecipeBookType getRecipeBookType(){return RecipeBookType.CRAFTING;}public boolean shouldMoveToInventory(int slot){return slot!=0;}
  public int progress(){return progress;}public int total(){return total;}
  private boolean canUseWorkbench(){
   return !closing&&(owner.level().isClientSide||owner.containerMenu==this&&stillValid(owner));
  }
  private int qualityXpCost(){return qualityXpCost(result.getItem(0));}
  private int qualityXpCost(ItemStack out){
   if(out==null||out.isEmpty()||!ClassicQualityCrafting.supportsQuality(currentRecipe,out))return 0;
   float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
   return ClassicQualityCrafting.xpCost(owner,baseDiff,ClassicQuality.get(out),false);
  }
  private boolean affordableExperience(){
   var out=result.getItem(0);
   return ClassicCoins.affordable(owner,out)&&ClassicQualityCrafting.availableXp(owner)>=ClassicCoins.mintCost(out)+qualityXpCost();
  }
  private boolean ready(){
   if(!canUseWorkbench())return false;
   refreshRecipeOnReload();
   return allowsIngredients()&&allowsWorkbench()&&SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,result.getItem(0))&&!SourceCraftingContext1533.upperBodyInWeb(owner)&&AdvancedCraftingFacility.ready(currentRecipe,grid)&&affordableExperience()&&(owner.isCreative()||progress>=total&&SurvivalFood.hasFoodEnergy(owner));
  }
  boolean allowsIngredients(){return SourceR196Crafting.allows(owner.level(),grid,currentRecipe);}
  boolean allowsWorkbench(){return SourceR196Crafting.allowsWorkbench(owner.level(),grid,currentRecipe,result.getItem(0),workbenchTier().hardness);}
  R196Workbench.Tier workbenchTier(){
   return owner.level().isClientSide?clientTier:access.evaluate((level,pos)->R196Workbench.tier(level.getBlockState(pos)),R196Workbench.Tier.FLINT);
  }
  private int currentPeriod(){
   int ordinaryPeriod=CraftingDifficulty.period(owner,grid,currentRecipe,true);
   return beaconRound?IteBeaconCrafting.period(ordinaryPeriod):ordinaryPeriod;
  }
  private void clearBeaconRound(){
   if(beaconRound){beaconRound=false;total=currentPeriod();}
  }
  private void save(){
   if(!owner.level().isClientSide){
    var t=PlayerState.data(owner);
    if(pureVanilla()){t.remove("CraftKey");t.remove("CraftProgress");return;}
    t.putString("CraftKey",signature);t.putInt("CraftProgress",progress);
    if(!result.isEmpty())CraftingCheckpoints.saveTable(owner,signature,progress,beaconRound);
   }
  }
  public void slotsChanged(Container c){
   if(owner.level().isClientSide||!canUseWorkbench())return;
   refreshRecipe();
   broadcastChanges();
  }
  private void refreshRecipeOnReload(){
   if(!closing&&!owner.level().isClientSide&&recipeManager!=owner.level().getRecipeManager()){
    refreshRecipe();
   }
  }
  private void refreshRecipe(){
   if(!signature.isEmpty())save();
   CraftingRecipe previousRecipe=currentRecipe;
   recipeManager=owner.level().getRecipeManager();
   var recipe=recipeManager.getRecipeFor(RecipeType.CRAFTING,grid,owner.level());
   currentRecipe=recipe.orElse(null);
   ItemStack out=recipe.map(r->r.assemble(grid,owner.level().registryAccess())).orElse(ItemStack.EMPTY);
   if(!out.isEmpty()&&(!out.isItemEnabled(owner.level().enabledFeatures())||ModernContentPolicy.removed(out)))out=ItemStack.EMPTY;
   if(out.getItem() instanceof SourceRunestoneItem){
    out=SourceRunestoneItem.select(out,runeSelection);
    qualitySelection=-1;
   }else if(ClassicQualityCrafting.supportsQuality(currentRecipe,out)){
    boolean hasSkill=ClassicQualityCrafting.hasSkill(owner,currentRecipe,grid,out);
    float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
    var minQ=ClassicQualityCrafting.minQuality(owner,out,hasSkill);
    var maxQ=ClassicQualityCrafting.maxQuality(owner,baseDiff,out,hasSkill,false);
    if(previousRecipe!=currentRecipe||qualitySelection<0){
     qualitySelection=minQ.ordinal();
    }else{
     qualitySelection=ClassicQualityCrafting.clampToRange(qualitySelection,minQ,maxQ).ordinal();
    }
    out=ClassicQuality.set(out.copy(),ClassicQuality.Level.byOrdinal(qualitySelection));
    runeSelection=0;
   }else if(!out.isEmpty()){
    runeSelection=0;
    qualitySelection=-1;
   }
   result.setItem(0,out);
   result.setRecipeUsed(out.isEmpty()?null:currentRecipe);
   StringBuilder key=new StringBuilder(recipe.map(r->r.getId().toString()).orElse(""));if(!out.isEmpty())key.append('|').append(out.save(new net.minecraft.nbt.CompoundTag()));
   for(int i=0;i<9;i++){var s=grid.getItem(i);if(!s.isEmpty()){key.append('|').append(i).append(':').append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem())).append(':').append(s.getDamageValue()).append(':').append(s.hasTag()?s.getTag().toString():"");}}
   String next=key.toString();
   if(!signature.equals(next)){
    save();requested=false;signature=next;
    if(pureVanilla()){
     progress=0;beaconRound=false;
    }else{
     var checkpoint=CraftingCheckpoints.restoreTable(owner,signature);
     int legacyProgress=signature.equals(resumedSignature)?resumedProgress:0;
     progress=Math.max(checkpoint.progress(),legacyProgress);
     beaconRound=progress==checkpoint.progress()&&checkpoint.beaconRound()&&IteBeaconCrafting.eligible(owner,currentRecipe,out);
    }
   }
   if(previousRecipe!=currentRecipe&&(!allowsIngredients()||!allowsWorkbench()||!SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,out)||!affordableExperience())){
    clearDisallowedProgress();
   }
   total=currentPeriod();
   progress=Math.min(total,Math.max(0,progress));save();
  }
  private void clearDisallowedProgress(){
   boolean matchingLegacy=signature.equals(resumedSignature)&&resumedProgress>0;
   if(!requested&&progress<=0&&!matchingLegacy)return;
   requested=false;
   progress=0;
   clearBeaconRound();
   if(matchingLegacy){
    resumedSignature="";
    resumedProgress=0;
   }
   if(!pureVanilla())CraftingCheckpoints.consume(owner,"table",signature);
   save();
  }
  public void broadcastChanges(){
   boolean activeWorkbench=!owner.level().isClientSide&&canUseWorkbench();
   if(activeWorkbench){
    boolean facilityChanged=AdvancedCraftingFacility.environmentChanged(grid);
    if(facilityChanged||recipeManager!=owner.level().getRecipeManager())refreshRecipe();
   }
   if(activeWorkbench&&(lastWorld!=owner.level()||lastTick!=owner.level().getGameTime())){
    lastWorld=owner.level();lastTick=owner.level().getGameTime();
    total=currentPeriod();progress=Math.min(progress,total);
    if(SourceCraftingContext1533.upperBodyInWeb(owner)&&(requested||progress>0)){requested=false;progress=0;clearBeaconRound();resumedSignature="";resumedProgress=0;if(!pureVanilla())CraftingCheckpoints.consume(owner,"table",signature);save();}
    if(!allowsIngredients()||!allowsWorkbench()||!SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,result.getItem(0))||!affordableExperience()){
     clearDisallowedProgress();
    }
    if(requested&&!result.isEmpty()&&SurvivalFood.hasFoodEnergy(owner)&&progress<total){
     if(progress==0&&IteBeaconCrafting.starts(owner,access,currentRecipe,result.getItem(0))){
      beaconRound=true;total=IteBeaconCrafting.period(total);
     }
     progress++;if(progress%20==0)save();
    }
    boolean overflow=pureVanilla();
    if(requested&&ready()&&(overflow||ContinuousCrafting.canStore(owner,result.getItem(0)))){
     int paidXp=qualityXpCost()+ClassicCoins.mintCost(result.getItem(0));
     String previous=signature;var taken=quickMoveStack(owner,0);
     if(!taken.isEmpty()){
      progress=0;clearBeaconRound();requested=(!overflow||paidXp==0)&&!result.isEmpty()&&previous.equals(signature);save();
     }
    }
   }
   super.broadcastChanges();
  }
  public void clicked(int slot,int button,ClickType type,Player p){
   if(slot==0){
    if(p!=owner||!canUseWorkbench())return;
    refreshRecipeOnReload();
   }
   if(slot==0&&button==1&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)&&result.getItem(0).getItem() instanceof SourceRunestoneItem){
    if(!owner.level().isClientSide&&p==owner){
     CraftingCheckpoints.consume(owner,"table",signature);requested=false;progress=0;clearBeaconRound();resumedSignature="";resumedProgress=0;
     runeSelection=(runeSelection+1)&15;slotsChanged(grid);
     CraftingCheckpoints.consume(owner,"table",signature);progress=0;clearBeaconRound();save();broadcastChanges();
    }
    return;
   }
   if(slot==0&&button==1&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)&&ClassicQualityCrafting.supportsQuality(currentRecipe,result.getItem(0))){
    if(!owner.level().isClientSide&&p==owner){
     var baseOut=currentRecipe.assemble(grid,owner.level().registryAccess());
     boolean hasSkill=ClassicQualityCrafting.hasSkill(owner,currentRecipe,grid,baseOut);
     float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
     var minQ=ClassicQualityCrafting.minQuality(owner,baseOut,hasSkill);
     var maxQ=ClassicQualityCrafting.maxQuality(owner,baseDiff,baseOut,hasSkill,false);
     CraftingCheckpoints.consume(owner,"table",signature);requested=false;progress=0;clearBeaconRound();resumedSignature="";resumedProgress=0;
     qualitySelection=ClassicQualityCrafting.cycleNext(qualitySelection,minQ,maxQ).ordinal();
     refreshRecipe();
     CraftingCheckpoints.consume(owner,"table",signature);progress=0;clearBeaconRound();save();broadcastChanges();
    }
    return;
   }
   if(slot==0&&!p.isCreative()){
    if(!owner.level().isClientSide&&p==owner&&!result.isEmpty()&&allowsIngredients()&&allowsWorkbench()&&SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,result.getItem(0))&&!SourceCraftingContext1533.upperBodyInWeb(owner)&&affordableExperience()&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)&&(button==0||button==1)){requested=true;OriginalCurseEffects.has(owner,8,true);}
    return;
   }
   super.clicked(slot,button,type,p);
  }
  public boolean stillValid(Player p){
   if(p!=owner||closing)return false;
   return access.evaluate((level,pos)->{
    if(level!=p.level()||!level.hasChunkAt(pos)||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64.0)return false;
    var state=level.getBlockState(pos);
    return state.getBlock()==openedWorkbench&&R196Workbench.canOpen(state,level);
   },p.level().isClientSide);
  }
  public void removed(Player p){
   closing=true;requested=false;
   if(pureVanilla()){
    progress=0;beaconRound=false;resumedSignature="";resumedProgress=0;
    if(!p.level().isClientSide){
     CraftingCheckpoints.consume(p,"table",signature);
     PlayerState.data(p).remove("CraftKey");
     PlayerState.data(p).remove("CraftProgress");
    }
   }else{
    save();
   }
   super.removed(p);
   if(!p.level().isClientSide)clearContainer(p,grid);
  }
  public ItemStack quickMoveStack(Player p,int index){
   if(index<0||index>=slots.size()||index==0&&(p!=owner||!ready()))return ItemStack.EMPTY;
   Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
   ItemStack s=slot.getItem(),copy=s.copy();
   if(index==0){
    boolean overflow=pureVanilla();
    if(!overflow&&!ContinuousCrafting.canStore(p,s))return ItemStack.EMPTY;
    if(!p.level().isClientSide)s.getItem().onCraftedBy(s,p.level(),p);
    SourceCraftBatch1722.move(this,s,10,46,true);
    if(!overflow&&s.getCount()==copy.getCount())return ItemStack.EMPTY;
    if(overflow)slot.onQuickCraft(ItemStack.EMPTY,copy);else slot.onQuickCraft(s,copy);
    if(s.isEmpty()||overflow)slot.set(ItemStack.EMPTY);else slot.setChanged();
    slot.onTake(p,copy);
    if(!s.isEmpty()&&!p.level().isClientSide)p.drop(s,false);
    return copy;
   }
   else if(index>=10&&index<37){if(!moveItemStackTo(s,37,46,false))return ItemStack.EMPTY;}
   else if(index>=37){if(!moveItemStackTo(s,10,37,false))return ItemStack.EMPTY;}
   else if(!moveItemStackTo(s,10,46,false))return ItemStack.EMPTY;
   if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
   if(s.getCount()==copy.getCount())return ItemStack.EMPTY;
   slot.onTake(p,s);
   return copy;
  }
 }
 public static boolean canOpen(net.minecraft.world.level.Level level,BlockPos pos){var above=level.getBlockState(pos.above());return above.getFluidState().isEmpty()&&!above.isSolid()&&!above.isSolidRender(level,pos.above());}
 public static void open(ServerPlayer p,BlockPos pos){
  var level=p.level();
  var state=level.getBlockState(pos);
  if(!R196Workbench.canOpen(state,level)||!canOpen(level,pos))return;
  Component title=Component.translatable("container.crafting");
  if(state.getBlock() instanceof R196Workbench bench){
   title=switch(bench.tier()){
    case FLINT->Component.translatable("container.crafting.mite.flint_workbench");
    case OBSIDIAN->Component.translatable("container.crafting.mite.obsidian_workbench");
    default->bench.getName();
   };
  }
  p.openMenu(new SimpleMenuProvider((id,inv,player)->new Menu(id,inv,ContainerLevelAccess.create(level,pos)),title));
 }
}

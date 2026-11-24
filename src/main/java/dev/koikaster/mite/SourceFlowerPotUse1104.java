package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Source pot interaction; direct block-use APIs do not impersonate the player action owner. */
public final class SourceFlowerPotUse1104 {
 private static final ThreadLocal<Frame> active=new ThreadLocal<>();
 private static final class Frame {
  final Player player;final Level level;final BlockPos pos;final ItemStack input;final Item item;boolean converted;
  Frame(Player p,Level l,BlockPos pos){player=p;level=l;this.pos=pos;input=p.getMainHandItem();item=input.getItem();}
 }
 static boolean owned(Block block){var id=BuiltInRegistries.BLOCK.getKey(block);return id!=null&&BuiltInRegistries.BLOCK.get(id)==block&&(id.getNamespace().equals("minecraft")||id.getNamespace().equals(MiteReforged.ID));}
 static boolean managed(Block block){return block instanceof FlowerPotBlock pot&&owned(block)&&pot.getEmptyPot()==Blocks.FLOWER_POT;}
 static boolean multiPlant(Block block){return block instanceof FlowerBlock&&block!=Blocks.POPPY&&block!=Blocks.DANDELION;}
 static int baseMetadata(Block b){
  if(b==Blocks.AIR||multiPlant(b))return 0;
  if(b==Blocks.POPPY)return 1;if(b==Blocks.DANDELION)return 2;
  if(b==Blocks.OAK_SAPLING)return 3;if(b==Blocks.SPRUCE_SAPLING)return 4;if(b==Blocks.BIRCH_SAPLING)return 5;if(b==Blocks.JUNGLE_SAPLING)return 6;
  if(b==Blocks.RED_MUSHROOM)return 7;if(b==Blocks.BROWN_MUSHROOM)return 8;if(b==Blocks.CACTUS)return 9;if(b==Blocks.DEAD_BUSH)return 10;if(b==Blocks.FERN)return 11;
  if(new ResourceLocation(MiteReforged.ID,"witherwood").equals(BuiltInRegistries.BLOCK.getKey(b)))return 12;
  return 1000+BuiltInRegistries.BLOCK.getId(b);
 }
 static int multiMetadata(Block b){
  if(!multiPlant(b))return 0;
  if(b==Blocks.BLUE_ORCHID)return 1;if(b==Blocks.ALLIUM)return 2;if(b==Blocks.ORANGE_TULIP)return 5;if(b==Blocks.PINK_TULIP)return 7;if(b==Blocks.OXEYE_DAISY)return 8;
  // Retained modern flowers are explicit additions to this family, not aliases of an old flower.
  return 1000+BuiltInRegistries.BLOCK.getId(b);
 }
 static FlowerPotBlock target(FlowerPotBlock pot,Block plant){var id=BuiltInRegistries.BLOCK.getKey(plant);var supplier=pot.getEmptyPot().getFullPotsView().get(id);return supplier!=null&&supplier.get() instanceof FlowerPotBlock p?p:null;}
 public static InteractionResult use(Object rawPot,Object rawState,Object rawLevel,Object rawPos,Object rawPlayer,Object rawHand){
  var pot=(FlowerPotBlock)rawPot;var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;
  if(!managed(pot)||ModernContentPolicy.removed((BlockState)rawState))return null;
  // The retained offhand whitelist has no pottable plant. It must not restore native emptyhand extraction.
  if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
  var input=player.getMainHandItem();if(input.isEmpty()||!(input.getItem() instanceof BlockItem item)||ModernContentPolicy.removed(input))return InteractionResult.PASS;
  var plant=item.getBlock();var incoming=target(pot,plant);if(incoming==null)return InteractionResult.PASS;
  if(!owned(plant)||!owned(incoming))return null; // A registered foreign pot/plant keeps its own native API contract.
  var host=new Adapter(pot,level,pos,player,input,plant,incoming);
  boolean used=host.multi?SourceFlowerPotRules1104.multi(host):SourceFlowerPotRules1104.base(host);
  return used?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
 }
 public static InteractionResult blockUse(Object rawState,Object rawLevel,Object rawPlayer,Object rawHand,Object rawHit){
  var state=(BlockState)rawState;var level=(Level)rawLevel;var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;var hit=(BlockHitResult)rawHit;
  if(hand!=InteractionHand.MAIN_HAND||!managed(state.getBlock()))return state.use(level,player,hand,hit);
  var previous=active.get();var frame=new Frame(player,level,hit.getBlockPos());active.set(frame);
  try{var result=state.use(level,player,hand,hit);if(frame.converted&&!level.isClientSide)player.awardStat(Stats.ITEM_USED.get(frame.item));return result;}
  finally{if(previous==null)active.remove();else active.set(previous);}
 }
 private static final class Adapter implements SourceFlowerPotRules1104.Host<ItemStack> {
  final FlowerPotBlock pot,incoming;final Level level;final BlockPos pos;final Player player;final ItemStack input;final Block plant;final boolean multi;Block previousPlant;
  Adapter(FlowerPotBlock pot,Level level,BlockPos pos,Player player,ItemStack input,Block plant,FlowerPotBlock incoming){this.pot=pot;this.level=level;this.pos=pos;this.player=player;this.input=input;this.plant=plant;this.incoming=incoming;multi=multiPlant(pot.getContent());}
  public ItemStack held(){return input;}
  public int baseMetadata(ItemStack stack){return SourceFlowerPotUse1104.baseMetadata(plant);}
  public int multiMetadata(ItemStack stack){return SourceFlowerPotUse1104.multiMetadata(plant);}
  public boolean server(){return !level.isClientSide;}
  public int metadata(){var b=level.getBlockState(pos).getBlock();previousPlant=b instanceof FlowerPotBlock p?p.getContent():Blocks.AIR;return multi?SourceFlowerPotUse1104.multiMetadata(previousPlant):SourceFlowerPotUse1104.baseMetadata(previousPlant);}
  public void drop(int metadata){SourcePotDrop1110.emit((ServerLevel)level,pos,level.getBlockState(pos),new ItemStack(previousPlant));}
  public float randomFloat(){return level.random.nextFloat();}
  public void pop(float volume,float pitch){level.playSeededSound(null,(float)pos.getX()+.5f,(float)pos.getY()+.5f,(float)pos.getZ()+.5f,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,volume,pitch,0L);}
  public void switchPot(boolean multi,int metadata,int flags){level.setBlock(pos,incoming.defaultBlockState(),flags);}
  public void changeMetadata(int metadata,int flags){level.setBlock(pos,incoming.defaultBlockState(),flags);}
  public boolean creative(){return player.getAbilities().instabuild;}
  public void convertHeld(){SourceHeldConversion1037.convert(player,InteractionHand.MAIN_HAND,ItemStack.EMPTY);var frame=active.get();if(frame!=null&&frame.player==player&&frame.level==level&&frame.pos.equals(pos)&&frame.input==input)frame.converted=true;}
 }
 private SourceFlowerPotUse1104(){}
}

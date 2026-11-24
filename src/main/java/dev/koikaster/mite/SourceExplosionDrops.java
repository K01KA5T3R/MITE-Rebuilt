package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** aqz exploded base drop rules, with the retained native loot entry for block
 * overrides. The original-specific override catalogue is tracked separately. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceExplosionDrops {
 private record Delayed(long time,ItemEntity item){}
 private static final Map<ServerLevel,List<Delayed>> PENDING=new IdentityHashMap<>();
 public static float hardness(BlockState state){return hardness(state,null);}
 static float hardness(BlockState state,ServerLevel level){
  Float gold=R196Gold.hardness(state,level);if(gold!=null)return gold;
  Float original=OriginalMining1587.explosionResistance(state);if(original!=null)return original;
  if(state.is(SourceMetalBuildings.OBSIDIAN_SLAB.get()))return 0;
  if(state.is(Blocks.TNT))return 1;
  if(state.getBlock() instanceof InfestedBlock||state.getBlock() instanceof SourceInfestation.CopperStone)return 0;
  return ClassicMining.sourceFixedHardness(state);
 }
 static void pending(ServerLevel world,ItemEntity item){PENDING.computeIfAbsent(world,key->new ArrayList<>()).add(new Delayed(world.getGameTime()+1,item));}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel world))return;var queue=PENDING.get(world);if(queue==null)return;for(var iterator=queue.iterator();iterator.hasNext();){var entry=iterator.next();if(entry.time<=world.getGameTime()){iterator.remove();if(!entry.item.isRemoved())world.addFreshEntity(entry.item);}}if(queue.isEmpty())PENDING.remove(world);}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel world)PENDING.remove(world);}
 private static boolean wood(BlockState state){return state.is(BlockTags.LOGS)||state.is(BlockTags.PLANKS)||state.is(BlockTags.WOODEN_STAIRS)||state.is(BlockTags.WOODEN_SLABS)||state.getBlock() instanceof DoorBlock&&state.getBlock()!=Blocks.IRON_DOOR||state.getBlock() instanceof BedBlock;}
 private static boolean stone(BlockState state){return state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)||state.is(Blocks.STONE_BRICKS)||state.is(Blocks.CRACKED_STONE_BRICKS)||state.is(Blocks.MOSSY_STONE_BRICKS)||state.is(Blocks.CHISELED_STONE_BRICKS)||state.is(Blocks.SANDSTONE)||state.is(Blocks.CHISELED_SANDSTONE)||state.is(Blocks.CUT_SANDSTONE);}
 private static boolean suppressedPlant(BlockState state){return state.getBlock() instanceof BushBlock||state.getBlock() instanceof VineBlock||state.getBlock() instanceof CactusBlock||state.getBlock() instanceof CakeBlock||state.getBlock() instanceof SugarCaneBlock;}
 private static float materialChance(BlockState state){return state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)||state.getBlock() instanceof AbstractGlassBlock||state.getBlock() instanceof StainedGlassPaneBlock||state.is(Blocks.GLASS_PANE)?.4f:1f;}
 static void drop(SourceExplosion explosion,BlockPos pos,BlockState captured,boolean currentMetadata){
  if(SourceFurnaceExplosion1610.drop(explosion,pos,captured,currentMetadata))return;
  // aqh returns sand for every exploded original sandstone subtype. Red is the
  // retained modern counterpart; slabs/stairs keep their separate callbacks.
  if(captured.is(Content.SACRED_SANDSTONE.get())||captured.is(Blocks.SANDSTONE)||captured.is(Blocks.CHISELED_SANDSTONE)||captured.is(Blocks.CUT_SANDSTONE)||captured.is(Blocks.RED_SANDSTONE)||captured.is(Blocks.CHISELED_RED_SANDSTONE)||captured.is(Blocks.CUT_RED_SANDSTONE)){boolean red=captured.is(Blocks.RED_SANDSTONE)||captured.is(Blocks.CHISELED_RED_SANDSTONE)||captured.is(Blocks.CUT_RED_SANDSTONE);spawnUnits(explosion,pos,new ItemStack(red?Items.RED_SAND:Items.SAND),1,1,captured,currentMetadata);return;}
  var world=explosion.world();
  if(captured.is(SourceMetalBuildings.OBSIDIAN_SLAB.get())){if(!currentMetadata&&captured.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)spawnUnits(explosion,pos,new ItemStack(Content.OBSIDIAN_SHARD.get()),6,.5f,captured,currentMetadata);return;}
  if(captured.is(SourceMetalBuildings.OBSIDIAN_STAIRS.get())){spawnUnits(explosion,pos,new ItemStack(Content.OBSIDIAN_SHARD.get()),6,.5f,captured,currentMetadata);return;}
  // aqs wasExploded has neither wasSilverfish nor a responsible player for a
  // mob's blast, so it returns0. The source TNT callback remains phaseB's owner.
  if(captured.getBlock() instanceof InfestedBlock||captured.getBlock() instanceof SourceInfestation.CopperStone||suppressedPlant(captured))return;
  if(captured.getBlock() instanceof SourceRunestoneBlock||captured.is(Blocks.OBSIDIAN)){spawnUnits(explosion,pos,new ItemStack(Content.OBSIDIAN_SHARD.get()),6,.5f,captured,currentMetadata);return;}
  if(captured.getBlock() instanceof SourceGiantBrownMushroomBlock){int count=world.random.nextInt(10)-8;if(count>0)spawnUnits(explosion,pos,new ItemStack(Items.BROWN_MUSHROOM),count,1,captured,currentMetadata);return;}
  if(captured.getBlock() instanceof BedBlock){spawnUnits(explosion,pos,new ItemStack(Items.STICK),1,1.5f,captured,currentMetadata);spawnUnits(explosion,pos,new ItemStack(Items.STRING),1,1.5f,captured,currentMetadata);return;}
  if(captured.is(BlockTags.TERRACOTTA)||captured.is(Blocks.NETHERRACK))return;
  // apr's own explosion branch precedes aqz's generic stone-material drop.
  if(captured.is(BlockTags.COAL_ORES))return;
  if(captured.is(BlockTags.EMERALD_ORES)){spawnUnits(explosion,pos,new ItemStack(Content.EMERALD_SHARD.get()),1,1,captured,currentMetadata);return;}
  if(captured.is(BlockTags.DIAMOND_ORES)){spawnUnits(explosion,pos,new ItemStack(Content.DIAMOND_SHARD.get()),1,1,captured,currentMetadata);return;}
  if(captured.is(BlockTags.LAPIS_ORES)){spawnUnits(explosion,pos,new ItemStack(Items.LAPIS_LAZULI),3+world.random.nextInt(3),1,captured,currentMetadata);return;}
  if(captured.is(Blocks.NETHER_QUARTZ_ORE)){spawnUnits(explosion,pos,new ItemStack(Content.QUARTZ_SHARD.get()),1,1,captured,currentMetadata);return;}
  if(captured.is(Blocks.BRICKS)){spawnUnits(explosion,pos,new ItemStack(Items.BRICK),1,1.5f,captured,currentMetadata);return;}
  // Later human N018 overrides this R195 conversion's mineral-bearing output:
  // renewable cobblestone must not become new nuggets through gravel screening.
  if(captured.is(Blocks.COBBLESTONE)||captured.is(Blocks.MOSSY_COBBLESTONE)){spawnUnits(explosion,pos,new ItemStack(WorldContentMode.rebuilt(world)?Content.CRUSHED_GRAVEL.get():Blocks.GRAVEL),1,1,captured,currentMetadata);return;}
  if(captured.is(Blocks.LAPIS_BLOCK)){spawnUnits(explosion,pos,new ItemStack(Items.LAPIS_LAZULI),9,.5f,captured,currentMetadata);return;}
  if(captured.is(Blocks.COAL_BLOCK)){spawnUnits(explosion,pos,new ItemStack(Items.COAL),9,.5f,captured,currentMetadata);return;}
  if(captured.is(BlockTags.WOOL)||captured.is(BlockTags.WOOL_CARPETS)){spawnUnits(explosion,pos,new ItemStack(Items.STRING),1,1,captured,currentMetadata);return;}
  if(wood(captured)){spawnUnits(explosion,pos,new ItemStack(Items.STICK),1,1,captured,currentMetadata);return;}
  if(stone(captured)){spawnUnits(explosion,pos,new ItemStack(Items.COBBLESTONE),1,1,captured,currentMetadata);return;}
  var state=currentMetadata?captured.getBlock().defaultBlockState():captured;
  var builder=new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.EXPLOSION_RADIUS,1f);
  if(explosion.exploder()!=null)builder.withParameter(LootContextParams.THIS_ENTITY,explosion.exploder());var blockEntity=world.getBlockEntity(pos);if(blockEntity!=null)builder.withParameter(LootContextParams.BLOCK_ENTITY,blockEntity);
  for(var stack:state.getDrops(builder))spawnUnits(explosion,pos,stack.copyWithCount(1),stack.getCount(),materialChance(captured),captured,currentMetadata);
 }
 static void spawnUnits(SourceExplosion explosion,BlockPos pos,ItemStack prototype,int count,float chance,BlockState captured,boolean defaultBounds){
  var world=explosion.world();
  for(int i=0;i<count;i++)if(world.random.nextFloat()<chance){spawn(explosion,pos,prototype.copy(),captured,defaultBounds);if(chance>1&&world.random.nextFloat()<chance-1)spawn(explosion,pos,prototype.copy(),captured,defaultBounds);}
 }
 private static void spawn(SourceExplosion explosion,BlockPos pos,ItemStack stack,BlockState state,boolean defaultBounds){
  var world=explosion.world();if(!world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS))return;AABB bounds=new AABB(.25,.25,.25,.75,.75,.75);
  if(!defaultBounds){var shape=state.getShape(world,pos);if(!shape.isEmpty()){var raw=shape.bounds();bounds=new AABB((raw.minX+raw.maxX)/2-(raw.maxX-raw.minX)/4,(raw.minY+raw.maxY)/2-(raw.maxY-raw.minY)/4,(raw.minZ+raw.maxZ)/2-(raw.maxZ-raw.minZ)/4,(raw.minX+raw.maxX)/2+(raw.maxX-raw.minX)/4,(raw.minY+raw.maxY)/2+(raw.maxY-raw.minY)/4,(raw.minZ+raw.maxZ)/2+(raw.maxZ-raw.minZ)/4);}}
  double x=pos.getX()+(double)world.random.nextFloat()*(bounds.maxX-bounds.minX)+bounds.minX,y=pos.getY()+(double)world.random.nextFloat()*(bounds.maxY-bounds.minY)+bounds.minY,z=pos.getZ()+(double)world.random.nextFloat()*(bounds.maxZ-bounds.minZ)+bounds.minZ;
  var entity=new ItemEntity(world,x,y,z,stack,0,0,0);entity.setPickUpDelay(10);SourceExplosionItems.motion(explosion,entity);explosion.pending(entity);
 }
 private SourceExplosionDrops(){}
}

package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.RegistryObject;
public final class SourceGelatinousContent {
 static final SpawnPlacements.Type PLACEMENT=SpawnPlacements.Type.create("MITE_SOURCE_GELATINOUS_039",(level,pos,type)->level instanceof net.minecraft.world.level.Level world&&!Double.isNaN(SourceTroopPlacement.surfaceY(world,pos)));
 static final String[] COLORS={"green","ochre","crimson","gray","black"};
 static final RegistryObject<EntityType<SourceGelatinousSphere>> SPHERE=Mobs.ENTITIES.register("gelatinous_sphere",()->EntityType.Builder.<SourceGelatinousSphere>of(SourceGelatinousSphere::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build(MiteReforged.ID+":gelatinous_sphere"));
 static void register(){for(int i=1;i<COLORS.length;i++){int subtype=i;Content.item(COLORS[i]+"_gelatinous_sphere",()->new SourceGelatinousSphere.SphereItem(subtype,new Item.Properties().stacksTo(64)));}}
 public static ItemStack sphere(int subtype){return new ItemStack(subtype==0?Items.SLIME_BALL:Content.get(COLORS[subtype]+"_gelatinous_sphere"));}
 static boolean allow(String name,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
  if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return false;
  if(reason==MobSpawnType.SPAWNER)return true;
  if(level.getLevel().dimension()==net.minecraft.world.level.Level.OVERWORLD){int cutoff=switch(name){case "crimson_blob"->40;case "gray_ooze"->32;case "black_pudding"->24;default->Integer.MAX_VALUE;};if(pos.getY()>cutoff)return false;}
  if((name.equals("gray_ooze")||name.equals("black_pudding"))&&!stone(level.getBlockState(pos.below()).getBlock()))return false;
  boolean roof=false;for(int y=pos.getY()+1;y<level.getMaxBuildHeight();y++){var state=level.getBlockState(new BlockPos(pos.getX(),y,pos.getZ()));if(state.isAir())continue;roof=stone(state.getBlock());break;}
  return roof&&SourceSpecialSpawns.light(level,pos,random)&&!level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
 }
 private static boolean stone(Block block){return block==Blocks.STONE||block==Blocks.DEEPSLATE;}
 private SourceGelatinousContent(){}
}

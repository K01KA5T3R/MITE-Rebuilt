package dev.koikaster.mite;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
/** Source Earth and Clay heat/type state, connected to native synced data and root NBT.
 * Preserves original numeric type IDs, independent of modern registry numeric IDs.
 */
public final class OriginalEarthElementalLife {
 public static final int STONE=1,OBSIDIAN=49,CLAY=82,NETHERRACK=87,END_STONE=121,HARDENED_CLAY=172,MAGMA_BIT=256;
 private final Mobs.Undead mob;private final IntSupplier readType;private final IntConsumer writeType;private final Consumer<Mobs.Undead> smoke;int heat,fizzDelay;
 public OriginalEarthElementalLife(Mobs.Undead mob,IntSupplier readType,IntConsumer writeType,Consumer<Mobs.Undead> smoke){this.mob=mob;this.readType=readType;this.writeType=writeType;this.smoke=smoke;}
 public int type(){return readType.getAsInt();}public void type(int value){writeType.accept((short)value);}
 public boolean clay(){return Mobs.name(mob).equals("clay_golem");}
 public boolean hardened(){return clay()&&(!mob.level().isClientSide&&heat>=100||type()==HARDENED_CLAY);}
 public boolean magma(){return !clay()&&(mob.level().isClientSide?(type()&MAGMA_BIT)!=0:heat>=100);}
 public void save(CompoundTag tag){tag.putShort("type",(short)type());tag.putShort("heat",(short)heat);}
 public void load(CompoundTag tag){type(tag.getShort("type"));heat=tag.getShort("heat");}
 public void magmaNow(){heat=1000;int prior=type();type(clay()?HARDENED_CLAY:prior|MAGMA_BIT);if(prior!=type())smoke();}
 public void normal(boolean steam){heat=0;if(clay())return;int prior=type();type(prior&~MAGMA_BIT);if(prior!=type()&&steam)Network.environmentSteam(mob);}
 // The owner must supply the actual source smoke network emitter; no substitute wet steam/hiss is emitted for heating.
 private void smoke(){smoke.accept(mob);}
 public void lightning(){if(!magma()&&!hardened())magmaNow();else if(heat<1000)heat=1000;}
 public static int sourceBlock(Block block){return block==Blocks.END_STONE?END_STONE:block==Blocks.NETHERRACK?NETHERRACK:block==Blocks.OBSIDIAN?OBSIDIAN:STONE;}
 public static Block blockForType(int type){return switch(type&255){case STONE->Blocks.STONE;case OBSIDIAN->Blocks.OBSIDIAN;case CLAY->Blocks.CLAY;case NETHERRACK->Blocks.NETHERRACK;case END_STONE->Blocks.END_STONE;case HARDENED_CLAY->Blocks.TERRACOTTA;default->null;};}
 public void forBlock(Block block,boolean heated){if(clay()){type(heated?HARDENED_CLAY:CLAY);return;}type(sourceBlock(block));if(heated)magmaNow();}
 public boolean precipitation(){var level=mob.level();var pos=mob.blockPosition();return level.isRaining()&&(level.getBiome(pos).value().hasPrecipitation()||OriginalWeather.blood(level))&&level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())<=pos.getY();}
 private boolean freezing(BlockPos pos){var state=mob.level().getBlockState(pos);return state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)||state.is(Blocks.POWDER_SNOW)||state.is(BlockTags.ICE);}
 private boolean inFire(){var box=mob.getBoundingBox().deflate(.001);for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(mob.level().getBlockState(pos).getBlock() instanceof BaseFireBlock)return true;return false;}
 public void afterAi(){if(mob.level().isClientSide){if(!mob.isInWater()&&magma()&&precipitation())ClientFireElementalEffects.singleSteam(mob);return;}if(mob.isInWater()){if(magma())normal(true);heat=0;}else{if(mob.isInLava()||clay()&&inFire()){if(heat<1000&&++heat==100)magmaNow();}else if(heat>0){boolean prior=magma();int cooling=1;boolean wet=precipitation();if(wet)++cooling;var pos=mob.blockPosition();if(mob.level().getBiome(pos).value().getBaseTemperature()<=.15f)++cooling;if(freezing(pos))++cooling;if(freezing(pos.below()))++cooling;heat=Math.max(0,heat-cooling);if(prior&&!magma())normal(wet);}if(magma()&&precipitation()&&--fizzDelay<=0){mob.playSound(SoundEvents.FIRE_EXTINGUISH,.7f,1.6f+(mob.getRandom().nextFloat()-mob.getRandom().nextFloat())*.4f);fizzDelay=mob.getRandom().nextInt(7)+2;}}}
}

package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Actual normal-fire contact replaces modern ignite-before-damage only for
 * sb. Other entities and retained soul fire keep their existing callback. */
public final class SourceSnowFireContact {
 public static boolean inside(Object rawState,Object rawLevel,Object rawPos,Object rawEntity){
  if(!(rawEntity instanceof SourceSnowGolem actor)||!((BlockState)rawState).is(Blocks.FIRE))return false;
  if(!((Level)rawLevel).isClientSide&&actor.beginSourceFireContact())SourceConstructRules.spentFire(new Host(actor));return true;
 }
 static void ignite(SourceSnowGolem actor,int seconds){SourceConstructRules.ignite(new Host(actor),seconds);}
 private record Host(SourceSnowGolem actor) implements SourceConstructRules.FireHost {
  public boolean removed(){return actor.isRemoved();}public boolean server(){return !actor.level().isClientSide;}
  public void contactDamage(int amount){actor.hurt(actor.damageSources().inFire(),amount);}
  public boolean extinguishBox(){boolean changed=false;var box=actor.getBoundingBox();var level=actor.level();for(int x=Mth.floor(box.minX);x<=Mth.ceil(box.maxX)-1;x++)for(int y=Mth.floor(box.minY);y<=Mth.ceil(box.maxY)-1;y++)for(int z=Mth.floor(box.minZ);z<=Mth.ceil(box.maxZ)-1;z++){var pos=new BlockPos(x,y,z);if(level.getBlockState(pos).is(Blocks.FIRE)){level.levelEvent(null,1009,pos,0);level.removeBlock(pos,false);changed=true;}}return changed;}
  public void quench(){SourceChemicalFx.entity(actor,SourceChemicalFx.ENTITY_STEAM);}public boolean wet(){return actor.wet();}
  public int fireTicks(){return actor.getRemainingFireTicks();}public void fireTicks(int value){actor.setRemainingFireTicks(value);}public void ignite(int seconds){actor.setSecondsOnFire(seconds);}public int protectedFireTicks(int value){return ProtectionEnchantment.getFireAfterDampener(actor,value);}
 }
 private SourceSnowFireContact(){}
}

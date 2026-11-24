package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;

/** R195 gt/gs: failed lava collection consumes one bucket without removing fluid; successful collection removes it. */
public final class VesselDispense extends DefaultDispenseItemBehavior {
 public static final VesselDispense INSTANCE=new VesselDispense();private boolean handled;
 public static void register(){for(String material:Content.METALS)DispenserBlock.registerBehavior(Content.get(material+"_bucket"),INSTANCE);DispenserBlock.registerBehavior(Content.ANCIENT_METAL_BUCKET.get(),INSTANCE);for(var item:new Item[]{Items.BUCKET,Items.WATER_BUCKET,Items.LAVA_BUCKET})DispenserBlock.registerBehavior(item,INSTANCE);BucketUse342.register();}
 protected ItemStack execute(BlockSource source,ItemStack stack){handled=false;var level=source.getLevel();var pos=source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));String contents=ClassicVessels.fluid(stack);
  if(contents.isEmpty()){var powder=BucketUse342.collectPowder(source,stack);if(powder!=null){handled=true;return powder;}var fluid=level.getFluidState(pos);if(!fluid.is(FluidTags.WATER)&&!fluid.is(FluidTags.LAVA))return super.execute(source,stack);handled=true;boolean lava=fluid.is(FluidTags.LAVA);if(lava&&level.random.nextFloat()<ClassicVessels.melting(stack)){stack.shrink(1);return stack;}var result=ClassicVessels.peer(stack,lava?"lava":"water");ClassicVessels.clearFluid(level,pos);stack.shrink(1);if(stack.isEmpty())return result;var dispenser=(DispenserBlockEntity)source.getEntity();if(dispenser.addItem(result)<0)super.execute(source,result);return stack;}
  if((contents.equals("water")||contents.equals("lava"))&&ClassicVessels.place(level,null,pos,contents.equals("water")?Fluids.WATER:Fluids.LAVA,false)){handled=true;return ClassicVessels.peer(stack,"");}return super.execute(source,stack);
 }
 protected void playAnimation(BlockSource source,Direction direction){if(!handled)super.playAnimation(source,direction);}
 private VesselDispense(){}
}

package dev.koikaster.mite;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
public final class Tools {
 public static class Sword extends SwordItem {
  public Sword(Tier t,int d,float s,Properties p){super(t,d,s,p);}
  public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand h){if(!ClassicToolTraits.canBlock(p.getItemInHand(h)))return net.minecraft.world.InteractionResultHolder.pass(p.getItemInHand(h));p.startUsingItem(h);return net.minecraft.world.InteractionResultHolder.consume(p.getItemInHand(h));}
  public int getUseDuration(ItemStack s){return ClassicToolTraits.canBlock(s)?72000:0;}
  public UseAnim getUseAnimation(ItemStack s){return ClassicToolTraits.canBlock(s)?UseAnim.BLOCK:UseAnim.NONE;}
 }
 public static class MetalArrow extends ArrowItem {
  public MetalArrow(int l,Properties p){super(p.stacksTo(16));}
  public MetalArrow(String material,Properties p){super(p.stacksTo(16));}
  public net.minecraft.world.entity.projectile.AbstractArrow createArrow(net.minecraft.world.level.Level l,ItemStack s,net.minecraft.world.entity.LivingEntity shooter){var arrow=super.createArrow(l,s,shooter);arrow.setBaseDamage(ClassicToolCatalogue.arrowDamage(ClassicToolCatalogue.material(s)));var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());if(id!=null)arrow.getPersistentData().putString("R195ArrowItem",id.toString());return arrow;}
 }
 public static class MetalBow extends BowItem {
  private final int level;public MetalBow(int l,Properties p){super(p);level=l;}
  public net.minecraft.world.entity.projectile.AbstractArrow customArrow(net.minecraft.world.entity.projectile.AbstractArrow a){return a;}
 }
 public static class Axe extends AxeItem {public Axe(Tier t,float d,float s,Properties p){super(t,d,s,p);}}
 public static class Hoe extends HoeItem {
  public Hoe(Tier t,int d,float s,Properties p){super(t,d,s,p);}
  public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return !ClassicToolWear.scythe(stack)&&super.canPerformAction(stack,action);}
  public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return ClassicToolWear.scythe(context.getItemInHand())?net.minecraft.world.InteractionResult.PASS:super.useOn(context);}
 }
 public static class Mattock extends ShovelItem {
  public Mattock(Tier t,int d,float s,Properties p){super(t,d,s,p);}
  public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.SHOVEL_DIG||action==net.minecraftforge.common.ToolActions.HOE_TILL;}
  public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
   var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
   if(EnchantmentEffects.denyTilling(context))return net.minecraft.world.InteractionResult.FAIL;
   if(context.getClickedFace()!=net.minecraft.core.Direction.UP||player==null||!player.mayUseItemAt(pos.above(),context.getClickedFace(),context.getItemInHand())||!level.getBlockState(pos.above()).isAir())return net.minecraft.world.InteractionResult.PASS;
   var state=level.getBlockState(pos);if(!state.is(net.minecraft.world.level.block.Blocks.DIRT)&&!state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))return net.minecraft.world.InteractionResult.PASS;
   var tilled=state.getToolModifiedState(context,net.minecraftforge.common.ToolActions.HOE_TILL,false);if(tilled==null)return net.minecraft.world.InteractionResult.PASS;
   var before=EnchantmentEffects.beforeTill(context);level.playSound(player,pos,net.minecraft.sounds.SoundEvents.HOE_TILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
   if(!level.isClientSide){level.setBlock(pos,tilled,11);level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos,net.minecraft.world.level.gameevent.GameEvent.Context.of(player,tilled));context.getItemInHand().hurtAndBreak(50,player,p->p.broadcastBreakEvent(context.getHand()));}
   return EnchantmentEffects.afterTill(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide),before,context);
  }
 }
}

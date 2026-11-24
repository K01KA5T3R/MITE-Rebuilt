package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicToolTraits {
 public static boolean canBlock(ItemStack stack){var p=ClassicToolWear.profile(stack);return p!=null&&ClassicToolCatalogue.canBlock(p.kind());}
 @SubscribeEvent public static void attributes(ItemAttributeModifierEvent e){if(e.getSlotType()!=EquipmentSlot.MAINHAND)return;var p=ClassicToolWear.profile(e.getItemStack());if(p==null)return;e.removeAttribute(Attributes.ATTACK_DAMAGE);e.addModifier(Attributes.ATTACK_DAMAGE,new AttributeModifier(java.util.UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf"),"R195 tool modifier",ClassicToolCatalogue.baseDamage(p.kind())+ClassicToolCatalogue.materialDamage(p.material()),AttributeModifier.Operation.ADDITION));}
 public static boolean shearEffective(BlockState state){Boolean original=OriginalMining1587.shearEffective(state);if(original!=null)return original;return state.is(BlockTags.WOOL)||state.is(BlockTags.WOOL_CARPETS)||state.getBlock() instanceof LeavesBlock||state.getBlock() instanceof BushBlock||state.getBlock() instanceof VineBlock||state.is(Blocks.COBWEB)||state.getBlock() instanceof TripWireBlock;}
 public static boolean cudgelEffective(BlockState s){return s.is(Blocks.MELON)||s.is(Blocks.PUMPKIN)||s.is(Blocks.CARVED_PUMPKIN)||s.is(Blocks.JACK_O_LANTERN)||s.is(Blocks.TNT)||s.is(Blocks.CLAY)||s.getBlock() instanceof AbstractGlassBlock||s.getBlock() instanceof StainedGlassPaneBlock||s.is(Blocks.GLASS_PANE)||s.is(Blocks.GLOWSTONE)||s.is(Blocks.SEA_LANTERN)||s.is(Blocks.ICE)||s.is(Blocks.PACKED_ICE)||s.is(Blocks.BLUE_ICE);}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void shearSheep(PlayerInteractEvent.EntityInteract e){var stack=e.getItemStack();if(!(stack.getItem() instanceof ShearsItem)||!ClassicToolCatalogue.sourceItem(stack)||!(e.getTarget() instanceof Sheep sheep)||sheep.isBaby()||sheep.isSheared())return;e.setCanceled(true);e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide));if(!e.getLevel().isClientSide){sheep.shear(SoundSource.PLAYERS);stack.hurtAndBreak(50,e.getEntity(),p->p.broadcastBreakEvent(e.getHand()));}}
 @SubscribeEvent public static void shearAttack(net.minecraftforge.event.entity.living.LivingHurtEvent e){if(e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.player.Player p&&p.getMainHandItem().getItem() instanceof ShearsItem&&ClassicToolCatalogue.sourceItem(p.getMainHandItem())&&!p.level().isClientSide)p.getMainHandItem().hurtAndBreak(200,p,v->v.broadcastBreakEvent(EquipmentSlot.MAINHAND));}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void shearBlock(PlayerInteractEvent.RightClickBlock e){var p=e.getEntity();var stack=e.getItemStack();if(!(stack.getItem() instanceof ShearsItem)||!ClassicToolCatalogue.sourceItem(stack))return;var level=e.getLevel();var pos=e.getPos();var state=level.getBlockState(pos);if(!shearEffective(state)||state.getBlock() instanceof CropBlock||state.getBlock().asItem()==Items.AIR||!p.mayUseItemAt(pos,e.getFace(),stack)||!level.mayInteract(p,pos))return;e.setCanceled(true);e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));if(!level.isClientSide){if(state.getBlock() instanceof SourceBlueberryBush)SourceBlueberryBush.dropRipe(state,level,pos);Block.popResource(level,pos,new ItemStack(state.getBlock()));level.removeBlock(pos,false);int wear=LegacyToolWear.blockDamage(ClassicMining.hardness(state,level,pos),1);if(wear>0)stack.hurtAndBreak(wear,p,v->v.broadcastBreakEvent(e.getHand()));level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,SoundSource.PLAYERS,1,1);}}
 private ClassicToolTraits(){}
}

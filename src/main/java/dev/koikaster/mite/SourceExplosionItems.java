package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** ss.handleExplosion/applyExplosionMotion, using the actual per-item RNG bridge. */
public final class SourceExplosionItems {
 private static final Set<String> METALS=Set.of("rusted_iron","copper","silver","gold","iron","mithril","adamantium","ancient_metal","bronze","steel","netherite");
 static void motion(SourceExplosion explosion,ItemEntity entity){
  var delta=entity.position().subtract(explosion.origin());double square=delta.x*delta.x+(delta.y*.5)*(delta.y*.5)+delta.z*delta.z;if(square<1)square=1;float force=(float)((double)explosion.blockSize()/Math.pow(square,.75));var random=((SourceFireRandom)entity).mite$fireRandom();
  double dx=delta.x*(double)force*(double).175f,dy=(double).4f*Math.sqrt((double)explosion.blockSize()/Math.pow(square,.75)),dz=delta.z*(double)force*(double).175f;
  dx*=(double)(.8f+random.nextFloat()*.4f);dy*=(double)(.8f+random.nextFloat()*.4f);dz*=(double)(.8f+random.nextFloat()*.4f);entity.setDeltaMovement(dx,dy,dz);entity.hasImpulse=true;
 }
 private static boolean immune(ItemStack stack){
  if(stack.getItem() instanceof BlockItem||stack.isDamageableItem())return false;var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null)return false;String name=id.getPath();
  return stack.is(Items.REDSTONE)||name.endsWith("_nugget")||name.endsWith("_ingot")||ClassicVessels.handled(stack)||stack.getItem() instanceof HorseArmorItem armor&&METALS.contains(ClassicToolCatalogue.material(stack));
 }
 private static void damageDurable(ItemEntity entity,float raw){
  var stack=entity.getItem();float scaled=raw*20f*5f;if(stack.getItem() instanceof ArmorItem)scaled*=(float)new ItemStack(Items.IRON_CHESTPLATE).getMaxDamage()/(float)new ItemStack(Items.IRON_SWORD).getMaxDamage();else if(ClassicToolWear.profile(stack)==null)scaled=raw;
  int damage=Math.round(Math.max(1,scaled));float chance=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,stack)/5f*.75f;
  if(chance>0){var random=new Random();for(int i=0,n=damage;i<n;i++)if(random.nextFloat()<chance)--damage;}
  if(damage>0){stack.setDamageValue(stack.getDamageValue()+damage);if(stack.getDamageValue()>=stack.getMaxDamage())entity.discard();else{entity.setItem(stack.copy());var saved=new net.minecraft.nbt.CompoundTag();entity.addAdditionalSaveData(saved);saved.putShort("Health",(short)Math.max(1,5*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));saved.put("Item",stack.save(new net.minecraft.nbt.CompoundTag()));entity.readAdditionalSaveData(saved);}}
 }
 static boolean handle(SourceExplosion explosion,ItemEntity entity){
  motion(explosion,entity);var stack=entity.getItem();if(immune(stack))return true;if(stack.isEmpty())return false;
  var delta=entity.position().subtract(explosion.origin());double square=delta.x*delta.x+(delta.y*.5)*(delta.y*.5)+delta.z*delta.z;float force=(float)((double)explosion.blockSize()/Math.pow(square,.75));
  if(stack.getItem() instanceof BlockItem block&&!(stack.getItem() instanceof BedItem)&&!(block.getBlock() instanceof DoorBlock)){
   var state=block.getBlock().defaultBlockState();if(force*2<SourceExplosionDrops.hardness(state,explosion.world()))return true;
   for(int i=0;i<stack.getCount();i++)SourceExplosionDrops.drop(explosion,entity.blockPosition(),state,false);
  }else{
   if(stack.isDamageableItem()){damageDurable(entity,force*64f);return true;}
   if(force<.2f)return true;
   if(stack.getItem() instanceof BedItem||stack.getItem() instanceof DoubleHighBlockItem&&((DoubleHighBlockItem)stack.getItem()).getBlock() instanceof DoorBlock){
    // ss source has a deliberate bed-state override even in its wood-door arm.
    SourceExplosionDrops.drop(explosion,entity.blockPosition(),Blocks.RED_BED.defaultBlockState(),false);entity.discard();return true;
   }
   Item shard=stack.is(Items.FLINT)?Content.FLINT_SHARD.get():stack.is(Items.EMERALD)?Content.EMERALD_SHARD.get():stack.is(Items.DIAMOND)?Content.DIAMOND_SHARD.get():null;
   if(shard!=null)for(int i=0;i<stack.getCount();i++){var piece=new ItemEntity(explosion.world(),entity.getX(),entity.getY(),entity.getZ(),new ItemStack(shard),0,0,0);motion(explosion,piece);explosion.world().addFreshEntity(piece);}
  }
  entity.discard();return true;
 }
 private SourceExplosionItems(){}
}

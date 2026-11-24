package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
public final class TorchBowShot extends ThrowableItemProjectile {
 private boolean resolved;private int age;
 public TorchBowShot(EntityType<? extends TorchBowShot> type,Level level){super(type,level);}
 public TorchBowShot(Level level,LivingEntity owner,ItemStack payload){super(TorchBowContent.SHOT.get(),owner,level);setItem(payload.copyWithCount(1));}
 @Override protected Item getDefaultItem(){return Items.TORCH;}
 @Override protected float getGravity(){return .05f;}
 @Override protected boolean canHitEntity(Entity entity){return false;}
 @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
 @Override public void tick(){if(!level().isClientSide&&resolved){discard();return;}if(!level().isClientSide&&++age>600){resolved=true;drop(position());discard();return;}super.tick();}
 @Override protected void onHit(HitResult hit){
  if(level().isClientSide||resolved||!(hit instanceof BlockHitResult block))return;resolved=true;
  try{super.onHit(hit);if(!place(block)){var normal=Vec3.atLowerCornerOf(block.getDirection().getNormal());drop(hit.getLocation().add(normal.scale(.1)));}}finally{discard();}
 }
 private boolean place(BlockHitResult hit){
  if(!(getOwner() instanceof Player player)||player.isSpectator())return false;
  var payload=getItem().copyWithCount(1);var context=new UseOnContext(level(),player,InteractionHand.MAIN_HAND,payload,hit);var placement=new BlockPlaceContext(context);BlockPos support=hit.getBlockPos(),target=placement.getClickedPos();
  if(!level().hasChunkAt(support)||!level().hasChunkAt(target)||level().isOutsideBuildHeight(target)||!level().getWorldBorder().isWithinBounds(target)||!level().mayInteract(player,support)||!level().mayInteract(player,target)||!player.mayUseItemAt(target,hit.getDirection(),payload))return false;
  // Retain Forge placement snapshots,cancellation and normal torch support checks.
  return payload.useOn(context).consumesAction();
 }
 private void drop(Vec3 pos){if(!getItem().isEmpty())level().addFreshEntity(new ItemEntity(level(),pos.x,pos.y,pos.z,getItem().copyWithCount(1)));}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("TorchResolved",resolved);tag.putInt("TorchAge",age);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);resolved=tag.getBoolean("TorchResolved");age=Math.max(0,tag.getInt("TorchAge"));}
}

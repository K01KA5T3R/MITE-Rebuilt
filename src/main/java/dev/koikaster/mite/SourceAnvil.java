package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.level.*;
import net.minecraft.nbt.NbtUtils;

/** TileEntityAnvil/amv deterministic damage, shared by saved block/carry/falling data. */
public final class SourceAnvil extends BlockEntity {
 private int damage;
 public Component name;
 public SourceAnvil(BlockPos pos,BlockState state){super(Content.ANVIL_ENTITY.get(),pos,state);}
 public String material(){return ((MetalAnvilBlock)getBlockState().getBlock()).material;}
 public int damage(){return damage;}
 public static int durability(String material){return durability(material,R196Gold.equipment(material));}
 private static int durability(String material,boolean r196){return (int)(1600*31*LegacyToolWear.durability(material,r196));}
 public static int stage(String material,int damage){return stage(damage,durability(material));}
 private static int stage(int damage,int maximum){float factor=(float)damage/maximum;return factor>=1?3:factor>=.8f?2:factor>=.5f?1:0;}
 public static int minimumStage(String material,int stage){int maximum=durability(material);int damage=(int)(maximum*(stage==2?.8f:stage==1?.5f:0));while(stage(damage,maximum)<stage)damage++;return damage;}
 public void setDamage(int value){damage=Math.max(0,value);setChanged();if(level!=null&&!level.isClientSide){int stage=stage(damage,durability(material(),WorldContentMode.r196(level)));if(stage==3){level.destroyBlock(worldPosition,true);return;}var state=getBlockState();if(state.getValue(MetalAnvilBlock.DAMAGE)!=stage)level.setBlock(worldPosition,state.setValue(MetalAnvilBlock.DAMAGE,stage),3);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
 public void addDamage(int amount){if(amount>0)setDamage(damage+amount);}
 public ItemStack carried(){var stack=new ItemStack(getBlockState().getBlock());stack.getOrCreateTag().put("BlockEntityTag",saveWithoutMetadata());if(name!=null)stack.setHoverName(name);return stack;}
 protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("damage",damage);if(name!=null)tag.putString("CustomName",Component.Serializer.toJson(name));}
 public void load(CompoundTag tag){super.load(tag);damage=Math.max(0,tag.getInt("damage"));name=tag.contains("CustomName")?Component.Serializer.fromJson(tag.getString("CustomName")):null;}
 public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
 public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 /** Separate entity keeps original carry NBT even if landing fails or entity times out. */
 public static final class Falling extends FallingBlockEntity {
  public Falling(EntityType<? extends FallingBlockEntity> type,Level level){super(type,level);}
  public static Falling start(Level level,BlockPos pos,BlockState state){var entity=new Falling(Content.ANVIL_FALLING.get(),level);var tag=new CompoundTag();tag.put("BlockState",NbtUtils.writeBlockState(state));tag.putInt("Time",1);tag.putBoolean("DropItem",true);tag.putBoolean("HurtEntities",true);tag.putFloat("FallHurtAmount",2f);tag.putInt("FallHurtMax",40);if(level.getBlockEntity(pos) instanceof SourceAnvil anvil)tag.put("TileEntityData",anvil.saveWithoutMetadata());entity.readAdditionalSaveData(tag);entity.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);entity.setStartPos(pos);entity.xo=entity.getX();entity.yo=entity.getY();entity.zo=entity.getZ();level.setBlock(pos,state.getFluidState().createLegacyBlock(),3);level.addFreshEntity(entity);return entity;}
  public ItemEntity spawnAtLocation(ItemLike item){var stack=new ItemStack(item);if(blockData!=null){stack.getOrCreateTag().put("BlockEntityTag",blockData.copy());if(blockData.contains("CustomName"))stack.setHoverName(Component.Serializer.fromJson(blockData.getString("CustomName")));}return super.spawnAtLocation(stack);}
  /** sr damages every intersecting entity with anvil damage and never randomly wears the anvil. */
  public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource cause){int steps=net.minecraft.util.Mth.ceil(distance-1f);if(!level().isClientSide&&steps>0){int amount=Math.min(net.minecraft.util.Mth.floor(steps*2f),40);for(var target:level().getEntities(this,getBoundingBox()))target.hurt(damageSources().anvil(this),amount);}return false;}
 }
}

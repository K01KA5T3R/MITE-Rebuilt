package dev.koikaster.mite;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import com.google.gson.*;
import net.minecraft.core.BlockPos;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.level.ServerLevel;import net.minecraft.sounds.SoundSource;import net.minecraft.tags.DamageTypeTags;import net.minecraft.world.damagesource.*;import net.minecraft.world.entity.ExperienceOrb;import net.minecraft.world.entity.item.ItemEntity;import net.minecraft.world.item.*;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.level.gameevent.GameEvent;import net.minecraft.world.phys.*;import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.registries.ForgeRegistries;

/** Original ss/xx cooked-food fire path, after the existing native damage guards. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceFoodFire463 {
 public record Pair(String raw,String cooked,int xpEach,boolean meat){}
 private static final Map<String,Pair> RAW=load();private static final Set<String> COOKED=cooked();
 private static final Map<ServerLevel,TreeMap<Long,Set<BlockPos>>> PENDING=new WeakHashMap<>();
 private static Map<String,Pair> load(){try(var in=SourceFoodFire463.class.getResourceAsStream("/data/mite_reforged/source_food_cooking463.json")){var result=new LinkedHashMap<String,Pair>();var root=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();for(var e:root.getAsJsonArray("pairs")){var r=e.getAsJsonObject();var p=new Pair(r.get("raw").getAsString(),r.get("cooked").getAsString(),r.get("xp_each").getAsInt(),r.get("meat").getAsBoolean());if(result.put(p.raw(),p)!=null)throw new IllegalStateException("Duplicate original cooking input");}return Map.copyOf(result);}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 private static Set<String> cooked(){var result=new HashSet<String>();for(var p:RAW.values())result.add(p.cooked());return Set.copyOf(result);}
 private static String key(ItemStack stack){return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
 private static void schedule(ServerLevel level,BlockPos origin){long tick=(level.getGameTime()/10L+1L)*10L;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){var pos=origin.offset(dx,0,dz);if(level.getBlockState(pos).is(Blocks.FIRE))PENDING.computeIfAbsent(level,k->new TreeMap<>()).computeIfAbsent(tick,k->new LinkedHashSet<>()).add(pos.immutable());}}
 public static boolean extinguish(ServerLevel level,BlockPos pos){if(!level.getBlockState(pos).is(Blocks.FIRE))return false;int count=0;for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos.getX()-.125,pos.getY(),pos.getZ()-.125,pos.getX()+1.125,pos.getY()+1,pos.getZ()+1.125)))if(RAW.containsKey(key(entity.getItem())))count+=entity.getItem().getCount();if(count<2)return false;if(count<=15){float chance=.01f*(float)Math.pow(2d,count);if(chance<1f&&level.random.nextFloat()>=chance)return false;}level.levelEvent(null,1009,pos,0);return level.removeBlock(pos,false);}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;var pending=PENDING.get(level);if(pending==null)return;long now=level.getGameTime();while(!pending.isEmpty()&&pending.firstKey()<=now){var positions=pending.pollFirstEntry().getValue();for(var pos:positions)extinguish(level,pos);}if(pending.isEmpty())PENDING.remove(level);}
 public static boolean hurt(ItemEntity entity,DamageSource source,float amount){
  if(!(entity.level() instanceof ServerLevel level)||entity.isRemoved())return false;var stack=entity.getItem();String id=key(stack);var pair=RAW.get(id);if(pair==null&&!COOKED.contains(id))return false;
  // R195 lava is a distinct immediate-destruction source, not nb.m fire.
  if(source.is(DamageTypes.LAVA)){stack.onDestroyed(entity,source);entity.discard();return true;}
  if(!source.is(DamageTypeTags.IS_FIRE))return false;
  if(pair!=null)schedule(level,entity.blockPosition());var state=(SourceFoodCookingState463)entity;float progress=state.mite$foodCooking()+amount*3f;state.mite$foodCooking(progress);entity.gameEvent(GameEvent.ENTITY_DAMAGE,source.getEntity());
  if(progress>=100f){if(pair==null){stack.onDestroyed(entity,source);entity.discard();return true;}int count=stack.getCount();var cooked=ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair.cooked()));if(cooked==null||cooked==Items.AIR)throw new IllegalStateException("Missing original cooking counterpart "+pair.cooked());
   if(pair.meat())level.playSound(null,entity.getX(),entity.getY(),entity.getZ(),SourceCookedWorm461.SIZZLE.get(),SoundSource.NEUTRAL,1f,1f);
   entity.setItem(new ItemStack(cooked,count));var saved=new CompoundTag();entity.addAdditionalSaveData(saved);saved.putShort("Health",(short)5);entity.readAdditionalSaveData(saved);if(pair.xpEach()>0)ExperienceOrb.award(level,new Vec3(entity.getX(),entity.getY()+.5,entity.getZ()),pair.xpEach()*count);
  }
  return true;
 }
 private SourceFoodFire463(){}
}

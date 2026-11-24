package dev.koikaster.mite;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** V063 native block/entity transport. No new UI, raid bar, loot rewards,
 * dimension access or player-adjustable configuration accompanies this event. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class BloodBannerEvents {
 public static final String MARK="MiteBloodBannerMob074";
 private static final Map<ServerLevel,Runtime> WORLDS=new ConcurrentHashMap<>();
 private static final class Runtime {final BloodBannerRules.State clock=new BloodBannerRules.State();final Queue<BlockPos> placed=new ConcurrentLinkedQueue<>();final Set<ChunkPos> chunks=ConcurrentHashMap.newKeySet();}
 private static Runtime state(ServerLevel l){return WORLDS.computeIfAbsent(l,k->new Runtime());}
 public static boolean ominous(ServerLevel level,BlockPos pos){
  return level.getBlockEntity(pos) instanceof BannerBlockEntity banner&&ominous(banner.getItem());
 }
 static boolean ominous(ItemStack stack){
  if(!stack.is(Items.WHITE_BANNER))return false;
  ListTag patterns=BannerBlockEntity.getItemPatterns(stack);
  return patterns!=null&&patterns.equals(BannerBlockEntity.getItemPatterns(Raid.getLeaderBannerInstance()));
 }
 private static void reconcile(ServerLevel level,BloodBannerSites sites,BlockPos pos){if(!level.hasChunkAt(pos))return;if(ominous(level,pos))sites.include(pos);else sites.remove(pos);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l&&l.dimension()==Level.OVERWORLD&&e.getPlacedBlock().getBlock() instanceof AbstractBannerBlock)state(l).placed.add(e.getPos().immutable());}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l&&l.dimension()==Level.OVERWORLD&&e.getState().getBlock() instanceof AbstractBannerBlock)state(l).placed.add(e.getPos().immutable());}
 @SubscribeEvent public static void loaded(ChunkEvent.Load e){if(e.getLevel() instanceof ServerLevel l&&l.dimension()==Level.OVERWORLD&&e.getChunk() instanceof LevelChunk)state(l).chunks.add(e.getChunk().getPos());}
 @SubscribeEvent public static void chunkGone(ChunkEvent.Unload e){if(e.getLevel() instanceof ServerLevel l){var runtime=WORLDS.get(l);if(runtime!=null)runtime.chunks.remove(e.getChunk().getPos());}}
 @SubscribeEvent public static void unload(LevelEvent.Unload e){if(e.getLevel() instanceof ServerLevel l)WORLDS.remove(l);}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){if(e.phase==TickEvent.Phase.END&&e.level instanceof ServerLevel l&&l.dimension()==Level.OVERWORLD)pulse(l);}
 static int pulse(ServerLevel level){var runtime=state(level);var sites=BloodBannerSites.get(level);BlockPos pos;while((pos=runtime.placed.poll())!=null)reconcile(level,sites,pos);for(var cp:List.copyOf(runtime.chunks)){var chunk=level.getChunkSource().getChunkNow(cp.x,cp.z);if(chunk==null)continue;runtime.chunks.remove(cp);for(var at:chunk.getBlockEntitiesPos())if(chunk.getBlockState(at).getBlock() instanceof AbstractBannerBlock)reconcile(level,sites,at);}return BloodBannerRules.pulse(runtime.clock,new World(level,sites));}
 private record World(ServerLevel level,BloodBannerSites sites) implements BloodBannerRules.Host {
  private BlockPos pos(BloodBannerRules.Anchor a){return new BlockPos(a.x(),a.y(),a.z());}public long time(){return level.getGameTime();}
  public boolean allowed(){return WorldContentMode.rebuilt(level)&&level.dimension()==Level.OVERWORLD&&level.getDifficulty()!=Difficulty.PEACEFUL&&level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)&&SourceSpecialSpawns.bloodUp(level);}
  public List<BloodBannerRules.Anchor> anchors(){return sites.anchors();}public boolean loaded(BloodBannerRules.Anchor a){return level.hasChunkAt(pos(a));}public boolean ominous(BloodBannerRules.Anchor a){return BloodBannerEvents.ominous(level,pos(a));}public void forget(BloodBannerRules.Anchor a){sites.remove(pos(a));}
  public boolean playerNear(BloodBannerRules.Anchor a){for(var p:level.players())if(!p.isSpectator()&&p.isAlive()&&p.distanceToSqr(a.x()+.5,a.y()+.5,a.z()+.5)<=4096)return true;return false;}
  public int nearby(BloodBannerRules.Anchor a){return level.getEntitiesOfClass(Raider.class,new AABB(pos(a)).inflate(32),LivingEntity::isAlive).size();}
  public int globalCount(){int count=0;for(var e:level.getAllEntities())if(e instanceof Raider r&&r.isAlive()&&r.getPersistentData().getBoolean(MARK))count++;return count;}
  public int players(){return (int)level.players().stream().filter(p->p.isAlive()&&!p.isSpectator()).count();}public int random(int bound){return level.random.nextInt(bound);}public boolean spawn(BloodBannerRules.Anchor a,BloodBannerRules.Kind kind){return spawnAt(level,pos(a),kind);}
 }
 static boolean spawnAt(ServerLevel level,BlockPos anchor,BloodBannerRules.Kind kind){
  EntityType<? extends Raider> type=switch(kind){case PILLAGER->EntityType.PILLAGER;case VINDICATOR->EntityType.VINDICATOR;case EVOKER->EntityType.EVOKER;case RAVAGER->EntityType.RAVAGER;};
  var mob=type.create(level);if(mob==null)return false;
  for(int attempt=0;attempt<12;attempt++){int dx=level.random.nextInt(49)-24,dz=level.random.nextInt(49)-24;if(Math.max(Math.abs(dx),Math.abs(dz))<8)continue;
   for(int v=0;v<=16;v++){int dy=v==0?0:(v+1)/2*(v%2==1?1:-1);var pos=anchor.offset(dx,dy,dz);if(pos.getY()<level.getMinBuildHeight()||pos.getY()+type.getDimensions().height>level.getMaxBuildHeight()||!level.hasChunkAt(pos))continue;if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;
    mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,level.random.nextFloat()*360,0);if(!level.getWorldBorder().isWithinBounds(mob.getBoundingBox())||!level.noCollision(mob)||level.containsAnyLiquid(mob.getBoundingBox()))continue;
    mob.getPersistentData().putBoolean(MARK,true);var finalized=SourceSpawnInitialization.finish(mob,level,MobSpawnType.EVENT,null).actor();if(!(finalized instanceof Raider raider)||raider.isSpawnCancelled())return false;
    // EVENT already bypasses native random captain assignment. Calling
    // setPatrolLeader(false) would itself set patrolling=true, so do not call it.
    raider.setCanJoinRaid(false);return level.addFreshEntity(raider);
   }
  }return false;
 }
 private BloodBannerEvents(){}
}

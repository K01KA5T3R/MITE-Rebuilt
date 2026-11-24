package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** rm/rj/ri door membership used by qc, independent of modern job-site POIs.
 * This is the door/navigation slice; village life is supplied by the separate rj consumer. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceVillageDoors extends SavedData {
 private static final String ID="mite_r195_village_doors";
 private static final Map<Villager,Integer> SCHEDULE=new WeakHashMap<>();
 public static final class Door {
  public final BlockPos pos;public final int insideX,insideZ;int seen,usage;boolean removed;
  Door(BlockPos pos,int dx,int dz,int seen){this.pos=pos.immutable();insideX=dx;insideZ=dz;this.seen=seen;}
  public int distance(BlockPos other){int x=other.getX()-pos.getX(),y=other.getY()-pos.getY(),z=other.getZ()-pos.getZ();return x*x+y*y+z*z;}
  public BlockPos inside(){return pos.offset(insideX,0,insideZ);}
  public int insideDistance(BlockPos other){int x=other.getX()-pos.getX()-insideX,y=other.getY()-pos.getY(),z=other.getZ()-pos.getZ()-insideZ;return x*x+y*y+z*z;}
 }
 public static final class Village {
  public final SourceVillageLife.State life=new SourceVillageLife.State();public final List<Door> doors=new ArrayList<>();public BlockPos center=BlockPos.ZERO;public int radius;
  void recalculate(){if(doors.isEmpty()){center=BlockPos.ZERO;radius=0;return;}int x=0,y=0,z=0;for(var d:doors){x+=d.pos.getX();y+=d.pos.getY();z+=d.pos.getZ();}center=new BlockPos(x/doors.size(),y/doors.size(),z/doors.size());int far=0;for(var d:doors)far=Math.max(far,d.distance(center));radius=Math.max(32,(int)Math.sqrt(far)+1);}
 }
 private final List<BlockPos> queue=new ArrayList<>();private final List<Village> villages=new ArrayList<>();private int clock;
 public static SourceVillageDoors get(ServerLevel level){return level.getDataStorage().computeIfAbsent(SourceVillageDoors::load,SourceVillageDoors::new,ID);}
 public void enqueue(BlockPos pos){if(queue.size()>64||queue.contains(pos))return;queue.add(pos.immutable());}
 public Village closest(BlockPos pos,int extra){Village result=null;float best=Float.MAX_VALUE;for(var v:villages){int x=v.center.getX()-pos.getX(),y=v.center.getY()-pos.getY(),z=v.center.getZ()-pos.getZ();float distance=x*x+y*y+z*z,limit=extra+v.radius;if(distance>=best||distance>limit*limit)continue;result=v;best=distance;}return result;}
 private static boolean wooden(ServerLevel level,BlockPos pos){return level.getBlockState(pos).is(BlockTags.WOODEN_DOORS);}
 private Door known(BlockPos pos,List<Door> pending){for(var d:pending)if(same(d,pos))return d;for(var v:villages){int x=v.center.getX()-pos.getX(),y=v.center.getY()-pos.getY(),z=v.center.getZ()-pos.getZ();if((float)(x*x+y*y+z*z)>(float)(v.radius*v.radius))continue;for(var d:v.doors)if(same(d,pos))return d;}return null;}
 private static boolean same(Door d,BlockPos pos){return d.pos.getX()==pos.getX()&&d.pos.getZ()==pos.getZ()&&Math.abs(d.pos.getY()-pos.getY())<=1;}
 private Door discover(ServerLevel level,BlockPos pos){var state=level.getBlockState(pos);boolean xAxis=state.getValue(DoorBlock.FACING).getAxis()==Direction.Axis.X;int sky=0;for(int i=-5;i<0;i++)if(level.canSeeSky(xAxis?pos.offset(i,0,0):pos.offset(0,0,i)))sky--;for(int i=1;i<=5;i++)if(level.canSeeSky(xAxis?pos.offset(i,0,0):pos.offset(0,0,i)))sky++;if(sky==0)return null;int inside=sky>0?-2:2;return new Door(pos,xAxis?inside:0,xAxis?0:inside,clock);}
 public void tick(ServerLevel level){
  clock++;
  for(var vi=villages.iterator();vi.hasNext();){var v=vi.next();boolean changed=false,resetUsage=level.getRandom().nextInt(50)==0;for(var it=v.doors.iterator();it.hasNext();){var d=it.next();if(resetUsage)d.usage=0;if(wooden(level,d.pos)&&Math.abs(clock-d.seen)<=1200)continue;d.removed=true;it.remove();changed=true;}if(changed)v.recalculate();SourceVillageLife.tick(level,v,clock);if(v.doors.isEmpty()){vi.remove();setDirty();}}
  var pending=new ArrayList<Door>();if(!queue.isEmpty()){var at=queue.remove(0);for(int x=at.getX()-16;x<at.getX()+16;x++)for(int y=at.getY()-4;y<at.getY()+4;y++)for(int z=at.getZ()-16;z<at.getZ()+16;z++){var pos=new BlockPos(x,y,z);if(!wooden(level,pos))continue;var d=known(pos,pending);if(d!=null)d.seen=clock;else{d=discover(level,pos);if(d!=null)pending.add(d);}}}
  for(var d:pending){Village joined=null;for(var v:villages){int range=32+v.radius;if(d.distance(v.center)<=range*range){joined=v;break;}}if(joined==null){joined=new Village();villages.add(joined);}joined.doors.add(d);joined.recalculate();joined.life.stable=d.seen;}
  if(!pending.isEmpty()||clock%400==0)setDirty();
 }
 public static SourceVillageDoors load(CompoundTag tag){var data=new SourceVillageDoors();data.clock=tag.getInt("Tick");var villages=tag.getList("Villages",Tag.TAG_COMPOUND);for(int i=0;i<villages.size();i++){var v=new Village();var doors=villages.getCompound(i).getList("Doors",Tag.TAG_COMPOUND);for(int j=0;j<doors.size();j++){var d=doors.getCompound(j);v.doors.add(new Door(new BlockPos(d.getInt("X"),d.getInt("Y"),d.getInt("Z")),d.getInt("IDX"),d.getInt("IDZ"),d.getInt("TS")));}v.recalculate();SourceVillageLife.load(v,villages.getCompound(i));data.villages.add(v);}return data;}
 @Override public CompoundTag save(CompoundTag tag){tag.putInt("Tick",clock);var list=new ListTag();for(var v:villages){var village=new CompoundTag();var doors=new ListTag();for(var d:v.doors){var n=new CompoundTag();n.putInt("X",d.pos.getX());n.putInt("Y",d.pos.getY());n.putInt("Z",d.pos.getZ());n.putInt("IDX",d.insideX);n.putInt("IDZ",d.insideZ);n.putInt("TS",d.seen);doors.add(n);}village.put("Doors",doors);SourceVillageLife.save(v,village);list.add(village);}tag.put("Villages",list);return tag;}
 @SubscribeEvent public static void villager(LivingEvent.LivingTickEvent event){if(!(event.getEntity() instanceof Villager v)||v instanceof SourceVillager||!(v.level() instanceof ServerLevel level)||v.isNoAi()||!v.isAlive())return;int remaining=SCHEDULE.getOrDefault(v,0)-1;if(remaining<=0){get(level).enqueue(v.blockPosition());remaining=70+v.getRandom().nextInt(50);SourceVillageLife.bind(v,get(level).closest(v.blockPosition(),32));}SCHEDULE.put(v,remaining);}
 @SubscribeEvent public static void world(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level&&!EmptyServerPause.paused(level.getServer()))get(level).tick(level);}
}

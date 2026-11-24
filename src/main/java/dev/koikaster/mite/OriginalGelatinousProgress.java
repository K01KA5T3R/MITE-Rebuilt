package dev.koikaster.mite;
import java.util.*;
import java.util.function.Consumer;

/** New039 native adapter wired, not runtime-verified. EntityGelatinousCube's fixed64/six-int
 * state, nearby extended progress, source counters and20-tick block update.
 * Native adapter must supply stable block/dimension tokens, original dissolve
 * periods and no-drop/steam operations; this class invents no material rules. */
public final class OriginalGelatinousProgress {
 public record Occupied(int blockToken,int x,int y,int z){}
 public interface World {
  int dimensionToken();int blockTokenAt(int x,int y,int z);
  double distanceSquaredToBlock(int x,int y,int z);
  int dissolvePeriodInSourceSeconds(Occupied block);
  void destroyWithoutDrop(Occupied block);void steam(Occupied block);
 }
 private int[] active=new int[384];
 private final List<int[]> extended=new ArrayList<>();
 private final Consumer<Boolean> feedingChanged;
 private int blockFeeding,itemFeeding;
 public OriginalGelatinousProgress(Consumer<Boolean> update){feedingChanged=Objects.requireNonNull(update);}
 public boolean feeding(){return blockFeeding>0||itemFeeding>0;}
 public int blockFeeding(){return blockFeeding;}
 public int itemFeeding(){return itemFeeding;}
 public void blockFeeding(int value){blockFeeding=Math.max(0,Math.min(20,value));feedingChanged.accept(feeding());}
 public void itemFeeding(int value){itemFeeding=Math.max(0,Math.min(20,value));feedingChanged.accept(feeding());}
 public void beforeAi(){if(blockFeeding>0)blockFeeding(blockFeeding-1);if(itemFeeding>0)itemFeeding(itemFeeding-1);}
 public int[] savedActive(){return active.clone();}
 public int[] savedExtended(){var data=new int[extended.size()*6];int at=0;for(var row:extended)for(int value:row)data[at++]=value;return data;}
 public void load(int[] fixed,int[] extra){active=Arrays.copyOf(fixed,Math.max(384,fixed.length));extended.clear();for(int index=0;index<extra.length/6;index++)extended.add(Arrays.copyOfRange(extra,index*6,index*6+6));}
 private boolean same(int offset,Occupied block,int dimension){return block!=null&&active[offset]==block.blockToken&&active[offset+1]==dimension&&active[offset+2]==block.x&&active[offset+3]==block.y&&active[offset+4]==block.z;}
 private int find(Occupied block,int dimension){for(int i=0;i<64;i++)if(same(i*6,block,dimension))return i;return -1;}
 private int add(Occupied block,int dimension,int progress){for(int i=0;i<64;i++){int at=i*6;if(active[at]!=0)continue;active[at]=block.blockToken;active[at+1]=dimension;active[at+2]=block.x;active[at+3]=block.y;active[at+4]=block.z;active[at+5]=progress;return i;}throw new IllegalStateException("Original64 dissolving-block slots exceeded");}
 private void remember(int offset){for(var row:extended){boolean matches=true;for(int field=0;field<5;field++)if(row[field]!=active[offset+field]){matches=false;break;}if(matches){row[5]=active[offset+5];return;}}extended.add(Arrays.copyOfRange(active,offset,offset+6));}
 private int resumed(Occupied block,int dimension){for(var row:extended)if(row[0]==block.blockToken&&row[1]==dimension&&row[2]==block.x&&row[3]==block.y&&row[4]==block.z)return row[5];return 0;}
 public void everySourceSecond(World world,Occupied[] occupied,int size){
  int dimension=world.dimensionToken();extended.removeIf(row->dimension!=row[1]||world.distanceSquaredToBlock(row[2],row[3],row[4])>=256||world.blockTokenAt(row[2],row[3],row[4])!=row[0]);
  for(int i=0;i<64;i++){int at=i*6;if(active[at]==0)continue;boolean found=false;for(var block:occupied)if(same(at,block,dimension)){found=true;break;}if(!found){remember(at);active[at]=0;}}
  boolean clearFeeding=true;
  for(var block:occupied){if(block==null)continue;
   if(world.blockTokenAt(block.x,block.y,block.z)!=block.blockToken){int index=find(block,dimension);if(index>=0)active[index*6]=0;continue;}
   int period=world.dissolvePeriodInSourceSeconds(block);if(period<1)continue;int index=find(block,dimension);if(index<0)index=add(block,dimension,resumed(block,dimension));int at=index*6;active[at+5]+=size;
   if(active[at+5]>=period){world.destroyWithoutDrop(block);active[at]=0;continue;}world.steam(block);clearFeeding=false;
  }blockFeeding(clearFeeding?0:20);
 }
}

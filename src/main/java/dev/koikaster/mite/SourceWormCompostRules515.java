package dev.koikaster.mite;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** ary's per-live-worm, uniform-slot transaction. A fork prevents partial remainder loss. */
public final class SourceWormCompostRules515 {
 public record Food(float value,List<String> remains){public Food{remains=List.copyOf(remains);}}
 public interface Inventory {
  int size();String item(int slot);int count(int slot);int limit(int slot);int limit(String item);
  void removeOne(int slot);void putOne(int slot,String item);void growOne(int slot);
  Inventory fork();void apply(Inventory plan);
 }
 public interface Foods {Food food(String item);}
 public record Result(float compost,boolean changed,int consumed){}
 public static final String WORM="mite_reforged:worm",MANURE="mite_reforged:manure";
 private static int destination(Inventory inv,String item,boolean existingOnly){
  for(int i=0;i<inv.size();i++)if(item.equals(inv.item(i))&&inv.count(i)<inv.limit(i))return i;
  if(!existingOnly)for(int i=0;i<inv.size();i++)if(inv.count(i)==0)return i;
  return -1;
 }
 private static boolean insertOne(Inventory inv,String item){int slot=destination(inv,item,false);if(slot<0||inv.limit(item)<1)return false;if(inv.count(slot)==0)inv.putOne(slot,item);else inv.growOne(slot);return true;}
 private static float convert(Inventory inv,float compost){while(compost>=1&&insertOne(inv,MANURE))compost-=1;return compost;}
 public static Result tick(Inventory inv,Foods foods,float saved,IntUnaryOperator random){
  float compost=Float.isFinite(saved)&&saved>=0?saved:0;boolean changed=Float.floatToIntBits(saved)!=Float.floatToIntBits(compost);int worms=0,consumed=0;
  for(int i=0;i<inv.size();i++)if(WORM.equals(inv.item(i)))worms+=inv.count(i);
  if(worms<1)return new Result(compost,changed,0);
  if(compost>=1){float remaining=convert(inv,compost);return new Result(remaining,changed||remaining!=compost,0);}
  for(int attempt=0;attempt<worms;attempt++){
   var slots=new ArrayList<Integer>();for(int i=0;i<inv.size();i++){if(inv.count(i)<1)continue;var food=foods.food(inv.item(i));if(food!=null&&Float.isFinite(food.value())&&food.value()>0)slots.add(i);}
   if(slots.isEmpty())break;
   int source=slots.get(random.applyAsInt(slots.size()));var food=foods.food(inv.item(source));int chance=(int)(100f*food.value());if(chance<1||random.applyAsInt(chance)!=0)continue;
   int required=destination(inv,MANURE,true)<0?1:0,empty=0;
   for(var item:food.remains())if(destination(inv,item,true)<0)required++;
   if(required>0&&inv.count(source)==1)required--;
   for(int i=0;i<inv.size();i++)if(inv.count(i)==0)empty++;
   if(empty<required)continue;
   var plan=inv.fork();plan.removeOne(source);boolean fits=true;for(var item:food.remains())if(!insertOne(plan,item)){fits=false;break;}
   // The original reserves a manure destination even below one complete unit.
   // Test it on another fork, preserving source remainder-before-manure order.
   if(!fits||!insertOne(plan.fork(),MANURE))continue;
   float next=convert(plan,compost+food.value());inv.apply(plan);compost=next;changed=true;consumed++;
  }
  return new Result(compost,changed,consumed);
 }
 private SourceWormCompostRules515(){}
}

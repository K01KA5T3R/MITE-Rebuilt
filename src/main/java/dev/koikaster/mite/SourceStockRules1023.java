package dev.koikaster.mite;
import java.util.Objects;
/** All original similarity overrides plus ud.getSimilarityOfItems. Native identities/subtypes are explicit inputs. */
public final class SourceStockRules1023 {
 public enum Special {NONE,STICK,BONE,GLASS_BOTTLE}
 public enum Block {NONE,GRAVEL,OTHER_GRAVEL}
 public record Item(String identity,String type,boolean subtypes,String material,String contents,String emptyVessel,Special special,Block block){
  public Item{Objects.requireNonNull(identity);Objects.requireNonNull(type);Objects.requireNonNull(special);Objects.requireNonNull(block);SourceStockTypes1023.profile(type);}
 }
 private static boolean same(Item a,Item b){return a.identity.equals(b.identity);}
 private static int base(Item a,Item b){
  if(b==null)return 0;if(same(a,b))return 100;var profile=SourceStockTypes1023.profile(a.type);
  if(a.type.equals(b.type)&&profile.sameClass())return 99;
  var fallback=profile.fallback();for(int i=0;i<fallback.size();i++)if(fallback.get(i).equals(b.type))return 100-(i+2);return 0;
 }
 private static int vessel(Item a,Item b){if(b!=null&&SourceStockTypes1023.is(b.type,"ItemVessel")&&Objects.equals(b.emptyVessel,a.emptyVessel))return 1;return base(a,b);}
 public static int raw(Item a,Item b){
  String owner=SourceStockTypes1023.profile(a.type).rawOwner();
  return switch(owner){
   case "ItemBowl" -> b!=null&&SourceStockTypes1023.is(b.type,"ItemBowl")&&(b.contents==null||a.contents==null)?2:vessel(a,b);
   case "ItemVessel" -> vessel(a,b);
   case "wr" -> {
    if(b!=null&&SourceStockTypes1023.is(b.type,"wr")){
     if(Objects.equals(b.contents,a.contents))yield 99;
     if(b.contents==null||a.contents==null)yield 100-(Objects.equals(a.material,b.material)?2:3);
    }
    yield vessel(a,b);
   }
   case "ItemCudgel" -> b!=null&&(b.special==Special.STICK||b.special==Special.BONE)?1:base(a,b);
   case "yp" -> b!=null&&b.special==Special.GLASS_BOTTLE?1:base(a,b);
   case "zh" -> a.block==Block.GRAVEL&&b!=null&&SourceStockTypes1023.is(b.type,"ItemShovel")?1:0;
   case "yc" -> base(a,b);
   default -> throw new AssertionError(owner);
  };
 }
 public static int score(Item previous,int previousSubtype,Item candidate,int count,int candidateSubtype){
  if(candidate==null||count==0)return 0;
  if(SourceStockTypes1023.is(previous.type,"zh")&&previous.block!=Block.NONE&&SourceStockTypes1023.is(candidate.type,"ItemShovel"))return 1;
  if(previous.subtypes&&previousSubtype!=candidateSubtype)return 0;
  return same(previous,candidate)?100:Math.max(0,Math.min(99,raw(previous,candidate)));
 }
 private SourceStockRules1023(){}
}

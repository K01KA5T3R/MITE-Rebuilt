package dev.koikaster.mite;
import java.util.List;

/** ain.b/c/d + aep creature-list routing. Input order is the source HashMap
 * order supplied by the modern world's persisted structure-start catalogue. */
public final class OriginalFortressSelection {
 public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
  boolean xz(int x,int z){return x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
  boolean xyz(int x,int y,int z){return xz(x,z)&&y>=minY&&y<=maxY;}
 }
 public record Start(boolean valid,Bounds bounds,List<Bounds> pieces){}
 public record Row(String id,int weight,int min,int max){}
 public static final List<Row> ROWS=List.of(new Row("minecraft:blaze",100,2,3),new Row("minecraft:zombified_piglin",50,4,4),new Row("minecraft:skeleton",100,4,4),new Row("minecraft:magma_cube",30,4,4));
 public static boolean piece(Iterable<Start> starts,int x,int y,int z){
  for(var start:starts){if(!start.valid||!start.bounds.xz(x,z))continue;for(var piece:start.pieces)if(piece.xyz(x,y,z))return true;}return false;
 }
 public static boolean outer(Iterable<Start> starts,int x,int z){
  for(var start:starts){if(!start.valid)continue;return start.bounds.xz(x,z);}return false;
 }
 public static boolean fortressPool(boolean hostile,Iterable<Start> starts,int x,int y,int z,boolean netherBrickBelow){return hostile&&(piece(starts,x,y,z)||outer(starts,x,z)&&netherBrickBelow);}
 private OriginalFortressSelection(){}
}

package dev.koikaster.mite;
/** aof.a(World,x,y,z,Entity,float), with the original callback/query order. */
public final class SourceFarmFall619 {
 public interface Actor{boolean gelatinous();boolean player();}
 public interface World{boolean client();float nextFloat();void tramplePlant(int x,int y,int z,Actor actor);int metadata(int x,int y,int z);boolean mobGriefing();void dirt(int x,int y,int z);}
 public static void fall(World world,int x,int y,int z,Actor actor,float distance){
  if(actor.gelatinous())return;
  if(!world.client()&&world.nextFloat()<distance-.5f){
   world.tramplePlant(x,y+1,z,actor);
   int metadata=world.metadata(x,y,z);if((metadata&7)>0||(metadata&8)!=0)return;
   if(!actor.player()&&!world.mobGriefing())return;
   world.dirt(x,y,z);
  }
 }
 public static boolean drops(int kind,boolean cow,boolean pig,boolean sheep){return switch(kind){case 1->!cow&&!pig&&!sheep;case 2,3->false;default->!cow;};}
 private SourceFarmFall619(){}
}

package dev.koikaster.mite;
/** rs.a(oi), including og's first Gaussian and the source float constants.
 * Entity registries, Forge callbacks and the human's modern breeding choice
 * are supplied by the native owner rather than altering this random sequence. */
public final class OriginalHorseSpawn {
 public interface Random {int nextInt(int bound);double nextDouble();double nextGaussian();}
 public record Group(int kind,int variant){}
 public record Result(Group group,int kind,int variant,boolean newborn,double followBonus,double health,double speed,double jump){}
 public static Result initialize(Random random,Group group){
  double follow=random.nextGaussian()*.05;int kind,variant=0;Group result=group;
  if(group!=null){kind=group.kind;variant=group.variant&255|random.nextInt(5)<<8;}
  else{if(random.nextInt(10)==0)kind=1;else{int color=random.nextInt(7),mark=random.nextInt(5);kind=0;variant=color|mark<<8;}result=new Group(kind,variant);}
  boolean newborn=random.nextInt(5)==0;double health,speed,jump;
  if(kind!=4&&kind!=3){health=15f+(float)random.nextInt(8)+(float)random.nextInt(9);speed=kind==0?((double).45f+random.nextDouble()*.3+random.nextDouble()*.3+random.nextDouble()*.3)*.25:(double).175f;}
  else{health=15;speed=(double).2f;}
  jump=kind!=2&&kind!=1?(double).4f+random.nextDouble()*.2+random.nextDouble()*.2+random.nextDouble()*.2:.5;
  return new Result(result,kind,variant,newborn,follow,health,speed,jump);
 }
 private OriginalHorseSpawn(){}
}

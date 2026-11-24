package dev.koikaster.mite;
/** Exact of.calcFallDamage/of.b and uf.b ordering; world, sound, stats and damage APIs are transports. */
public final class SourceFallRules1151 {
 public interface View<S,B> {
  Integer jumpAmplifier();S resting(float allowance);int blockId(S support);B block(int id);int metadata(S support,int dy);float cushioning(B block,int metadata);boolean liquid(B block);B below(S support);int aboveId(S support);boolean arachnid();boolean woodSpider();
 }
 public interface Impact<S,B> extends View<S,B> {
  void parent(float distance);boolean client();boolean player();boolean fallImmune();void fallenCentimeters(int amount);boolean glass(S support);boolean snowBlock(S support);boolean passableBelow(S support);void destroy(S support);float maxHealth();void massive();void damageSound(String name);void attack(float amount);String stepName(S support);void stepSound(String name,float volume,float pitch);
 }
 private record Evaluation<S>(S support,float before,float damage,boolean eligible){}
 private static <S,B> Evaluation<S> evaluate(View<S,B> h,float distance){
  Integer jump=h.jumpAmplifier();float boost=jump!=null?(float)(jump+1):0f;float before=distance-2.5f-boost,damage=before*2f;S support=h.resting(.1f);boolean eligible=damage>=1f;
  if(eligible){int id=0;if(support!=null&&(id=h.blockId(support))>0){B block=h.block(id);float cushion=h.cushioning(block,h.metadata(support,0));if(h.liquid(block)){block=h.below(support);cushion=block==null?cushion+1f:cushion+h.cushioning(block,h.metadata(support,-1));}block=h.block(h.aboveId(support));if(block!=null)cushion+=h.cushioning(block,h.metadata(support,1));if(cushion>1f)cushion=1f;damage-=cushion*10f;damage*=1f-cushion;}if(h.arachnid())damage*=h.woodSpider()?.25f:.5f;}
  return new Evaluation<>(support,before,damage,eligible);
 }
 public static <S,B> void calculate(View<S,B> h,float distance,float[] damages){damages[0]=0;damages[1]=0;if(distance<.5f)return;var e=evaluate(h,distance);if(e.eligible){damages[0]=e.before;damages[1]=e.damage;}}
 public static <S,B> void impact(Impact<S,B> h,float distance){
  h.parent(distance);if(h.client())return;if(distance<.5f)return;var e=evaluate(h,distance);float damage=e.damage;S support=e.support;
  if(e.eligible){if(e.before>4f&&!h.client()&&support!=null&&(h.glass(support)||h.snowBlock(support)&&h.passableBelow(support))){h.destroy(support);if(damage>5f)damage=5f;}if(damage>=1f){if(damage>=h.maxHealth()*.5f)h.massive();h.damageSound(damage>4f?"damage.fallbig":"damage.fallsmall");h.attack(damage);}}
  if(support!=null){String step=h.stepName(support);if(step!=null)h.stepSound("step."+step,h.player()?Math.min(distance*.1f,1f):Math.min(distance*.2f,2f),1f);}
 }
 public static <S,B> void playerImpact(Impact<S,B> h,float distance){if(!h.fallImmune()){if(distance>=2f)h.fallenCentimeters((int)Math.round((double)distance*100d));impact(h,distance);}}
 private SourceFallRules1151(){}
}

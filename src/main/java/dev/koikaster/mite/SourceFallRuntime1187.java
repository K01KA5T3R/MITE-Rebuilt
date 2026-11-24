package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
/** Native source impact owner. Player's existing mayfly/stat wrapper remains native. */
public final class SourceFallRuntime1187 {
 private static boolean scoped(Entity actor){var key=BuiltInRegistries.ENTITY_TYPE.getKey(actor.getType());return actor instanceof Player||key.getNamespace().equals("minecraft")||key.getNamespace().equals(MiteReforged.ID);}
 public static boolean handles(Object raw){return raw instanceof LivingEntity actor&&scoped(actor)&&!actor.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);}
 public static boolean impact(Object raw,float distance,float multiplier,Object rawSource){
  var actor=(LivingEntity)raw;var source=(DamageSource)rawSource;
  float[] event=ForgeHooks.onLivingFall(actor,distance,multiplier);if(event==null)return false;
  var host=new Impact(actor,source,event[1]);
  if(event[1]==1f)SourceFallRules1151.impact(host,event[0]);else scaledImpact(host,event[0],event[1]);
  return host.attacked||host.passengerDamaged;
 }
 /** Forge's optional multiplier has no source equivalent. It scales the source
  * float damage after cushioning, before the original fragile-floor cap. The
  * unmodified source kernel owns every ordinary multiplier-one call. */
 private static void scaledImpact(Impact h,float distance,float multiplier){
  h.parent(distance);if(h.client()||distance<.5f)return;
  float[] values=new float[2];SourceFallRules1151.calculate(h,distance,values);var support=h.resting;float damage=values[1]*multiplier;
  if(values[0]*2f>=1f){
   if(values[0]>4f&&support!=null&&(h.glass(support)||h.snowBlock(support)&&h.passableBelow(support))){h.destroy(support);if(damage>5f)damage=5f;}
   if(damage>=1f){if(damage>=h.maxHealth()*.5f)h.massive();h.damageSound(damage>4f?"damage.fallbig":"damage.fallsmall");h.attack(damage);}
  }
  if(support!=null){String step=h.stepName(support);if(step!=null)h.stepSound("step."+step,h.player()?Math.min(distance*.1f,1f):Math.min(distance*.2f,2f),1f);}
 }
 public static boolean blockFall(Object raw,Object rawLevel,float distance){
  if(!(raw instanceof Entity actor)||!scoped(actor))return false;
  actor.causeFallDamage(distance,1f,((Level)rawLevel).damageSources().fall());return true;
 }
 public static boolean stopBedBounce(Object raw){
  if(!(raw instanceof Entity actor)||!scoped(actor))return false;
  var motion=actor.getDeltaMovement();actor.setDeltaMovement(motion.x,0d,motion.z);return true;
 }
 private static final class Impact implements SourceFallRules1151.Impact<SourceFallView1182.Support,Block> {
  final LivingEntity actor;final DamageSource source;final float multiplier;final SourceFallView1182 view;
  boolean attacked,passengerDamaged;SourceFallView1182.Support resting;
  Impact(LivingEntity a,DamageSource s,float m){actor=a;source=s;multiplier=m;view=new SourceFallView1182(a);}
  public Integer jumpAmplifier(){return view.jumpAmplifier();}public SourceFallView1182.Support resting(float allowance){return resting=view.resting(allowance);}
  public int blockId(SourceFallView1182.Support s){return view.blockId(s);}public Block block(int id){return view.block(id);}public int metadata(SourceFallView1182.Support s,int dy){return view.metadata(s,dy);}public float cushioning(Block b,int m){return view.cushioning(b,m);}public boolean liquid(Block b){return view.liquid(b);}public Block below(SourceFallView1182.Support s){return view.below(s);}public int aboveId(SourceFallView1182.Support s){return view.aboveId(s);}public boolean arachnid(){return view.arachnid();}public boolean woodSpider(){return view.woodSpider();}
  public void parent(float distance){for(var passenger:actor.getPassengers())passengerDamaged|=passenger.causeFallDamage(distance,multiplier,source);}
  public boolean client(){return actor.level().isClientSide;}public boolean player(){return actor instanceof Player;}public boolean fallImmune(){return actor instanceof Player p&&p.getAbilities().mayfly;}public void fallenCentimeters(int n){((Player)actor).awardStat(net.minecraft.stats.Stats.FALL_ONE_CM,n);}
  public boolean glass(SourceFallView1182.Support s){return s.state().is(Blocks.GLASS);}public boolean snowBlock(SourceFallView1182.Support s){return s.state().is(Blocks.SNOW_BLOCK);}
  public boolean passableBelow(SourceFallView1182.Support s){var pos=s.pos().below();var level=actor.level();return level.isOutsideBuildHeight(pos)||level.hasChunkAt(pos)&&!SourcePhaseNativeWorld.solid(level.getBlockState(pos));}
  public void destroy(SourceFallView1182.Support s){
   if(!(actor.level() instanceof net.minecraft.server.level.ServerLevel level))return;
   var state=level.getBlockState(s.pos());if(state.getBlock()!=s.state().getBlock())return;
   level.levelEvent(2001,s.pos(),Block.getId(state));
   if(glass(s))SourceSoilDrops258.emit(level,s.pos(),state,new ItemStack(Content.GLASS_SHARD.get()),6,1f,0);
   else if(snowBlock(s))SourceSoilDrops258.emit(level,s.pos(),state,new ItemStack(Items.SNOWBALL),8,1f,0);
   level.setBlock(s.pos(),Blocks.AIR.defaultBlockState(),3);level.gameEvent(GameEvent.BLOCK_DESTROY,s.pos(),GameEvent.Context.of(actor,state));
  }
  public float maxHealth(){return actor.getMaxHealth();}public void massive(){((SourceFallAccess1187)(Object)actor).mite$massiveFall1187(true);}
  public void damageSound(String name){sound(name.equals("damage.fallbig")?SoundEvents.GENERIC_BIG_FALL:SoundEvents.GENERIC_SMALL_FALL,1f,1f);}
  public void attack(float amount){attacked=true;actor.hurt(source,amount);}
  public String stepName(SourceFallView1182.Support s){return s.state().getSoundType(actor.level(),s.pos(),actor).getStepSound().getLocation().toString();}
  public void stepSound(String name,float volume,float pitch){var event=BuiltInRegistries.SOUND_EVENT.get(new net.minecraft.resources.ResourceLocation(name.substring(5)));if(event!=null)sound(event,volume,pitch);}
  private void sound(SoundEvent event,float volume,float pitch){var access=(SourceFallAccess1187)(Object)actor;actor.playSound(event,access.mite$fallSoundVolume1187()*volume,access.mite$fallSoundPitch1187()*pitch);}
 }
 private SourceFallRuntime1187(){}
}

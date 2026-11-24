package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
/** bcw block/entity FX, nn.spawnParticles. Old summoned-particle owner is unchanged. */
final class SourceChemicalClientFx {
 private static final RandomSource PITCH=RandomSource.create();
 static void accept(SourceChemicalFx packet){var level=Minecraft.getInstance().level;if(level==null)return;
  if(packet.entity()<0){var pos=BlockPos.of(packet.block());int count=packet.kind()==SourceChemicalFx.BLOCK_SMOKE_STEAM?6:8;if(packet.kind()==SourceChemicalFx.BLOCK_SMOKE_STEAM)blockParticles(pos,ParticleTypes.LARGE_SMOKE,6);blockParticles(pos,ParticleTypes.POOF,count);if(packet.kind()!=SourceChemicalFx.BLOCK_STEAM_PARTICLES)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.5f,2.6f+(PITCH.nextFloat()-PITCH.nextFloat())*.8f,false);return;}
  var entity=level.getEntity(packet.entity());if(entity==null)return;
  if(packet.kind()==SourceChemicalFx.ENTITY_SMOKE_STEAM){particles(entity,ParticleTypes.LARGE_SMOKE,6);particles(entity,ParticleTypes.POOF,6);level.playLocalSound(entity.getX(),entity.getY()+entity.getBbHeight()/2,entity.getZ(),SoundEvents.FIRE_EXTINGUISH,SoundSource.HOSTILE,.5f,2.6f+(PITCH.nextFloat()-PITCH.nextFloat())*.8f,false);}
  else{particles(entity,ParticleTypes.POOF,packet.kind()==SourceChemicalFx.ENTITY_STEAM&&entity.isInWater()?10:5);if(packet.kind()==SourceChemicalFx.ENTITY_STEAM)level.playLocalSound(entity.getX(),entity.getY()+entity.getBbHeight()/2,entity.getZ(),SoundEvents.FIRE_EXTINGUISH,SoundSource.HOSTILE,.7f,1.6f+(PITCH.nextFloat()-PITCH.nextFloat())*.4f,false);}
 }
 private static void blockParticles(BlockPos pos,ParticleOptions kind,int count){var level=Minecraft.getInstance().level;if(level==null)return;for(int i=0;i<count;i++)level.addParticle(kind,pos.getX()+Math.random(),pos.getY()+Math.random(),pos.getZ()+Math.random(),0,0,0);}
 static void particles(Entity entity,ParticleOptions kind,int count){var level=Minecraft.getInstance().level;if(level==null)return;var random=((SourceFireRandom)entity).mite$fireRandom();for(int i=0;i<count;i++){double dx=random.nextGaussian()*.02f,dy=random.nextGaussian()*.02f,dz=random.nextGaussian()*.02f;level.addParticle(kind,entity.getX()+random.nextFloat()*entity.getBbWidth()*2-entity.getBbWidth(),entity.getY()+random.nextFloat()*entity.getBbHeight(),entity.getZ()+random.nextFloat()*entity.getBbWidth()*2-entity.getBbWidth(),dx,dy,dz);}}
 static void lava(Entity entity){particles(entity,ParticleTypes.POOF,5);particles(entity,ParticleTypes.LARGE_SMOKE,5);}
 private SourceChemicalClientFx(){}
}

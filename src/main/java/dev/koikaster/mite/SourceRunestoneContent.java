package dev.koikaster.mite;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public final class SourceRunestoneContent {
 public static final RegistryObject<SourceRunestoneBlock> MITHRIL=block("mithril_runestone",false),ADAMANTIUM=block("adamantium_runestone",true);
 public static final RegistryObject<SoundEvent> SOUND=SourceCreatureSounds.SOUNDS.register("portal.runegate",()->SoundEvent.createVariableRangeEvent(Content.id("portal.runegate")));
 public static final RegistryObject<SimpleParticleType> UNDERWORLD=SourcePhaseParticles.PARTICLES.register("source_portal_underworld",()->new SimpleParticleType(false));
 public static final RegistryObject<SimpleParticleType> NETHER=SourcePhaseParticles.PARTICLES.register("source_portal_nether",()->new SimpleParticleType(false));
 private static RegistryObject<SourceRunestoneBlock> block(String name,boolean adamantium){
  var block=Content.BLOCKS.register(name,()->new SourceRunestoneBlock(adamantium));
  Content.item(name,()->new SourceRunestoneItem(block.get()));return block;
 }
 public static void register(){}
 private SourceRunestoneContent(){}
}

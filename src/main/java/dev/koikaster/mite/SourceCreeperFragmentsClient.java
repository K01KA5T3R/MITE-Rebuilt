package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.*;
import net.minecraft.world.item.ItemStack;
final class SourceCreeperFragmentsClient {
 static void accept(SourceCreeperFragments packet){var world=Minecraft.getInstance().level;if(world==null)return;var actor=world.getEntity(packet.entity());if(actor==null)return;var random=((SourceFireRandom)actor).mite$fireRandom();var particle=new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(packet.infernal()?SourceCreeperFragments.INFERNAL.get():SourceCreeperFragments.NORMAL.get()));for(int i=0;i<40;i++){float x=random.nextFloat()*.2f-.1f,y=random.nextFloat()*.2f-.1f,z=random.nextFloat()*.2f-.1f;while(x*x+y*y+z*z<.25f){x*=2;y*=2;z*=2;}while(x*x+y*y+z*z<1){x*=1.1f;y*=1.1f;z*=1.1f;}world.addParticle(particle,actor.getX()+(double)(random.nextFloat()*actor.getBbWidth())-(double)(actor.getBbWidth()/2f),actor.getY()+(double)(random.nextFloat()*actor.getBbHeight()),actor.getZ()+(double)(random.nextFloat()*actor.getBbWidth())-(double)(actor.getBbWidth()/2f),x,y,z);}}
 private SourceCreeperFragmentsClient(){}
}

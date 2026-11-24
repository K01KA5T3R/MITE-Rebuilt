package dev.koikaster.mite;
import com.mojang.blaze3d.systems.RenderSystem;import com.mojang.blaze3d.vertex.*;import net.minecraft.client.*;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.client.particle.*;import net.minecraft.client.renderer.ShaderInstance;import net.minecraft.client.renderer.texture.*;import net.minecraft.world.level.Level;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.RegisterShadersEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
/** Source sacred sprite100 on the unchanged R195 atlas; no alternate hand-drawn asset. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSacredParticles1120 {
 private static SourceSacredRules1120.Table table;private static int next;
 private static ShaderInstance shader;
 @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws java.io.IOException{e.registerShader(new ShaderInstance(e.getResourceProvider(),Content.id("source_sacred"),DefaultVertexFormat.PARTICLE),value->shader=value);}
 private static final ParticleRenderType TYPE=new ParticleRenderType(){public void begin(BufferBuilder b,TextureManager textures){RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_PARTICLES);RenderSystem.depthMask(true);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}public void end(Tesselator t){t.end();}public String toString(){return "mite_source_sacred";}};
 public static void spawn(Level raw,double x,double y,double z){
  if(!(raw instanceof ClientLevel level)||!OriginalRandomTables.random_numbers_have_been_initialized||shader==null)return;
  var mc=Minecraft.getInstance();var camera=mc.getCameraEntity();if(camera==null)return;var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;if(camera.distanceToSqr(x,y,z)>24d*24d||setting==ParticleStatus.MINIMAL)return;
  if(table==null)table=SourceSacredRules1120.table(OriginalRandomTables.float_1,Math::random);mc.particleEngine.add(new Sacred(level,x,y,z));
 }
 static final class Sacred extends TextureSheetParticle implements SourceSacredRules1120.Motion {
  final SourceSacredRules1120.State state;
  Sacred(ClientLevel level,double x,double y,double z){super(level,x,y,z);state=SourceSacredRules1120.create(table,OriginalRandomTables.float_1,OriginalRandomTables.int_32,random.nextInt(),next,OriginalRandomTables.random_number_index,1);next=state.nextMotion;OriginalRandomTables.random_number_index=state.sharedIndex;setSize(.2f,.2f);hasPhysics=true;gravity=0;setColor(1,1,.9f);setAlpha(.15f);lifetime=state.lifetime;quadSize=SourceSacredRules1120.quad(state,1);setSprite(((ClientFertilizerAtlas933)Minecraft.getInstance().particleEngine).mite$fertilizerAtlas().getSprite(Content.id("source_particles")));}
  @Override public void tick(){SourceSacredRules1120.tick(state,this);age=state.age;alpha=state.alpha;xd=state.vx;yd=state.vy;zd=state.vz;}
  public void previous(){xo=x;yo=y;zo=z;}public double y(){return y;}public boolean ground(){return onGround;}public void moveSource(double x,double y,double z){move(x,y,z);}public void removeSource(){remove();}
  @Override public float getQuadSize(float partial){return SourceSacredRules1120.quad(state,partial);}
  @Override public int getLightColor(float partial){return 240|(super.getLightColor(partial)>>16&255)<<16;}
  @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(4f/16);}@Override protected float getU1(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(4f/16+.0624375f);}@Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*(6f/16);}@Override protected float getV1(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*(6f/16+.0624375f);}
  @Override public ParticleRenderType getRenderType(){return TYPE;}
 }
 private ClientSacredParticles1120(){}
}

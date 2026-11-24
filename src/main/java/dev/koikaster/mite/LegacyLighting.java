package dev.koikaster.mite;
/** Scalar lightmap equations adapted from the user's R195 bfe.h(float) and aei/aej brightness tables.
 * GPU rendering, weather sampling and full original renderer behavior still require in-game comparison.
 */
public final class LegacyLighting {
 public record Color(float r,float g,float b){
  public int abgr(){return 0xff000000|((int)(clamp(b)*255))<<16|((int)(clamp(g)*255))<<8|(int)(clamp(r)*255);}
 }
 public record Sky(float ascension,float moonBrightness,float lightningBlend,Rules.Moon moon,boolean overworld){
  public static final Sky NONE=new Sky(0,1,0,Rules.Moon.NORMAL,false);
 }
 private static final float[] SIN=new float[65536];
 static{for(int i=0;i<SIN.length;i++)SIN[i]=(float)Math.sin(i*Math.PI*2/65536);}
 public static float sin(float value){return SIN[(int)(value*10430.378f)&65535];}
 public static float cos(float value){return SIN[(int)(value*10430.378f+16384f)&65535];}
 public static float celestialAngle(long time,float partial,int dimension){
  if(dimension==-1||dimension==-2)return .5f;if(dimension==1)return 0;
  float initial=((int)Math.floorMod(time,24000)+partial)/24000f-.25f;if(initial<0)initial++;if(initial>1)initial--;
  float curved=1-(float)((Math.cos(initial*Math.PI)+1)/2);return initial+(curved-initial)/3;
 }
 public static float sunBrightness(long time,float partial,float rain,float thunder,int dimension){
  float darkness=clamp(1-(cos(celestialAngle(time,partial,dimension)*(float)Math.PI*2)*2+.2f));
  float light=1-darkness;light=(float)(light*(1-(double)(rain*5)/16));light=(float)(light*(1-(double)(thunder*5)/16));return light*.8f+.2f;
 }
 public static float visionScale(int duration,float partial){return duration>200?1:.7f+sin((duration-partial)*(float)Math.PI*.2f)*.3f;}
 public static float clamp(float v){return Math.max(0,Math.min(1,v));}
 public static float brightness(int light,float ambient){
  int level=Math.max(0,Math.min(15,light));float n=1-level/15f;
  float result=(1-n)/(n*3+1)*(1-clamp(ambient))+clamp(ambient);
  // R195's surface, Underworld and End tables subtract an additional low-light falloff.
  if(ambient==0&&level>0&&level<15)result-=.001f*(15-level);
  return result;
 }
 public static float moonAscension(long dayTime){int adjusted=(int)Math.floorMod(dayTime+6000,24000);return adjusted<=5000?1-adjusted/5000f:adjusted>=19000?(adjusted-19000)/5000f:0;}
 public static float moonBrightness(long dayTime,float rain,Rules.Moon moon){
  float[] phases={1,.75f,.5f,.25f,0,.25f,.5f,.75f};
  float brightness=switch(moon){case BLOOD->.6f;case HARVEST->1;case BLUE->1.1f;default->phases[(int)Math.floorMod(dayTime/24000+1,8)]*.5f+.75f;};
  if(brightness>.75f&&rain>0)brightness=brightness*(1-clamp(rain))+.75f*clamp(rain);
  return brightness;
 }
 public static Color sample(int sky,int block,float sun,float ambient,float flicker,float gamma,float nightVision,float darken,boolean end){return sample(sky,block,sun,ambient,flicker,gamma,nightVision,darken,end,Sky.NONE);}
 public static Color sample(int sky,int block,float sun,float ambient,float flicker,float gamma,float nightVision,float darken,boolean end,Sky conditions){
  sun=clamp(sun);gamma=clamp(gamma);nightVision=clamp(nightVision);darken=clamp(darken);
  float skyLight=brightness(sky,ambient)*(sun*.95f+.05f);
  float torch=brightness(block,ambient)*(flicker*.1f+1.5f);
  float moonFactor=Math.max(0,Math.min(.2f,conditions.ascension()))*5;
  if(conditions.overworld()&&conditions.ascension()>0)skyLight=skyLight*(1-moonFactor)+skyLight*moonFactor*conditions.moonBrightness();
  float lightning=clamp(conditions.lightningBlend());skyLight=brightness(sky,ambient)*lightning+skyLight*(1-lightning);
  float warmSky=skyLight*(sun*.65f+.35f);
  float greenTorch=torch*((torch*.6f+.4f)*.6f+.4f),blueTorch=torch*(torch*torch*.6f+.4f);
  float r=warmSky+torch,g=warmSky+greenTorch,b=skyLight+blueTorch;
  r=r*.96f+.03f;g=g*.96f+.03f;b=b*.96f+.03f;
  if(darken>0){r=r*(1-darken)+r*.7f*darken;g=g*(1-darken)+g*.6f*darken;b=b*(1-darken)+b*.6f*darken;}
  if(end){r=.22f+torch*.75f;g=.28f+greenTorch*.75f;b=.25f+blueTorch*.75f;}
  if(nightVision>0){float scale=Math.min(1/r,Math.min(1/g,1/b));r=r*(1-nightVision)+r*scale*nightVision;g=g*(1-nightVision)+g*scale*nightVision;b=b*(1-nightVision)+b*scale*nightVision;}
  r=finish(Math.min(1,r),gamma);g=finish(Math.min(1,g),gamma);b=finish(Math.min(1,b),gamma);
  if(conditions.overworld()){
   float rise=Math.max(0,Math.min(.2f,conditions.ascension())),skyFraction=sky/15f,attenuation=clamp(1-block/15f*2);
   switch(conditions.moon()){
    case BLOOD->{r*=1+1f*rise*5*skyFraction*attenuation;g*=1+.4f*rise*5*skyFraction*attenuation;}
    case HARVEST->{r*=1+.6f*rise*4*skyFraction*attenuation;g*=1+.4f*rise*4*skyFraction*attenuation;b*=1-.2f*rise*4*skyFraction*attenuation;}
    case BLUE->{b*=1+.5f*rise*2.5f*skyFraction*attenuation;g*=1+.2f*rise*2.5f*skyFraction*attenuation;}
    default->{}
   }
  }
  r=clamp(r);g=clamp(g);b=clamp(b);
  if(sky==0&&block==0){r=Math.max(.062f,r);g=Math.max(.062f,g);b=Math.max(.062f,b);}
  return new Color(r,g,b);
 }
 private static float finish(float value,float gamma){float inv=1-value;float bright=1-inv*inv*inv*inv;return (value*(1-gamma)+bright*gamma)*.96f+.03f;}
 /** R195 EntityRenderer terrain void fog, with old Y=0 mapped to the modern dimension floor.
  * Fog-post relief and seeded biome weather fog are separate mechanics and are not implemented here.
  */
 public static float fogFar(float distance,float heightAboveBottom,float skyLight,boolean voidFog){
  if(!voidFog)return distance;
  double visibility=Math.max(0,skyLight/16d+(heightAboveBottom+4d)/20d);
  double power=1+(16d-heightAboveBottom)/2d;
  if(power>1)visibility=Math.pow(visibility,power);
  float limit=Math.max(5,100*(float)visibility);
  return Math.min(distance,Math.max(24,Math.min(distance,limit)));
 }
 public static float underworldFogGray(long time){return Math.abs(Math.floorMod(Math.floorDiv(time+6000,24000)+1,32)-16)*.004f;}
 public static float approachFogSky(float current,int target,float seconds,boolean belowBottom){
  float delta=target-current,maximum=Math.abs(delta)<12||belowBottom?.6f*Math.max(0,seconds):Math.abs(delta);
  return current+Math.copySign(Math.min(Math.abs(delta),maximum),delta);
 }
 private LegacyLighting(){}
}

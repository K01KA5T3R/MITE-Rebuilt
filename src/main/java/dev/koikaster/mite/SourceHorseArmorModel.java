package dev.koikaster.mite;
import java.util.*;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.animal.horse.Horse;
/** Original R195/1.6.4 horse-armor128x128 face UVs fitted to the native1.20 horse surface.
 * Texture bytes stay untouched. Leaf scaling changes only mesh positions, never the source UV rectangles.
 * Native parent poses keep the fitted layer on the horse while walking, eating, rearing and growing.
 */
public final class SourceHorseArmorModel extends HorseModel<Horse> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Content.id("source_horse_armor"),"main");
 private record Patch(String path,int u,int v,float x,float y,float z,int w,int h,int d,float nx,float ny,float nz,float nw,float nh,float nd){
  float sx(){return (nw+.2f)/w;}float sy(){return (nh+.2f)/h;}float sz(){return (nd+.2f)/d;}
 }
 private static Patch patch(String path,int u,int v,float x,float y,float z,int w,int h,int d,float nx,float ny,float nz,float nw,float nh,float nd){return new Patch(path,u,v,x,y,z,w,h,d,nx,ny,nz,nw,nh,nd);}
 private static final List<Patch> PATCHES=patches();
 private static List<Patch> patches(){var all=new ArrayList<Patch>(List.of(
  patch("body/armor_body",0,34,-5,-8,-19,10,10,24,-5,-8,-17,10,10,22),
  patch("head_parts/armor_neck",0,12,-2.05f,-9.8f,-2,4,14,8,-2.05f,-6,-2,4,12,7),
  patch("head_parts/armor_head",0,0,-2.5f,-10,-1.5f,5,5,7,-3,-11,-2,6,5,7),
  patch("head_parts/armor_upper_mouth",24,18,-2,-10,-7,4,3,6,-2,-11,-7,4,3,5),
  patch("head_parts/armor_lower_mouth",24,27,-2,-7,-6.5f,4,2,5,-2,-8,-7,4,2,5),
  patch("head_parts/armor_mane",58,0,-1,-11.5f,5,2,16,4,-1,-11,5.01f,2,16,2),
  patch("head_parts/armor_left_ear",0,0,.45f,-12,4,2,3,1,.5f,-14,1.5f,2,3,1),
  patch("head_parts/armor_right_ear",0,0,-2.45f,-12,4,2,3,1,-2.5f,-14,1.5f,2,3,1),
  patch("left_hind_leg/armor_thigh",78,29,-2.5f,-2,-2.5f,4,9,5,-3,-1.01f,-1,4,6,4),
  patch("left_hind_leg/armor_shin",78,43,-2,0,-1.5f,3,5,3,-3,4.99f,-1,4,3,4),
  patch("left_hind_leg/armor_hoof",78,51,-2.5f,5.1f,-2,4,3,4,-3,7.99f,-1,4,2,4),
  patch("right_hind_leg/armor_thigh",96,29,-1.5f,-2,-2.5f,4,9,5,-1,-1.01f,-1,4,6,4),
  patch("right_hind_leg/armor_shin",96,43,-1,0,-1.5f,3,5,3,-1,4.99f,-1,4,3,4),
  patch("right_hind_leg/armor_hoof",96,51,-1.5f,5.1f,-2,4,3,4,-1,7.99f,-1,4,2,4),
  patch("left_front_leg/armor_thigh",44,29,-1.9f,-1,-2.1f,3,8,4,-3,-1.01f,-1.9f,4,6,4),
  patch("left_front_leg/armor_shin",44,41,-1.9f,0,-1.6f,3,5,3,-3,4.99f,-1.9f,4,3,4),
  patch("left_front_leg/armor_hoof",44,51,-2.4f,5.1f,-2.1f,4,3,4,-3,7.99f,-1.9f,4,2,4),
  patch("right_front_leg/armor_thigh",60,29,-1.1f,-1,-2.1f,3,8,4,-1,-1.01f,-1.9f,4,6,4),
  patch("right_front_leg/armor_shin",60,41,-1.1f,0,-1.6f,3,5,3,-1,4.99f,-1.9f,4,3,4),
  patch("right_front_leg/armor_hoof",60,51,-1.6f,5.1f,-2.1f,4,3,4,-1,7.99f,-1.9f,4,2,4),
  patch("body/tail/armor_base",44,0,-1,-1,0,2,2,3,-1.5f,0,0,3,2,4),
  patch("body/tail/armor_middle",38,7,-1.5f,-2,3,3,4,7,-1.5f,2,0,3,5,4),
  patch("body/tail/armor_tip",24,3,-1.5f,-4.5f,9,3,4,7,-1.5f,7,0,3,7,4),
  patch("head_parts/head_saddle/armor_wrap",80,12,-2.5f,-10.1f,-7,5,5,12,-3,-11,-7,6,5,12),
  patch("head_parts/left_saddle_mouth/armor_bit",74,13,1.5f,-8,-4,1,2,2,2,-9,-6,1,2,2),
  patch("head_parts/right_saddle_mouth/armor_bit",74,13,-2.5f,-8,-4,1,2,2,-3,-9,-6,1,2,2),
  patch("body/saddle/armor_seat",80,0,-5,0,-3,10,1,8,-5,-9,-6,10,1,8),
  patch("body/saddle/armor_pommel",106,9,-1.5f,-1,-3,3,1,2,-1.5f,-10,-6,3,1,2),
  patch("body/saddle/armor_cantle",80,9,-4,-1,3,8,1,2,-4,-10,0,8,1,2)
 ));for(var p:new ArrayList<>(all))if(p.path().contains("_leg/"))all.add(patch(p.path().replace("_leg/","_baby_leg/"),p.u(),p.v(),p.x(),p.y(),p.z(),p.w(),p.h(),p.d(),p.nx(),p.ny()*2-4.49f,p.nz(),p.nw(),p.nh()*2,p.nd()));return List.copyOf(all);
 }
 private static ModelPart part(ModelPart root,String path){for(String name:path.split("/"))root=root.getChild(name);return root;}
 public SourceHorseArmorModel(ModelPart root){super(root);for(var p:PATCHES){var leaf=part(root,p.path());leaf.xScale=p.sx();leaf.yScale=p.sy();leaf.zScale=p.sz();}}
 private static PartDefinition group(PartDefinition parent,String name,PartPose pose){return parent.addOrReplaceChild(name,CubeListBuilder.create(),pose);}
 public static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();var body=group(root,"body",PartPose.offset(0,11,5));var head=group(root,"head_parts",PartPose.offsetAndRotation(0,4,-12,.5235988f,0,0));
  group(body,"tail",PartPose.offsetAndRotation(0,-5,2,.5235988f,0,0));group(body,"saddle",PartPose.ZERO);
  for(String name:new String[]{"left_saddle_mouth","right_saddle_mouth","left_saddle_line","right_saddle_line","head_saddle","mouth_saddle_wrap"})group(head,name,PartPose.ZERO);
  group(root,"left_hind_leg",PartPose.offset(4,14,7));group(root,"right_hind_leg",PartPose.offset(-4,14,7));group(root,"left_front_leg",PartPose.offset(4,14,-12));group(root,"right_front_leg",PartPose.offset(-4,14,-12));
  group(root,"left_hind_baby_leg",PartPose.offset(4,14,7));group(root,"right_hind_baby_leg",PartPose.offset(-4,14,7));group(root,"left_front_baby_leg",PartPose.offset(4,14,-12));group(root,"right_front_baby_leg",PartPose.offset(-4,14,-12));
  for(var p:PATCHES){var tokens=p.path().split("/");var parent=root;for(int i=0;i<tokens.length-1;i++)parent=parent.getChild(tokens[i]);parent.addOrReplaceChild(tokens[tokens.length-1],CubeListBuilder.create().texOffs(p.u(),p.v()).addBox(p.x(),p.y(),p.z(),p.w(),p.h(),p.d()),PartPose.offset(p.nx()-.1f-p.x()*p.sx(),p.ny()-.1f-p.y()*p.sy(),p.nz()-.1f-p.z()*p.sz()));}
  return LayerDefinition.create(mesh,128,128);
 }
}

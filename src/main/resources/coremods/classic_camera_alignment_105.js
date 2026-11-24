// Only replace the two world/culling pose operands. Native camera, picking and hand poses stay intact.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),T=Java.type('org.objectweb.asm.tree.TypeInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 function previous(n){for(n=n.getPrevious();n!==null&&n.getOpcode()<0;n=n.getPrevious()){}return n;}
 return {classic_camera_alignment_105:{target:{type:'CLASS',name:'net.minecraft.client.renderer.GameRenderer'},transformer:function(node){
  var anchors=[],kinds={};
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i),name=String(method.name);
   if((name!=='renderLevel'&&name!==String(A.mapMethod('m_109089_')))||String(method.desc)!=='(FJLcom/mojang/blaze3d/vertex/PoseStack;)V'||(method.access&O.ACC_STATIC)!==0)continue;
   for(var n=method.instructions.getFirst();n!==null;n=n.getNext()){
    if(!(n instanceof M)||String(n.owner)!=='net/minecraft/client/renderer/LevelRenderer')continue;
    var kind=null,call=String(n.name),desc=String(n.desc);
    if((call==='prepareCullFrustum'||call===String(A.mapMethod('m_253210_')))&&desc==='(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;)V')kind='cull';
    if((call==='renderLevel'||call===String(A.mapMethod('m_109599_')))&&desc==='(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V')kind='world';
    if(kind===null)continue;
    var pose=null,steps=0;
    for(var p=n.getPrevious();p!==null&&steps++<64;p=p.getPrevious())if(p instanceof V&&p.getOpcode()===O.ALOAD&&p.var===4){pose=p;break;}
    var receiver=pose===null?null:previous(pose);
    if(receiver===null||!(receiver instanceof F)||String(receiver.owner)!=='net/minecraft/client/Minecraft'||String(receiver.desc)!=='Lnet/minecraft/client/renderer/LevelRenderer;')continue;
    anchors.push({method:method,pose:pose});kinds[kind]=(kinds[kind]||0)+1;
   }
  }
  if(anchors.length!==2||kinds.cull!==1||kinds.world!==1){A.log('WARN','Classic camera: unsupported world renderer; keeping native view.');return node;}
  for(var i=0;i<anchors.length;i++){var code=new L();code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicCameraAlignment','worldPose','(Ljava/lang/Object;)Ljava/lang/Object;',false));code.add(new T(O.CHECKCAST,'com/mojang/blaze3d/vertex/PoseStack'));anchors[i].method.instructions.insert(anchors[i].pose,code);}
  return node;
 }} };
}

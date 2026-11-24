// Only the native sky-pass fog flag is adapted; terrain fog and boss state stay native.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 return {classic_end_sky_121:{target:{type:'CLASS',name:'net.minecraft.client.renderer.LevelRenderer'},transformer:function(node){
  var methods=[];for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),name=String(m.name);if((name==='renderSky'||name===String(A.mapMethod('m_202423_')))&&String(m.desc)==='(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V')methods.push(m);}
  if(methods.length!==1){A.log('WARN','Classic End sky: unsupported sky pass; retaining native rendering.');return node;}
  var method=methods[0],code=new L();code.add(new V(O.ILOAD,5));code.add(new V(O.ALOAD,4));code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicEndSky','foggy','(ZLjava/lang/Object;)Z',false));code.add(new V(O.ISTORE,5));method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,2);return node;
 }} };
}

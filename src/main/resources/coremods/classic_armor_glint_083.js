function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_classic_armor_glint_083:{target:{type:'CLASS',name:'net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer'},transformer:function(node){
  var method=null,desc='(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V';
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)===desc&&(String(m.name)==='renderGlint'||String(m.name)===String(A.mapMethod('m_289597_')))){if(method!==null)return node;method=m;}}
  if(method===null)return node;
  var calls=[];for(var it=method.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.INVOKEINTERFACE&&String(x.owner)==='net/minecraft/client/renderer/MultiBufferSource'&&String(x.desc)==='(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;'&&(String(x.name)==='getBuffer'||String(x.name)===String(A.mapMethod('m_6299_'))))calls.push(x);}
  if(calls.length!==1)return node;
  var code=new List();code.add(new Var(O.ALOAD,2));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicGlint','armor','(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/MultiBufferSource;)Lcom/mojang/blaze3d/vertex/VertexConsumer;',false));method.instructions.insert(calls[0],code);method.maxStack=Math.max(method.maxStack,12);return node;
 }}};
}

function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_classic_player_pose_078:{target:{type:'CLASS',name:'net.minecraft.client.model.HumanoidModel'},transformer:function(node){
  var found=null,desc='(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V';
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)===desc&&(String(m.name)==='setupAnim'||String(m.name)===String(A.mapMethod('m_6973_')))){if(found!==null)return node;found=m;}}
  if(found===null)return node; // Unrecognized renderer: native animation remains valid.
  var returns=[];for(var it=found.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.RETURN)returns.push(x);}
  for(var i=0;i<returns.length;i++){var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,1));for(var slot=2;slot<=6;slot++)code.add(new Var(O.FLOAD,slot));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicThirdPersonAnimations','apply','(Ljava/lang/Object;Ljava/lang/Object;FFFFF)V',false));found.instructions.insertBefore(returns[i],code);}
  found.maxStack=Math.max(found.maxStack,7);return node;
 }}};
}

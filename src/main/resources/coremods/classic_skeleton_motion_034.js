function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_skeleton_last_received_motion_034:{target:{type:'CLASS',name:'net.minecraft.server.network.ServerGamePacketListenerImpl'},transformer:function(node){
  var field=null;for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if((String(f.name)==='player'||String(f.name)==='f_9743_')&&String(f.desc)==='Lnet/minecraft/server/level/ServerPlayer;')field=String(f.name);}
  if(field===null)throw new Error('Missing native player owner for skeleton motion');var sites=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)!=='handleMovePlayer'&&String(m.name)!=='m_7185_')||String(m.desc)!=='(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V')continue;
   var code=new L();code.add(new V(O.ALOAD,0));code.add(new F(O.GETFIELD,node.name,field,'Lnet/minecraft/server/level/ServerPlayer;'));code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceSkeletonAim','received','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insert(code);m.maxStack=Math.max(2,m.maxStack);sites++;
  }if(sites!==1)throw new Error('Skeleton motion packet entry sites '+sites);return node;
 }}};
}

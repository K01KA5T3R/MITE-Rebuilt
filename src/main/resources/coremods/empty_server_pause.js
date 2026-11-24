// Replace one existing call; no new control-flow branches or stack-map frames are needed.
function initializeCoreMod(){
 var Opcodes=Java.type('org.objectweb.asm.Opcodes');
 var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {'mite_empty_server_pause':{
  target:{type:'CLASS',name:'net.minecraft.server.MinecraftServer'},
  transformer:function(node){
   var matches=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);
    for(var instruction=method.instructions.getFirst();instruction!==null;){
     var next=instruction.getNext();
     if(instruction.getOpcode()===Opcodes.INVOKEVIRTUAL&&String(instruction.owner)==='net/minecraft/server/level/ServerLevel'&&String(instruction.desc)==='(Ljava/util/function/BooleanSupplier;)V'){
      method.instructions.set(instruction,new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/EmptyServerPause','tick','(Ljava/lang/Object;Ljava/lang/Object;)V',false));matches++;
     }
     instruction=next;
    }
   }
   if(matches!==1)throw new Error('Empty server pause: unexpected MinecraftServer level tick layout: '+matches);
   return node;
  }
 }};
}

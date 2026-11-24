// Classic voxel opacity, without replacing networking or chunk scheduling.
function initializeCoreMod() {
 var Opcodes=Java.type('org.objectweb.asm.Opcodes');
 var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
 var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
 var InsnNode=Java.type('org.objectweb.asm.tree.InsnNode');
 var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return { 'classic_light_opacity': {
  target: {type:'CLASS',name:'net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase'},
  transformer: function(node) {
   var matches=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);
    // getLightBlock is the sole int(BlockGetter, BlockPos) method in the 1.20.1 target.
    if(String(method.desc)!=='(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I')continue;
    matches++;
    for(var instruction=method.instructions.getFirst();instruction!==null;instruction=instruction.getNext()){
     if(instruction.getOpcode()!==Opcodes.IRETURN)continue;
     var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new InsnNode(Opcodes.SWAP));
     code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/LegacyOcclusion','adjust','(Ljava/lang/Object;I)I',false));
     method.instructions.insertBefore(instruction,code);
    }
    method.maxStack+=1;
   }
   if(matches!==1)throw new Error('Classic opacity: unexpected BlockStateBase layout: '+matches);
   return node;
  }
 }};
}

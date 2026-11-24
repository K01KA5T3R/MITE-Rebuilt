/** Convert the caller's candidate too, so native success comparisons, neighbor
 * updates and heightmap consumers observe the same state actually written. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 function boundary(owner,a,b,desc){return {target:{type:'CLASS',name:owner},transformer:function(node){var n=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===a||String(m.name)===b)&&String(m.desc)===desc){var code=new L();code.add(new V(O.ALOAD,2));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/DeepslateRemoval','state','(Ljava/lang/Object;)Lnet/minecraft/world/level/block/state/BlockState;',false));code.add(new V(O.ASTORE,2));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);n++;}}if(n!==1)throw new Error('Deepslate caller boundary '+owner+' '+n);return node;}};}
 return {
  deepslate_level_candidate_063:boundary('net.minecraft.world.level.Level','setBlock','m_6933_','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z'),
  deepslate_chunk_candidate_063:boundary('net.minecraft.world.level.chunk.LevelChunk','setBlockState','m_6978_','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;'),
  deepslate_proto_candidate_063:boundary('net.minecraft.world.level.chunk.ProtoChunk','setBlockState','m_6978_','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;')
 };
}

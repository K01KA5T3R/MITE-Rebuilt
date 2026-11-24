function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),FieldNode=Java.type('org.objectweb.asm.tree.FieldNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var P='dev/koikaster/mite/';
 return {
  mite_new_natural_mycelium_chunk_flag_036:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.ChunkAccess'},transformer:function(node){
   var name='mite$hadNaturalMycelium';node.fields.add(new FieldNode(O.ACC_PRIVATE|O.ACC_VOLATILE,name,'Z',null,null));node.interfaces.add(P+'SourceMyceliumChunkAccess');
   var get=new Method(O.ACC_PUBLIC,'mite$naturalMycelium','()Z',null,null);get.instructions.add(new Var(O.ALOAD,0));get.instructions.add(new Field(O.GETFIELD,node.name,name,'Z'));get.instructions.add(new Insn(O.IRETURN));get.maxStack=1;get.maxLocals=1;node.methods.add(get);
   var set=new Method(O.ACC_PUBLIC,'mite$naturalMycelium','(Z)V',null,null);set.instructions.add(new Var(O.ALOAD,0));set.instructions.add(new Var(O.ILOAD,1));set.instructions.add(new Field(O.PUTFIELD,node.name,name,'Z'));set.instructions.add(new Insn(O.RETURN));set.maxStack=2;set.maxLocals=2;node.methods.add(set);return node;
  }},
  mite_new_natural_mycelium_proto_promotion_036:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.LevelChunk'},transformer:function(node){
   var sites=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<init>'||String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V')continue;
    for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.RETURN)continue;var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,2));code.add(new Call(O.INVOKESTATIC,P+'SourceMyceliumFlags','promote','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insertBefore(x,code);m.maxStack=Math.max(4,m.maxStack);sites++;}
   }if(sites!==1)throw new Error('Expected one new proto-promotion return, got '+sites);return node;
  }}
 };
}

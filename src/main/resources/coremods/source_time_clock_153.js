// Source chunk historical time: storage, promotion, wrappers and normal ticking.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),F=Java.type('org.objectweb.asm.tree.FieldNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),N=Java.type('org.objectweb.asm.tree.FieldInsnNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),T=Java.type('org.objectweb.asm.tree.TypeInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var face='dev/koikaster/mite/SourceChunkClock',helper='dev/koikaster/mite/SourceFastForwardClock';
 function fresh(node,name,desc){for(var k=0;k<node.methods.size();k++)if(String(node.methods.get(k).name)===name&&String(node.methods.get(k).desc)===desc)throw new Error('Clock method already exists: '+name+desc);return new M(O.ACC_PUBLIC,name,desc,null,null);}
 function storeMethods(node,wrapped){
  var properties=[['mite$randomTickTime','J',O.LLOAD,O.LRETURN],['mite$saveAfterForward','Z',O.ILOAD,O.IRETURN]];
  for(var p=0;p<properties.length;p++){var q=properties[p];
   for(var setter=0;setter<2;setter++){var desc=setter?'('+q[1]+')V':'()'+q[1],m=fresh(node,q[0],desc);m.instructions.add(new V(O.ALOAD,0));
    if(wrapped){m.instructions.add(new C(O.INVOKEVIRTUAL,String(node.name),wrapped,'()Lnet/minecraft/world/level/chunk/LevelChunk;',false));m.instructions.add(new T(O.CHECKCAST,face));if(setter)m.instructions.add(new V(q[2],1));m.instructions.add(new C(O.INVOKEINTERFACE,face,q[0],desc,true));}
    else{if(setter)m.instructions.add(new V(q[2],1));m.instructions.add(new N(setter?O.PUTFIELD:O.GETFIELD,String(node.name),q[0],q[1]));}
    m.instructions.add(new I(setter?O.RETURN:q[3]));m.maxStack=setter&&q[1]==='J'?3:2;m.maxLocals=setter?(q[1]==='J'?3:2):1;node.methods.add(m);
   }
  }
 }
 return {
  source_chunk_clock_storage_153:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.ChunkAccess'},transformer:function(node){
   if(node.interfaces.contains(face))throw new Error('Chunk clock interface already present');for(var k=0;k<node.fields.size();k++)if(String(node.fields.get(k).name).indexOf('mite$randomTickTime')===0||String(node.fields.get(k).name).indexOf('mite$saveAfterForward')===0)throw new Error('Chunk clock field collision');
   node.interfaces.add(face);node.fields.add(new F(O.ACC_PRIVATE,'mite$randomTickTime','J',null,null));node.fields.add(new F(O.ACC_PRIVATE,'mite$saveAfterForward','Z',null,null));storeMethods(node,null);return node;
  }},
  source_chunk_clock_wrapper_153:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.ImposterProtoChunk'},transformer:function(node){
   var name=null,count=0;for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k);if(String(m.desc)==='()Lnet/minecraft/world/level/chunk/LevelChunk;'&&(String(m.name)==='getWrapped'||String(m.name)===String(A.mapMethod('m_62768_')))){name=String(m.name);count++;}}if(count!==1)throw new Error('Exact native wrapped LevelChunk getter required');storeMethods(node,name);return node;
  }},
  source_chunk_clock_promote_153:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.LevelChunk'},transformer:function(node){
   var count=0;for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k);if(String(m.name)!=='<init>'||String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V')continue;var code=m.instructions.toArray();for(var j=0;j<code.length;j++)if(code[j].getOpcode()===O.RETURN){var extra=new L();extra.add(new V(O.ALOAD,0));extra.add(new V(O.ALOAD,2));extra.add(new C(O.INVOKESTATIC,helper,'promoted','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insertBefore(code[j],extra);count++;}m.maxStack=Math.max(m.maxStack,2);}if(count!==1)throw new Error('One native ProtoChunk promotion return required');return node;
  }},
  source_chunk_clock_serialize_153:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.storage.ChunkSerializer'},transformer:function(node){
   var reads=0,writes=0;for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k),name=String(m.name),desc=String(m.desc),read=(name==='read'||name===String(A.mapMethod('m_188230_')))&&desc==='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;',write=(name==='write'||name===String(A.mapMethod('m_63454_')))&&desc==='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/nbt/CompoundTag;';if(!read&&!write)continue;var code=m.instructions.toArray();for(var j=0;j<code.length;j++)if(code[j].getOpcode()===O.ARETURN){var extra=new L();extra.add(new V(O.ALOAD,read?3:1));extra.add(new C(O.INVOKESTATIC,helper,read?'loadTag':'saveTag',read?'(Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/chunk/ProtoChunk;':'(Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/nbt/CompoundTag;',false));m.instructions.insertBefore(code[j],extra);if(read)reads++;else writes++;}m.maxStack=Math.max(m.maxStack,2);}if(reads!==2||writes!==1)throw new Error('Exact two native chunk-read branches and one write return required: '+reads+'/'+writes);return node;
  }},
  source_chunk_clock_normal_153:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerLevel'},transformer:function(node){
   function previous(n){do{n=n.getPrevious();}while(n!==null&&n.getOpcode()<0);return n;}
   var count=0,guards=0;for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k);if((String(m.name)!=='tickChunk'&&String(m.name)!==String(A.mapMethod('m_8714_')))||String(m.desc)!=='(Lnet/minecraft/world/level/chunk/LevelChunk;I)V')continue;var code=m.instructions.toArray();for(var j=0;j<code.length;j++)if(code[j].getOpcode()===O.RETURN){var branch=previous(code[j]),call=branch===null?null:previous(branch),owner=call===null?null:previous(call);
    if(branch instanceof J&&branch.getOpcode()===O.IFNE&&call instanceof C&&String(call.owner)==='dev/koikaster/mite/SourceDimensionClock'&&String(call.name)==='chunkAllowed'&&String(call.desc)==='(Ljava/lang/Object;)Z'&&owner instanceof V&&owner.getOpcode()===O.ALOAD&&owner.var===0){guards++;continue;}
    var extra=new L();extra.add(new V(O.ALOAD,0));extra.add(new V(O.ALOAD,1));extra.add(new C(O.INVOKESTATIC,helper,'normalTick','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insertBefore(code[j],extra);count++;}m.maxStack=Math.max(m.maxStack,2);}if(count!==1||guards>1)throw new Error('One actual normal chunk-tick return and at most one exact dimension pause guard required');return node;
  }}
 };
}

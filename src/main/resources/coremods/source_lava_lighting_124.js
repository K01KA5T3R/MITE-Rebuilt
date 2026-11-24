// R195 lava emission and a one-time native block-light cache revision.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),D=Java.type('org.objectweb.asm.tree.LdcInsnNode'),Y=Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode'),H=Java.type('org.objectweb.asm.Handle'),N=Java.type('org.objectweb.asm.tree.IntInsnNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 return {
  source_lava_emission_124:{target:{type:'CLASS',name:'net.minecraft.world.level.block.Blocks'},transformer:function(node){
   var lava=false,selected=null,found=0;
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;var a=m.instructions.toArray();for(var j=0;j<a.length;j++){var x=a[j];if(x instanceof D&&String(x.cst)==='lava'){lava=true;selected=null;}else if(lava&&x instanceof Y&&String(x.desc).indexOf('ToIntFunction')>=0){for(var k=0;k<x.bsmArgs.length;k++)if(x.bsmArgs[k] instanceof H)selected=x.bsmArgs[k];}else if(lava&&x instanceof F&&x.getOpcode()===O.PUTSTATIC&&selected!==null){found++;lava=false;break;}}}
   if(found!==1||selected===null||String(selected.getOwner())!==String(node.name))throw new Error('Exact native lava light initializer required');
   var references=0;for(var i=0;i<node.methods.size();i++){var a=node.methods.get(i).instructions.toArray();for(var j=0;j<a.length;j++)if(a[j] instanceof Y){var args=a[j].bsmArgs;for(var k=0;k<args.length;k++)if(args[k] instanceof H&&String(args[k].getOwner())===String(selected.getOwner())&&String(args[k].getName())===String(selected.getName())&&String(args[k].getDesc())===String(selected.getDesc()))references++;}}
   if(references!==1)throw new Error('Lava emission lambda must not be shared by other light sources');
   var changed=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!==String(selected.getName())||String(m.desc)!==String(selected.getDesc()))continue;var real=[];for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()>=0)real.push(x);}if(real.length!==2||!(real[0] instanceof N)||real[0].operand!==15||real[1].getOpcode()!==O.IRETURN)throw new Error('Native lava light must be the exact constant15 body');real[0].operand=13;changed++;}if(changed!==1)throw new Error('One lava emission function required');return node;
  }},
  source_lava_chunk_cache_124:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.storage.ChunkSerializer'},transformer:function(node){
   var reads=0,writes=0;
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),name=String(m.name),desc=String(m.desc);
    if((name==='read'||name===String(A.mapMethod('m_188230_')))&&desc==='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;'){var code=new L();code.add(new V(O.ALOAD,3));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceLavaLighting','loadTag','(Ljava/lang/Object;)Lnet/minecraft/nbt/CompoundTag;',false));code.add(new V(O.ASTORE,3));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);reads++;}
    if((name==='write'||name===String(A.mapMethod('m_63454_')))&&desc==='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/nbt/CompoundTag;'){var a=m.instructions.toArray();for(var j=0;j<a.length;j++)if(a[j].getOpcode()===O.ARETURN){var code=new L();code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceLavaLighting','saveTag','(Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/nbt/CompoundTag;',false));m.instructions.insertBefore(a[j],code);writes++;}m.maxStack=Math.max(m.maxStack,2);}
   }if(reads!==1||writes!==1)throw new Error('Exact native chunk read/write revision sites required: '+reads+'/'+writes);return node;
  }}
 };
}

/** aqw placement boundary plus safe scope for retained native ritual exits. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {
 source_end_portal_placement_072:{target:{type:'CLASS',name:'net.minecraft.world.level.block.EndPortalBlock'},transformer:function(node){var desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V';for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)==='onPlace'||String(m.name)==='m_6807_')&&String(m.desc)===desc)throw new Error('072 EndPortal unexpected own placement body');}var contact=0;
 for(var i=0;i<node.methods.size();i++){
  var method=node.methods.get(i);
  if((String(method.name)!=='entityInside'&&String(method.name)!=='m_7892_')||String(method.desc)!=='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V')continue;
  var nodes=method.instructions.toArray();
  for(var j=0;j<nodes.length;j++){
   var insn=nodes[j];
   if(insn.getOpcode()!==O.INVOKEVIRTUAL||String(insn.owner)!=='net/minecraft/server/MinecraftServer'||String(insn.desc)!=='(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;')continue;
   for(var arg=2;arg<=4;arg++)method.instructions.insertBefore(insn,new V(O.ALOAD,arg));
   method.instructions.set(insn,new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceEndPortalPlacement','legacyTarget','(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/server/level/ServerLevel;',false));
   method.maxStack=Math.max(method.maxStack,5);
   contact++;
  }
 }
 if(contact!==1)throw new Error('End portal destination call sites '+contact);
 var official=false;for(var k=0;k<node.methods.size();k++)if(String(node.methods.get(k).name)==='entityInside')official=true;var m=new M(O.ACC_PUBLIC,official?'onPlace':'m_6807_',desc,null,null);for(var j=1;j<=4;j++)m.instructions.add(new V(O.ALOAD,j));m.instructions.add(new V(O.ILOAD,5));m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceEndPortalPlacement','placed',desc,false));m.instructions.add(new I(O.RETURN));m.maxStack=5;m.maxLocals=6;node.methods.add(m);return node;}},
 source_native_exit_scope_072:{target:{type:'CLASS',name:'net.minecraft.world.level.dimension.end.EndDragonFight'},transformer:function(node){if(node.interfaces.contains('dev/koikaster/mite/SourcePortalBuilderAccess'))throw new Error('072 duplicate exit scope');var original=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)==='spawnExitPortal'||String(m.name)==='m_64093_')&&String(m.desc)==='(Z)V')original=m;}if(original===null)throw new Error('072 native exit constructor missing');var name=String(original.name);original.name='mite$nativeExit072';original.access=(original.access&~(O.ACC_PRIVATE|O.ACC_PROTECTED))|O.ACC_PUBLIC;node.interfaces.add('dev/koikaster/mite/SourcePortalBuilderAccess');var wrapper=new M(O.ACC_PRIVATE,name,'(Z)V',null,null);wrapper.instructions.add(new V(O.ALOAD,0));wrapper.instructions.add(new V(O.ILOAD,1));wrapper.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceEndPortalPlacement','nativeExit','(Lnet/minecraft/world/level/dimension/end/EndDragonFight;Z)V',false));wrapper.instructions.add(new I(O.RETURN));wrapper.maxStack=wrapper.maxLocals=2;node.methods.add(wrapper);return node;}}
 ,source_end_return_credits_2277:{
  target:{type:'CLASS',name:'net.minecraft.server.level.ServerPlayer'},
  transformer:function(node){
   var player='net/minecraft/server/level/ServerPlayer';
   var teleporter='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;';
   var event='net/minecraft/network/protocol/game/ClientboundGameEventPacket';
   var matches=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);
    if(String(method.name)!=='changeDimension'||String(method.desc)!==teleporter)continue;
    var instructions=method.instructions.toArray();
    for(var j=0;j<instructions.length;j++){
     var signal=instructions[j];
     if(signal.getOpcode()!==O.GETSTATIC||String(signal.owner)!==event
       ||(String(signal.name)!=='WIN_GAME'&&String(signal.name)!=='f_132157_'))continue;
     var load=signal.getNext();
     var seen=load===null?null:load.getNext();
     if(load===null||load.getOpcode()!==O.ALOAD||load.var!==0||seen===null||seen.getOpcode()!==O.GETFIELD
       ||String(seen.owner)!==player||String(seen.desc)!=='Z'
       ||(String(seen.name)!=='seenCredits'&&String(seen.name)!=='f_8928_'))throw new Error('2277 unexpected credits operand');
     method.instructions.set(seen,new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceEndPortalPlacement',
       'resumeInterruptedCredits','(L'+player+';)Z',false));
     matches++;
    }
   }
   if(matches!==1)throw new Error('2277 credits operand count '+matches);
   return node;
  }
 }
 };
}

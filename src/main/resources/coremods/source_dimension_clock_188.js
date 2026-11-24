function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),B=Java.type('org.objectweb.asm.tree.LabelNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 var H='dev/koikaster/mite/SourceDimensionClock';
 function call(name,desc){return new C(O.INVOKESTATIC,H,name,desc,false);}
 function match(m,a,b,desc){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===desc;}
 function guard(m,name,returnOnTrue){var code=new L(),resume=new B();code.add(new V(O.ALOAD,0));code.add(call(name,'(Ljava/lang/Object;)Z'));code.add(new J(returnOnTrue?O.IFEQ:O.IFNE,resume));code.add(new I(O.RETURN));code.add(resume);code.add(new F(O.F_SAME,0,null,0,null));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);}
 function previous(n){do{n=n.getPrevious();}while(n!==null&&n.getOpcode()<0);return n;}
 function packets(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),array=m.instructions.toArray();for(var k=0;k<array.length;k++){var x=array[k];if(!(x instanceof C)||x.getOpcode()!==O.INVOKESPECIAL||String(x.owner)!=='net/minecraft/network/protocol/game/ClientboundSetTimePacket'||String(x.name)!=='<init>'||String(x.desc)!=='(JJZ)V')continue;
  var bool=previous(x),key=previous(bool),rules=previous(key),world=previous(rules);
  if(!(bool instanceof C)||String(bool.owner)!=='net/minecraft/world/level/GameRules'||String(bool.desc)!=='(Lnet/minecraft/world/level/GameRules$Key;)Z'||!(rules instanceof C)||String(rules.desc)!=='()Lnet/minecraft/world/level/GameRules;'||!(world instanceof V)||world.getOpcode()!==O.ALOAD)throw new Error('Unexpected native time packet argument layout in '+node.name);
  var code=new L();code.add(new V(O.ALOAD,world.var));code.add(call('daylight','(ZLjava/lang/Object;)Z'));m.instructions.insert(bool,code);m.maxStack++;count++;
 }}if(count!==1)throw new Error('Expected one native time packet in '+node.name+', got '+count);return node;}
 return {
  source_dimension_level_data188:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerLevel'},transformer:function(node){var constructors=0,tick=0,chunks=0,blocks=0,fluids=0;
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);
    if(String(m.name)==='<init>'&&String(m.desc).indexOf('(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;')===0){var code=new L();code.add(new V(O.ALOAD,1));code.add(new V(O.ALOAD,4));code.add(new V(O.ALOAD,5));code.add(call('wrap','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/storage/ServerLevelData;'));code.add(new V(O.ASTORE,4));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,3);constructors++;}
    if(match(m,'tickTime','m_8809_','()V')){guard(m,'tick',true);tick++;}
    if(match(m,'tickChunk','m_8714_','(Lnet/minecraft/world/level/chunk/LevelChunk;I)V')){guard(m,'chunkAllowed',false);chunks++;var array=m.instructions.toArray();for(var k=0;k<array.length;k++){var x=array[k];if(!(x instanceof C)||x.getOpcode()!==O.INVOKEVIRTUAL||(String(x.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V'&&String(x.desc)!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V'))continue;
     if(String(x.owner)==='net/minecraft/world/level/block/state/BlockState'){m.instructions.set(x,call('blockRandom','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V'));blocks++;}
     else if(String(x.owner)==='net/minecraft/world/level/material/FluidState'){m.instructions.set(x,call('fluidRandom','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V'));fluids++;}
    }}
   }
   if(constructors!==1||tick!==1||chunks!==1||blocks!==1||fluids!==1)throw new Error('Dimension clock sites '+constructors+'/'+tick+'/'+chunks+'/'+blocks+'/'+fluids);return node;
  }},
  source_dimension_regular_packet188:{target:{type:'CLASS',name:'net.minecraft.server.MinecraftServer'},transformer:packets},
  source_dimension_entry_packet188:{target:{type:'CLASS',name:'net.minecraft.server.players.PlayerList'},transformer:packets}
 };
}

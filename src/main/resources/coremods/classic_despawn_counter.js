function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),FieldNode=Java.type('org.objectweb.asm.tree.FieldNode'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 var H='dev/koikaster/mite/OriginalDespawn',obj='Ljava/lang/Object;',bridge='dev/koikaster/mite/SourceDespawnState',field='mite_source_despawn_counter';
 function match(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function load(c,op,i){c.add(new Var(op,i));}function call(c,n,d){c.add(new Call(O.INVOKESTATIC,H,n,d,false));}
 function entity(node){
  if(node.interfaces.contains(bridge))throw new Error('Duplicate source despawn field bridge');node.interfaces.add(bridge);node.fields.add(new FieldNode(O.ACC_PRIVATE,field,'I',null,null));
  var getter=new Method(O.ACC_PUBLIC,'mite$getDespawnCounter','()I',null,null);getter.instructions.add(new Var(O.ALOAD,0));getter.instructions.add(new Field(O.GETFIELD,String(node.name),field,'I'));getter.instructions.add(new Insn(O.IRETURN));getter.maxStack=1;getter.maxLocals=1;node.methods.add(getter);
  var setter=new Method(O.ACC_PUBLIC,'mite$setDespawnCounter','(I)V',null,null);setter.instructions.add(new Var(O.ALOAD,0));setter.instructions.add(new Var(O.ILOAD,1));setter.instructions.add(new Field(O.PUTFIELD,String(node.name),field,'I'));setter.instructions.add(new Insn(O.RETURN));setter.maxStack=2;setter.maxLocals=2;node.methods.add(setter);
  var saves=0,loads=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),name=null;if(match(m,'saveWithoutId','m_20240_','(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;')){name='save';saves++;}else if(match(m,'load','m_20258_','(Lnet/minecraft/nbt/CompoundTag;)V')){name='load';loads++;}if(name!==null){var c=new List();load(c,O.ALOAD,0);load(c,O.ALOAD,1);call(c,name,'('+obj+obj+')V');m.instructions.insert(c);m.maxStack=Math.max(8,m.maxStack);}}
  if(saves!==1||loads!==1)throw new Error('Source counter NBT entries '+saves+'/'+loads);return node;
 }
 function world(name,single,passenger){return {target:{type:'CLASS',name:name},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(!(match(m,'tickNonPassenger',single,'(Lnet/minecraft/world/entity/Entity;)V')||match(m,'tickPassenger',passenger,'(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V')))continue;var sites=0;for(var it=m.instructions.iterator();it.hasNext();){var insn=it.next();if((String(insn.name)==='setOldPosAndRot'||String(insn.name)==='m_146867_')&&String(insn.desc)==='()V'){var c=new List();c.add(new Insn(O.DUP));call(c,'tick','('+obj+')V');m.instructions.insertBefore(insn,c);sites++;count++;}}if(sites!==1)throw new Error('Source counter actual world update site '+name+' '+m.name+' '+sites);m.maxStack=Math.max(8,m.maxStack);}if(count!==2)throw new Error('Source counter world entries '+name+' '+count);return node;}};}
 return {
  'mite_source_entity_counter_state':{target:{type:'CLASS',name:'net.minecraft.world.entity.Entity'},transformer:entity},
  'mite_source_server_entity_counter':world('net.minecraft.server.level.ServerLevel','m_8647_','m_8662_'),
  'mite_source_client_entity_counter':world('net.minecraft.client.multiplayer.ClientLevel','m_104639_','m_104641_')
 };
}

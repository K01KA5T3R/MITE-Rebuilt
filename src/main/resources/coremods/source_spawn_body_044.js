/** New per-individual natural-spawn substitution, not another weighted selection. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {source_spawn_body_044:{target:{type:'CLASS',name:'net.minecraft.world.level.NaturalSpawner'},transformer:function(node){var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;')continue;
   var factories=0;for(var it=m.instructions.iterator();it.hasNext();){var call=it.next();if(call instanceof C&&String(call.owner)==='net/minecraft/world/entity/EntityType'&&String(call.desc)==='(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;')factories++;}if(factories!==1)throw new Error('Unexpected native spawning factory');
   var prefix=new L();prefix.add(new V(O.ALOAD,0));prefix.add(new V(O.ALOAD,1));prefix.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceSpawnBody','substitute','(Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/entity/EntityType;',false));prefix.add(new V(O.ASTORE,1));m.instructions.insert(prefix);m.maxStack=Math.max(m.maxStack,2);count++;
  }if(count!==1)throw new Error('Source individual substitution method count '+count);return node;
 }}};
}

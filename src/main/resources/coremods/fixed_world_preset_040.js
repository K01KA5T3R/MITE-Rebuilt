function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Ldc=Java.type('org.objectweb.asm.tree.LdcInsnNode');
 return {mite_fixed_server_world_type_040:{target:{type:'CLASS',name:'net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<init>'||String(m.desc)!=='(Lcom/google/gson/JsonObject;Ljava/lang/String;)V')continue;
   for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.PUTFIELD||String(x.owner)!==String(node.name)||String(x.desc)!=='Ljava/lang/String;')continue;
    var c=new List();c.add(new Insn(O.POP));c.add(new Ldc('minecraft:large_biomes'));m.instructions.insertBefore(x,c);count++;
   }
  }if(count!==1)throw new Error('Expected one dedicated world type assignment, got '+count);return node;
 }}};
}

// R195 has no villager ->witch lightning conversion. Use the native parent
// lightning handling so the villager still takes its normal damage/fire.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 return {mite_source_villager_lightning_042:{target:{type:'CLASS',name:'net.minecraft.world.entity.npc.Villager'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V')continue;
  var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,1));code.add(new Var(O.ALOAD,2));code.add(new Call(O.INVOKESPECIAL,String(node.superName),String(m.name),String(m.desc),false));code.add(new Insn(O.RETURN));m.instructions=code;m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.visibleLocalVariableAnnotations=null;m.invisibleLocalVariableAnnotations=null;m.maxStack=3;m.maxLocals=3;count++;
 }if(count!==1)throw new Error('Unexpected new042 Villager lightning layout '+count);return node;}}};
}

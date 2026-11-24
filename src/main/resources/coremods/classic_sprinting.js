function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 return {'mite_classic_sprinting':{target:{type:'CLASS',name:'net.minecraft.client.player.LocalPlayer'},transformer:function(node){
  var matches=0;
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i);if(String(method.desc)!=='()Z')continue;
   var foodRead=false;
   for(var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext())
    if(insn.getOpcode()===O.INVOKEVIRTUAL&&String(insn.owner)==='net/minecraft/world/food/FoodData'&&String(insn.desc)==='()I'&&(String(insn.name)==='getFoodLevel'||String(insn.name)==='m_38702_'))foodRead=true;
   if(!foodRead)continue;
   var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','canSprint','(Ljava/lang/Object;)Z',false));code.add(new Insn(O.IRETURN));method.instructions=code;
   method.tryCatchBlocks.clear();if(method.localVariables!==null)method.localVariables.clear();method.visibleLocalVariableAnnotations=null;method.invisibleLocalVariableAnnotations=null;method.maxStack=1;matches++;
  }
  if(matches!==1)throw new Error('Unexpected LocalPlayer food/sprint gate count '+matches);return node;
 }}};
}

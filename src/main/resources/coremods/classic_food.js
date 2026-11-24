function initializeCoreMod() {
 var Opcodes=Java.type('org.objectweb.asm.Opcodes');
 var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
 var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
 var InsnNode=Java.type('org.objectweb.asm.tree.InsnNode');
 var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 function replace(method,code,returnOpcode,stack) {
  code.add(new InsnNode(returnOpcode));method.instructions=code;method.tryCatchBlocks.clear();
  if(method.localVariables!==null)method.localVariables.clear();
  method.visibleLocalVariableAnnotations=null;method.invisibleLocalVariableAnnotations=null;method.maxStack=stack;
 }
 return {
  'mite_classic_food_player':{target:{type:'CLASS',name:'net.minecraft.world.entity.player.Player'},transformer:function(node){
   var ticks=0,eligibility=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);
    for(var insn=method.instructions.getFirst();insn!==null;){var next=insn.getNext();
     if(insn.getOpcode()===Opcodes.INVOKEVIRTUAL&&String(insn.owner)==='net/minecraft/world/food/FoodData'&&String(insn.desc)==='(Lnet/minecraft/world/entity/player/Player;)V'){
      method.instructions.set(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','tick','(Ljava/lang/Object;Ljava/lang/Object;)V',false));ticks++;
     }insn=next;
    }
    if((String(method.name)==='canEat'||String(method.name)==='m_36391_')&&String(method.desc)==='(Z)Z'){
     var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ILOAD,1));
     code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','canEat','(Ljava/lang/Object;Z)Z',false));
     replace(method,code,Opcodes.IRETURN,2);eligibility++;
    }
   }
   if(ticks!==1||eligibility!==1)throw new Error('Classic food: unexpected Player layout '+ticks+'/'+eligibility);return node;
  }},
  'mite_classic_food_eating':{target:{type:'CLASS',name:'net.minecraft.world.food.FoodData'},transformer:function(node){
   var generic=0,forge=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i),code=new InsnList(),desc=String(method.desc);
    if(desc==='(IF)V'){
     code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ILOAD,1));code.add(new VarInsnNode(Opcodes.FLOAD,2));
     code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','eatGeneric','(Ljava/lang/Object;IF)V',false));replace(method,code,Opcodes.RETURN,3);generic++;
    }else if(String(method.name)==='eat'&&desc==='(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V'){
     code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ALOAD,2));code.add(new VarInsnNode(Opcodes.ALOAD,3));
     code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','eat','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',false));replace(method,code,Opcodes.RETURN,3);forge++;
    }
   }
   if(generic!==1||forge!==1)throw new Error('Classic food: unexpected Forge FoodData layout '+generic+'/'+forge);return node;
  }}
 };
}

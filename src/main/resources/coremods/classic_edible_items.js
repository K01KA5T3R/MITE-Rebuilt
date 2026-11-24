function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Type=Java.type('org.objectweb.asm.tree.TypeInsnNode');
 return {'mite_classic_edible_items':{target:{type:'CLASS',name:'net.minecraft.world.item.Item'},transformer:function(node){
  var getter=null,readCount=0,edibleCount=0;
  for(var i=0;i<node.methods.size();i++){var method=node.methods.get(i);if(String(method.desc)==='()Lnet/minecraft/world/food/FoodProperties;'){if(getter!==null)throw new Error('Multiple Item food getters');getter=method;}}
  if(getter===null)throw new Error('Missing Item food getter');
  for(var insn=getter.instructions.getFirst();insn!==null;){var next=insn.getNext();
   if(insn.getOpcode()===O.ARETURN){var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/Diet','foodProperties','(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;',false));code.add(new Type(O.CHECKCAST,'net/minecraft/world/food/FoodProperties'));getter.instructions.insertBefore(insn,code);readCount++;}insn=next;
  }
  getter.maxStack=Math.max(2,getter.maxStack);
  for(var i=0;i<node.methods.size();i++){var method=node.methods.get(i);if(String(method.desc)!=='()Z')continue;
   for(var insn=method.instructions.getFirst();insn!==null;){var next=insn.getNext();
    if(insn.getOpcode()===O.GETFIELD&&String(insn.owner)===String(node.name)&&String(insn.desc)==='Lnet/minecraft/world/food/FoodProperties;'){
     method.instructions.set(insn,new Call(O.INVOKEVIRTUAL,String(node.name),String(getter.name),String(getter.desc),false));edibleCount++;
    }insn=next;
   }
  }
  if(readCount!==1||edibleCount!==1)throw new Error('Unexpected Item food layout '+readCount+'/'+edibleCount);return node;
 }}};
}

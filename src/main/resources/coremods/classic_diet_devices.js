function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {
  'mite_classic_cake':{target:{type:'CLASS',name:'net.minecraft.world.level.block.CakeBlock'},transformer:function(node){
   var gates=0,foods=0,slices=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);
    if(String(method.desc)!=='(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;')continue;
    for(var insn=method.instructions.getFirst();insn!==null;){var next=insn.getNext();
     if(insn.getOpcode()===O.INVOKEVIRTUAL&&String(insn.owner)==='net/minecraft/world/entity/player/Player'&&String(insn.desc)==='(Z)Z'){
      method.instructions.set(insn,new Call(O.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','canEatCake','(Ljava/lang/Object;Z)Z',false));gates++;
     }else if(insn.getOpcode()===O.INVOKEVIRTUAL&&String(insn.owner)==='net/minecraft/world/food/FoodData'&&String(insn.desc)==='(IF)V'){
      method.instructions.set(insn,new Call(O.INVOKESTATIC,'dev/koikaster/mite/SurvivalFood','eatCake','(Ljava/lang/Object;IF)V',false));foods++;
     }else if(insn.getOpcode()===O.BIPUSH&&insn.operand===6){insn.operand=5;slices++;}
     insn=next;
    }
   }
   if(gates!==1||foods!==1||slices!==1)throw new Error('Unexpected CakeBlock layout '+gates+'/'+foods+'/'+slices);return node;
  }},
  'mite_classic_milk':{target:{type:'CLASS',name:'net.minecraft.world.item.MilkBucketItem'},transformer:function(node){
   var count=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i);if(String(method.desc)!=='(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;')continue;
    for(var x=method.instructions.getFirst();x!==null;){var next=x.getNext();if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.name)==='curePotionEffects'&&String(x.desc)==='(Lnet/minecraft/world/item/ItemStack;)Z'){method.instructions.set(x,new Call(O.INVOKESTATIC,'dev/koikaster/mite/SourceMilkFinish1066','nativeEffects','(Ljava/lang/Object;Ljava/lang/Object;)Z',false));count++;}x=next;}
   }
   if(count!==1)throw new Error('Unexpected MilkBucketItem layout '+count);return node;
  }}
 };
}

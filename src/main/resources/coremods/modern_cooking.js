function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode');
 var helper='dev/koikaster/mite/ModernCooking',obj='Ljava/lang/Object;';
 function matches(m,name,srg,desc){return (String(m.name)===name||String(m.name)===srg)&&String(m.desc)===desc;}
 function guard(code,index){var resume=new Label();code.add(new Var(O.ALOAD,index));code.add(new Call(O.INVOKESTATIC,helper,'managed','('+obj+')Z',false));code.add(new Jump(O.IFEQ,resume));return resume;}
 function replace(m,method,descriptor,loads,result){var code=new List();for(var i=0;i<loads.length;i++)code.add(new Var(loads[i][0],loads[i][1]));code.add(new Call(O.INVOKESTATIC,helper,method,descriptor,false));code.add(new Insn(result));m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.instructions.add(code);m.maxStack=Math.max(m.maxStack,loads.length);}
 return {
  'mite_native_cooking_rules':{target:{type:'CLASS',name:'net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity'},transformer:function(node){
   var found={tick:0,place:0,take:0,set:0,xp:0};
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),code=new List(),resume;
    if(matches(m,'serverTick','m_155013_','(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)V')){
     resume=guard(code,3);for(var v=0;v<4;v++)code.add(new Var(O.ALOAD,v));code.add(new Call(O.INVOKESTATIC,helper,'furnaceTick','('+obj+obj+obj+obj+')V',false));code.add(new Insn(O.RETURN));code.add(resume);found.tick++;
    }else if(matches(m,'canPlaceItem','m_7013_','(ILnet/minecraft/world/item/ItemStack;)Z')){
     resume=guard(code,0);code.add(new Var(O.ALOAD,0));code.add(new Var(O.ILOAD,1));code.add(new Var(O.ALOAD,2));code.add(new Call(O.INVOKESTATIC,helper,'canPlace','('+obj+'I'+obj+')Z',false));code.add(new Insn(O.IRETURN));code.add(resume);found.place++;
    }else if(matches(m,'canTakeItemThroughFace','m_7157_','(ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Z')){
     resume=guard(code,0);code.add(new Var(O.ALOAD,0));code.add(new Var(O.ILOAD,1));code.add(new Var(O.ALOAD,2));code.add(new Var(O.ALOAD,3));code.add(new Call(O.INVOKESTATIC,helper,'canTake','('+obj+'I'+obj+obj+')Z',false));code.add(new Insn(O.IRETURN));code.add(resume);found.take++;
    }else if(matches(m,'setItem','m_6836_','(ILnet/minecraft/world/item/ItemStack;)V')){
     code.add(new Var(O.ALOAD,0));code.add(new Var(O.ILOAD,1));code.add(new Var(O.ALOAD,2));code.add(new Call(O.INVOKESTATIC,helper,'changedInput','('+obj+'I'+obj+')V',false));found.set++;
    }else if(matches(m,'getRecipesToAwardAndPopExperience','m_154995_','(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;')){
     resume=guard(code,0);code.add(new Call(O.INVOKESTATIC,'java/util/Collections','emptyList','()Ljava/util/List;',false));code.add(new Insn(O.ARETURN));code.add(resume);found.xp++;
    }else continue;
    m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,4);
   }
   for(var key in found)if(found[key]!==1)throw new Error('Unexpected native cooking '+key+':'+found[key]);return node;
  }},
  'mite_paid_campfire':{target:{type:'CLASS',name:'net.minecraft.world.level.block.entity.CampfireBlockEntity'},transformer:function(node){
   var found={cook:0,cold:0,recipe:0,place:0};
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);
    if(matches(m,'cookTick','m_155306_','(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;)V')){replace(m,'campfireTick','('+obj+obj+obj+obj+')V',[[O.ALOAD,0],[O.ALOAD,1],[O.ALOAD,2],[O.ALOAD,3]],O.RETURN);found.cook++;}
    else if(matches(m,'cooldownTick','m_155313_','(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;)V')){replace(m,'campfireCold','('+obj+obj+obj+obj+')V',[[O.ALOAD,0],[O.ALOAD,1],[O.ALOAD,2],[O.ALOAD,3]],O.RETURN);found.cold++;}
    else if(matches(m,'getCookableRecipe','m_59051_','(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;')){replace(m,'campfireRecipe','('+obj+obj+')Ljava/util/Optional;',[[O.ALOAD,0],[O.ALOAD,1]],O.ARETURN);found.recipe++;}
    else if(matches(m,'placeFood','m_238284_','(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;I)Z')){replace(m,'placeFood','('+obj+obj+obj+'I)Z',[[O.ALOAD,0],[O.ALOAD,1],[O.ALOAD,2],[O.ILOAD,3]],O.IRETURN);found.place++;}
   }
   for(var key in found)if(found[key]!==1)throw new Error('Unexpected campfire cooking '+key+':'+found[key]);return node;
  }}
 };
}

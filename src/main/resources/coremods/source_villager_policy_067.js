/** Fixed human V060. No configuration or player-visible explanatory text. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList');
 return {source_villager_policy_067:{target:{type:'CLASS',name:'net.minecraft.world.entity.npc.Villager'},transformer:function(node){var changes=0;
  for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k),name=String(m.name),desc=String(m.desc);
   if((name==='canBreed'||name==='m_35506_')&&desc==='()Z'||(name==='wantsToPickUp'||name==='m_7243_')&&desc==='(Lnet/minecraft/world/item/ItemStack;)Z'){
    m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.instructions.add(new I(O.ICONST_0));m.instructions.add(new I(O.IRETURN));m.maxStack=1;changes++;
   }else if(desc==='(Lnet/minecraft/world/entity/npc/VillagerData;)V'){
    var code=new L();code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceVillagerPolicy','data','(Lnet/minecraft/world/entity/npc/VillagerData;)Lnet/minecraft/world/entity/npc/VillagerData;',false));code.add(new V(O.ASTORE,1));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);changes++;
   }
  }if(changes!==3)throw new Error('Villager V060 method count '+changes);return node;
 }},source_villager_job_goals_067:{target:{type:'CLASS',name:'net.minecraft.world.entity.ai.behavior.VillagerGoalPackages'},transformer:function(node){var changes=0;
  for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k),name=String(m.name);
   if(name==='getCorePackage'||name==='m_24585_'){
    var count=0;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.ARETURN)continue;m.instructions.insertBefore(x,new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceVillagerPolicy','core','(Lcom/google/common/collect/ImmutableList;)Lcom/google/common/collect/ImmutableList;',false));count++;}if(count!==1)throw new Error('Villager core return count '+count);changes++;
   }else if(name==='getWorkPackage'||name==='m_24589_'){
    m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.instructions.add(new C(O.INVOKESTATIC,'com/google/common/collect/ImmutableList','of','()Lcom/google/common/collect/ImmutableList;',false));m.instructions.add(new I(O.ARETURN));m.maxStack=1;changes++;
   }
  }if(changes!==2)throw new Error('Villager job goal count '+changes);return node;
 }}};
}

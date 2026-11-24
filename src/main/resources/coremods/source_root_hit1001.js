function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode');
 function named(m,a,b){return String(m.name)===a||String(m.name)===b;}
 return {
  source_root_item_hit1001:{target:{type:'CLASS',name:'net.minecraft.world.item.Item'},transformer:function(n){var count=0;for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i);if(named(m,'hurtEnemy','m_7579_')&&String(m.desc)==='(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z'){
   m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();for(var j=0;j<4;j++)m.instructions.add(new V(O.ALOAD,j));m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceRootHit1001','hurtRoot1001','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));m.instructions.add(new I(O.IRETURN));m.maxStack=4;m.maxLocals=4;count++;
  }}if(count!==1)throw new Error('Source root Item hit entries '+count);return n;}},
  source_root_restock_use1001:{target:{type:'CLASS',name:'net.minecraft.client.Minecraft'},transformer:function(n){var count=0;for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i);if(named(m,'startUseItem','m_91277_')&&String(m.desc)==='()V'){
   var last=m.instructions.getLast();while(last!==null&&last.getOpcode()!==O.RETURN)last=last.getPrevious();if(last===null)throw new Error('Source root right-use fallthrough missing');var code=new L();code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/ClientRootItems1001','afterNoUse1001','()V',false));m.instructions.insertBefore(last,code);count++;
  }}if(count!==1)throw new Error('Source root use entries '+count);return n;}},
  source_root_restock_controller1001:{target:{type:'CLASS',name:'net.minecraft.client.multiplayer.MultiPlayerGameMode'},transformer:function(n){var found=null;for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i);if(named(m,'ensureHasSentCarriedItem','m_105297_')&&String(m.desc)==='()V'){if(found!==null)throw new Error('Duplicate carried sender');found=m;}}
   if(found===null)throw new Error('Source root carried sender missing');n.interfaces.add('dev/koikaster/mite/SourceRestockController1001');var m=new M(O.ACC_PUBLIC|O.ACC_SYNTHETIC,'mite$sendRestockSelection1001','()V',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new C(O.INVOKESPECIAL,String(n.name),String(found.name),'()V',false));m.instructions.add(new I(O.RETURN));m.maxStack=1;m.maxLocals=1;n.methods.add(m);return n;
  }}
 };
}

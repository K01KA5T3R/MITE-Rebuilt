function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 function previous(x){do{x=x.getPrevious();}while(x!==null&&x.getOpcode()<0);return x;}
 function constant(x,value){return x!==null&&((x.getOpcode()===O.BIPUSH||x.getOpcode()===O.SIPUSH)&&x.operand===value||x.getOpcode()===O.ICONST_1&&value===1);}
 return {'mite_craft_batch1722':{target:{type:'CLASS',name:'net.minecraft.world.inventory.InventoryMenu'},transformer:function(node){
  var count=0,nativeName=null,methodCount=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;')continue;methodCount++;
   for(var x=m.instructions.getFirst();x!==null;){var next=x.getNext();
    if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.desc)==='(Lnet/minecraft/world/item/ItemStack;IIZ)Z'&&(String(x.owner)===String(node.name)||String(x.owner)==='net/minecraft/world/inventory/AbstractContainerMenu')){
     var reverse=previous(x),end=previous(reverse),start=previous(end);
     if(constant(start,9)&&constant(end,45)&&constant(reverse,1)){
      nativeName=String(x.name);m.instructions.set(x,new Call(O.INVOKESTATIC,'dev/koikaster/mite/SourceCraftBatch1722','move','(Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;IIZ)Z',false));count++;
     }
    }x=next;
   }
  }
  if(methodCount!==1||count!==1)throw new Error('Expected one inventory result-only batch transfer, got '+methodCount+'/'+count);
  var iface='dev/koikaster/mite/SourceCraftBatch1722$Access';if(node.interfaces.contains(iface))throw new Error('Duplicate crafting batch bridge');node.interfaces.add(iface);
  var bridge=new Method(O.ACC_PUBLIC,'mite$moveOneCraft1722','(Lnet/minecraft/world/item/ItemStack;IIZ)Z',null,null);
  bridge.instructions.add(new Var(O.ALOAD,0));bridge.instructions.add(new Var(O.ALOAD,1));bridge.instructions.add(new Var(O.ILOAD,2));bridge.instructions.add(new Var(O.ILOAD,3));bridge.instructions.add(new Var(O.ILOAD,4));
  bridge.instructions.add(new Call(O.INVOKEVIRTUAL,String(node.name),nativeName,'(Lnet/minecraft/world/item/ItemStack;IIZ)Z',false));bridge.instructions.add(new Insn(O.IRETURN));bridge.maxStack=5;bridge.maxLocals=5;node.methods.add(bridge);return node;
 }}};
}

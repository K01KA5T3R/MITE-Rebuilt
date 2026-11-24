function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 function hook(name){var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/InventoryCrafting',name,'(Ljava/lang/Object;)V',false));return code;}
 return {'mite_inventory_crafting':{target:{type:'CLASS',name:'net.minecraft.world.inventory.InventoryMenu'},transformer:function(node){
  var allocations=0,constructors=0,changes=0,closeStart=0,closeEnd=0,quick=0;
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i),desc=String(method.desc);
   for(var insn=method.instructions.getFirst();insn!==null;){var next=insn.getNext();
    if(String(method.name)==='<init>'){
     if(insn.getOpcode()===O.NEW&&String(insn.desc)==='net/minecraft/world/inventory/ResultSlot'){insn.desc='dev/koikaster/mite/InventoryCrafting$OutputSlot';allocations++;}
     if(insn.getOpcode()===O.INVOKESPECIAL&&String(insn.owner)==='net/minecraft/world/inventory/ResultSlot'&&String(insn.name)==='<init>'){insn.owner='dev/koikaster/mite/InventoryCrafting$OutputSlot';constructors++;}
    }
    if(desc==='(Lnet/minecraft/world/Container;)V'&&insn.getOpcode()===O.RETURN){method.instructions.insertBefore(insn,hook('changed'));changes++;}
    if(desc==='(Lnet/minecraft/world/entity/player/Player;)V'&&insn.getOpcode()===O.RETURN){method.instructions.insertBefore(insn,hook('endClose'));closeEnd++;}
    insn=next;
   }
   if(desc==='(Lnet/minecraft/world/entity/player/Player;)V'){method.instructions.insert(hook('beginClose'));closeStart++;}
   if(desc==='(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;'){
    var code=new List(),allowed=new Label();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ILOAD,2));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/InventoryCrafting','canQuickMove','(Ljava/lang/Object;I)Z',false));code.add(new Jump(O.IFNE,allowed));
    code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/InventoryCrafting','emptyResult','()Lnet/minecraft/world/item/ItemStack;',false));code.add(new Insn(O.ARETURN));code.add(allowed);code.add(new Frame(O.F_SAME,0,null,0,null));method.instructions.insert(code);quick++;
   }
   method.maxStack=Math.max(method.maxStack,2);
  }
  if(allocations!==1||constructors!==1||changes!==1||closeStart!==1||closeEnd!==1||quick!==1)throw new Error('Unexpected InventoryMenu layout '+allocations+'/'+constructors+'/'+changes+'/'+closeStart+'/'+closeEnd+'/'+quick);
  return node;
 }}};
}

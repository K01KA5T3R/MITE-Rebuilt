function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 return {'mite_click_crafting':{target:{type:'CLASS',name:'net.minecraft.world.inventory.AbstractContainerMenu'},transformer:function(node){
  var count=0;
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i);
   if(String(method.desc)!=='(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V'||(method.access&O.ACC_PUBLIC)===0)continue;
   var code=new List(),proceed=new Label();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ILOAD,1));code.add(new Var(O.ILOAD,2));code.add(new Var(O.ALOAD,3));code.add(new Var(O.ALOAD,4));
   code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/InventoryCrafting','click','(Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;)Z',false));code.add(new Jump(O.IFEQ,proceed));code.add(new Insn(O.RETURN));code.add(proceed);code.add(new Frame(O.F_SAME,0,null,0,null));method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,5);count++;
  }
  if(count!==1)throw new Error('Unexpected AbstractContainerMenu click dispatch '+count);return node;
 }}};
}

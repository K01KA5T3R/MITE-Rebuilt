function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {'mite_container_stack182':{target:{type:'CLASS',name:'net.minecraft.world.inventory.AbstractContainerMenu'},transformer:function(node){
  var methods=0,count=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/world/item/ItemStack;IIZ)Z')continue;methods++;
   for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/inventory/Slot'&&(String(x.name)==='getMaxStackSize'||String(x.name)==='m_6641_')&&String(x.desc)==='()I'){
    m.instructions.insertBefore(x,new Var(O.ALOAD,1));m.instructions.set(x,new Call(O.INVOKESTATIC,'dev/koikaster/mite/ClassicStacks','slotMax','(Ljava/lang/Object;Ljava/lang/Object;)I',false));count++;
   }}m.maxStack++;
  }
  if(methods!==1||count!==3)throw new Error('Expected three quick-move slot limit reads, got '+methods+'/'+count);
  return node;
 }}};
}

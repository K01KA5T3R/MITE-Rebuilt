function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_source_repair_native_loot:{target:{type:'CLASS',name:'net.minecraft.world.entity.Mob'},transformer:function(node){
  var methods=0,returns=0;
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i);
   if((String(method.name)!=='wantsToPickUp'&&String(method.name)!=='m_7243_')||String(method.desc)!=='(Lnet/minecraft/world/item/ItemStack;)Z')continue;
   methods++;
   for(var it=method.instructions.iterator();it.hasNext();){
    var x=it.next();
    if(x.getOpcode()===O.INVOKESTATIC&&String(x.owner)==='dev/koikaster/mite/OriginalRepairNativeLoot')throw new Error('Duplicate native repair loot guard');
    if(x.getOpcode()!==O.IRETURN)continue;
    var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,1));
    code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/OriginalRepairNativeLoot','preserve','(ZLjava/lang/Object;Ljava/lang/Object;)Z',false));
    method.instructions.insertBefore(x,code);returns++;
   }
   method.maxStack=Math.max(3,method.maxStack);
  }
  if(methods!==1||returns!==1)throw new Error('Native repair loot guard sites '+methods+'/'+returns);
  return node;
 }}};
}

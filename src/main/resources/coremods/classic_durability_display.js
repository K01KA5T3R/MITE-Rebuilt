function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 return {mite_consistent_item_durability:{target:{type:'CLASS',name:'net.minecraft.world.item.Item'},transformer:function(node){
  var baseline=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)==='()I'&&(String(m.name)==='getMaxDamage'||String(m.name)==='m_41462_'))baseline=String(m.name);if(String(m.name)==='getMaxDamage'&&String(m.desc)==='(Lnet/minecraft/world/item/ItemStack;)I')throw new Error('Item already overrides Forge stack maximum');}if(baseline===null)throw new Error('Item baseline durability method missing');
  var m=new Method(O.ACC_PUBLIC,'getMaxDamage','(Lnet/minecraft/world/item/ItemStack;)I',null,null);m.instructions.add(new Var(O.ALOAD,0));m.instructions.add(new Call(O.INVOKEVIRTUAL,node.name,baseline,'()I',false));m.instructions.add(new Var(O.ALOAD,1));m.instructions.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/ClassicToolWear','maxDamage','(ILjava/lang/Object;)I',false));m.instructions.add(new Insn(O.IRETURN));m.maxStack=2;m.maxLocals=2;node.methods.add(m);return node;
 }}};
}

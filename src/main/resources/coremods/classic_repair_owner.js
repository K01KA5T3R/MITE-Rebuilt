function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),FieldNode=Java.type('org.objectweb.asm.tree.FieldNode'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_source_repair_owner:{target:{type:'CLASS',name:'net.minecraft.world.entity.Mob'},transformer:function(node){
  var descriptor='Ljava/lang/Object;',field='mite$repairState';node.interfaces.add('dev/koikaster/mite/SourceRepairOwner');node.fields.add(new FieldNode(O.ACC_PRIVATE|O.ACC_TRANSIENT,field,descriptor,null,null));
  var get=new Method(O.ACC_PUBLIC,'mite$repairOwner','()'+descriptor,null,null);get.instructions.add(new Var(O.ALOAD,0));get.instructions.add(new Field(O.GETFIELD,node.name,field,descriptor));get.instructions.add(new Insn(O.ARETURN));get.maxStack=1;get.maxLocals=1;node.methods.add(get);
  var set=new Method(O.ACC_PUBLIC,'mite$repairOwner','('+descriptor+')V',null,null);set.instructions.add(new Var(O.ALOAD,0));set.instructions.add(new Var(O.ALOAD,1));set.instructions.add(new Field(O.PUTFIELD,node.name,field,descriptor));set.instructions.add(new Insn(O.RETURN));set.maxStack=2;set.maxLocals=2;node.methods.add(set);
  var count=0;for(var i=0;i<node.methods.size();i++){var method=node.methods.get(i);if((String(method.name)!=='aiStep'&&String(method.name)!=='m_8107_')||String(method.desc)!=='()V')continue;
   for(var it=method.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.RETURN)continue;var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/OriginalRepairOwners','afterAi','('+descriptor+')V',false));method.instructions.insertBefore(x,code);count++;}method.maxStack=Math.max(2,method.maxStack);
  }if(count!==1)throw new Error('Native repair post-AI sites '+count);return node;
 }}};
}

/**075 admission plus077 source parent knockback before health iframes. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode');
 return {source_melee_admission_075:{target:{type:'CLASS',name:'net.minecraft.world.entity.LivingEntity'},transformer:function(node){
  var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)!=='hurt'&&String(m.name)!=='m_6469_')||String(m.desc)!=='(Lnet/minecraft/world/damagesource/DamageSource;F)Z')continue;
   for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x instanceof C&&x.getOpcode()===O.INVOKEVIRTUAL&&(String(x.name)==='isSleeping'||String(x.name)==='m_5803_')&&String(x.desc)==='()Z'){
    var code=new L(),resume=new Label();code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceMeleeResultScope','admitted','(Ljava/lang/Object;Ljava/lang/Object;)V',false));
    code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new V(O.FLOAD,2));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceKnockbackNative','beforeHurt','(Ljava/lang/Object;Ljava/lang/Object;F)Z',false));code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceSnowballTraits','parentGate','(ZLjava/lang/Object;Ljava/lang/Object;)Z',false));code.add(new J(O.IFEQ,resume));code.add(new I(O.POP));code.add(new I(O.ICONST_0));code.add(new I(O.IRETURN));code.add(resume);m.instructions.insert(x,code);count++;
   }}m.maxStack=Math.max(m.maxStack,5);
  }if(count!==1)throw new Error('075 actual hurt admission boundary count '+count);return node;
 }}};
}

/** Runtime-only of clock/yaw fields and exclusion of duplicate modern hurt
 * knockback. Standalone native knockback calls retain their own behavior. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),FN=Java.type('org.objectweb.asm.tree.FieldNode'),MN=Java.type('org.objectweb.asm.tree.MethodNode');
 function method(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function field(node,name,desc,load,ret){
  node.fields.add(new FN(O.ACC_PRIVATE|O.ACC_SYNTHETIC,name,desc,null,null));
  var get=new MN(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,name,'()'+desc,null,null);get.instructions.add(new V(O.ALOAD,0));get.instructions.add(new F(O.GETFIELD,node.name,name,desc));get.instructions.add(new I(ret));get.maxStack=1;get.maxLocals=1;node.methods.add(get);
  var set=new MN(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,name,'('+desc+')V',null,null);set.instructions.add(new V(O.ALOAD,0));set.instructions.add(new V(load,1));set.instructions.add(new F(O.PUTFIELD,node.name,name,desc));set.instructions.add(new I(O.RETURN));set.maxStack=2;set.maxLocals=2;node.methods.add(set);
 }
 return {source_knockback_077:{target:{type:'CLASS',name:'net.minecraft.world.entity.LivingEntity'},transformer:function(node){
  var clock=0,duplicate=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),a=m.instructions.toArray();
   if(method(m,'baseTick','m_6075_','()V')){
    for(var k=0;k<a.length;k++){var x=a[k];if(x instanceof C&&(String(x.name)==='isDeadOrDying'||String(x.name)==='m_21224_')&&String(x.desc)==='()Z'){
     var code=new L();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceKnockbackNative','tick','(Ljava/lang/Object;)V',false));m.instructions.insertBefore(x,code);clock++;
    }}m.maxStack=Math.max(m.maxStack,3);
   }
   if(method(m,'hurt','m_6469_','(Lnet/minecraft/world/damagesource/DamageSource;F)Z')){
    // The source wrapper already selected direction, including coincident RNG.
    // Skip the entire later modern branch, avoiding a second RNG draw/impulse.
    for(var k=0;k<a.length;k++){var x=a[k];if(x instanceof C&&(String(x.name)==='knockback'||String(x.name)==='m_147240_')&&String(x.desc)==='(DDD)V'){
     var gate=null;
     for(var p=x.getPrevious();p!==null;p=p.getPrevious())if(p instanceof J&&p.getOpcode()===O.IFNE){var prev=p.getPrevious();if(prev instanceof C&&String(prev.owner)==='net/minecraft/world/damagesource/DamageSource'&&String(prev.desc)==='(Lnet/minecraft/tags/TagKey;)Z'){gate=p;break;}}
     if(gate===null)throw new Error('077 missing native explosion/knockback guard');
     m.instructions.insertBefore(gate,new I(O.POP));gate.setOpcode(O.GOTO);duplicate++;
    }}m.maxStack=Math.max(m.maxStack,5);
   }
  }
  if(clock!==1||duplicate!==1)throw new Error('077 parent knockback boundaries '+clock+'/'+duplicate);
  node.interfaces.add('dev/koikaster/mite/SourceKnockbackBody');field(node,'mite$knockbackTicks','I',O.ILOAD,O.IRETURN);field(node,'mite$attackedYaw','F',O.FLOAD,O.FRETURN);return node;
 }}};
}

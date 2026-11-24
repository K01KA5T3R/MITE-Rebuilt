/** Add the missing source speed-unit adapter; all existing Spider methods,
 * old AI/attack/climb/poison owners and native air movement are unchanged. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {mite_source_arachnid_mobility_043:{target:{type:'CLASS',name:'net.minecraft.world.entity.monster.Spider'},transformer:function(node){
  var official=false,srg=false;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if(String(m.name)==='registerGoals')official=true;if(String(m.name)==='m_8099_')srg=true;
   if((String(m.name)==='setSpeed'||String(m.name)==='m_7910_')&&String(m.desc)==='(F)V')throw new Error('Unexpected existing Spider speed owner');
  }
  if(official===srg)throw new Error('Unknown Spider mapping');
  var name=srg?'m_7910_':'setSpeed';var m=new M(O.ACC_PUBLIC,name,'(F)V',null,null),code=m.instructions;
  code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,0));code.add(new V(O.FLOAD,1));
  code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceArachnidMobility','motor','(Ljava/lang/Object;F)F',false));
  code.add(new C(O.INVOKESPECIAL,node.superName,name,'(F)V',false));
  code.add(new V(O.ALOAD,0));code.add(new V(O.FLOAD,1));
  code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceArachnidMobility','forward','(Ljava/lang/Object;F)V',false));
  code.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=2;node.methods.add(m);return node;
 }}};
}

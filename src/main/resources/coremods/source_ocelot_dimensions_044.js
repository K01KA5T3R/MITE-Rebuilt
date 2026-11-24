/** New rx dimensions only; the previously verified lifecycle transformer is frozen. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {source_ocelot_dimensions_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.Ocelot'},transformer:function(node){
  var official=false,srg=false;for(var n=0;n<node.methods.size();n++){var m=node.methods.get(n);if(String(m.name)==='registerGoals')official=true;if(String(m.name)==='m_8099_')srg=true;if((String(m.name)==='getDimensions'||String(m.name)==='m_6972_')&&String(m.desc)==='(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;')throw new Error('Unexpected existing Ocelot dimensions');}
  if(official===srg)throw new Error('Unknown Ocelot mapping');var m=new M(O.ACC_PUBLIC,srg?'m_6972_':'getDimensions','(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceOcelotOwners','dimensions','(Ljava/lang/Object;)Lnet/minecraft/world/entity/EntityDimensions;',false));m.instructions.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=2;node.methods.add(m);return node;
 }}};
}

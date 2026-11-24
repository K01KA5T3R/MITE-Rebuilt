/** New041 Bat parent bridges and only its EntityType constructor handle.
 * Native Bat methods and all other factory instructions remain unchanged. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),H=Java.type('org.objectweb.asm.Handle'),D=Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {
  mite_source_bat_parent_041:{target:{type:'CLASS',name:'net.minecraft.world.entity.ambient.Bat'},transformer:function(node){
   var tick=null,hurt=null,hd='(Lnet/minecraft/world/damagesource/DamageSource;F)Z';
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)==='()V'&&(String(m.name)==='tick'||String(m.name)==='m_8119_'))tick=String(m.name);if(String(m.desc)===hd&&(String(m.name)==='hurt'||String(m.name)==='m_6469_'))hurt=String(m.name);if(String(m.name).startsWith('mite$batParent'))throw new Error('Bat bridge already exists');}
   if(tick===null||hurt===null||String(node.superName)!=='net/minecraft/world/entity/ambient/AmbientCreature')throw new Error('Unexpected native Bat parent API');
   node.interfaces.add('dev/koikaster/mite/SourceBatNativeParent');
   var t=new M(O.ACC_PUBLIC,'mite$batParentTick','()V',null,null);t.instructions.add(new V(O.ALOAD,0));t.instructions.add(new C(O.INVOKESPECIAL,node.superName,tick,'()V',false));t.instructions.add(new I(O.RETURN));t.maxStack=1;t.maxLocals=1;node.methods.add(t);
   var h=new M(O.ACC_PUBLIC,'mite$batParentHurt',hd,null,null);h.instructions.add(new V(O.ALOAD,0));h.instructions.add(new V(O.ALOAD,1));h.instructions.add(new V(O.FLOAD,2));h.instructions.add(new C(O.INVOKESPECIAL,node.superName,hurt,hd,false));h.instructions.add(new I(O.IRETURN));h.maxStack=3;h.maxLocals=3;node.methods.add(h);return node;
  }},
  mite_source_bat_factory_041:{target:{type:'CLASS',name:'net.minecraft.world.entity.EntityType'},transformer:function(node){
   var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(!(x instanceof D))continue;for(var k=0;k<x.bsmArgs.length;k++){var h=x.bsmArgs[k];if(!(h instanceof H)||String(h.getOwner())!=='net/minecraft/world/entity/ambient/Bat'||String(h.getName())!=='<init>')continue;if(h.getTag()!==O.H_NEWINVOKESPECIAL||String(h.getDesc())!=='(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V')throw new Error('Unexpected Bat factory handle');x.bsmArgs[k]=new H(h.getTag(),'dev/koikaster/mite/SourceBat',h.getName(),h.getDesc(),h.isInterface());count++;}}}
   if(count!==1)throw new Error('Expected one native Bat constructor handle, got '+count);return node;
  }}
 };
}

/** Additional tw.kill parent bridge, separate from the four sealed64 proofs. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {source_pigman_kill_parent_064:{target:{type:'CLASS',name:'net.minecraft.world.entity.monster.Zombie'},transformer:function(node){
  if(String(node.superName)!=='net/minecraft/world/entity/monster/Monster')throw new Error('Unexpected Zombie kill parent');var old=null,desc='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)Z';
  for(var k=0;k<node.methods.size();k++){var m=node.methods.get(k);if((String(m.name)==='killedEntity'||String(m.name)==='m_214076_')&&String(m.desc)===desc)old=m;}if(old===null)throw new Error('Missing native Zombie killedEntity');
  var m=new M(O.ACC_PUBLIC,'mite$pigmanMonsterKilledEntity',desc,null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new V(O.ALOAD,2));m.instructions.add(new C(O.INVOKESPECIAL,node.superName,String(old.name),desc,false));m.instructions.add(new I(O.IRETURN));m.maxStack=m.maxLocals=3;node.methods.add(m);return node;
 }}};
}

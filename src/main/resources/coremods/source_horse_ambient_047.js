/** Original of.f() eye plane for the five rs appearances only. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {source_horse_ambient_047:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.horse.AbstractHorse'},transformer:function(n){var found=0;
  for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i),name=String(m.name);if((name==='getStandingEyeHeight'||name==='m_6431_')&&String(m.desc)==='(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F')for(var it=m.instructions.iterator();it.hasNext();){var at=it.next();if(at.getOpcode()===O.FRETURN){var code=new L();code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,2));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceHorseAmbient','eyeHeight','(FLjava/lang/Object;Ljava/lang/Object;)F',false));m.instructions.insertBefore(at,code);m.maxStack=Math.max(m.maxStack,3);found++;}}}
  if(found!==1)throw new Error('Horse047 eye plane sites '+found);return n;
 }}};
}

/** New rs environment/riding state only. Existing hurt and save/load owner stays intact. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),F=Java.type('org.objectweb.asm.tree.FieldNode'),FI=Java.type('org.objectweb.asm.tree.FieldInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode');
 var owner='dev/koikaster/mite/SourceHorseBehavior';
 return {source_horse_behavior_047:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.horse.AbstractHorse'},transformer:function(n){var ticks=0,eat=0,ride=0;
  for(var i=0;i<n.fields.size();i++)if(String(n.fields.get(i).name)==='mite$horseBehavior')throw new Error('Duplicate047 behavior state');
  for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i),name=String(m.name),code=new L();
   if((name==='aiStep'||name==='m_8107_')&&String(m.desc)==='()V'){for(var it=m.instructions.iterator();it.hasNext();){var at=it.next();if(at.getOpcode()===O.RETURN){code=new L();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,owner,'afterAi','(Ljava/lang/Object;)V',false));m.instructions.insertBefore(at,code);ticks++;}}m.maxStack=Math.max(m.maxStack,1);}
   else if((name==='setEating'||name==='m_30661_')&&String(m.desc)==='(Z)V'){var resume=new Label();code.add(new V(O.ALOAD,0));code.add(new V(O.ILOAD,1));code.add(new C(O.INVOKESTATIC,owner,'allowGrazing','(Ljava/lang/Object;Z)Z',false));code.add(new J(O.IFNE,resume));code.add(new I(O.RETURN));code.add(resume);m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,2);eat++;}
   else if((name==='doPlayerRide'||name==='m_6835_')&&String(m.desc)==='(Lnet/minecraft/world/entity/player/Player;)V'){var resume=new Label();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,owner,'allowRide','(Ljava/lang/Object;)Z',false));code.add(new J(O.IFNE,resume));code.add(new I(O.RETURN));code.add(resume);m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);ride++;}
  }if(ticks!==1||eat!==1||ride!==1)throw new Error('Horse047 sites '+ticks+'/'+eat+'/'+ride);
  n.interfaces.add('dev/koikaster/mite/SourceHorseBehaviorHost');n.fields.add(new F(O.ACC_PRIVATE,'mite$horseBehavior','Ljava/lang/Object;',null,null));
  var m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseBehavior','()Ljava/lang/Object;',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new FI(O.GETFIELD,n.name,'mite$horseBehavior','Ljava/lang/Object;'));m.instructions.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;n.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseBehavior','(Ljava/lang/Object;)V',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new FI(O.PUTFIELD,n.name,'mite$horseBehavior','Ljava/lang/Object;'));m.instructions.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=2;n.methods.add(m);return n;
 }}};
}

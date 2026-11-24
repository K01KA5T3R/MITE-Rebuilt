/** rs lifecycle without replacing native feeding, breeding or inventory. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),F=Java.type('org.objectweb.asm.tree.FieldNode'),FI=Java.type('org.objectweb.asm.tree.FieldInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),N=Java.type('org.objectweb.asm.tree.IntInsnNode');
 var OWNER='dev/koikaster/mite/SourceHorseOwners',HD='(Lnet/minecraft/world/damagesource/DamageSource;F)Z';
 function matches(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function hook(m,name,head,argument){var make=function(){var x=new L();x.add(new V(O.ALOAD,0));if(argument)x.add(new V(O.ALOAD,1));x.add(new C(O.INVOKESTATIC,OWNER,name,argument?'(Ljava/lang/Object;Ljava/lang/Object;)V':'(Ljava/lang/Object;)V',false));return x;};if(head){m.instructions.insert(make());return 1;}var n=0;for(var it=m.instructions.iterator();it.hasNext();){var at=it.next();if(at.getOpcode()===O.RETURN){m.instructions.insertBefore(at,make());n++;}}return n;}
 return {source_horse_harm_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.horse.AbstractHorse'},transformer:function(node){
  var hurt=null,ticks=0,saves=0,loads=0,poses=0,srg=false,stand=null,setEating=null,setFlag=null,angry=null;
  for(var k=0;k<node.fields.size();k++)if(String(node.fields.get(k).name)==='mite$horseState')throw new Error('Duplicate horse state');
  for(var n=0;n<node.methods.size();n++){
   var m=node.methods.get(n);
   if(matches(m,'hurt','m_6469_',HD)){if(hurt!==null)throw new Error('Duplicate horse hurt');hurt=m;srg=String(m.name)==='m_6469_';}
   else if(matches(m,'aiStep','m_8107_','()V')){ticks+=hook(m,'beforeAi',true,false);m.maxStack=Math.max(m.maxStack,1);}
   else if(matches(m,'addAdditionalSaveData','m_7380_','(Lnet/minecraft/nbt/CompoundTag;)V')){saves+=hook(m,'save',false,true);m.maxStack=Math.max(m.maxStack,2);}
   else if(matches(m,'readAdditionalSaveData','m_7378_','(Lnet/minecraft/nbt/CompoundTag;)V')){loads+=hook(m,'load',false,true);m.maxStack=Math.max(m.maxStack,2);}
   if(matches(m,'setEating','m_30661_','(Z)V')||matches(m,'setStanding','m_30665_','(Z)V')){
    if(matches(m,'setEating','m_30661_','(Z)V'))setEating=String(m.name);
    var x=new L(),resume=new Label();x.add(new V(O.ALOAD,0));x.add(new V(O.ILOAD,1));x.add(new C(O.INVOKESTATIC,OWNER,'allowIdlePose','(Ljava/lang/Object;Z)Z',false));x.add(new J(O.IFNE,resume));x.add(new I(O.RETURN));x.add(resume);m.instructions.insert(x);m.maxStack=Math.max(m.maxStack,2);poses++;
   }
   if(matches(m,'standIfPossible','m_247525_','()V'))for(var it=m.instructions.iterator();it.hasNext();){var a=it.next();if(a instanceof FI&&a.getOpcode()===O.PUTFIELD&&String(a.desc)==='I'){if(stand!==null)throw new Error('Ambiguous rearing timer');stand=String(a.name);}}
   if(String(m.desc)==='(IZ)V')for(var it=m.instructions.iterator();it.hasNext();){var a=it.next();if(a instanceof C&&String(a.owner)==='net/minecraft/network/syncher/SynchedEntityData')setFlag=String(m.name);}
   if(matches(m,'getAngrySound','m_7871_','()Lnet/minecraft/sounds/SoundEvent;'))angry=String(m.name);
   if(matches(m,'setSpeed','m_7910_','(F)V'))throw new Error('Unexpected existing horse speed owner');
  }
  if(hurt===null||ticks!==1||saves!==1||loads!==1||poses!==2||stand===null||setEating===null||setFlag===null||angry===null)throw new Error('Horse entry sites '+ticks+'/'+saves+'/'+loads+'/'+poses+'/'+stand+'/'+setEating+'/'+setFlag+'/'+angry);
  var nativeName=String(hurt.name);hurt.name='mite$vanillaHorseHurt';node.interfaces.add('dev/koikaster/mite/SourceHorseNativeHurt');node.fields.add(new F(O.ACC_PRIVATE,'mite$horseState','Ljava/lang/Object;',null,null));
  var m=new M(O.ACC_PUBLIC,nativeName,HD,null,null),x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new V(O.FLOAD,2));x.add(new C(O.INVOKESTATIC,OWNER,'hurt','(Ljava/lang/Object;Ljava/lang/Object;F)Z',false));x.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=3;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseState','()Ljava/lang/Object;',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new FI(O.GETFIELD,node.name,'mite$horseState','Ljava/lang/Object;'));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseState','(Ljava/lang/Object;)V',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new FI(O.PUTFIELD,node.name,'mite$horseState','Ljava/lang/Object;'));x.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=2;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseHurtRear','()V',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new I(O.ICONST_1));x.add(new FI(O.PUTFIELD,node.name,stand,'I'));x.add(new V(O.ALOAD,0));x.add(new I(O.ICONST_0));x.add(new C(O.INVOKEVIRTUAL,node.name,setEating,'(Z)V',false));x.add(new V(O.ALOAD,0));x.add(new N(O.BIPUSH,32));x.add(new I(O.ICONST_1));x.add(new C(O.INVOKEVIRTUAL,node.name,setFlag,'(IZ)V',false));x.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=1;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$horseAngrySound','()Lnet/minecraft/sounds/SoundEvent;',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKEVIRTUAL,node.name,angry,'()Lnet/minecraft/sounds/SoundEvent;',false));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;node.methods.add(m);
  m=new M(O.ACC_PUBLIC,srg?'m_7910_':'setSpeed','(F)V',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,0));x.add(new V(O.FLOAD,1));x.add(new C(O.INVOKESTATIC,OWNER,'motor','(Ljava/lang/Object;F)F',false));x.add(new C(O.INVOKESPECIAL,node.superName,srg?'m_7910_':'setSpeed','(F)V',false));x.add(new V(O.ALOAD,0));x.add(new V(O.FLOAD,1));x.add(new C(O.INVOKESTATIC,OWNER,'forward','(Ljava/lang/Object;F)V',false));x.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=2;node.methods.add(m);
  return node;
 }}};
}

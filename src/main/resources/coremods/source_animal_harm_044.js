/** New livestock caller/AI/NBT hooks; original Animal hurt body retained verbatim. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),F=Java.type('org.objectweb.asm.tree.FieldNode'),FI=Java.type('org.objectweb.asm.tree.FieldInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode');
 var OWNER='dev/koikaster/mite/SourceLivestockOwners',HD='(Lnet/minecraft/world/damagesource/DamageSource;F)Z';
 function matches(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function hook(m,name,atHead,argument){var make=function(){var x=new L();x.add(new V(O.ALOAD,0));if(argument)x.add(new V(O.ALOAD,1));x.add(new C(O.INVOKESTATIC,OWNER,name,argument?'(Ljava/lang/Object;Ljava/lang/Object;)V':'(Ljava/lang/Object;)V',false));return x;};if(atHead){m.instructions.insert(make());return 1;}var n=0;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.RETURN){m.instructions.insertBefore(x,make());n++;}}return n;}
 return {source_animal_harm_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.Animal'},transformer:function(node){
  var hurt=null,ticks=0,saves=0,loads=0,srg=false;
  for(var k=0;k<node.fields.size();k++)if(String(node.fields.get(k).name)==='mite$livestockState')throw new Error('Duplicate livestock state');
  for(var n=0;n<node.methods.size();n++){
   var m=node.methods.get(n);
   if(matches(m,'hurt','m_6469_',HD)){if(hurt!==null)throw new Error('Duplicate Animal hurt');hurt=m;srg=String(m.name)==='m_6469_';}
   else if(matches(m,'aiStep','m_8107_','()V')){ticks+=hook(m,'beforeAi',true,false);if(hook(m,'afterAi',false,false)!==1)throw new Error('Animal ai returns');m.maxStack=Math.max(m.maxStack,1);}
   else if(matches(m,'addAdditionalSaveData','m_7380_','(Lnet/minecraft/nbt/CompoundTag;)V')){saves+=hook(m,'save',false,true);m.maxStack=Math.max(m.maxStack,2);}
   else if(matches(m,'readAdditionalSaveData','m_7378_','(Lnet/minecraft/nbt/CompoundTag;)V')){loads+=hook(m,'load',false,true);m.maxStack=Math.max(m.maxStack,2);}
  }
  if(hurt===null||ticks!==1||saves!==1||loads!==1)throw new Error('Animal entry sites '+ticks+'/'+saves+'/'+loads);
  var nativeName=String(hurt.name);hurt.name='mite$vanillaAnimalHurt';node.interfaces.add('dev/koikaster/mite/SourceAnimalNativeHurt');node.fields.add(new F(O.ACC_PRIVATE,'mite$livestockState','Ljava/lang/Object;',null,null));
  var m=new M(O.ACC_PUBLIC,nativeName,HD,null,null),x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new V(O.FLOAD,2));x.add(new C(O.INVOKESTATIC,OWNER,'hurt','(Ljava/lang/Object;Ljava/lang/Object;F)Z',false));x.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=3;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$animalParentHurt',HD,null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new V(O.FLOAD,2));x.add(new C(O.INVOKESPECIAL,node.superName,nativeName,HD,false));x.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=3;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$animalState','()Ljava/lang/Object;',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new FI(O.GETFIELD,node.name,'mite$livestockState','Ljava/lang/Object;'));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;node.methods.add(m);
  m=new M(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$animalState','(Ljava/lang/Object;)V',null,null);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new FI(O.PUTFIELD,node.name,'mite$livestockState','Ljava/lang/Object;'));x.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=2;node.methods.add(m);
  return node;
 }}};
}

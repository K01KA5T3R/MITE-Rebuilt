/** Wild rx lifecycle; modern Cat and the frozen Wolf/TamableAnimal paths are separate. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),F=Java.type('org.objectweb.asm.tree.FieldNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),FI=Java.type('org.objectweb.asm.tree.FieldInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),D=Java.type('org.objectweb.asm.tree.LdcInsnNode');
 var P='dev/koikaster/mite/',H=P+'SourceOcelotOwners',OBJ='Ljava/lang/Object;',HD='(Lnet/minecraft/world/damagesource/DamageSource;F)Z',FD='(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/entity/SpawnGroupData;';
 function match(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 return {
 source_ocelot_food_order_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.Mob'},transformer:function(node){var count=0;for(var n=0;n<node.methods.size();n++){var m=node.methods.get(n);if(!match(m,'aiStep','m_8107_','()V'))continue;for(var it=m.instructions.iterator();it.hasNext();){var at=it.next();if(at.getOpcode()!==O.RETURN)continue;var x=new L();x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESTATIC,H,'afterMobAi','('+OBJ+')V',false));m.instructions.insertBefore(at,x);count++;}m.maxStack=Math.max(m.maxStack,1);}if(count!==1)throw new Error('Ocelot food order sites '+count);return node;}},
 source_ocelot_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.Ocelot'},transformer:function(node){
  var srg=false,found={},replace=[],sound='()Lnet/minecraft/sounds/SoundEvent;';
  for(var k=0;k<node.interfaces.size();k++)if(String(node.interfaces.get(k))===P+'SourceOcelotNativeHost')throw new Error('Duplicate Ocelot host');
  for(var n=0;n<node.methods.size();n++){
   var m=node.methods.get(n),kind=null;
   if(match(m,'registerGoals','m_8099_','()V')){kind='goals';srg=String(m.name)==='m_8099_';}
   else if(match(m,'reassessTrustingGoals','m_29037_','()V'))kind='trust';
   else if(match(m,'finalizeSpawn','m_6518_',FD))kind='spawn';
   else if(match(m,'doHurtTarget','m_7327_','(Lnet/minecraft/world/entity/Entity;)Z'))kind='strike';
   else if(match(m,'getAmbientSound','m_7515_',sound))kind='ambient';
   else if(match(m,'getHurtSound','m_7975_','(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;'))kind='hurtSound';
   else if(match(m,'getDeathSound','m_5592_',sound))kind='deathSound';
   if(kind!==null){if(found[kind])throw new Error('Duplicate Ocelot '+kind);found[kind]=true;replace.push({m:m,kind:kind,name:String(m.name),desc:String(m.desc)});}
   var hook=match(m,'addAdditionalSaveData','m_7380_','(Lnet/minecraft/nbt/CompoundTag;)V')?'save':match(m,'readAdditionalSaveData','m_7378_','(Lnet/minecraft/nbt/CompoundTag;)V')?'load':null;
   if(hook!==null){var count=0;for(var it=m.instructions.iterator();it.hasNext();){var at=it.next();if(at.getOpcode()!==O.RETURN)continue;var x=new L();x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new C(O.INVOKESTATIC,H,hook,'('+OBJ+OBJ+')V',false));m.instructions.insertBefore(at,x);count++;}if(count!==1)throw new Error('Ocelot NBT returns '+hook+'/'+count);m.maxStack=Math.max(m.maxStack,2);found[hook]=true;}
  }
  var keys=['goals','trust','spawn','strike','ambient','hurtSound','deathSound','save','load'];for(var ki=0;ki<keys.length;ki++)if(!found[keys[ki]])throw new Error('Missing Ocelot '+keys[ki]);
  function add(name,desc,access){for(var n=0;n<node.methods.size();n++){var m=node.methods.get(n);if(String(m.name)===name&&String(m.desc)===desc)throw new Error('Unexpected Ocelot override '+name);}var m=new M(access,name,desc,null,null);node.methods.add(m);return m;}
  for(var ri=0;ri<replace.length;ri++){var r=replace[ri];r.m.name='mite$originalOcelot044$'+r.kind;var m=add(r.name,r.desc,r.m.access),x=m.instructions;
   if(r.kind==='goals'){x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESPECIAL,node.name,String(r.m.name),'()V',false));x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESTATIC,H,'install','('+OBJ+')V',false));x.add(new I(O.RETURN));m.maxStack=1;m.maxLocals=1;}
   else if(r.kind==='trust'){x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESTATIC,H,'reassess','('+OBJ+')V',false));x.add(new I(O.RETURN));m.maxStack=1;m.maxLocals=1;}
   else if(r.kind==='spawn'){var slots=[0,1,3,4];for(var si=0;si<slots.length;si++)x.add(new V(O.ALOAD,slots[si]));x.add(new C(O.INVOKESTATIC,H,'initialize','('+OBJ+OBJ+OBJ+OBJ+')Lnet/minecraft/world/entity/SpawnGroupData;',false));x.add(new I(O.ARETURN));m.maxStack=4;m.maxLocals=6;}
   else if(r.kind==='strike'){x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new C(O.INVOKESTATIC,H,'strike','('+OBJ+OBJ+')Z',false));x.add(new I(O.IRETURN));m.maxStack=2;m.maxLocals=2;}
   else if(r.kind==='ambient'){x.add(new I(O.ACONST_NULL));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;}
   else{x.add(new C(O.INVOKESTATIC,H,'hurtSound',sound,false));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=r.kind==='hurtSound'?2:1;}
  }
  node.interfaces.add(P+'SourceOcelotNativeHost');node.fields.add(new F(O.ACC_PRIVATE,'mite$ocelotState',OBJ,null,null));
  var m=add('mite$ocelotOwner','()'+OBJ,O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC),x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new FI(O.GETFIELD,node.name,'mite$ocelotState',OBJ));x.add(new I(O.ARETURN));m.maxStack=1;m.maxLocals=1;
  m=add('mite$ocelotOwner','('+OBJ+')V',O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new FI(O.PUTFIELD,node.name,'mite$ocelotState',OBJ));x.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=2;
  m=add(srg?'m_6469_':'hurt',HD,O.ACC_PUBLIC);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,1));x.add(new V(O.FLOAD,2));x.add(new C(O.INVOKESTATIC,H,'hurt','('+OBJ+OBJ+'F)Z',false));x.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=3;
  m=add(srg?'m_8107_':'aiStep','()V',O.ACC_PUBLIC);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESTATIC,H,'beforeAi','('+OBJ+')V',false));x.add(new V(O.ALOAD,0));x.add(new C(O.INVOKESPECIAL,node.superName,srg?'m_8107_':'aiStep','()V',false));x.add(new I(O.RETURN));m.maxStack=1;m.maxLocals=1;
  m=add(srg?'m_7910_':'setSpeed','(F)V',O.ACC_PUBLIC);x=m.instructions;x.add(new V(O.ALOAD,0));x.add(new V(O.ALOAD,0));x.add(new V(O.FLOAD,1));x.add(new C(O.INVOKESTATIC,H,'motor','('+OBJ+'F)F',false));x.add(new C(O.INVOKESPECIAL,node.superName,srg?'m_7910_':'setSpeed','(F)V',false));x.add(new V(O.ALOAD,0));x.add(new V(O.FLOAD,1));x.add(new C(O.INVOKEVIRTUAL,node.name,srg?'m_21564_':'setZza','(F)V',false));x.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=2;
  m=add(srg?'m_6121_':'getSoundVolume','()F',O.ACC_PROTECTED);m.instructions.add(new C(O.INVOKESTATIC,H,'volume','()F',false));m.instructions.add(new I(O.FRETURN));m.maxStack=1;m.maxLocals=1;
  m=add(srg?'m_142535_':'causeFallDamage','(FFLnet/minecraft/world/damagesource/DamageSource;)Z',O.ACC_PUBLIC);m.instructions.add(new I(O.ICONST_0));m.instructions.add(new I(O.IRETURN));m.maxStack=1;m.maxLocals=4;
  return node;
 }}
 };
}

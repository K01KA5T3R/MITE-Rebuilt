/** Initial population/quota and source finalization bridges only. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),M=Java.type('org.objectweb.asm.tree.MethodNode'),F=Java.type('org.objectweb.asm.tree.FieldNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),N=Java.type('org.objectweb.asm.tree.IntInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode'),P='dev/koikaster/mite/';
 return {
 source_initial_animal_quota_044:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.ChunkAccess'},transformer:function(node){
  if(node.interfaces.contains(P+'SourceAnimalQuotaChunk'))throw new Error('Animal quota already installed');var field='mite$initialAnimals';node.fields.add(new F(O.ACC_PRIVATE|O.ACC_VOLATILE,field,'I',null,null));node.interfaces.add(P+'SourceAnimalQuotaChunk');
  var get=new M(O.ACC_PUBLIC,field,'()I',null,null);get.instructions.add(new V(O.ALOAD,0));get.instructions.add(new Field(O.GETFIELD,node.name,field,'I'));get.instructions.add(new I(O.IRETURN));get.maxStack=1;get.maxLocals=1;node.methods.add(get);
  var set=new M(O.ACC_PUBLIC,field,'(I)V',null,null);set.instructions.add(new V(O.ALOAD,0));set.instructions.add(new V(O.ILOAD,1));set.instructions.add(new Field(O.PUTFIELD,node.name,field,'I'));set.instructions.add(new I(O.RETURN));set.maxStack=2;set.maxLocals=2;node.methods.add(set);return node;
 }},
 source_animal_quota_promotion_044:{target:{type:'CLASS',name:'net.minecraft.world.level.chunk.LevelChunk'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<init>'||String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V')continue;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.RETURN)continue;var c=new L();c.add(new V(O.ALOAD,0));c.add(new V(O.ALOAD,2));c.add(new C(O.INVOKESTATIC,P+'SourceAnimalQuotas','promote','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insertBefore(x,c);m.maxStack=Math.max(m.maxStack,2);count++;}}if(count!==1)throw new Error('Quota promotion sites '+count);return node;}},
 source_worldgen_entry_044:{target:{type:'CLASS',name:'net.minecraft.world.level.NaturalSpawner'},transformer:function(node){
  function populationCall(name){
   var code=new L();
   for(var a=0;a<4;a++)code.add(new V(O.ALOAD,a));
   code.add(new C(O.INVOKESTATIC,P+'SourceWorldgenSpawning',name,'(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',false));
   return code;
  }
  var entries=0,pools=0,exits=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);
   if(String(m.name)!=='spawnMobsForChunkGeneration'&&String(m.name)!=='m_220450_')continue;
   if(String(m.desc)!=='(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/util/RandomSource;)V')throw new Error('Worldgen spawning ABI');
   m.instructions.insert(populationCall('run'));
   m.maxStack=Math.max(m.maxStack,4);
   entries++;
   for(var it=m.instructions.iterator();it.hasNext();){
    var x=it.next();
    if(x.getOpcode()===O.RETURN){
     m.instructions.insertBefore(x,populationCall('runAquatic'));
     exits++;
    }
    if(!(x instanceof C)||String(x.owner)!=='net/minecraft/world/level/biome/MobSpawnSettings'||String(x.desc)!=='(Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/util/random/WeightedRandomList;')continue;
    m.instructions.insert(x,new C(O.INVOKESTATIC,P+'SourceNaturalSpawning','nativePool','(Ljava/lang/Object;)Lnet/minecraft/util/random/WeightedRandomList;',false));
    pools++;
   }
  }
  if(entries!==1||pools!==1||exits!==1)throw new Error('Worldgen entry/pool/exit sites '+entries+'/'+pools+'/'+exits);
  return node;
 }},
 source_spawn_base_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.Mob'},transformer:function(node){var spawnField=null;for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if(String(f.desc)==='Lnet/minecraft/world/entity/MobSpawnType;'){if(spawnField!==null)throw new Error('Multiple spawn-type fields');spawnField=String(f.name);}}if(spawnField===null)throw new Error('Missing native spawn type');node.interfaces.add(P+'SourceSpawnBaseAccess');var setter=new M(O.ACC_PUBLIC,'mite$sourceSpawnType','(Lnet/minecraft/world/entity/MobSpawnType;)V',null,null);setter.instructions.add(new V(O.ALOAD,0));setter.instructions.add(new V(O.ALOAD,1));setter.instructions.add(new Field(O.PUTFIELD,node.name,spawnField,'Lnet/minecraft/world/entity/MobSpawnType;'));setter.instructions.add(new I(O.RETURN));setter.maxStack=2;setter.maxLocals=2;node.methods.add(setter);var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='finalizeSpawn'&&String(m.name)!=='m_6518_')continue;if(String(m.desc)!=='(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/entity/SpawnGroupData;')throw new Error('Mob finalization ABI');var label=new Label(),c=new L();c.add(new V(O.ALOAD,0));c.add(new C(O.INVOKESTATIC,P+'SourceSpawnInitialization','active','(Ljava/lang/Object;)Z',false));c.add(new Jump(O.IFEQ,label));c.add(new V(O.ALOAD,0));c.add(new V(O.ALOAD,3));c.add(new V(O.ALOAD,4));c.add(new C(O.INVOKESTATIC,P+'SourceSpawnInitialization','base','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/entity/SpawnGroupData;',false));c.add(new I(O.ARETURN));c.add(label);c.add(new Frame(O.F_SAME,0,null,0,null));m.instructions.insert(c);m.maxStack=Math.max(m.maxStack,3);count++;}if(count!==1)throw new Error('Mob source base sites '+count);return node;
 }},
 source_horse_variant_044:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.horse.Horse'},transformer:function(node){var setter=null,variant=null,markings=null,vd='net/minecraft/world/entity/animal/horse/Variant',md='net/minecraft/world/entity/animal/horse/Markings',sd='(L'+vd+';L'+md+';)V';for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)===sd){if(setter!==null)throw new Error('Multiple horse coat setters');setter=String(m.name);}for(var it=m.instructions.iterator();it.hasNext();){var c=it.next();if(!(c instanceof C))continue;if(String(c.owner)===vd&&String(c.desc)==='(I)L'+vd+';')variant=String(c.name);if(String(c.owner)===md&&String(c.desc)==='(I)L'+md+';')markings=String(c.name);}}if(setter===null||variant===null||markings===null)throw new Error('Horse color/marking API');node.interfaces.add(P+'SourceHorseVariantAccess');var m=new M(O.ACC_PUBLIC,'mite$sourceHorseVariant','(I)V',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ILOAD,1));m.instructions.add(new N(O.SIPUSH,255));m.instructions.add(new I(O.IAND));m.instructions.add(new C(O.INVOKESTATIC,vd,variant,'(I)L'+vd+';',false));m.instructions.add(new V(O.ILOAD,1));m.instructions.add(new N(O.BIPUSH,8));m.instructions.add(new I(O.ISHR));m.instructions.add(new N(O.SIPUSH,255));m.instructions.add(new I(O.IAND));m.instructions.add(new C(O.INVOKESTATIC,md,markings,'(I)L'+md+';',false));m.instructions.add(new C(O.INVOKESPECIAL,node.name,setter,sd,false));m.instructions.add(new I(O.RETURN));m.maxStack=4;m.maxLocals=2;node.methods.add(m);return node;
 }}
 };
}

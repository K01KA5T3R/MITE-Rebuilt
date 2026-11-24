/** Original flower parents, grass fertilizer and original-flower worldgen ownership only. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 function find(node,name,srg,desc){for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===name||String(m.name)===srg)&&String(m.desc)===desc)return m;}throw new Error('Flora056 missing '+name);}
 function clean(m,stack,locals){m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.maxStack=stack;m.maxLocals=locals;}
 function load(m,n){m.instructions.add(new V(O.ALOAD,n));}
 return {
  source_grass_fertilizer_056:{target:{type:'CLASS',name:'net.minecraft.world.level.block.GrassBlock'},transformer:function(node){
   var target=find(node,'isValidBonemealTarget','m_7370_','(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z');clean(target,1,5);target.instructions.add(new I(O.ICONST_1));target.instructions.add(new I(O.IRETURN));
   var m=find(node,'performBonemeal','m_214148_','(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V');clean(m,4,5);for(var j=1;j<=4;j++)load(m,j);m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceGrassFertilizer','perform','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.add(new I(O.RETURN));return node;
  }},
  source_flower_parent_056:{target:{type:'CLASS',name:'net.minecraft.world.level.block.FlowerBlock'},transformer:function(node){
   var shape=find(node,'getShape','m_5940_','(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;'),srg=String(shape.name)!=='getShape',sites=0;
   for(var at=shape.instructions.getFirst();at!==null;at=at.getNext())if(at.getOpcode()===O.ARETURN){var code=new L();for(var j=1;j<=3;j++)code.add(new V(O.ALOAD,j));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceFlora','shape','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/phys/shapes/VoxelShape;',false));shape.instructions.insertBefore(at,code);sites++;}if(sites!==1)throw new Error('Flora056 shape return sites '+sites);shape.maxStack+=3;
   var desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z',name=srg?'m_7898_':'canSurvive';for(var k=0;k<node.methods.size();k++)if(String(node.methods.get(k).name)===name&&String(node.methods.get(k).desc)===desc)throw new Error('Flora056 preexisting flower survival override');
   var m=new M(O.ACC_PUBLIC,name,desc,null,null);for(var j=0;j<=3;j++)load(m,j);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));for(var j=1;j<=3;j++)load(m,j);m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceFlora','survives','(ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));m.instructions.add(new I(O.IRETURN));m.maxStack=4;m.maxLocals=4;node.methods.add(m);
   desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;';name=srg?'m_7417_':'updateShape';for(var k=0;k<node.methods.size();k++)if(String(node.methods.get(k).name)===name&&String(node.methods.get(k).desc)===desc)throw new Error('Flora056 preexisting flower update override');m=new M(O.ACC_PUBLIC,name,desc,null,null);for(var j=0;j<=6;j++)load(m,j);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));load(m,4);load(m,5);m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceFlora','afterUpdate','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/block/state/BlockState;',false));m.instructions.add(new I(O.ARETURN));m.maxStack=7;m.maxLocals=7;node.methods.add(m);return node;
  }},
  source_grass_fertilizer_particles_056:{target:{type:'CLASS',name:'net.minecraft.world.item.BoneMealItem'},transformer:function(node){
   var m=find(node,'useOn','m_6225_','(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;'),sites=0;
   for(var at=m.instructions.getFirst();at!==null;at=at.getNext())if(at.getOpcode()===O.INVOKEVIRTUAL&&String(at.owner)==='net/minecraft/world/level/Level'&&String(at.desc)==='(ILnet/minecraft/core/BlockPos;I)V'){
    at.setOpcode(O.INVOKESTATIC);at.owner='dev/koikaster/mite/SourceGrassFertilizer';at.name='particles';at.desc='(Ljava/lang/Object;ILjava/lang/Object;I)V';at.itf=false;sites++;
   }if(sites!==2)throw new Error('Flora056 bone meal particle sites '+sites);return node;
  }},
  source_flower_generation_owner_056:{target:{type:'CLASS',name:'net.minecraft.world.level.levelgen.feature.SimpleBlockFeature'},transformer:function(node){
   var m=find(node,'place','m_142674_','(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z'),sites=0;
   for(var at=m.instructions.getFirst();at!==null;at=at.getNext())if(at.getOpcode()===O.INVOKEVIRTUAL&&String(at.owner)==='net/minecraft/world/level/block/state/BlockState'&&String(at.desc)==='(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z'){
    at.setOpcode(O.INVOKESTATIC);at.owner='dev/koikaster/mite/SourceFlora';at.name='worldgenSurvives';at.desc='(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z';at.itf=false;sites++;
   }if(sites!==1)throw new Error('Flora056 native plant sites '+sites);return node;
  }}
 };
}

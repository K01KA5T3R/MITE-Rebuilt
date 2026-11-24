/** aon state, recovery/legality and the actual native render-region biome source. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode');
 function add(node,name,desc,access,stack,locals){for(var i=0;i<node.methods.size();i++)if(String(node.methods.get(i).name)===name&&String(node.methods.get(i).desc)===desc)throw new Error('Grass055 duplicate override '+name);var m=new M(access,name,desc,null,null);m.maxStack=stack;m.maxLocals=locals;node.methods.add(m);return m;}
 function load(m,index){m.instructions.add(new V(O.ALOAD,index));}function end(m,opcode){m.instructions.add(new I(opcode));}
 function bridge(m,name,desc){m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceGrass',name,desc,false));}
 return {
  source_grass_055:{target:{type:'CLASS',name:'net.minecraft.world.level.block.GrassBlock'},transformer:function(node){
   var srg=null;for(var i=0;i<node.methods.size();i++){var x=node.methods.get(i);if(String(x.desc)==='(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z')srg=String(x.name)!=='isValidBonemealTarget';}if(srg===null)throw new Error('Grass055 mapping anchor missing');
   var desc='(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V',name=srg?'m_7926_':'createBlockStateDefinition';var m=add(node,name,desc,O.ACC_PROTECTED,2,2);load(m,0);load(m,1);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));load(m,1);bridge(m,'define','(Ljava/lang/Object;)V');end(m,O.RETURN);
   desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V';m=add(node,srg?'m_213898_':'randomTick',desc,O.ACC_PUBLIC,4,5);for(var j=1;j<=4;j++)load(m,j);bridge(m,'randomTick','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V');end(m,O.RETURN);
   desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z';m=add(node,srg?'m_7898_':'canSurvive',desc,O.ACC_PUBLIC,3,4);for(var j=1;j<=3;j++)load(m,j);bridge(m,'survives','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z');end(m,O.IRETURN);
   desc='(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;';name=srg?'m_5573_':'getStateForPlacement';m=add(node,name,desc,O.ACC_PUBLIC,2,2);load(m,0);load(m,1);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));load(m,1);bridge(m,'placement','(Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/block/state/BlockState;');end(m,O.ARETURN);
   desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;';name=srg?'m_7417_':'updateShape';m=add(node,name,desc,O.ACC_PUBLIC,7,7);for(var j=0;j<=6;j++)load(m,j);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));load(m,4);load(m,5);bridge(m,'update','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/block/state/BlockState;');end(m,O.ARETURN);
   return node;
  }},
  source_grass_render_biomes_055:{target:{type:'CLASS',name:'net.minecraft.client.renderer.chunk.RenderChunkRegion'},transformer:function(node){
   var field=null;for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if(String(f.desc)==='Lnet/minecraft/world/level/Level;'){if(field!==null)throw new Error('Grass055 ambiguous region level');field=f;}}if(field===null)throw new Error('Grass055 region level missing');node.interfaces.add('dev/koikaster/mite/SourceGrassBiomeAccess');
   var m=add(node,'mite$grassBiomes','()Lnet/minecraft/world/level/LevelReader;',O.ACC_PUBLIC,1,1);load(m,0);m.instructions.add(new F(O.GETFIELD,String(node.name),String(field.name),String(field.desc)));end(m,O.ARETURN);return node;
  }}
 };
}

/** V001 tall-flower root adaptation only; original native methods remain unchanged. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 function load(m,n){m.instructions.add(new V(O.ALOAD,n));}
 function add(node,name,desc,helper,helperDesc,count,extra){
  for(var i=0;i<node.methods.size();i++)if(String(node.methods.get(i).name)===name&&String(node.methods.get(i).desc)===desc)throw new Error('Flower539 preexisting override '+name);
  var m=new M(O.ACC_PUBLIC,name,desc,null,null);for(var n=0;n<=count;n++)load(m,n);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),name,desc,false));
  for(var i=0;i<extra.length;i++)load(m,extra[i]);m.instructions.add(new C(O.INVOKESTATIC,helper[0],helper[1],helperDesc,false));m.instructions.add(new I(helper[1]==='survives'?O.IRETURN:O.ARETURN));m.maxStack=count+1;m.maxLocals=count+1;node.methods.add(m);
 }
 return {modern_tall_flower_parent539:{target:{type:'CLASS',name:'net.minecraft.world.level.block.TallFlowerBlock'},transformer:function(node){
  var srg=false;for(var i=0;i<node.methods.size();i++)if(String(node.methods.get(i).name)==='m_7370_')srg=true;
  add(node,srg?'m_7898_':'canSurvive','(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z',['dev/koikaster/mite/SourceModernFlowerParent539','survives'],'(ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',3,[1,2,3]);
  add(node,srg?'m_7417_':'updateShape','(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;',['dev/koikaster/mite/SourceFlora','afterUpdate'],'(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/level/block/state/BlockState;',6,[4,5]);return node;
 }}};
}

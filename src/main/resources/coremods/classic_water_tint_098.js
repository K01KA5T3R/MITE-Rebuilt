// Adapt only the actual native liquid tint result; preserve fluid simulation and foreign fluids.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList');
 return {classic_water_mesh_tint_098:{target:{type:'CLASS',name:'net.minecraft.client.renderer.block.LiquidBlockRenderer'},transformer:function(node){
  var anchors=[];
  for(var i=0;i<node.methods.size();i++){
   var method=node.methods.get(i);
   if((method.access&O.ACC_STATIC)!==0)continue;
   if(String(method.desc)!=='(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V')continue;
   for(var n=method.instructions.getFirst();n!==null;n=n.getNext())if(n instanceof M&&String(n.owner)==='net/minecraftforge/client/extensions/common/IClientFluidTypeExtensions'&&String(n.name)==='getTintColor'&&String(n.desc)==='(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I')anchors.push({method:method,call:n});
  }
  if(anchors.length!==1){Java.type('net.minecraftforge.coremod.api.ASMAPI').log('WARN','Classic water: unsupported liquid renderer; keeping native tint.');return node;}
  var anchor=anchors[0],code=new L();code.add(new V(O.ALOAD,5));code.add(new V(O.ALOAD,1));code.add(new V(O.ALOAD,2));code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicWater','fluidTint','(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I',false));anchor.method.instructions.insert(anchor.call,code);anchor.method.maxStack+=3;return node;
 }} };
}

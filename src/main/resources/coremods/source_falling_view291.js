/** Exact source falling-cube gate; every other native renderer path remains. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 return {source_falling_view291:{target:{type:'CLASS',name:'net.minecraft.client.renderer.entity.FallingBlockRenderer'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)!=='render'&&String(m.name)!=='m_7392_')||String(m.desc)!=='(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V')continue;
   var code=new L(),fallback=new Label();code.add(new V(O.ALOAD,1));code.add(new V(O.FLOAD,2));code.add(new V(O.FLOAD,3));code.add(new V(O.ALOAD,4));code.add(new V(O.ALOAD,5));code.add(new V(O.ILOAD,6));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/visual/SourceFallingView291','render','(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z',false));code.add(new J(O.IFEQ,fallback));code.add(new I(O.RETURN));code.add(fallback);code.add(new Frame(O.F_SAME,0,null,0,null));m.instructions.insert(code);m.maxStack=Math.max(6,m.maxStack);count++;
  }if(count!==1)throw new Error('Falling291 exact native render entry '+count);return node;
 }}};
}

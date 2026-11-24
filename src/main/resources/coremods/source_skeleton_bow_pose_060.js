/** SkeletonModel's two literal vanilla-bow tests must also accept source metal
 * bows. The fallback remains the exact native ItemStack.is for other actors. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {source_skeleton_bow_model_060:{target:{type:'CLASS',name:'net.minecraft.client.model.SkeletonModel'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),all=m.instructions.toArray();for(var k=0;k<all.length;k++){var x=all[k];if(x.getOpcode()!==O.INVOKEVIRTUAL||String(x.owner)!=='net/minecraft/world/item/ItemStack'||String(x.desc)!=='(Lnet/minecraft/world/item/Item;)Z')continue;m.instructions.insertBefore(x,new V(O.ALOAD,1));m.instructions.set(x,new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceSkeletonBowPose','renderBow','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));m.maxStack=Math.max(16,m.maxStack);count++;}}if(count!==2)throw new Error('Skeleton native bow model sites '+count);return node;}}};
}

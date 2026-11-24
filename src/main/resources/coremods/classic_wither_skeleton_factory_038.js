/** Only the native WitherSkeleton constructor handle. Existing037 Skeleton
 * factory and every native field, method and other handle are preserved. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),H=Java.type('org.objectweb.asm.Handle'),D=Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
 return {mite_source_wither_skeleton_factory_038:{target:{type:'CLASS',name:'net.minecraft.world.entity.EntityType'},transformer:function(node){var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(!(x instanceof D))continue;for(var k=0;k<x.bsmArgs.length;k++){var h=x.bsmArgs[k];if(!(h instanceof H)||String(h.getOwner())!=='net/minecraft/world/entity/monster/WitherSkeleton'||String(h.getName())!=='<init>')continue;if(h.getTag()!==O.H_NEWINVOKESPECIAL||String(h.getDesc())!=='(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V')throw new Error('Unexpected native WitherSkeleton factory handle');x.bsmArgs[k]=new H(h.getTag(),'dev/koikaster/mite/SourceWitherSkeleton',h.getName(),h.getDesc(),h.isInterface());count++;}}}
  if(count!==1)throw new Error('WitherSkeleton factory handles '+count);return node;
 }}};
}

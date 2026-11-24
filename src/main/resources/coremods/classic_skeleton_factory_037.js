/** Only the native Skeleton factory's constructor handle. Native registry IDs,
 * builders, dimensions and all other factory instructions remain intact. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),H=Java.type('org.objectweb.asm.Handle'),D=Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
 return {mite_source_skeleton_factory_037:{target:{type:'CLASS',name:'net.minecraft.world.entity.EntityType'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(!(x instanceof D))continue;for(var k=0;k<x.bsmArgs.length;k++){var h=x.bsmArgs[k];if(!(h instanceof H)||String(h.getOwner())!=='net/minecraft/world/entity/monster/Skeleton'||String(h.getName())!=='<init>')continue;if(h.getTag()!==O.H_NEWINVOKESPECIAL||String(h.getDesc())!=='(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V')throw new Error('Unexpected native Skeleton factory handle');x.bsmArgs[k]=new H(h.getTag(),'dev/koikaster/mite/SourceSkeleton',h.getName(),h.getDesc(),h.isInterface());count++;}}}
  if(count!==1)throw new Error('Skeleton factory handles '+count);return node;
 }}};
}

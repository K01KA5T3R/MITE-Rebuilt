function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),H=Java.type('org.objectweb.asm.Handle'),D=Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 var old='net/minecraft/world/entity/projectile/ThrownEgg',replacement='dev/koikaster/mite/SourceEgg887';
 return {
source_egg_factory887:{target:{type:'CLASS',name:'net.minecraft.world.entity.EntityType'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(!(x instanceof D))continue;for(var j=0;j<x.bsmArgs.length;j++){var h=x.bsmArgs[j];if(!(h instanceof H)||String(h.getOwner())!==old)continue;if(h.getTag()!==O.H_NEWINVOKESPECIAL||String(h.getDesc())!=='(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V')throw new Error('077 unexpected ThrownEgg factory');x.bsmArgs[j]=new H(h.getTag(),replacement,h.getName(),h.getDesc(),h.isInterface());count++;}}}if(count!==1)throw new Error('077 ThrownEgg factory count '+count);return node;}}

 };
}

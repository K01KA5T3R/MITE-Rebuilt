/** Existing collectible-amethyst override: extras keeps 20x slower growth.
 * Select bound5 or100 from this actual ServerLevel; no extra RNG calls. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {slow_amethyst_growth_051:{target:{type:'CLASS',name:'net.minecraft.world.level.block.BuddingAmethystBlock'},transformer:function(node){
  var methods=0,sites=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V')continue;methods++;
   for(var n=m.instructions.getFirst();n!==null;n=n.getNext()){if(n.getOpcode()!==O.ICONST_5)continue;var next=n.getNext();if(next===null||next.getOpcode()!==O.INVOKEINTERFACE||String(next.owner)!=='net/minecraft/util/RandomSource'||String(next.desc)!=='(I)I')continue;var bound=new L();bound.add(new V(O.ALOAD,2));bound.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/WorldAmethystGrowth','chanceBound','(Lnet/minecraft/world/level/Level;)I',false));m.instructions.insertBefore(n,bound);m.instructions.remove(n);m.maxStack=Math.max(m.maxStack,2);sites++;break;}
  }if(methods!==1||sites!==1)throw new Error('Slow amethyst growth sites '+methods+'/'+sites);return node;
 }}};
}

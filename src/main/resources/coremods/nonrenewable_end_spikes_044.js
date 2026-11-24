/** N018: change only the cage gate inside native placeSpike. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {nonrenewable_end_spikes_044:{target:{type:'CLASS',name:'net.minecraft.world.level.levelgen.feature.SpikeFeature'},transformer:function(node){
  var methods=0,sites=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);
   if(String(m.desc)!=='(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/configurations/SpikeConfiguration;Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;)V')continue;
   methods++;
   for(var it=m.instructions.iterator();it.hasNext();){
    var x=it.next();
    if(x.getOpcode()!==O.INVOKEVIRTUAL||String(x.owner)!=='net/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike'||String(x.desc)!=='()Z')continue;
    m.instructions.insertBefore(x,new V(O.ALOAD,3));
    m.instructions.set(x,new C(O.INVOKESTATIC,'dev/koikaster/mite/NonrenewableEndSpikes','cage','(Ljava/lang/Object;Ljava/lang/Object;)Z',false));sites++;
   }
   m.maxStack=Math.max(m.maxStack,2);
  }
  if(methods!==1||sites!==1)throw new Error('Native spike cage sites '+methods+'/'+sites);
  return node;
 }}};
}

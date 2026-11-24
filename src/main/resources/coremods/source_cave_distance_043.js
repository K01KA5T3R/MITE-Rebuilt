/** Only the native spawn loop's distance call changes. Candidate selection,
 * category caps, placement/body checks, chunk guard and spawn callback remain. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode');
 return {mite_source_cave_distance_043:{target:{type:'CLASS',name:'net.minecraft.world.level.NaturalSpawner'},transformer:function(node){
  var expected='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z';
  var count=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);
   for(var it=m.instructions.iterator();it.hasNext();){
    var x=it.next();
    if(x.getOpcode()!==O.INVOKESTATIC||String(x.owner)!=='net/minecraft/world/level/NaturalSpawner'||String(x.desc)!==expected)continue;
    if((m.access&O.ACC_STATIC)===0||String(m.desc).indexOf('(Lnet/minecraft/world/entity/MobCategory;')!==0)throw new Error('Unexpected source cave-distance caller');
    var added=new L();added.add(new V(O.ALOAD,0));m.instructions.insertBefore(x,added);
    x.owner='dev/koikaster/mite/SourceCaveSpawnRange';x.name='allow';x.desc='(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;DLjava/lang/Object;)Z';x.itf=false;
    m.maxStack=Math.max(m.maxStack+1,7);count++;
   }
  }
  if(count!==1)throw new Error('Source cave-distance call sites '+count);
  return node;
 }}};
}

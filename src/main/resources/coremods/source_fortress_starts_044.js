/** One new native generation hook; existing spawning transformers untouched. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {source_fortress_starts_044:{target:{type:'CLASS',name:'net.minecraft.world.level.StructureManager'},transformer:function(node){
  var world=null;for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if(String(f.desc)==='Lnet/minecraft/world/level/LevelAccessor;'){if(world!==null)throw new Error('Multiple structure world fields');world=String(f.name);}}if(world===null)throw new Error('Missing structure world field');
  var methods=0,sites=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V')continue;methods++;
   for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()!==O.RETURN)continue;var c=new L();c.add(new V(O.ALOAD,0));c.add(new F(O.GETFIELD,node.name,world,'Lnet/minecraft/world/level/LevelAccessor;'));for(var a=1;a<=3;a++)c.add(new V(O.ALOAD,a));c.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceFortressSpawns','generated','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.insertBefore(x,c);m.maxStack=Math.max(m.maxStack,4);sites++;}
  }if(methods!==1||sites!==1)throw new Error('Structure-start capture sites '+methods+'/'+sites);return node;
 }}};
}

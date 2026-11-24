function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),L=Java.type('org.objectweb.asm.tree.LabelNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 var H='dev/koikaster/mite/SourceWormCompost515',STATE='Lnet/minecraft/world/level/block/state/BlockState;',TICK='('+STATE+'Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V';
 function named(node,name){for(var i=0;i<node.methods.size();i++)if(String(node.methods.get(i).name)===name)return true;return false;}
 function load(m,n){m.instructions.add(new V(O.ALOAD,n));}
 return {
  source_chest_random_compost515:{target:{type:'CLASS',name:'net.minecraft.world.level.block.ChestBlock'},transformer:function(node){
   var official=named(node,'getStateForPlacement'),eligibility=official?'isRandomlyTicking':'m_6724_',tick=official?'randomTick':'m_213898_';
   if(named(node,eligibility)||named(node,tick))throw new Error('Unexpected existing native chest random overrides');
   var m=new M(O.ACC_PUBLIC,eligibility,'('+STATE+')Z',null,null);load(m,1);load(m,0);load(m,1);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),eligibility,'('+STATE+')Z',false));m.instructions.add(new C(O.INVOKESTATIC,H,'randomlyTicking','(Ljava/lang/Object;Z)Z',false));m.instructions.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=2;node.methods.add(m);
   m=new M(O.ACC_PUBLIC,tick,TICK,null,null);for(var i=1;i<=4;i++)load(m,i);m.instructions.add(new C(O.INVOKESTATIC,H,'tick','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));var nativePath=new L();m.instructions.add(new J(O.IFEQ,nativePath));m.instructions.add(new I(O.RETURN));m.instructions.add(nativePath);m.instructions.add(new Frame(O.F_SAME,0,null,0,null));for(var i=0;i<=4;i++)load(m,i);m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),tick,TICK,false));m.instructions.add(new I(O.RETURN));m.maxStack=5;m.maxLocals=5;node.methods.add(m);return node;
  }},
  source_chest_delayed_loot515:{target:{type:'CLASS',name:'net.minecraft.world.level.block.entity.ChestBlockEntity'},transformer:function(node){
   if(named(node,'mite$lootUnpacked'))throw new Error('Duplicate chest delayed-loot bridge');var official=named(node,'load');node.interfaces.add('dev/koikaster/mite/SourceChestLoot515');
   var m=new M(O.ACC_PUBLIC,'mite$lootUnpacked','()Z',null,null),loaded=new L();load(m,0);m.instructions.add(new F(O.GETFIELD,'net/minecraft/world/level/block/entity/RandomizableContainerBlockEntity',official?'lootTable':'f_59605_','Lnet/minecraft/resources/ResourceLocation;'));m.instructions.add(new J(O.IFNULL,loaded));m.instructions.add(new I(O.ICONST_0));m.instructions.add(new I(O.IRETURN));m.instructions.add(loaded);m.instructions.add(new Frame(O.F_SAME,0,null,0,null));m.instructions.add(new I(O.ICONST_1));m.instructions.add(new I(O.IRETURN));m.maxStack=1;m.maxLocals=1;node.methods.add(m);return node;
  }}
 };
}

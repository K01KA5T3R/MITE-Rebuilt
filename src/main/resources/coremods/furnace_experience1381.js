function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),N=Java.type('org.objectweb.asm.tree.LabelNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 return {mite_furnace_manual_experience1381:{target:{type:'CLASS',name:'net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity'},transformer:function(node){var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;')continue;
   var code=new L(),resume=new N();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/FurnaceExperience','automaticPayout','(Ljava/lang/Object;)Ljava/util/List;',false));code.add(new I(O.DUP));code.add(new J(O.IFNULL,resume));code.add(new I(O.ARETURN));code.add(resume);code.add(new F(O.F_SAME1,0,null,1,['java/util/List']));code.add(new I(O.POP));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,2);count++;
  }if(count!==1)throw new Error('Furnace automatic experience payout sites '+count);return node;
 }}};
}

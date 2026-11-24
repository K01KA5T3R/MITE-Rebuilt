function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 return {source_egg_use487:{target:{type:'CLASS',name:'net.minecraft.world.item.EggItem'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);
   if((String(m.name)!=='use'&&String(m.name)!=='m_7203_')||String(m.desc)!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;')continue;
   var c=new List(),original=new Label();for(var n=1;n<=3;n++)c.add(new Var(O.ALOAD,n));
   c.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/SourceEggUse487','use','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/InteractionResultHolder;',false));
   c.add(new Insn(O.DUP));c.add(new Jump(O.IFNULL,original));c.add(new Insn(O.ARETURN));c.add(original);c.add(new Frame(O.F_SAME1,0,null,1,['net/minecraft/world/InteractionResultHolder']));c.add(new Insn(O.POP));
   m.instructions.insert(c);m.maxStack=Math.max(m.maxStack,3);count++;
  }if(count!==1)throw new Error('Unexpected original egg use entry '+count);return node;
 }}};
}

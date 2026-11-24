function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 return {source_bow_815:{target:{type:'CLASS',name:'net.minecraft.world.item.BowItem'},transformer:function(node){var uses=0,releases=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),name=String(m.name),desc=String(m.desc),c=new L(),fallback=new Label();
   if((name==='use'||name==='m_7203_')&&desc==='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;'){
    for(var n=1;n<=3;n++)c.add(new V(O.ALOAD,n));c.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceBowUse815','use','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/world/InteractionResultHolder;',false));c.add(new I(O.DUP));c.add(new Jump(O.IFNULL,fallback));c.add(new I(O.ARETURN));c.add(fallback);c.add(new F(O.F_SAME1,0,null,1,['net/minecraft/world/InteractionResultHolder']));c.add(new I(O.POP));m.instructions.insert(c);m.maxStack=Math.max(m.maxStack,3);uses++;
   }
   if((name==='releaseUsing'||name==='m_5551_')&&desc==='(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V'){
    for(var n=1;n<=3;n++)c.add(new V(O.ALOAD,n));c.add(new V(O.ILOAD,4));c.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceBowUse815','release','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)Z',false));c.add(new Jump(O.IFEQ,fallback));c.add(new I(O.RETURN));c.add(fallback);c.add(new F(O.F_SAME,0,null,0,null));m.instructions.insert(c);m.maxStack=Math.max(m.maxStack,4);releases++;
   }
  }if(uses!==1||releases!==1)throw new Error('Source bow entries '+uses+'/'+releases);return node;
 }}};
}

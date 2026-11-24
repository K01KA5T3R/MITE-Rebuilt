function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode');
 var H='dev/koikaster/mite/SourceFlowerPotUse1104',obj='Ljava/lang/Object;',result='Lnet/minecraft/world/InteractionResult;';
 return {
  source_flower_pot_use1104:{target:{type:'CLASS',name:'net.minecraft.world.level.block.FlowerPotBlock'},transformer:function(node){
   var count=0;for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);if((String(m.name)!=='use'&&String(m.name)!=='m_6227_')||String(m.desc)!=='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)'+result)continue;
    var code=new List(),nativePath=new Label();for(var slot=0;slot<6;slot++)code.add(new Var(O.ALOAD,slot));code.add(new Call(O.INVOKESTATIC,H,'use','('+obj+obj+obj+obj+obj+obj+')'+result,false));code.add(new Insn(O.DUP));code.add(new Jump(O.IFNULL,nativePath));code.add(new Insn(O.ARETURN));code.add(nativePath);code.add(new Insn(O.POP));m.instructions.insert(code);m.maxStack=Math.max(16,m.maxStack);count++;
   }if(count!==1)throw new Error('Unexpected flower pot use '+count);return node;
  }},
  source_flower_pot_player_owner1104:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerPlayerGameMode'},transformer:function(node){
   var count=0;for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);if((String(m.name)!=='useItemOn'&&String(m.name)!=='m_7179_')||String(m.desc)!=='(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)'+result)continue;
    var ins=m.instructions.toArray();for(var j=0;j<ins.length;j++){var c=ins[j];if(c.getOpcode()!==O.INVOKEVIRTUAL||String(c.owner)!=='net/minecraft/world/level/block/state/BlockState'||String(c.desc)!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)'+result)continue;m.instructions.set(c,new Call(O.INVOKESTATIC,H,'blockUse','('+obj+obj+obj+obj+obj+')'+result,false));count++;}
   }if(count!==1)throw new Error('Unexpected actual pot player-use owner '+count);return node;
  }}
 };
}

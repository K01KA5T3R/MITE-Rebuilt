function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var P='dev/koikaster/mite/';
 return {mite_new_runegate_scheduled_tick_and_atmosphere_035:{target:{type:'CLASS',name:'net.minecraft.world.level.block.NetherPortalBlock'},transformer:function(node){
  var changed=0,srg=false;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);
   if((String(m.name)==='animateTick'||String(m.name)==='m_214162_')&&String(m.desc)==='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V'){
    srg=String(m.name)==='m_214162_';m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();
    for(var arg=1;arg<=4;arg++)m.instructions.add(new Var(O.ALOAD,arg));
    m.instructions.add(new Call(O.INVOKESTATIC,P+'SourceRuneGateClient','animate','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.add(new Insn(O.RETURN));m.maxStack=4;changed++;
   }
  }
  if(changed!==1)throw new Error('Runegate atmosphere site count '+changed);
  var name=srg?'m_213897_':'tick',desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V';
  for(var i=0;i<node.methods.size();i++)if(String(node.methods.get(i).name)===name&&String(node.methods.get(i).desc)===desc)throw new Error('Scheduled tick override already exists');
  var tick=new Method(O.ACC_PUBLIC,name,desc,null,null);tick.instructions.add(new Var(O.ALOAD,2));tick.instructions.add(new Var(O.ALOAD,3));
  tick.instructions.add(new Call(O.INVOKESTATIC,P+'SourceRuneGate','scheduledTick','(Ljava/lang/Object;Ljava/lang/Object;)V',false));tick.instructions.add(new Insn(O.RETURN));tick.maxStack=2;tick.maxLocals=5;node.methods.add(tick);return node;
 }}};
}

function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 // Descriptor/shape matching keeps these hooks independent of official/SRG method names.
 var tickDesc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V';
 function randomTick(method){return String(method.name)==='randomTick'||String(method.name)==='m_213898_';}
 function call(name,desc){return new Call(O.INVOKESTATIC,'dev/koikaster/mite/Agriculture',name,desc,false);}
 return {
  'mite_classic_farmland':{target:{type:'CLASS',name:'net.minecraft.world.level.block.FarmBlock'},transformer:function(node){
   var define=0,defaults=0,ticks=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i),desc=String(method.desc);
    if(desc==='(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V'){
     for(var p=method.instructions.getFirst();p!==null;p=p.getNext())if(p.getOpcode()===O.RETURN){var code=new List();code.add(new Var(O.ALOAD,1));code.add(call('defineSoil','(Ljava/lang/Object;)V'));method.instructions.insertBefore(p,code);define++;}
    }
    if(String(method.name)==='<init>')for(var p=method.instructions.getFirst();p!==null;p=p.getNext()){
     if(p.getOpcode()===O.INVOKEVIRTUAL&&String(p.desc)==='(Lnet/minecraft/world/level/block/state/BlockState;)V'){method.instructions.insertBefore(p,call('soilDefaults','(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;'));defaults++;}
    }
    if(desc===tickDesc&&randomTick(method)){
     var code=new List();for(var n=0;n<5;n++)code.add(new Var(O.ALOAD,n));code.add(call('soilTick','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V'));code.add(new Insn(O.RETURN));method.instructions.clear();method.instructions.add(code);method.tryCatchBlocks.clear();if(method.localVariables!==null)method.localVariables.clear();method.maxLocals=5;method.maxStack=5;ticks++;
    }
    method.maxStack=Math.max(method.maxStack,2);
   }
   if(define!==1||defaults!==1||ticks!==1)throw new Error('Unexpected FarmBlock hooks '+define+'/'+defaults+'/'+ticks);return node;
  }},
  'mite_classic_crops':{target:{type:'CLASS',name:'net.minecraft.world.level.block.CropBlock'},transformer:function(node){
   var ticks=0,random=0,bone=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i),desc=String(method.desc),name=String(method.name);
    if(desc===tickDesc&&randomTick(method)){
     var code=new List(),vanilla=new Label();for(var n=0;n<5;n++)code.add(new Var(O.ALOAD,n));code.add(call('cropTick','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z'));code.add(new Jump(O.IFEQ,vanilla));code.add(new Insn(O.RETURN));code.add(vanilla);code.add(new Frame(O.F_SAME,0,null,0,null));method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,5);ticks++;
    }
    if(desc==='(Lnet/minecraft/world/level/block/state/BlockState;)Z'&&(name==='isRandomlyTicking'||name==='m_6724_'))for(var p=method.instructions.getFirst();p!==null;p=p.getNext())if(p.getOpcode()===O.IRETURN){var code=new List();code.add(new Var(O.ALOAD,1));code.add(call('cropRandomTicks','(ZLjava/lang/Object;)Z'));method.instructions.insertBefore(p,code);method.maxStack=Math.max(method.maxStack,2);random++;}
    if(desc==='(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z')for(var p=method.instructions.getFirst();p!==null;p=p.getNext())if(p.getOpcode()===O.IRETURN){var code=new List();code.add(new Var(O.ALOAD,3));code.add(call('fertilizerTarget','(ZLjava/lang/Object;)Z'));method.instructions.insertBefore(p,code);method.maxStack=Math.max(method.maxStack,2);bone++;}
   }
   if(ticks!==1||random!==1||bone!==1)throw new Error('Unexpected CropBlock hooks '+ticks+'/'+random+'/'+bone);return node;
  }}
 };
}

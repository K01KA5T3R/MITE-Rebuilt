function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 var owner='dev/koikaster/mite/SourceCropBlight';
 var boneDesc='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V';
 return {
  'mite_source_crop_blight_039':{target:{type:'CLASS',name:'net.minecraft.world.level.block.CropBlock'},transformer:function(node){
   var defaults=0,define=0,fert=0;
   for(var i=0;i<node.methods.size();i++){
    var method=node.methods.get(i),desc=String(method.desc),name=String(method.name);
    if(name==='<init>'){
     for(var p=method.instructions.getFirst();p!==null;p=p.getNext()){
      if(p.getOpcode()===O.INVOKEVIRTUAL&&String(p.desc)==='(Lnet/minecraft/world/level/block/state/BlockState;)V'){
       method.instructions.insertBefore(p,new Call(O.INVOKESTATIC,owner,'defaults','(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;',false));
       method.maxStack=Math.max(method.maxStack,2);
       defaults++;
      }
     }
    }
    if(desc==='(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V'){
     for(var p=method.instructions.getFirst();p!==null;p=p.getNext()){
      if(p.getOpcode()===O.RETURN){
       var code=new List();
       code.add(new Var(O.ALOAD,1));
       code.add(new Call(O.INVOKESTATIC,owner,'define','(Ljava/lang/Object;)V',false));
       method.instructions.insertBefore(p,code);
       method.maxStack=Math.max(method.maxStack,2);
       define++;
      }
     }
    }
    if(desc===boneDesc&&(name==='performBonemeal'||name==='m_214148_')){
     var code=new List(),vanilla=new Label();
     code.add(new Var(O.ALOAD,1));
     code.add(new Var(O.ALOAD,3));
     code.add(new Var(O.ALOAD,4));
     code.add(new Call(O.INVOKESTATIC,owner,'fertilize','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));
     code.add(new Jump(O.IFEQ,vanilla));
     code.add(new Insn(O.RETURN));
     code.add(vanilla);
     code.add(new Frame(O.F_SAME,0,null,0,null));
     method.instructions.insert(code);
     method.maxStack=Math.max(method.maxStack,3);
     fert++;
    }
   }
   if(defaults!==1||define!==1||fert!==1)throw new Error('Unexpected CropBlock blight hooks '+defaults+'/'+define+'/'+fert);
   return node;
  }}
 };
}

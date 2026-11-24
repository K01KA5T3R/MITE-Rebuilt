function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 return {mite_source_mob_ai_access:{target:{type:'CLASS',name:'net.minecraft.world.entity.Mob'},transformer:function(node){
  node.interfaces.add('dev/koikaster/mite/SourceMobAi');var count=0;
  for(var i=0;i<node.fields.size();i++){var field=node.fields.get(i),name=String(field.name),bridge=name==='goalSelector'||name==='f_21345_'?'mite$goals':name==='targetSelector'||name==='f_21346_'?'mite$targets':null;if(bridge===null)continue;
   var method=new Method(O.ACC_PUBLIC,bridge,'()Lnet/minecraft/world/entity/ai/goal/GoalSelector;',null,null);method.instructions.add(new Var(O.ALOAD,0));method.instructions.add(new Field(O.GETFIELD,node.name,name,'Lnet/minecraft/world/entity/ai/goal/GoalSelector;'));method.instructions.add(new Insn(O.ARETURN));method.maxStack=1;method.maxLocals=1;node.methods.add(method);count++;
  }if(count!==2)throw new Error('Source AI selector bridges '+count);
  var srg=false;for(var i=0;i<node.fields.size();i++)if(String(node.fields.get(i).name)==='f_21345_')srg=true;
  var recent=new Method(O.ACC_PUBLIC,'mite$recentPlayerHitTicks','()I',null,null);recent.instructions.add(new Var(O.ALOAD,0));recent.instructions.add(new Field(O.GETFIELD,'net/minecraft/world/entity/LivingEntity',srg?'f_20889_':'lastHurtByPlayerTime','I'));recent.instructions.add(new Insn(O.IRETURN));recent.maxStack=1;recent.maxLocals=1;node.methods.add(recent);return node;
 }}};
}

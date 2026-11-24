/*031 owns actual Spider parent-hurt scope and pre-super memory tick. The former
 * generated Spider.hurt block is removed from classic_arachnid_owners.js;
 * no ordering dependency exists between the two class transformers. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),MN=Java.type('org.objectweb.asm.tree.MethodNode');
 var H='dev/koikaster/mite/SourceParentHurtFrame',A='dev/koikaster/mite/SourceArachnidOwners',hurtDesc='(Lnet/minecraft/world/damagesource/DamageSource;F)Z';
 function match(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function call(code,owner,name,desc){code.add(new C(O.INVOKESTATIC,owner,name,desc,false));}
 function marker(name){var c=new L();c.add(new V(O.ALOAD,0));call(c,H,name,'(Ljava/lang/Object;)V');return c;}
 return {
  mite_source_parent_hurt_outcome:{target:{type:'CLASS',name:'net.minecraft.world.entity.LivingEntity'},transformer:function(node){
   var death=0;
   for(var n=0;n<node.methods.size();n++){
    var m=node.methods.get(n),instructions=m.instructions.toArray(),edited=false;
    if(match(m,'hurt','m_6469_',hurtDesc)){
     for(var j=0;j<instructions.length;j++){
      var x=instructions[j];
      // Source memory/decision RNG precedes the actual death/loot caller.
      if(x instanceof C&&(String(x.name)==='die'||String(x.name)==='m_6667_')&&String(x.desc)==='(Lnet/minecraft/world/damagesource/DamageSource;)V'){
       m.instructions.insertBefore(x,marker('beforeDeath'));death++;edited=true;
      }
     }
    }
    if(edited)m.maxStack=Math.max(m.maxStack,8);
   }
   //077 marks the actual of wrapper result, rather than native motion.
   if(death!==1)throw new Error('Source031 hurt death site '+death);return node;
  }},
  mite_source_parent_spider_callers:{target:{type:'CLASS',name:'net.minecraft.world.entity.monster.Spider'},transformer:function(node){
   var srg=false,tick=null;
   for(var n=0;n<node.methods.size();n++){
    var m=node.methods.get(n);
    if(match(m,'hurt','m_6469_',hurtDesc)||String(m.name)==='mite$nativeParentHurt')throw new Error('Source031 Spider hurt responsibility must have one owner');
    if(match(m,'tick','m_8119_','()V')){if(tick!==null)throw new Error('Duplicate native Spider tick');tick=m;srg=String(m.name)==='m_8119_';}
   }
   if(tick===null)throw new Error('Missing concrete native Spider tick');
   var head=new L();head.add(new V(O.ALOAD,0));call(head,A,'beforeTick','(Ljava/lang/Object;)V');tick.instructions.insert(head);tick.maxStack=Math.max(tick.maxStack,1);
   node.interfaces.add('dev/koikaster/mite/SourceParentNativeHurt');
   var name=srg?'m_6469_':'hurt',m=new MN(O.ACC_PUBLIC,name,hurtDesc,null,null),code=m.instructions,resume=new Label();
   // Preserve030 source snowball mapping/rejection before opening the scope.
   code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new V(O.FLOAD,2));call(code,A,'snowballDamage','(Ljava/lang/Object;Ljava/lang/Object;F)F');code.add(new V(O.FSTORE,3));
   code.add(new V(O.FLOAD,3));code.add(new I(O.FCONST_0));code.add(new I(O.FCMPG));code.add(new J(O.IFGE,resume));code.add(new I(O.ICONST_0));code.add(new I(O.IRETURN));code.add(resume);
   code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new V(O.FLOAD,3));code.add(new V(O.ALOAD,0));call(code,A,'parentLife','(Ljava/lang/Object;)Ljava/lang/Object;');call(code,'dev/koikaster/mite/SourceParentHurtCaller','hurt','(Ljava/lang/Object;Ljava/lang/Object;FLjava/lang/Object;)Z');code.add(new I(O.IRETURN));m.maxStack=4;m.maxLocals=4;node.methods.add(m);
   m=new MN(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$nativeParentHurt',hurtDesc,null,null);code=m.instructions;
   code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new V(O.FLOAD,2));code.add(new C(O.INVOKESPECIAL,node.superName,name,hurtDesc,false));code.add(new I(O.IRETURN));m.maxStack=3;m.maxLocals=3;node.methods.add(m);
   return node;
  }}
 };
}

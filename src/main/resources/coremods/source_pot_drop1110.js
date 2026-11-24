function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var H='dev/koikaster/mite/SourcePotDrop1110',obj='Ljava/lang/Object;';
 return {source_pot_item_constructor1110:{target:{type:'CLASS',name:'net.minecraft.world.entity.item.ItemEntity'},transformer:function(node){
  var random=0,bob=0,yaw=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if(String(m.name)!=='<init>'||String(m.desc)!=='(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V')continue;
   var ins=m.instructions.toArray();for(var j=0;j<ins.length;j++){
    var c=ins[j];
    if(c.getOpcode()===O.INVOKEINTERFACE&&String(c.owner)==='net/minecraft/util/RandomSource'&&(String(c.name)==='nextFloat'||String(c.name)==='m_188501_')&&String(c.desc)==='()F'){
     m.instructions.insertBefore(c,new Var(O.ALOAD,0));m.instructions.set(c,new Call(O.INVOKESTATIC,H,'nextFloat','('+obj+obj+')F',false));random++;
    }else if(c.getOpcode()===O.PUTFIELD&&String(c.owner)===String(node.name)&&(String(c.name)==='bobOffs'||String(c.name)==='f_31983_')&&String(c.desc)==='F'){
     var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,H,'bob','(F'+obj+')F',false));m.instructions.insertBefore(c,code);bob++;
    }else if(c.getOpcode()===O.INVOKEVIRTUAL&&(String(c.name)==='setYRot'||String(c.name)==='m_146922_')&&String(c.desc)==='(F)V'){
     var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,H,'yaw','(F'+obj+')F',false));m.instructions.insertBefore(c,code);yaw++;
    }
   }m.maxStack=Math.max(16,m.maxStack);
  }
  if(random!==2||bob!==1||yaw!==1)throw new Error('Unexpected selected item ctor '+random+'/'+bob+'/'+yaw);return node;
 }}};
}

function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var obj='Ljava/lang/Object;',holder='Lnet/minecraft/world/InteractionResultHolder;';
 var use='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)'+holder;
 var swap='(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)'+holder;
 return {'source_armor_equip1096':{target:{type:'CLASS',name:'net.minecraft.world.item.ArmorItem'},transformer:function(node){
  var count=0;
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if((String(m.name)!=='use'&&String(m.name)!=='m_7203_')||String(m.desc)!==use)continue;
   var ins=m.instructions.toArray();
   for(var j=0;j<ins.length;j++){
    var c=ins[j];
    if(c.getOpcode()!==O.INVOKEVIRTUAL||(String(c.owner)!=='net/minecraft/world/item/ArmorItem'&&String(c.owner)!=='net/minecraft/world/item/Equipable')||String(c.desc)!==swap)continue;
    m.instructions.set(c,new Call(O.INVOKESTATIC,'dev/koikaster/mite/SourceArmorEquip1096','swap','('+obj+obj+obj+obj+obj+')'+holder,false));count++;
   }
  }
  if(count!==1)throw new Error('Unexpected armor equip call '+count);
  return node;
 }}};
}

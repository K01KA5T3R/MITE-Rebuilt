/** Preserve old raw-metal stacks at the actual common ItemStack NBT load boundary. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {legacy_metal_ores_055:{target:{type:'CLASS',name:'net.minecraft.world.item.ItemStack'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)==='of'||String(m.name)==='m_41712_')&&String(m.desc)==='(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;'){
   if((m.access&O.ACC_STATIC)===0)throw new Error('Raw055 native load must be static');var code=new L();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/LegacyMetalOres','migrate','(Ljava/lang/Object;)Lnet/minecraft/nbt/CompoundTag;',false));code.add(new V(O.ASTORE,0));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);count++;
  }}if(count!==1)throw new Error('Raw055 native load sites '+count);return node;
 }}};
}

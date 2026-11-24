/** Client rendering consumers only; shared PotionItem, effects and NBT are untouched. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {mite_classic_potion_foil1301:{target:{type:'CLASS',name:'net.minecraft.client.renderer.entity.ItemRenderer'},transformer:function(node){
  var count=0,methods=0,desc='(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V';
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);if(String(m.desc)!==desc||(String(m.name)!=='render'&&String(m.name)!==String(A.mapMethod('m_115143_'))))continue;
   methods++;var instructions=m.instructions.toArray();
   for(var j=0;j<instructions.length;j++){
    var x=instructions[j];
    if(x instanceof C&&x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/item/ItemStack'&&String(x.desc)==='()Z'&&(String(x.name)==='hasFoil'||String(x.name)===String(A.mapMethod('m_41790_')))){
     m.instructions.set(x,new C(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicPotionAppearance','hasFoil','(Ljava/lang/Object;)Z',false));count++;
    }
   }
  }
  if(methods!==1||count!==3)throw new Error('Classic potion renderer consumers '+methods+'/'+count);
  return node;
 }}};
}

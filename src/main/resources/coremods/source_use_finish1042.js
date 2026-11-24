function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var H='dev/koikaster/mite/SourceUseFinish1042',S='net/minecraft/world/item/ItemStack',R='L'+S+';';
 function named(m,a,b){return String(m.name)===a||String(m.name)===b;}
 function owner(className,methodName,srgName,descriptor,actor,shrinks,adds){return {target:{type:'CLASS',name:className},transformer:function(node){var methods=0,consumed=0,inserted=0;for(var mi=0;mi<node.methods.size();mi++){
  var m=node.methods.get(mi);if(!named(m,methodName,srgName)||String(m.desc)!==descriptor)continue;methods++;
  var all=m.instructions.toArray();for(var j=0;j<all.length;j++){var x=all[j];if(x.getOpcode()!==O.INVOKEVIRTUAL)continue;
   if(String(x.owner)===S&&(String(x.name)==='shrink'||String(x.name)==='m_41774_')&&String(x.desc)==='(I)V'){
    m.instructions.insertBefore(x,new V(O.ALOAD,actor));m.instructions.set(x,new C(O.INVOKESTATIC,H,'consume','(Ljava/lang/Object;ILjava/lang/Object;)V',false));consumed++;
   }else if(String(x.owner)==='net/minecraft/world/entity/player/Inventory'&&(String(x.name)==='add'||String(x.name)==='m_36054_')&&String(x.desc)==='('+R+')Z'){
    m.instructions.set(x,new C(O.INVOKESTATIC,H,'addContainer','(Ljava/lang/Object;Ljava/lang/Object;)Z',false));inserted++;
   }
  }m.maxStack=Math.max(m.maxStack+1,4);
 }if(methods!==1||consumed!==shrinks||inserted!==adds)throw new Error('Source finish sites '+className+' '+methods+'/'+consumed+'/'+inserted);return node;}};}
 var itemFinish='('+R+'Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)'+R;
 return {
  source_stack_finish1042:{target:{type:'CLASS',name:'net.minecraft.world.item.ItemStack'},transformer:function(node){var found=0;for(var mi=0;mi<node.methods.size();mi++){var m=node.methods.get(mi);if(!named(m,'finishUsingItem','m_41671_')||String(m.desc)!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)'+R)continue;
   m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new V(O.ALOAD,2));m.instructions.add(new C(O.INVOKESTATIC,H,'finish','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)'+R,false));m.instructions.add(new I(O.ARETURN));m.maxStack=3;found++;
  }if(found!==1)throw new Error('Source stack finish owner '+found);return node;}},
  source_food_consume1042:owner('net.minecraft.world.entity.LivingEntity','eat','m_5584_','(Lnet/minecraft/world/level/Level;'+R+')'+R,0,1,0),
  source_milk_consume1042:owner('net.minecraft.world.item.MilkBucketItem','finishUsingItem','m_5922_',itemFinish,3,1,0),
  source_potion_consume1042:owner('net.minecraft.world.item.PotionItem','finishUsingItem','m_5922_',itemFinish,3,1,1),
  source_honey_container1042:owner('net.minecraft.world.item.HoneyBottleItem','finishUsingItem','m_5922_',itemFinish,3,0,1)
 };
}

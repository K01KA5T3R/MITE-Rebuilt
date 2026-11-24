function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),N=Java.type('org.objectweb.asm.tree.LabelNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 var H='dev/koikaster/mite/SourcePlacement1075',U='Lnet/minecraft/world/item/context/UseOnContext;',R='Lnet/minecraft/world/InteractionResult;',S='net/minecraft/world/item/ItemStack';
 function method(node,a,b,desc){var found=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===a||String(m.name)===b)&&String(m.desc)===desc){if(found!==null)throw new Error('Placement1075 duplicate '+a);found=m;}}if(found===null)throw new Error('Placement1075 missing '+a);return found;}
 return {
  mite_source_item_use_context:{target:{type:'CLASS',name:'net.minecraft.world.item.ItemStack'},transformer:function(node){
   var m=method(node,'useOn','m_41661_','('+U+')'+R),calls=0;
   for(var x=m.instructions.getFirst();x!==null;x=x.getNext())if(x instanceof C){
    if(x.getOpcode()===O.INVOKESTATIC&&String(x.owner)==='net/minecraftforge/common/ForgeHooks'&&String(x.name)==='onPlaceItemIntoWorld'&&String(x.desc)==='('+U+')'+R){x.owner=H;x.name='server';x.desc='(Ljava/lang/Object;)'+R;calls++;}
    else if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)===S&&String(x.name)==='onItemUse'&&String(x.desc)==='('+U+'Ljava/util/function/Function;)'+R){x.setOpcode(O.INVOKESTATIC);x.owner=H;x.name='client';x.desc='(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)'+R;x.itf=false;calls++;}
   }if(calls!==2)throw new Error('Placement1075 use entry calls '+calls);
   var old=method(node,'onItemUse','onItemUse','('+U+'Ljava/util/function/Function;)'+R);node.interfaces.add('dev/koikaster/mite/SourcePlacementAccess1075');var bridge=new M(O.ACC_PUBLIC|O.ACC_SYNTHETIC,'mite$placeNativeUse1075',String(old.desc),null,null);for(var i=0;i<3;i++)bridge.instructions.add(new V(O.ALOAD,i));bridge.instructions.add(new C(O.INVOKESPECIAL,S,String(old.name),String(old.desc),false));bridge.instructions.add(new I(O.ARETURN));bridge.maxStack=3;bridge.maxLocals=3;node.methods.add(bridge);return node;
  }},
  source_place_consume1075:{target:{type:'CLASS',name:'net.minecraft.world.item.BlockItem'},transformer:function(node){
   var m=method(node,'place','m_40576_','(Lnet/minecraft/world/item/context/BlockPlaceContext;)'+R),shrinks=0,success=0;
   for(var x=m.instructions.getFirst();x!==null;){var next=x.getNext();if(x instanceof C){
    if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)===S&&(String(x.name)==='shrink'||String(x.name)==='m_41774_')&&String(x.desc)==='(I)V'){x.setOpcode(O.INVOKESTATIC);x.owner=H;x.name='consume';x.desc='(Ljava/lang/Object;I)V';x.itf=false;shrinks++;}
    else if(x.getOpcode()===O.INVOKESTATIC&&String(x.owner)==='net/minecraft/world/InteractionResult'&&(String(x.name)==='sidedSuccess'||String(x.name)==='m_19078_')&&String(x.desc)==='(Z)'+R){m.instructions.insertBefore(x,new V(O.ALOAD,1));x.owner=H;x.name='success';x.desc='(ZLjava/lang/Object;)'+R;success++;}
   }x=next;}if(shrinks!==1||success!==1)throw new Error('Placement1075 block sites '+shrinks+'/'+success);m.maxStack=Math.max(3,m.maxStack);return node;
  }},
  source_place_commit1075:{target:{type:'CLASS',name:'net.minecraftforge.common.ForgeHooks'},transformer:function(node){
   var m=method(node,'onPlaceItemIntoWorld','onPlaceItemIntoWorld','('+U+')'+R),stats=0,snapshots=0;
   for(var x=m.instructions.getFirst();x!==null;){var next=x.getNext();if(x instanceof C){
    if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/entity/player/Player'&&String(x.desc)==='(Lnet/minecraft/stats/Stat;)V'){x.setOpcode(O.INVOKESTATIC);x.owner=H;x.name='awardUse';x.desc='(Ljava/lang/Object;Ljava/lang/Object;)V';x.itf=false;stats++;}
    if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='java/util/ArrayList'&&String(x.name)==='clone'&&String(x.desc)==='()Ljava/lang/Object;'){var at=x.getNext();while(at!==null&&!(at.getOpcode()===O.ASTORE&&at.var===11))at=at.getNext();if(at===null)throw new Error('Placement1075 snapshot local');var code=new L();code.add(new V(O.ALOAD,11));code.add(new C(O.INVOKESTATIC,H,'snapshots','(Ljava/lang/Object;)V',false));m.instructions.insert(at,code);snapshots++;}
   }x=next;}if(stats!==1||snapshots!==1)throw new Error('Placement1075 commit sites '+stats+'/'+snapshots);m.maxStack=Math.max(m.maxStack,1);return node;
  }},
  source_place_delay1075:{target:{type:'CLASS',name:'net.minecraft.client.Minecraft'},transformer:function(node){
   var m=method(node,'startUseItem','m_91277_','()V'),code=new L(),allowed=new N();code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourcePlacementClient1075','allowed','()Z',false));code.add(new J(O.IFNE,allowed));code.add(new I(O.RETURN));code.add(allowed);code.add(new F(O.F_SAME,0,null,0,null));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);return node;
  }}
 };
}

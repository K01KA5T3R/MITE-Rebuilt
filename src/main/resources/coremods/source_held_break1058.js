function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),N=Java.type('org.objectweb.asm.tree.LabelNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 var helper='dev/koikaster/mite/SourceHeldBreak1058';
 function method(node,a,b,desc){var found=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===a||String(m.name)===b)&&String(m.desc)===desc){if(found!==null)throw new Error('HeldBreak1058 duplicate method');found=m;}}if(found===null)throw new Error('HeldBreak1058 missing '+a);return found;}
 function fullFrame(locals,stack,slot){var out=locals.slice();while(out.length<slot)out.push(O.TOP);out.push('java/lang/Object');return new F(O.F_FULL,out.length,out,stack.length,stack);}
 return {
  source_held_break1058:{target:{type:'CLASS',name:'net.minecraft.world.item.ItemStack'},transformer:function(node){
   var m=method(node,'hurtAndBreak','m_41622_','(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V'),slot=m.maxLocals,initial=[String(node.name),O.INTEGER,'net/minecraft/world/entity/LivingEntity','java/util/function/Consumer'],locals=initial.slice(),all=m.instructions.toArray(),target=null;
   for(var i=0;i<all.length;i++){
    var x=all[i];if(x instanceof F){
     if(x.type===O.F_FULL||x.type===O.F_NEW){locals=[];for(var k=0;k<x.local.size();k++)locals.push(x.local.get(k));}
     else if(x.type===O.F_APPEND){for(var k=0;k<x.local.size();k++)locals.push(x.local.get(k));}
     else if(x.type===O.F_CHOP)locals.splice(locals.length-x.local.size());
     var stack=[];if(x.stack!==null)for(var k=0;k<x.stack.size();k++)stack.push(x.stack.get(k));m.instructions.set(x,fullFrame(locals,stack,slot));
    }
    if(x instanceof C&&String(x.owner)==='net/minecraft/world/item/ItemStack'&&(String(x.name)==='hurt'||String(x.name)==='m_220157_')&&String(x.desc)==='(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z'){
     var next=x.getNext();while(next!==null&&next.getOpcode()<0)next=next.getNext();if(!(next instanceof J)||next.getOpcode()!==O.IFEQ||target!==null)throw new Error('HeldBreak1058 hurt result branch');target=next;
    }
   }
   if(target===null)throw new Error('HeldBreak1058 missing damage result');
   var head=new L();head.add(new V(O.ALOAD,0));head.add(new V(O.ALOAD,2));head.add(new C(O.INVOKESTATIC,helper,'capture','(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;',false));head.add(new V(O.ASTORE,slot));m.instructions.insert(head);
   var code=new L(),nativePath=new N();code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,2));code.add(new V(O.ALOAD,slot));code.add(new C(O.INVOKESTATIC,helper,'finish','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));code.add(new J(O.IFEQ,nativePath));code.add(new I(O.RETURN));code.add(nativePath);code.add(fullFrame(initial,[],slot));m.instructions.insert(target,code);m.maxLocals=slot+1;m.maxStack=Math.max(m.maxStack,3);return node;
  }},
  source_break_pickup1058:{target:{type:'CLASS',name:'net.minecraft.world.entity.item.ItemEntity'},transformer:function(node){
   var m=method(node,'playerTouch','m_6123_','(Lnet/minecraft/world/entity/player/Player;)V'),code=new L(),allowed=new N();code.add(new V(O.ALOAD,1));code.add(new C(O.INVOKESTATIC,helper,'mayPickup','(Ljava/lang/Object;)Z',false));code.add(new J(O.IFNE,allowed));code.add(new I(O.RETURN));code.add(allowed);code.add(new F(O.F_SAME,0,null,0,null));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);return node;
  }}
 };
}

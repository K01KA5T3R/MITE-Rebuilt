function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes'), L=Java.type('org.objectweb.asm.tree.InsnList');
    var V=Java.type('org.objectweb.asm.tree.VarInsnNode'), C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var J=Java.type('org.objectweb.asm.tree.JumpInsnNode'), N=Java.type('org.objectweb.asm.tree.InsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    return {mite_offline_signed_chat1158:{target:{type:'CLASS',name:'net.minecraft.network.chat.OutgoingChatMessage$Player'},transformer:function(node){
        var count=0;
        for(var i=0;i<node.methods.size();i++){
            var m=node.methods.get(i);
            if(String(m.desc)!=='(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/network/chat/ChatType$Bound;)V')continue;
            var code=new L(),original=new Label();
            code.add(new V(O.ALOAD,0));code.add(new V(O.ALOAD,1));code.add(new V(O.ILOAD,2));code.add(new V(O.ALOAD,3));
            code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/OfflineChat1158','send','(Lnet/minecraft/network/chat/OutgoingChatMessage$Player;Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/network/chat/ChatType$Bound;)Z',false));
            code.add(new J(O.IFEQ,original));code.add(new N(O.RETURN));code.add(original);
            m.instructions.insert(code);m.maxStack=Math.max(4,m.maxStack);count++;
        }
        if(count!==1)throw new Error('Offline chat1158 native send sites '+count);
        return node;
    }}};
}

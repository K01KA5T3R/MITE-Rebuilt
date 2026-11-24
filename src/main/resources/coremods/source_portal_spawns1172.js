function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes'), L=Java.type('org.objectweb.asm.tree.InsnList');
    var V=Java.type('org.objectweb.asm.tree.VarInsnNode'), C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var J=Java.type('org.objectweb.asm.tree.JumpInsnNode'), N=Java.type('org.objectweb.asm.tree.InsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    return {source_portal_spawn_route1172:{target:{type:'CLASS',name:'net.minecraft.world.level.block.NetherPortalBlock'},transformer:function(node){
        var count=0;
        for(var i=0;i<node.methods.size();i++){
            var m=node.methods.get(i);
            if((String(m.name)!=='randomTick'&&String(m.name)!=='m_213898_')||String(m.desc)!=='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V')continue;
            var code=new L(),original=new Label();
            code.add(new V(O.ALOAD,1));code.add(new V(O.ALOAD,2));code.add(new V(O.ALOAD,3));
            code.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourcePortalSpawns1172','allows','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z',false));
            code.add(new J(O.IFNE,original));code.add(new N(O.RETURN));code.add(original);
            m.instructions.insert(code);m.maxStack=Math.max(3,m.maxStack);count++;
        }
        if(count!==1)throw new Error('Portal1172 native random-tick sites '+count);
        return node;
    }}};
}

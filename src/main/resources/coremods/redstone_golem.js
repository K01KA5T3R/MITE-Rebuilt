/** Preempt only a successful redstone T; native snow and iron patterns keep their own paths. */
function initializeCoreMod() {
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsn=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsn=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var JumpInsn=Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Insn=Java.type('org.objectweb.asm.tree.InsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame=Java.type('org.objectweb.asm.tree.FrameNode');
    return {redstone_golem_pattern:{
        target:{type:'CLASS',name:'net.minecraft.world.level.block.CarvedPumpkinBlock'},
        transformer:function(node) {
            var matched=0;
            for(var i=0;i<node.methods.size();i++) {
                var method=node.methods.get(i);
                var name=String(method.name);
                if((name!=='trySpawnGolem' && name!=='m_51378_') ||
                    String(method.desc)!=='(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V') continue;
                var entry=new InsnList();
                var vanilla=new Label();
                entry.add(new VarInsn(Opcodes.ALOAD,1));
                entry.add(new VarInsn(Opcodes.ALOAD,2));
                entry.add(new MethodInsn(Opcodes.INVOKESTATIC,'dev/koikaster/mite/RedstoneGolemSummon','trySpawn',
                    '(Ljava/lang/Object;Ljava/lang/Object;)Z',false));
                entry.add(new JumpInsn(Opcodes.IFEQ,vanilla));
                entry.add(new Insn(Opcodes.RETURN));
                entry.add(vanilla);
                entry.add(new Frame(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(entry);
                method.maxStack=Math.max(2,method.maxStack);
                matched++;
            }
            if(matched!==1)throw new Error('Redstone golem native entry count '+matched);
            return node;
        }
    }};
}

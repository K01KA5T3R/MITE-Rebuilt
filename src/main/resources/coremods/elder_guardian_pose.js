function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var guardian = 'Lnet/minecraft/world/entity/monster/Guardian;';
    return {
        mite_elder_guardian_pose: {
            target: {type: 'CLASS', name: 'net.minecraft.client.model.GuardianModel'},
            transformer: function(node) {
                var changed = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'setupAnim' && name !== 'm_6973_') || String(method.desc) !== '(' + guardian + 'FFFFF)V') continue;
                    for (var insns = method.instructions.iterator(); insns.hasNext();) {
                        var insn = insns.next();
                        if (insn.getOpcode() !== O.RETURN) continue;
                        var code = new List();
                        code.add(new Var(O.ALOAD, 0));
                        code.add(new Var(O.ALOAD, 1));
                        code.add(new Var(O.FLOAD, 4));
                        code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClientElderGuardianPose', 'pose',
                            '(Lnet/minecraft/client/model/GuardianModel;' + guardian + 'F)V', false));
                        method.instructions.insertBefore(insn, code);
                        ++changed;
                    }
                    method.maxStack = Math.max(method.maxStack, 3);
                }
                if (changed !== 1) throw new Error('Elder Guardian model pose sites ' + changed);
                return node;
            }
        }
    };
}

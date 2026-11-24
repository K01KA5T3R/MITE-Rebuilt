function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var part = 'net/minecraft/client/model/geom/ModelPart';
    var dragon = 'Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;';
    var signature = '(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V';

    function member(node, named, mapped, desc) {
        for (var i = 0; i < node.fields.size(); ++i) {
            var field = node.fields.get(i);
            if ((String(field.name) === named || String(field.name) === mapped) && String(field.desc) === desc) return String(field.name);
        }
        throw new Error('Missing DragonModel field ' + named);
    }

    return {
        mite_dragon_jaw_pose: {
            target: {type: 'CLASS', name: 'net.minecraft.client.renderer.entity.EnderDragonRenderer$DragonModel'},
            transformer: function(node) {
                var jaw = member(node, 'jaw', 'f_114237_', 'L' + part + ';');
                var owner = member(node, 'entity', 'f_114233_', dragon);
                var partial = member(node, 'a', 'f_114234_', 'F');
                var changed = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== signature) continue;
                    var settingJaw = false;
                    for (var it = method.instructions.iterator(); it.hasNext();) {
                        var insn = it.next();
                        if (insn.getOpcode() === O.GETFIELD && String(insn.owner) === String(node.name) && String(insn.name) === jaw) {
                            settingJaw = true;
                        }
                        if (insn.getOpcode() !== O.PUTFIELD || String(insn.owner) !== part || String(insn.desc) !== 'F') continue;
                        if (settingJaw && (String(insn.name) === 'xRot' || String(insn.name) === 'f_104203_')) {
                            var code = new List();
                            code.add(new Var(O.ALOAD, 0));
                            code.add(new Field(O.GETFIELD, node.name, owner, dragon));
                            code.add(new Var(O.ALOAD, 0));
                            code.add(new Field(O.GETFIELD, node.name, partial, 'F'));
                            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClientDragonAttackPose', 'jaw', '(F' + dragon + 'F)F', false));
                            method.instructions.insertBefore(insn, code);
                            ++changed;
                        }
                        settingJaw = false;
                    }
                    method.maxStack += 2;
                }
                if (changed !== 1) throw new Error('Dragon jaw pose sites ' + changed);
                return node;
            }
        }
    };
}

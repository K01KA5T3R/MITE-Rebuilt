function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var P = 'dev/koikaster/mite/ClientGiantZombiePose';
    var monster = 'Lnet/minecraft/world/entity/monster/';
    var models = 'Lnet/minecraft/client/model/';
    var stack = 'Lcom/mojang/blaze3d/vertex/PoseStack;';

    function inject(node, named, mapped, descriptor, add) {
        var changed = 0;
        for (var i = 0; i < node.methods.size(); ++i) {
            var method = node.methods.get(i);
            var name = String(method.name);
            if ((name !== named && name !== mapped) || String(method.desc) !== descriptor) continue;
            var exits = 0;
            for (var insns = method.instructions.iterator(); insns.hasNext();) {
                var insn = insns.next();
                if (insn.getOpcode() !== O.RETURN) continue;
                var code = new List();
                add(code);
                method.instructions.insertBefore(insn, code);
                ++exits;
            }
            if (exits === 0) throw new Error('No render exit for ' + named);
            method.maxStack = Math.max(method.maxStack, 3);
            ++changed;
        }
        if (changed !== 1) throw new Error('Boss pose sites for ' + named + ': ' + changed);
        return node;
    }

    function pose(model, entity, callback) {
        return function(node) {
            return inject(node, 'setupAnim', 'm_6973_', '(' + monster + entity + ';FFFFF)V', function(code) {
                code.add(new Var(O.ALOAD, 0));
                code.add(new Var(O.ALOAD, 1));
                code.add(new Var(O.FLOAD, 4));
                code.add(new Call(O.INVOKESTATIC, P, callback, '(' + models + model + ';' + monster + entity + ';F)V', false));
            });
        };
    }

    return {
        mite_giant_zombie_pose: {target: {type: 'CLASS', name: 'net.minecraft.client.model.AbstractZombieModel'},
            transformer: pose('HumanoidModel', 'Monster', 'giant')},
        mite_giant_zombie_scale: {target: {type: 'CLASS', name: 'net.minecraft.client.renderer.entity.GiantMobRenderer'},
            transformer: function(node) {
                return inject(node, 'scale', 'm_7546_', '(' + monster + 'Giant;' + stack + 'F)V', function(code) {
                    code.add(new Var(O.ALOAD, 1));
                    code.add(new Var(O.ALOAD, 2));
                    code.add(new Var(O.FLOAD, 3));
                    code.add(new Call(O.INVOKESTATIC, P, 'giantScale', '(' + monster + 'Giant;' + stack + 'F)V', false));
                });
            }}
    };
}

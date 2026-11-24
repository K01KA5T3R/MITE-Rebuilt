function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var model = 'net/minecraft/client/model/WitherBossModel';
    var actor = 'Lnet/minecraft/world/entity/boss/wither/WitherBoss;';
    var builders = 'net/minecraft/client/model/geom/builders/';
    var deform = 'L' + builders + 'CubeDeformation;';
    var mesh = 'L' + builders + 'MeshDefinition;';
    var layer = 'L' + builders + 'LayerDefinition;';
    var owner = 'dev/koikaster/mite/ClientWitherModel';
    return { wither_combat_model: {
        target: {type: 'CLASS', name: model.replace(/\//g, '.')},
        transformer: function(node) {
            var meshes = 0;
            var poses = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                var desc = String(method.desc);
                if (desc !== '(' + deform + ')' + layer && desc !== '(' + actor + 'FFFFF)V') continue;
                var instructions = method.instructions.toArray();
                for (var j = 0; j < instructions.length; j++) {
                    var instruction = instructions[j];
                    if (desc === '(' + deform + ')' + layer && instruction instanceof Call
                            && String(instruction.desc) === '(' + mesh + 'II)' + layer) {
                        method.instructions.insertBefore(instruction, new Var(O.ALOAD, 0));
                        method.instructions.set(instruction, new Call(O.INVOKESTATIC, owner,
                            'layer', '(' + mesh + 'II' + deform + ')' + layer, false));
                        method.maxStack = Math.max(method.maxStack, 4);
                        meshes++;
                    } else if (desc === '(' + actor + 'FFFFF)V' && instruction.getOpcode() === O.RETURN) {
                        var code = new List();
                        code.add(new Var(O.ALOAD, 0));
                        code.add(new Var(O.ALOAD, 1));
                        code.add(new Var(O.FLOAD, 4));
                        code.add(new Call(O.INVOKESTATIC, owner, 'pose', '(L' + model + ';' + actor + 'F)V', false));
                        method.instructions.insertBefore(instruction, code);
                        method.maxStack = Math.max(method.maxStack, 3);
                        poses++;
                    }
                }
            }
            if (meshes !== 1 || poses !== 1) throw new Error('Wither model hook counts ' + meshes + '/' + poses);
            return node;
        }
    }};
}

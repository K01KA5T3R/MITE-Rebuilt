function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var stem = 'Lnet/minecraft/world/level/dimension/LevelStem;';
    var server = 'Lnet/minecraft/server/MinecraftServer;';
    var key = 'Lnet/minecraft/resources/ResourceKey;';
    return {
        mite_content_mode_classic_terrain: {
            target: {type: 'CLASS', name: 'net.minecraft.server.level.ServerLevel'},
            transformer: function (node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.name) !== '<init>' ||
                            !String(method.desc).startsWith('(' + server + 'Ljava/util/concurrent/Executor;') ||
                            String(method.desc).indexOf(key + stem) === -1) continue;
                    var prefix = new List();
                    prefix.add(new Var(O.ALOAD, 6));
                    prefix.add(new Var(O.ALOAD, 1));
                    prefix.add(new Var(O.ALOAD, 5));
                    prefix.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicTerrainAdapters',
                        'select', '(' + stem + server + key + ')' + stem, false));
                    prefix.add(new Var(O.ASTORE, 6));
                    method.instructions.insert(prefix);
                    method.maxStack = Math.max(method.maxStack, 3);
                    count++;
                }
                if (count !== 1) throw new Error('Expected the native ServerLevel dimension/generator constructor');
                return node;
            }
        },
        mite_content_mode_classic_end_fight: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.dimension.end.EndDragonFight'},
            transformer: function (node) {
                var level = 'Lnet/minecraft/server/level/ServerLevel;';
                var levelField = null;
                for (var f = 0; f < node.fields.size(); f++) {
                    var field = node.fields.get(f);
                    if (String(field.desc) !== level ||
                            (String(field.name) !== 'level' && String(field.name) !== 'f_64061_')) continue;
                    levelField = field;
                }
                if (levelField == null) throw new Error('Missing original End fight level');
                var gateway = 0;
                var respawn = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '()V') continue;
                    var name = String(method.name);
                    var allowed;
                    if (name === 'spawnNewGateway' || name === 'm_64109_') {
                        gateway++;
                        allowed = 'modernEndGatewayAllowed';
                    } else if (name === 'tryRespawn' || name === 'm_64100_') {
                        respawn++;
                        allowed = 'rebuilt';
                    } else continue;
                    var enabled = new Label();
                    var code = new List();
                    code.add(new Var(O.ALOAD, 0));
                    code.add(new Field(O.GETFIELD, node.name, levelField.name, level));
                    code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentMode', allowed,
                        '(Lnet/minecraft/world/level/Level;)Z', false));
                    code.add(new Jump(O.IFNE, enabled));
                    code.add(new Insn(O.RETURN));
                    code.add(enabled);
                    code.add(new Frame(O.F_SAME, 0, null, 0, null));
                    method.instructions.insert(code);
                    method.maxStack = Math.max(method.maxStack, 1);
                }
                if (gateway !== 1 || respawn !== 1) throw new Error('Expected one gateway and one boss respawn site');
                return node;
            }
        }
    };
}

function initializeCoreMod() {
    var Op = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var loader = 'net/minecraft/client/renderer/texture/atlas/SpriteResourceLoader';

    return {
        furnace_ember_states: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.block.AbstractFurnaceBlock'},
            transformer: function (node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if (name !== 'createBlockStateDefinition' && name !== 'm_7926_') continue;
                    if (String(method.desc) !== '(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V') continue;
                    var code = new InsnList();
                    code.add(new Var(Op.ALOAD, 0));
                    code.add(new Var(Op.ALOAD, 1));
                    code.add(new Call(Op.INVOKESTATIC, 'dev/koikaster/mite/FurnaceEmbers', 'addNativeProperty',
                        '(Ljava/lang/Object;Ljava/lang/Object;)V', false));
                    method.instructions.insert(code);
                    method.maxStack = Math.max(method.maxStack, 2);
                    count++;
                }
                if (count !== 1) throw new Error('Furnace state definition hook count ' + count);
                return node;
            }
        },
        furnace_ember_sprites: {
            target: {type: 'CLASS', name: loader.replace(/\//g, '.')},
            transformer: function (node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if (name !== 'load' && name !== 'm_261166_') continue;
                    if (String(method.desc) !== '(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;)L' + loader + ';') continue;
                    for (var insn = method.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
                        if (insn.getOpcode() !== Op.INVOKESPECIAL || String(insn.owner) !== loader
                                || String(insn.name) !== '<init>' || String(insn.desc) !== '(Ljava/util/List;)V') continue;
                        var code = new InsnList();
                        code.add(new Var(Op.ALOAD, 1));
                        code.add(new Call(Op.INVOKESTATIC, 'dev/koikaster/mite/ClientFurnaceEmbers', 'append',
                            '(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;', false));
                        method.instructions.insertBefore(insn, code);
                        method.maxStack++;
                        count++;
                    }
                }
                if (count !== 1) throw new Error('Furnace atlas hook count ' + count);
                return node;
            }
        }
    };
}


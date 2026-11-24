function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var TypeOp = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var context = 'Lnet/minecraft/world/level/levelgen/feature/treedecorators/TreeDecorator$Context;';
    var fishing = 'net/minecraft/world/entity/projectile/FishingHook';
    var mode = 'dev/koikaster/mite/ClassicFishingMode';
    var data = 'Lnet/minecraft/network/syncher/EntityDataAccessor;';
    var watcher = 'net/minecraft/network/syncher/SynchedEntityData';
    var rng = 'Lnet/minecraft/util/RandomSource;';

    function appendNibbleBridge(node, field) {
        var read = new Method(O.ACC_PUBLIC, 'mite$classicNibble', '()I', null, null);
        read.instructions.add(new Var(O.ALOAD, 0));
        read.instructions.add(new Field(O.GETFIELD, fishing, field, 'I'));
        read.instructions.add(new Insn(O.IRETURN));
        read.maxLocals = read.maxStack = 1;
        node.methods.add(read);
        var write = new Method(O.ACC_PUBLIC, 'mite$classicNibble', '(I)V', null, null);
        write.instructions.add(new Var(O.ALOAD, 0));
        write.instructions.add(new Var(O.ILOAD, 1));
        write.instructions.add(new Field(O.PUTFIELD, fishing, field, 'I'));
        write.instructions.add(new Insn(O.RETURN));
        write.maxLocals = write.maxStack = 2;
        node.methods.add(write);
    }

    function appendBitingBridge(node, field, development) {
        var method = new Method(O.ACC_PUBLIC, 'mite$classicBiting', '(Z)V', null, null);
        method.instructions.add(new Var(O.ALOAD, 0));
        method.instructions.add(new Call(O.INVOKEVIRTUAL, fishing, development ? 'getEntityData' : 'm_20088_',
            '()L' + watcher + ';', false));
        method.instructions.add(new Field(O.GETSTATIC, fishing, field, data));
        method.instructions.add(new Var(O.ILOAD, 1));
        method.instructions.add(new Call(O.INVOKESTATIC, 'java/lang/Boolean', 'valueOf', '(Z)Ljava/lang/Boolean;', false));
        method.instructions.add(new Call(O.INVOKEVIRTUAL, watcher, development ? 'set' : 'm_135381_',
            '(' + data + 'Ljava/lang/Object;)V', false));
        method.instructions.add(new Insn(O.RETURN));
        method.maxLocals = 2;
        method.maxStack = 3;
        node.methods.add(method);
    }

    function fishingHook(node) {
        var nibble = null, biting = null, development = false;
        for (var i = 0; i < node.fields.size(); i++) {
            var field = node.fields.get(i);
            var name = String(field.name);
            if ((name === 'nibble' || name === 'f_37089_') && String(field.desc) === 'I') {
                nibble = name;
                development = name === 'nibble';
            }
            if ((name === 'DATA_BITING' || name === 'f_37102_') && String(field.desc) === data) biting = name;
        }
        if (nibble === null || biting === null) throw new Error('Missing native fishing bite fields');
        var ticks = 0, retrieves = 0;
        for (var m = 0; m < node.methods.size(); m++) {
            var method = node.methods.get(m);
            var name = String(method.name);
            if (String(method.desc) === '(Lnet/minecraft/core/BlockPos;)V' && (name === 'catchingFish' || name === 'm_37145_')) {
                var resume = new Label();
                var code = new List();
                code.add(new Var(O.ALOAD, 0));
                code.add(new Var(O.ALOAD, 0));
                code.add(new Field(O.GETFIELD, fishing, development ? 'random' : 'f_19796_', rng));
                code.add(new Call(O.INVOKESTATIC, mode, 'tick', '(L' + fishing + ';' + rng + ')Z', false));
                code.add(new Jump(O.IFEQ, resume));
                code.add(new Insn(O.RETURN));
                code.add(resume);
                code.add(new Frame(O.F_SAME, 0, null, 0, null));
                method.instructions.insert(code);
                method.maxStack = Math.max(method.maxStack, 2);
                ticks++;
            } else if (String(method.desc) === '(Lnet/minecraft/world/item/ItemStack;)I' && (name === 'retrieve' || name === 'm_37156_')) {
                var instructions = method.instructions.toArray();
                for (var at = 0; at < instructions.length; at++) {
                    var insn = instructions[at];
                    if (!(insn instanceof TypeOp) || insn.getOpcode() !== O.NEW
                            || String(insn.desc) !== 'net/minecraft/world/entity/item/ItemEntity') continue;
                    // This is the first caught-stack spawn, after Forge accepts the retrieval.
                    var consume = new List();
                    consume.add(new Var(O.ALOAD, 0));
                    consume.add(new Call(O.INVOKESTATIC, mode, 'consumeBait', '(L' + fishing + ';)V', false));
                    method.instructions.insertBefore(insn, consume);
                    retrieves++;
                }
                method.maxStack = Math.max(method.maxStack, 1);
            }
        }
        if (ticks !== 1 || retrieves !== 1) throw new Error('Expected one fishing tick and one catch spawn');
        node.interfaces.add(mode + '$BiteAccess');
        appendNibbleBridge(node, nibble);
        appendBitingBridge(node, biting, development);
        return node;
    }

    return {
        mite_content_mode_classic_temple_biome: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.chunk.ChunkGenerator'},
            transformer: function (node) {
                var attached = 0;
                var entry = 'Lnet/minecraft/world/level/levelgen/structure/StructureSet$StructureSelectionEntry;';
                var pos = 'Lnet/minecraft/world/level/ChunkPos;';
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    var descriptor = String(method.desc);
                    if ((name !== 'tryGenerateStructure' && name !== 'm_223104_') || !descriptor.startsWith('(' + entry)
                            || !descriptor.endsWith(pos + 'Lnet/minecraft/core/SectionPos;)Z')) continue;
                    var normal = new Label();
                    var code = new List();
                    code.add(new Var(O.ALOAD, 0));
                    code.add(new Var(O.ALOAD, 1));
                    code.add(new Var(O.ALOAD, 9));
                    code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicTemplePlacement', 'allows',
                        '(L' + node.name + ';' + entry + pos + ')Z', false));
                    code.add(new Jump(O.IFNE, normal));
                    code.add(new Insn(O.ICONST_0));
                    code.add(new Insn(O.IRETURN));
                    code.add(normal);
                    code.add(new Frame(O.F_SAME, 0, null, 0, null));
                    method.instructions.insert(code);
                    method.maxStack = Math.max(3, method.maxStack);
                    attached++;
                }
                if (attached !== 1) throw new Error('Expected native structure start predicate');
                return node;
            }
        },
        mite_content_mode_classic_spawn_pool: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.NaturalSpawner'},
            transformer: function (node) {
                var count = 0;
                var pool = 'Lnet/minecraft/util/random/WeightedRandomList;';
                var server = 'Lnet/minecraft/server/level/ServerLevel;';
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'mobsAt' && name !== 'm_220443_') || !String(method.desc).startsWith('(' + server)
                            || !String(method.desc).endsWith(')' + pool)) continue;
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; j++) {
                        var resolution = instructions[j];
                        if (resolution.getOpcode() !== O.ARETURN) continue;
                        var code = new List();
                        code.add(new Var(O.ALOAD, 0));
                        code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicModernContent',
                            'nativeSpawnPool', '(' + pool + server + ')' + pool, false));
                        method.instructions.insertBefore(resolution, code);
                        count++;
                    }
                    method.maxStack = Math.max(2, method.maxStack);
                }
                if (count < 1) throw new Error('Missing native natural-spawn selection');
                return node;
            }
        },
        mite_content_mode_classic_fishing: {
            target: {type: 'CLASS', name: 'net.minecraft.world.entity.projectile.FishingHook'},
            transformer: fishingHook
        },
        mite_content_mode_classic_beehive: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator'},
            transformer: function (node) {
                var attached = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if (String(method.desc) !== '(' + context + ')V' || name !== 'place' && name !== 'm_214187_') continue;
                    var normal = new Label();
                    var prefix = new List();
                    prefix.add(new Var(O.ALOAD, 1));
                    prefix.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicModernContent',
                        'beehive', '(' + context + ')Z', false));
                    prefix.add(new Jump(O.IFNE, normal));
                    prefix.add(new Insn(O.RETURN));
                    prefix.add(normal);
                    prefix.add(new Frame(O.F_SAME, 0, null, 0, null));
                    method.instructions.insert(prefix);
                    method.maxStack = Math.max(1, method.maxStack);
                    attached++;
                }
                if (attached !== 1) throw new Error('Expected native tree beehive decorator');
                return node;
            }
        }
    };
}

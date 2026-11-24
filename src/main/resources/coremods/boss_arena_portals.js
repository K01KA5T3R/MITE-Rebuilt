function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var O_FRAME = Java.type('org.objectweb.asm.tree.AbstractInsnNode').FRAME;
    var level = 'net/minecraft/world/level/Level';
    var pos = 'net/minecraft/core/BlockPos';
    var blockState = 'net/minecraft/world/level/block/state/BlockState';
    var entity = 'net/minecraft/world/entity/Entity';
    var gateway = 'net/minecraft/world/level/block/entity/TheEndGatewayBlockEntity';
    var desc = '(L' + level + ';L' + pos + ';L' + blockState + ';L' + entity + ';L' + gateway + ';)V';
    var blockedDesc = '(L' + level + ';L' + pos + ';)Z';

    function blocked() {
        return new Call(O.INVOKESTATIC, 'dev/koikaster/mite/BossArenaPortals', 'blocked', blockedDesc, false);
    }

    function field(node, official, srg, descriptor) {
        for (var i = 0; i < node.fields.size(); ++i) {
            var value = node.fields.get(i), name = String(value.name);
            if ((name === official || name === srg) && String(value.desc) === descriptor) return name;
        }
        throw new Error('Missing gateway field ' + official);
    }

    function resolvedExitStore(method) {
        var store = null;
        for (var insns = method.instructions.iterator(); insns.hasNext();) {
            var insn = insns.next();
            if (insn.getOpcode() !== O.INVOKESTATIC || String(insn.owner) !== gateway
                || (String(insn.name) !== 'findExitPosition' && String(insn.name) !== 'm_155825_')
                || String(insn.desc) !== '(L' + level + ';L' + pos + ';)L' + pos + ';') continue;
            var next = insn.getNext();
            while (next !== null && next.getOpcode() < 0) next = next.getNext();
            if (store !== null || next === null || next.getOpcode() !== O.ASTORE || next.var !== 6)
                throw new Error('Unexpected gateway destination local');
            store = next;
        }
        if (store === null) throw new Error('Missing resolved gateway destination');
        return store;
    }

    return {
        mite_boss_gateway_entry: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity'},
            transformer: function(node) {
                var changed = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'teleportEntity' && name !== 'm_155828_') || String(method.desc) !== desc
                        || !(method.access & O.ACC_STATIC)) continue;
                    var store = resolvedExitStore(method);
                    var mergeFrame = store.getPrevious();
                    while (mergeFrame !== null && mergeFrame.getType() !== O_FRAME) mergeFrame = mergeFrame.getPrevious();
                    var nextFrame = store.getNext();
                    while (nextFrame !== null && nextFrame.getType() !== O_FRAME) nextFrame = nextFrame.getNext();
                    if (mergeFrame === null || mergeFrame.stack === null || mergeFrame.stack.size() !== 1
                        || String(mergeFrame.stack.get(0)) !== pos || nextFrame === null || nextFrame.local === null
                        || nextFrame.local.size() < 7 || String(nextFrame.local.get(6)) !== pos)
                        throw new Error('Unexpected frame around gateway destination');
                    // Nashorn exposes getType() as .type, so inspect the merge frame's locals instead.
                    var expanded = mergeFrame.local !== null;
                    // FrameNode's array parameter converts script arrays without a sandboxed class lookup.
                    var initial = [level, pos, blockState, entity, gateway];
                    var destination = [level, pos, blockState, entity, gateway, O.TOP, pos];
                    var code = new List();
                    var open = new Label();
                    code.add(new Var(O.ALOAD, 0));
                    code.add(new Var(O.ALOAD, 1));
                    code.add(blocked());
                    code.add(new Jump(O.IFEQ, open));
                    code.add(new Insn(O.RETURN));
                    code.add(open);
                    code.add(expanded ? new Frame(O.F_NEW, 5, initial, 0, null) : new Frame(O.F_SAME, 0, null, 0, null));
                    method.instructions.insert(code);

                    // The exact and surface-adjusted native branches share this store.
                    // Reject before any pearl, player cooldown, or teleport side effects.
                    var linkedExit = field(node, 'exitPortal', 'f_59928_', 'L' + pos + ';');
                    var cooldown = field(node, 'teleportCooldown', 'f_59927_', 'I');
                    var gate = new List(), denied = new Label(), clear = new Label();
                    gate.add(new Var(O.ALOAD, 0));
                    gate.add(new Var(O.ALOAD, 4));
                    gate.add(new Field(O.GETFIELD, gateway, linkedExit, 'L' + pos + ';'));
                    gate.add(blocked());
                    gate.add(new Jump(O.IFNE, denied));
                    gate.add(new Var(O.ALOAD, 0));
                    gate.add(new Var(O.ALOAD, store.var));
                    gate.add(blocked());
                    gate.add(new Jump(O.IFEQ, clear));
                    gate.add(denied);
                    gate.add(new Frame(expanded ? O.F_NEW : O.F_FULL, 7, destination, 0, null));
                    gate.add(new Var(O.ALOAD, 4));
                    gate.add(new Insn(O.ICONST_0));
                    gate.add(new Field(O.PUTFIELD, gateway, cooldown, 'I'));
                    gate.add(new Insn(O.RETURN));
                    gate.add(clear);
                    gate.add(new Frame(expanded ? O.F_NEW : O.F_SAME, expanded ? 7 : 0, expanded ? destination : null, 0, null));
                    method.instructions.insert(store, gate);
                    method.maxStack = Math.max(method.maxStack, 2);
                    changed++;
                }
                if (changed !== 1) throw new Error('Expected one End gateway entry, found ' + changed);
                return node;
            }
        }
    };
}

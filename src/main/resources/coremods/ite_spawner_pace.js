function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
    var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
    var owner = 'net/minecraft/world/level/BaseSpawner';
    var lifetime = 'dev/koikaster/mite/SourceSpawnerLifetime';
    var world = 'Lnet/minecraft/world/level/Level;';
    var entity = 'Lnet/minecraft/world/entity/Entity;';
    var server = 'net/minecraft/server/level/ServerLevel';
    var level = 'Lnet/minecraft/server/level/ServerLevel;';
    var pos = 'Lnet/minecraft/core/BlockPos;';

    function previousInstruction(instruction) {
        var previous = instruction.getPrevious();
        while (previous !== null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    function sameField(instruction, opcode, name) {
        return instruction instanceof FieldInsnNode && instruction.getOpcode() === opcode &&
            String(instruction.owner) === owner && String(instruction.name) === name && String(instruction.desc) === 'I';
    }

    function countdownWrite(method, delay) {
        var match = null;
        var count = 0;
        for (var iterator = method.instructions.iterator(); iterator.hasNext();) {
            var write = iterator.next();
            if (!sameField(write, Opcodes.PUTFIELD, delay)) continue;
            var subtract = previousInstruction(write);
            if (subtract === null || subtract.getOpcode() !== Opcodes.ISUB) continue;
            var one = previousInstruction(subtract);
            if (one === null || one.getOpcode() !== Opcodes.ICONST_1) continue;
            var read = previousInstruction(one);
            if (!sameField(read, Opcodes.GETFIELD, delay)) continue;
            var duplicate = previousInstruction(read);
            if (duplicate === null || duplicate.getOpcode() !== Opcodes.DUP) continue;
            var receiver = previousInstruction(duplicate);
            if (!(receiver instanceof VarInsnNode) || receiver.getOpcode() !== Opcodes.ALOAD || receiver.var !== 0) continue;
            var branch = previousInstruction(receiver);
            if (branch === null || branch.getOpcode() !== Opcodes.IFLE ||
                    !sameField(previousInstruction(branch), Opcodes.GETFIELD, delay)) continue;
            var exit = write.getNext();
            while (exit !== null && exit.getOpcode() < 0) exit = exit.getNext();
            if (exit === null || exit.getOpcode() !== Opcodes.GOTO) continue;
            match = write;
            count++;
        }
        if (count !== 1) throw new Error('ITE spawner expected exactly one native positive countdown write, found ' + count);
        return match;
    }

    function installExhaustion(node, official) {
        var name = official ? 'isNearPlayer' : 'm_151343_';
        var target = null;
        var count = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var candidate = node.methods.get(i);
            if (String(candidate.name) !== name || String(candidate.desc) !== '(' + world + pos + ')Z') continue;
            target = candidate;
            count++;
        }
        if (count !== 1) throw new Error('R195 spawner activation method count ' + count);

        var continuing = new LabelNode();
        var check = new InsnList();
        check.add(new VarInsnNode(Opcodes.ALOAD, 0));
        check.add(new VarInsnNode(Opcodes.ALOAD, 1));
        check.add(new VarInsnNode(Opcodes.ALOAD, 2));
        check.add(new MethodInsnNode(Opcodes.INVOKESTATIC, lifetime, 'exhausted',
            '(L' + owner + ';' + world + pos + ')Z', false));
        check.add(new JumpInsnNode(Opcodes.IFEQ, continuing));
        check.add(new InsnNode(Opcodes.ICONST_0));
        check.add(new InsnNode(Opcodes.IRETURN));
        check.add(continuing);
        check.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        target.instructions.insert(check);
        target.maxStack = Math.max(target.maxStack, 3);
    }

    function installAdmission(method, official) {
        var name = official ? 'tryAddFreshEntityWithPassengers' : 'm_8860_';
        var target = null;
        var count = 0;
        for (var iterator = method.instructions.iterator(); iterator.hasNext();) {
            var candidate = iterator.next();
            if (!(candidate instanceof MethodInsnNode) || candidate.getOpcode() !== Opcodes.INVOKEVIRTUAL ||
                    String(candidate.owner) !== server || String(candidate.name) !== name ||
                    String(candidate.desc) !== '(' + entity + ')Z') continue;
            target = candidate;
            count++;
        }
        if (count !== 1) throw new Error('R195 spawner native root admission count ' + count);
        var context = new InsnList();
        context.add(new VarInsnNode(Opcodes.ALOAD, 0));
        context.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.insertBefore(target, context);
        method.instructions.set(target, new MethodInsnNode(Opcodes.INVOKESTATIC, lifetime, 'admit',
            '(' + level + entity + 'L' + owner + ';' + pos + ')Z', false));
        method.maxStack = Math.max(method.maxStack, 4);
    }

    return {
        mite_ite_spawner_pace: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.BaseSpawner'},
            transformer: function(node) {
                var method = null;
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var candidate = node.methods.get(i);
                    var name = String(candidate.name);
                    if ((name !== 'serverTick' && name !== 'm_151311_') ||
                            String(candidate.desc) !== '(' + level + pos + ')V') continue;
                    method = candidate;
                    count++;
                }
                if (count !== 1) throw new Error('ITE spawner expected exactly one native server tick, found ' + count);

                var official = String(method.name) === 'serverTick';
                var delay = official ? 'spawnDelay' : 'f_45442_';
                var range = official ? 'requiredPlayerRange' : 'f_45452_';
                var fields = 0;
                for (var j = 0; j < node.fields.size(); j++) {
                    var field = node.fields.get(j);
                    if (String(field.name) === range && String(field.desc) === 'I') fields++;
                }
                if (fields !== 1) throw new Error('ITE spawner expected the native activation range, found ' + fields);
                var write = countdownWrite(method, delay);
                var call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                call.add(new VarInsnNode(Opcodes.ALOAD, 2));
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new FieldInsnNode(Opcodes.GETFIELD, owner, range, 'I'));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'dev/koikaster/mite/IteSpawnerPace',
                    'countdown', '(IL' + owner + ';' + level + pos + 'I)I', false));
                method.instructions.insertBefore(write, call);
                method.maxStack = Math.max(6, method.maxStack);
                installExhaustion(node, official);
                installAdmission(method, official);
                return node;
            }
        }
    };
}


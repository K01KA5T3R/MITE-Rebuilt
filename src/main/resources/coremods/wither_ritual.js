function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var level = 'Lnet/minecraft/world/level/Level;';
    var pos = 'Lnet/minecraft/core/BlockPos;';
    var match = 'Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;';
    var skull = 'Lnet/minecraft/world/level/block/entity/SkullBlockEntity;';
    var entity = 'Lnet/minecraft/world/entity/Entity;';
    var owner = 'dev/koikaster/mite/WitherRitual';
    var pumpkin = 'net/minecraft/world/level/block/CarvedPumpkinBlock';
    var pattern = 'net/minecraft/world/level/block/state/pattern/BlockPattern';

    function nextOpcode(instruction) {
        do { instruction = instruction.getNext(); } while (instruction !== null && instruction.getOpcode() < 0);
        return instruction;
    }

    function previousOpcode(instruction) {
        do { instruction = instruction.getPrevious(); } while (instruction !== null && instruction.getOpcode() < 0);
        return instruction;
    }

    function calls(instruction, opcode, callOwner, name, srg, desc) {
        return instruction instanceof Call && instruction.getOpcode() === opcode
            && String(instruction.owner) === callOwner && String(instruction.desc) === desc
            && (String(instruction.name) === name || String(instruction.name) === srg);
    }

    function isLoad(instruction, slot) {
        return instruction !== null && instruction.getOpcode() === O.ALOAD && instruction.var === slot;
    }

    function hook(method) {
        var instructions = method.instructions;
        var nodes = instructions.toArray();
        var find = null;
        var clear = null;
        var charge = null;
        var join = null;
        var update = null;
        for (var i = 0; i < nodes.length; i++) {
            var call = nodes[i];
            if (call instanceof Call && String(call.owner) === pattern
                    && String(call.desc) === '(Lnet/minecraft/world/level/LevelReader;' + pos + ')' + match) {
                if (find !== null) throw new Error('Multiple Wither ritual pattern queries');
                find = call;
            } else if (calls(call, O.INVOKESTATIC, pumpkin, 'clearPatternBlocks', 'm_245585_', '(' + level + match + ')V')) {
                if (clear !== null) throw new Error('Multiple Wither ritual pattern removals');
                clear = call;
            } else if (calls(call, O.INVOKEVIRTUAL, 'net/minecraft/world/entity/boss/wither/WitherBoss',
                    'makeInvulnerable', 'm_31506_', '()V')) {
                if (charge !== null) throw new Error('Multiple Wither ritual charge initializers');
                charge = call;
            } else if (calls(call, O.INVOKEVIRTUAL, 'net/minecraft/world/level/Level',
                    'addFreshEntity', 'm_7967_', '(' + entity + ')Z')) {
                if (join !== null) throw new Error('Multiple Wither ritual entity registrations');
                join = call;
            } else if (calls(call, O.INVOKESTATIC, pumpkin, 'updatePatternBlocks', 'm_246758_', '(' + level + match + ')V')) {
                if (update !== null) throw new Error('Multiple Wither ritual pattern notifications');
                update = call;
            }
        }
        if (find === null || clear === null || charge === null || join === null || update === null) {
            throw new Error('Missing Wither ritual calls');
        }

        var store = nextOpcode(find);
        var matchSlot = store.var;
        var load = nextOpcode(store);
        var branch = nextOpcode(load);
        if (store.getOpcode() !== O.ASTORE || !isLoad(load, matchSlot) || branch.getOpcode() !== O.IFNONNULL) {
            throw new Error('Wither ritual pattern branch');
        }

        var clearMatch = previousOpcode(clear);
        var clearLevel = previousOpcode(clearMatch);
        var missingEntity = previousOpcode(clearLevel);
        var joinEntity = previousOpcode(join);
        var entitySlot = joinEntity.var;
        var joinLevel = previousOpcode(joinEntity);
        var result = nextOpcode(join);
        var updateMatch = previousOpcode(update);
        var updateLevel = previousOpcode(updateMatch);
        var criteria = nextOpcode(charge);
        if (!isLoad(clearMatch, matchSlot) || !isLoad(clearLevel, 0)
                || missingEntity.getOpcode() !== O.IFNULL || nextOpcode(missingEntity.label).getOpcode() !== O.RETURN
                || !isLoad(previousOpcode(missingEntity), entitySlot) || !isLoad(previousOpcode(charge), entitySlot)
                || !isLoad(joinEntity, entitySlot) || !isLoad(joinLevel, 0) || result.getOpcode() !== O.POP
                || nextOpcode(result) !== updateLevel || !isLoad(updateLevel, 0) || !isLoad(updateMatch, matchSlot)
                || !isLoad(criteria, 0) || instructions.indexOf(clear) >= instructions.indexOf(charge)
                || instructions.indexOf(charge) >= instructions.indexOf(join)) {
            throw new Error('Wither ritual commit boundaries');
        }

        var roomCheck = new List();
        roomCheck.add(new Var(O.ALOAD, 0));
        roomCheck.add(new Call(O.INVOKESTATIC, owner, 'hasRoom', '(' + match + level + ')Z', false));
        instructions.insertBefore(branch, roomCheck);
        instructions.set(branch, new Jump(O.IFNE, branch.label));

        var commit = new List();
        commit.add(new Var(O.ALOAD, 0));
        commit.add(new Var(O.ALOAD, entitySlot));
        commit.add(new Var(O.ALOAD, matchSlot));
        commit.add(new Call(O.INVOKESTATIC, owner, 'commitSummon', '(' + level + entity + match + ')Z', false));
        commit.add(new Jump(O.IFEQ, missingEntity.label));
        instructions.insertBefore(criteria, commit);

        instructions.remove(clearLevel);
        instructions.remove(clearMatch);
        instructions.remove(clear);
        instructions.remove(joinLevel);
        instructions.remove(joinEntity);
        instructions.remove(join);
        instructions.remove(result);
    }

    return { wither_ritual: {
        target: { type: 'CLASS', name: 'net.minecraft.world.level.block.WitherSkullBlock' },
        transformer: function(node) {
            var found = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                var name = String(method.name);
                if ((name === 'checkSpawn' || name === 'm_58255_') && String(method.desc) === '(' + level + pos + skull + ')V') {
                    hook(method);
                    found++;
                }
            }
            if (found !== 1) throw new Error('Wither ritual method count ' + found);
            return node;
        }
    }};
}

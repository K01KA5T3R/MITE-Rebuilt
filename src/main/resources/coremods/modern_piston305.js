function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var helper = 'dev/koikaster/mite/ModernPistonPolicy305';
    var state = 'net/minecraft/world/level/block/state/BlockState';
    var piston = 'net/minecraft/world/level/block/piston/PistonMovingBlockEntity';
    var blockEntity = 'net/minecraft/world/level/block/entity/BlockEntity';
    var slime = 'net/minecraft/world/level/block/SlimeBlock';
    var world = 'Lnet/minecraft/world/level/Level;';
    var reader = 'Lnet/minecraft/world/level/LevelReader;';
    var blockState = 'L' + state + ';';
    var position = 'Lnet/minecraft/core/BlockPos;';
    var entity = 'Lnet/minecraft/world/entity/Entity;';

    function method(node, named, srg, desc) {
        var found = null;
        for (var i = 0; i < node.methods.size(); i++) {
            var m = node.methods.get(i);
            if ((String(m.name) === named || String(m.name) === srg) && String(m.desc) === desc) {
                if (found !== null) throw new Error('Ambiguous piston method ' + named);
                found = m;
            }
        }
        if (found === null) throw new Error('Missing piston method ' + named + desc);
        return found;
    }

    function uniqueField(node, desc) {
        var found = null;
        for (var i = 0; i < node.fields.size(); i++) {
            var f = node.fields.get(i);
            if (String(f.desc) !== desc || (f.access & O.ACC_STATIC) !== 0) continue;
            if (found !== null) throw new Error('Ambiguous state field ' + desc);
            found = f;
        }
        if (found === null) throw new Error('Missing state field ' + desc);
        return found;
    }

    function loadField(code, node, field) {
        code.add(new Var(O.ALOAD, 0));
        code.add(new Field(O.GETFIELD, String(node.name), String(field.name), String(field.desc)));
    }

    function helperCall(name, desc) {
        return new Call(O.INVOKESTATIC, helper, name, desc, false);
    }

    function resumeOriginal(code, label) {
        code.add(label);
        code.add(new Frame(O.F_SAME, 0, null, 0, null));
    }

    function solve(node, target, level, expectedSticky, expectedPairs) {
        var sticky = 0;
        var pairs = 0;
        var original = target.instructions.toArray();
        for (var i = 0; i < original.length; i++) {
            var op = original[i];
            if (op.getOpcode() !== O.INVOKEVIRTUAL || String(op.owner) !== state) continue;
            var after;
            if (String(op.name) === 'isStickyBlock' && String(op.desc) === '()Z') {
                after = helperCall('isSticky', '(' + blockState + world + ')Z');
                sticky++;
            } else if (String(op.name) === 'canStickTo' && String(op.desc) === '(' + blockState + ')Z') {
                after = helperCall('canStick', '(' + blockState + blockState + world + ')Z');
                pairs++;
            } else continue;
            var load = new InsnList();
            loadField(load, node, level);
            target.instructions.insertBefore(op, load);
            target.instructions.set(op, after);
        }
        if (sticky !== expectedSticky || pairs !== expectedPairs) {
            throw new Error('Unexpected Forge adhesion calls in ' + target.name + ': ' + sticky + '/' + pairs);
        }
        target.maxStack += 1;
    }

    function movingPiston(node) {
        var move = method(node, 'moveCollidedEntities', 'm_155910_', '(' + world + position + 'FL' + piston + ';)V');
        if ((move.access & O.ACC_STATIC) === 0) throw new Error('Moving piston world local is not static');
        var hits = 0;
        var calls = move.instructions.toArray();
        for (var i = 0; i < calls.length; i++) {
            var op = calls[i];
            if (op.getOpcode() !== O.INVOKEVIRTUAL || String(op.owner) !== state || String(op.name) !== 'isSlimeBlock'
                    || String(op.desc) !== '()Z') continue;
            move.instructions.insertBefore(op, new Var(O.ALOAD, 0));
            move.instructions.set(op, helperCall('pistonBounce', '(' + blockState + world + ')Z'));
            hits++;
        }
        if (hits !== 1) throw new Error('Expected one piston launch check');
        move.maxStack += 1;

        var carry = method(node, 'isStickyForEntities', 'm_60404_', '()Z');
        var moved = uniqueField(node, blockState);
        var getLevel = String(carry.name) === 'isStickyForEntities' ? 'getLevel' : 'm_58904_';
        var code = new InsnList();
        var original = new Label();
        loadField(code, node, moved);
        code.add(new Var(O.ALOAD, 0));
        code.add(new Call(O.INVOKEVIRTUAL, blockEntity, getLevel, '()' + world, false));
        code.add(helperCall('restricted', '(' + blockState + world + ')Z'));
        code.add(new Jump(O.IFEQ, original));
        code.add(new Insn(O.ICONST_0));
        code.add(new Insn(O.IRETURN));
        resumeOriginal(code, original);
        carry.instructions.insert(code);
        carry.maxStack = Math.max(carry.maxStack, 2);
        return node;
    }

    function slimeSurface(node) {
        var step = method(node, 'stepOn', 'm_141947_', '(' + world + position + blockState + entity + ')V');
        var code = new InsnList();
        var original = new Label();
        code.add(new Var(O.ALOAD, 3));
        code.add(new Var(O.ALOAD, 1));
        code.add(helperCall('restricted', '(' + blockState + world + ')Z'));
        code.add(new Jump(O.IFEQ, original));
        for (var i = 0; i < 5; i++) code.add(new Var(O.ALOAD, i));
        code.add(new Call(O.INVOKESPECIAL, String(node.superName), String(step.name), String(step.desc), false));
        code.add(new Insn(O.RETURN));
        resumeOriginal(code, original);
        step.instructions.insert(code);
        step.maxStack = Math.max(step.maxStack, 5);

        var desc = '(' + blockState + reader + position + entity + ')F';
        for (var m = 0; m < node.methods.size(); m++) {
            var previous = node.methods.get(m);
            if (String(previous.name) === 'getFriction' && String(previous.desc) === desc) {
                throw new Error('Slime already overrides the Forge friction contract');
            }
        }
        var friction = new Method(O.ACC_PUBLIC, 'getFriction', desc, null, null);
        friction.instructions.add(new Var(O.ALOAD, 0));
        friction.instructions.add(new Var(O.ALOAD, 2));
        friction.instructions.add(helperCall('slimeFriction', '(L' + slime + ';' + reader + ')F'));
        friction.instructions.add(new Insn(O.FRETURN));
        friction.maxLocals = 5;
        friction.maxStack = 2;
        node.methods.add(friction);
        return node;
    }

    return {
        modern_adhesion305: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.block.piston.PistonStructureResolver'},
            transformer: function(node) {
                var level = uniqueField(node, world);
                solve(node, method(node, 'resolve', 'm_60422_', '()Z'), level, 1, 0);
                solve(node, method(node, 'addBlockLine', 'm_60433_', '(' + position + 'Lnet/minecraft/core/Direction;)Z'), level, 2, 2);
                solve(node, method(node, 'addBranchingBlocks', 'm_60431_', '(' + position + ')Z'), level, 0, 2);
                return node;
            }
        },
        modern_moving_piston305: {target: {type: 'CLASS', name: piston.replace(/\//g, '.')}, transformer: movingPiston},
        modern_slime_surface305: {target: {type: 'CLASS', name: slime.replace(/\//g, '.')}, transformer: slimeSurface}
    };
}

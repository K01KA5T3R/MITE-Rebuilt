function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var I = Java.type('org.objectweb.asm.tree.InsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var B = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var helper = 'dev/koikaster/mite/ClientSourceRain2146';
    var state = helper + '$State';
    var stateDesc = 'L' + state + ';';
    var box = 'Lnet/minecraft/world/phys/AABB;';
    var level = 'Lnet/minecraft/client/multiplayer/ClientLevel;';
    var random = 'Lnet/minecraft/util/RandomSource;';
    var object = 'Ljava/lang/Object;';
    var fieldName = 'mite$rainState2146';
    var methodName = 'mite$rainTick2146';

    function aload(code) { code.add(new V(O.ALOAD, 0)); }
    function call(code, op, owner, name, desc) { code.add(new M(op, owner, name, desc, false)); }
    function field(code, op, owner, name, desc) { code.add(new F(op, owner, name, desc)); }
    function jump(code, op, target) { code.add(new J(op, target)); }
    function same(code, label) {
        code.add(label);
        code.add(new Frame(O.F_SAME, 0, null, 0, null));
    }
    function real(method) {
        var result = [], nodes = method.instructions.toArray();
        for (var i = 0; i < nodes.length; i++) if (nodes[i].getOpcode() >= 0) result.push(nodes[i]);
        return result;
    }

    return {
        'source_rain_water_drop2146': {
            target: {type: 'CLASS', name: 'net.minecraft.client.particle.WaterDropParticle'},
            transformer: function(node) {
                var owner = String(node.name), tick = null, ctor = null;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name), desc = String(method.desc);
                    if ((name === 'tick' || name === 'm_5989_') && desc === '()V') tick = method;
                    if (name === '<init>' && desc === '(' + level + 'DDD)V') ctor = method;
                }
                if (tick === null || ctor === null) throw new Error('Rain constructor or tick missing');
                var srg = String(tick.name) === 'm_5989_';
                function mapped(a, b) { return srg ? b : a; }
                var x = mapped('x','f_107212_'), y = mapped('y','f_107213_'), z = mapped('z','f_107214_');
                var xo = mapped('xo','f_107209_'), yo = mapped('yo','f_107210_'), zo = mapped('zo','f_107211_');
                var xd = mapped('xd','f_107215_'), yd = mapped('yd','f_107216_'), zd = mapped('zd','f_107217_');
                var lifetime = mapped('lifetime','f_107225_'), gravity = mapped('gravity','f_107226_');
                var onGround = mapped('onGround','f_107218_'), physics = mapped('hasPhysics','f_107219_');
                var world = mapped('level','f_107208_'), rng = mapped('random','f_107223_');
                var getBox = mapped('getBoundingBox','m_107277_'), setBox = mapped('setBoundingBox','m_107259_');
                var remove = mapped('remove','m_107274_');
                function get(code, name, desc) { aload(code); field(code, O.GETFIELD, owner, name, desc); }
                function getState(code) { get(code, fieldName, stateDesc); }
                function previous(code, to, from) {
                    aload(code);
                    get(code, from, 'D');
                    field(code, O.PUTFIELD, owner, to, 'D');
                }
                function setFromState(code, name, getter, desc) {
                    aload(code);
                    getState(code);
                    call(code, O.INVOKEVIRTUAL, state, getter, '()' + desc);
                    field(code, O.PUTFIELD, owner, name, desc);
                }
                function syncBox(code) {
                    aload(code);
                    getState(code);
                    call(code, O.INVOKEVIRTUAL, state, 'box', '()' + box);
                    call(code, O.INVOKEVIRTUAL, owner, setBox, '(' + box + ')V');
                }

                var all = real(ctor), samples = [];
                for (var i = 0; i < all.length; i++) {
                    var insn = all[i];
                    if (insn.getOpcode() === O.INVOKESTATIC && String(insn.owner) === 'java/lang/Math'
                        && String(insn.name) === 'random' && String(insn.desc) === '()D') samples.push(i);
                }
                if (samples.length !== 2) throw new Error('Rain random sample sites changed');
                var first = samples[0], putY = all[first+5];
                if (all[first+1].getOpcode() !== O.LDC || Number(all[first+1].cst) !== 0.20000000298023224
                    || all[first+2].getOpcode() !== O.DMUL || all[first+3].getOpcode() !== O.LDC
                    || Number(all[first+3].cst) !== 0.10000000149011612 || all[first+4].getOpcode() !== O.DADD
                    || putY.getOpcode() !== O.PUTFIELD || String(putY.name) !== yd) {
                    throw new Error('Rain native vertical velocity expression changed');
                }
                var expression = new L();
                aload(expression);
                call(expression, O.INVOKESTATIC, helper, 'initialVertical', '(D' + object + ')D');
                ctor.instructions.insert(all[first], expression);
                for (var i = first+1; i < first+5; i++) ctor.instructions.remove(all[i]);
                if (all[all.length-1].getOpcode() !== O.RETURN || all[all.length-2].getOpcode() !== O.PUTFIELD
                    || String(all[all.length-2].name) !== lifetime) throw new Error('Rain constructor tail changed');
                node.fields.add(new Field(O.ACC_PRIVATE | O.ACC_FINAL | O.ACC_SYNTHETIC, fieldName, stateDesc, null, null));

                var start = new L(), noState = new B();
                aload(start);
                aload(start);
                get(start, world, level);
                get(start, x, 'D');
                get(start, y, 'D');
                get(start, z, 'D');
                get(start, rng, random);
                call(start, O.INVOKESTATIC, helper, 'create', '(' + object + level + 'DDD' + random + ')' + stateDesc);
                field(start, O.PUTFIELD, owner, fieldName, stateDesc);
                getState(start);
                jump(start, O.IFNULL, noState);
                setFromState(start, xd, 'xd', 'D');
                setFromState(start, zd, 'zd', 'D');
                syncBox(start);
                same(start, noState);
                ctor.instructions.insertBefore(all[all.length-1], start);
                ctor.maxStack = Math.max(ctor.maxStack, 12);

                var legacy = new Method(O.ACC_PRIVATE | O.ACC_SYNTHETIC, methodName, '()V', null, null);
                var code = legacy.instructions, alive = new B();
                previous(code, xo, x);
                previous(code, yo, y);
                previous(code, zo, z);
                getState(code);
                get(code, x, 'D');
                get(code, y, 'D');
                get(code, z, 'D');
                get(code, xd, 'D');
                get(code, yd, 'D');
                get(code, zd, 'D');
                get(code, gravity, 'F');
                aload(code);
                call(code, O.INVOKEVIRTUAL, owner, getBox, '()' + box);
                get(code, physics, 'Z');
                get(code, lifetime, 'I');
                call(code, O.INVOKESTATIC, helper, 'tick', '(' + stateDesc + 'DDDDDDF' + box + 'ZI)Z');
                setFromState(code, x, 'x', 'D');
                setFromState(code, y, 'y', 'D');
                setFromState(code, z, 'z', 'D');
                setFromState(code, xd, 'xd', 'D');
                setFromState(code, yd, 'yd', 'D');
                setFromState(code, zd, 'zd', 'D');
                setFromState(code, onGround, 'onGround', 'Z');
                syncBox(code);
                aload(code);
                code.add(new I(O.DUP));
                field(code, O.GETFIELD, owner, lifetime, 'I');
                code.add(new I(O.ICONST_1));
                code.add(new I(O.ISUB));
                field(code, O.PUTFIELD, owner, lifetime, 'I');
                jump(code, O.IFEQ, alive);
                aload(code);
                call(code, O.INVOKEVIRTUAL, owner, remove, '()V');
                same(code, alive);
                code.add(new I(O.RETURN));
                legacy.maxStack = 19;
                legacy.maxLocals = 1;
                node.methods.add(legacy);

                var guard = new L(), nativePath = new B();
                getState(guard);
                jump(guard, O.IFNULL, nativePath);
                aload(guard);
                call(guard, O.INVOKESPECIAL, owner, methodName, '()V');
                guard.add(new I(O.RETURN));
                same(guard, nativePath);
                tick.instructions.insert(guard);
                return node;
            }
        }
    };
}


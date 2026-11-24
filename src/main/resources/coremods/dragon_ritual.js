function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var level = 'net/minecraft/server/level/ServerLevel';
    var fight = 'net/minecraft/world/level/dimension/end/EndDragonFight';
    var owner = 'dev/koikaster/mite/DragonRitual';
    var uuid = 'Ljava/util/UUID;';

    function findField(node, official, srg, desc) {
        for (var i = 0; i < node.fields.size(); i++) {
            var field = node.fields.get(i);
            var name = String(field.name);
            if ((name === official || name === srg) && String(field.desc) === desc) return name;
        }
        throw new Error('Missing dragon ritual field ' + official);
    }

    function guard(method, field, tick, dragonReturn) {
        var code = new List();
        if (tick) code.add(new Var(O.ALOAD, 0));
        code.add(new Var(O.ALOAD, 0));
        code.add(new Field(O.GETFIELD, fight, field, 'L' + level + ';'));
        code.add(new Call(O.INVOKESTATIC, owner, tick ? 'beforeFightTick' : 'managed',
            (tick ? '(L' + fight + ';' : '(') + 'L' + level + ';)Z', false));
        var vanilla = new Label();
        code.add(new Jump(O.IFEQ, vanilla));
        if (dragonReturn) code.add(new Insn(O.ACONST_NULL));
        code.add(new Insn(dragonReturn ? O.ARETURN : O.RETURN));
        code.add(vanilla);
        code.add(new Frame(O.F_SAME, 0, null, 0, null));
        method.instructions.insert(code);
        method.maxStack = Math.max(method.maxStack, 2);
    }

    function store(code, name, desc, value) {
        code.add(new Var(O.ALOAD, 0));
        code.add(value);
        code.add(new Field(O.PUTFIELD, fight, name, desc));
    }

    return { dragon_ritual_fight: {
        target: {type: 'CLASS', name: fight.replace(/\//g, '.')},
        transformer: function(node) {
            var field = findField(node, 'level', 'f_64061_', 'L' + level + ';');
            var ticks = 0;
            var respawns = 0;
            var creates = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                var name = String(method.name);
                var desc = String(method.desc);
                if ((name === 'tick' || name === 'm_64095_') && desc === '()V') {
                    guard(method, field, true, false);
                    ticks++;
                } else if ((name === 'tryRespawn' || name === 'm_64100_') && desc === '()V') {
                    guard(method, field, false, false);
                    respawns++;
                } else if ((name === 'createNewDragon' || name === 'm_64110_') && desc === '()Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;') {
                    guard(method, field, false, true);
                    creates++;
                }
            }
            if (ticks !== 1 || respawns !== 1 || creates !== 1) throw new Error('Dragon ritual hook count');
            node.interfaces.add('dev/koikaster/mite/DragonRitualAccess');
            var bridge = new Method(O.ACC_PUBLIC, 'mite$trackRitualDragon', '(' + uuid + ')V', null, null);
            var code = bridge.instructions;
            store(code, findField(node, 'dragonUUID', 'f_64070_', uuid), uuid, new Var(O.ALOAD, 1));
            code.add(new Var(O.ALOAD, 0));
            code.add(new Var(O.ALOAD, 1));
            code.add(new Call(O.INVOKESTATIC, 'java/util/Objects', 'isNull', '(Ljava/lang/Object;)Z', false));
            code.add(new Field(O.PUTFIELD, fight, findField(node, 'dragonKilled', 'f_64068_', 'Z'), 'Z'));
            store(code, findField(node, 'needsStateScanning', 'f_64071_', 'Z'), 'Z', new Insn(O.ICONST_0));
            store(code, findField(node, 'ticksSinceDragonSeen', 'f_64064_', 'I'), 'I', new Insn(O.ICONST_0));
            store(code, findField(node, 'respawnTime', 'f_64074_', 'I'), 'I', new Insn(O.ICONST_0));
            var animation = 'Lnet/minecraft/world/level/dimension/end/DragonRespawnAnimation;';
            store(code, findField(node, 'respawnStage', 'f_64073_', animation), animation, new Insn(O.ACONST_NULL));
            store(code, findField(node, 'respawnCrystals', 'f_64075_', 'Ljava/util/List;'), 'Ljava/util/List;', new Insn(O.ACONST_NULL));
            var bar = 'Lnet/minecraft/server/level/ServerBossEvent;';
            code.add(new Var(O.ALOAD, 0));
            code.add(new Field(O.GETFIELD, fight, findField(node, 'dragonEvent', 'f_64060_', bar), bar));
            code.add(new Var(O.ALOAD, 1));
            code.add(new Call(O.INVOKESTATIC, owner, 'setBarVisible', '(' + bar + uuid + ')V', false));
            code.add(new Insn(O.RETURN));
            bridge.maxStack = bridge.maxLocals = 2;
            node.methods.add(bridge);
            return node;
        }
    }};
}

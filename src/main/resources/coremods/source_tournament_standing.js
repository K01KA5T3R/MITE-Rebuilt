function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Ldc = Java.type('org.objectweb.asm.tree.LdcInsnNode');
    var Type = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var mod = 'dev/koikaster/mite/';
    var block = 'net/minecraft/world/level/block/Block';
    var ore = 'net/minecraft/world/level/block/DropExperienceBlock';
    var state = 'Lnet/minecraft/world/level/block/state/BlockState;';
    var level = 'Lnet/minecraft/world/level/Level;';
    var server = 'Lnet/minecraft/server/level/ServerLevel;';
    var pos = 'Lnet/minecraft/core/BlockPos;';
    var tile = 'Lnet/minecraft/world/level/block/entity/BlockEntity;';
    var entity = 'Lnet/minecraft/world/entity/Entity;';
    var item = 'Lnet/minecraft/world/item/ItemStack;';
    var hook = 'Lnet/minecraft/world/entity/projectile/FishingHook;';
    var list = 'Ljava/util/List;';

    function find(node, mapped, srg, desc) {
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) === desc && (String(method.name) === mapped
                || String(method.name) === srg || (srg !== '<clinit>' && String(method.name) === String(ASMAPI.mapMethod(srg))))) return method;
        }
        throw new Error('Missing native tournament method ' + mapped + ' in ' + node.name);
    }
    function nameIs(call, mapped, srg) {
        return String(call.name) === mapped || String(call.name) === srg
            || String(call.name) === String(ASMAPI.mapMethod(srg));
    }

    return {
        'mite_score_fish': {target: {type:'CLASS', name:'net.minecraft.world.entity.projectile.FishingHook'},
            transformer: function(node) {
                var method = find(node, 'retrieve', 'm_37156_', '(' + item + ')I');
                var count = 0;
                var code = method.instructions.toArray();
                for (var i = 0; i < code.length; i++) {
                    var call = code[i];
                    if (call.getOpcode() !== O.INVOKEVIRTUAL || String(call.owner) !== 'net/minecraft/world/level/Level'
                        || String(call.desc) !== '(' + entity + ')Z' || !nameIs(call, 'addFreshEntity', 'm_7967_')) continue;
                    method.instructions.insertBefore(call, new Var(O.ALOAD, 0));
                    method.instructions.set(call, new Call(O.INVOKESTATIC, mod + 'SourceTournamentStanding',
                        'spawnFishingResult', '(' + level + entity + hook + ')Z', false));
                    count++;
                }
                if (count !== 2) throw new Error('Expected fishing loot and XP spawn calls: ' + count);
                method.maxStack = Math.max(method.maxStack, 3);
                return node;
            }},
        'mite_score_harvest': {target: {type:'CLASS', name:'net.minecraft.world.level.block.Block'},
            transformer: function(node) {
                var method = find(node, 'dropResources', 'm_49881_', '(' + state + level + pos + tile + entity + item + 'Z)V');
                var count = 0;
                for (var call = method.instructions.getFirst(); call !== null; call = call.getNext()) {
                    if (call.getOpcode() !== O.INVOKESTATIC || String(call.owner) !== block
                        || String(call.desc) !== '(' + state + server + pos + tile + entity + item + ')' + list
                        || !nameIs(call, 'getDrops', 'm_49874_')) continue;
                    var added = new List();
                    added.add(new Insn(O.DUP));
                    var locals = [0, 1, 2, 4, 5];
                    for (var i = 0; i < locals.length; i++) added.add(new Var(O.ALOAD, locals[i]));
                    added.add(new Call(O.INVOKESTATIC, mod + 'SourceHarvestAchievement579', 'scoreHarvest',
                        '(' + list + state + level + pos + entity + item + ')V', false));
                    method.instructions.insert(call, added);
                    count++;
                }
                if (count !== 1) throw new Error('Expected one final native harvest loot list: ' + count);
                method.maxStack = Math.max(method.maxStack, 6);
                return node;
            }},
        'mite_score_placed_ore': {target: {type:'CLASS', name:'net.minecraft.world.level.block.Blocks'},
            transformer: function(node) {
                var method = find(node, '<clinit>', '<clinit>', '()V');
                var names = {copper_ore: true, iron_ore: true, gold_ore: true};
                var pending = null;
                var created = null;
                var count = 0;
                var code = method.instructions.toArray();
                for (var i = 0; i < code.length; i++) {
                    var inst = code[i];
                    if (inst instanceof Ldc && names[String(inst.cst)] === true) {
                        if (pending !== null) throw new Error('Previous metal was not registered: ' + pending);
                        pending = String(inst.cst);
                    }
                    if (pending === null) continue;
                    if (inst instanceof Type && inst.getOpcode() === O.NEW && String(inst.desc) === ore) {
                        if (created !== null) throw new Error('Ambiguous metal constructor ' + pending);
                        created = inst;
                    }
                    if (inst instanceof Call && inst.getOpcode() === O.INVOKESPECIAL && String(inst.owner) === ore
                        && String(inst.name) === '<init>') {
                        if (created === null || String(inst.desc) !== '(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V')
                            throw new Error('Unexpected metal constructor ' + pending);
                        created.desc = mod + 'SourceTournamentOre';
                        inst.owner = mod + 'SourceTournamentOre';
                        pending = null;
                        created = null;
                        count++;
                    }
                }
                if (count !== 3 || pending !== null) throw new Error('Expected three vanilla metal ores: ' + count);
                return node;
            }}
    };
}

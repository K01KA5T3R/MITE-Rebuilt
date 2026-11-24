function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var helper = 'dev/koikaster/mite/WorldBerryPatch';
    var writable = 'net/minecraft/core/WritableRegistry';
    var patch = 'net/minecraft/world/level/levelgen/feature/configurations/RandomPatchConfiguration';
    var context = 'Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;';
    var resource = 'Lnet/minecraft/server/packs/resources/Resource;';
    var registerArgs = 'Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;';
    var registerReturn = ')Lnet/minecraft/core/Holder$Reference;';
    var registered = '(' + registerArgs + registerReturn;
    var registerWithSource = '(L' + writable + ';' + registerArgs + resource + registerReturn;
    var load = '(Lnet/minecraft/resources/RegistryOps$RegistryInfoLookup;Lnet/minecraft/server/packs/resources/ResourceManager;'
        + 'Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lcom/mojang/serialization/Decoder;Ljava/util/Map;)V';

    function method(node, named, mapped, desc) {
        for (var i = 0; i < node.methods.size(); i++) {
            var candidate = node.methods.get(i);
            if ((String(candidate.name) === named || String(candidate.name) === mapped) && String(candidate.desc) === desc) {
                return candidate;
            }
        }
        throw new Error('Missing berry patch target ' + named + ' in ' + node.name);
    }

    function redirect(body, opcode, owner, named, mapped, original, local, target, replacement) {
        var found = null;
        for (var instruction = body.instructions.getFirst(); instruction !== null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() === opcode && String(instruction.owner) === owner
                    && (String(instruction.name) === named || String(instruction.name) === mapped)
                    && String(instruction.desc) === original) {
                if (found !== null) throw new Error('Multiple berry patch call sites for ' + target);
                found = instruction;
            }
        }
        if (found === null) throw new Error('Missing berry patch call site for ' + target);
        body.instructions.insertBefore(found, new VarInsnNode(Opcodes.ALOAD, local));
        found.setOpcode(Opcodes.INVOKESTATIC);
        found.owner = helper;
        found.name = target;
        found.desc = replacement;
        found.itf = false;
        body.maxStack++;
    }

    return {
        world_berry_patch_source_047: {
            target: {type: 'CLASS', name: 'net.minecraft.resources.RegistryDataLoader'},
            transformer: function (node) {
                var body = method(node, 'loadRegistryContents', 'm_255048_', load);
                redirect(body, Opcodes.INVOKEINTERFACE, writable, 'register', 'm_255290_', registered, 13, 'register', registerWithSource);
                return node;
            }
        },
        world_berry_patch_tries_047: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.feature.RandomPatchFeature'},
            transformer: function (node) {
                var body = method(node, 'place', 'm_142674_', '(' + context + ')Z');
                redirect(body, Opcodes.INVOKEVIRTUAL, patch, 'tries', 'f_67907_', '()I', 1, 'tries', '(L' + patch + ';' + context + ')I');
                return node;
            }
        }
    };
}

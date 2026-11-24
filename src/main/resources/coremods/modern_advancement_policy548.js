/** Bind advancement definitions to their real world and data-load owner. */
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var owner = 'dev/koikaster/mite/ModernAdvancementPolicy548';
    var adapter = owner + '$LiveManager';
    var resources = 'Lnet/minecraft/server/packs/resources/ResourceManager;';
    var reloaded = 'net/minecraft/server/ReloadableServerResources';
    var nativeManager = 'Lnet/minecraft/server/ServerAdvancementManager;';
    var loadResult = 'Lnet/minecraft/server/WorldLoader$DataLoadOutput;';
    var future = 'Ljava/util/concurrent/CompletableFuture;';
    var executor = 'Ljava/util/concurrent/Executor;';
    var worldData = 'Lnet/minecraft/world/level/storage/WorldData;';
    var apply = '(Ljava/util/Map;' + resources + 'Lnet/minecraft/util/profiling/ProfilerFiller;)V';
    var args = resources + 'Lnet/minecraft/core/RegistryAccess$Frozen;Lnet/minecraft/world/flag/FeatureFlagSet;'
        + 'Lnet/minecraft/commands/Commands$CommandSelection;I' + executor + executor;
    var starter = '(Lnet/minecraft/server/WorldLoader$InitConfig;Lnet/minecraft/server/WorldLoader$WorldDataSupplier;'
        + 'Lnet/minecraft/server/WorldLoader$ResultFactory;' + executor + executor + ')' + future;

    function find(node, named, srg, descriptor) {
        var result = null;
        for (var i = 0; i < node.methods.size(); i++) {
            var candidate = node.methods.get(i), name = String(candidate.name);
            if (String(candidate.desc) !== descriptor || (name !== named && name !== srg)) continue;
            if (result !== null) throw new Error('Duplicate advancement resource owner ' + named);
            result = candidate;
        }
        if (result === null) throw new Error('Missing advancement resource owner ' + named);
        return result;
    }

    function replaceResources(method, world) {
        var count = 0;
        for (var cursor = method.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (!(cursor instanceof Call) || cursor.getOpcode() !== O.INVOKESTATIC || String(cursor.owner) !== reloaded
                    || String(cursor.desc) !== '(' + args + ')' + future) continue;
            if (++count !== 1 || (String(cursor.name) !== 'loadResources' && String(cursor.name) !== 'm_247740_')) {
                throw new Error('Unexpected duplicate or unmapped advancement resource load');
            }
            method.instructions.insertBefore(cursor, new Var(O.ALOAD, world));
            var wrapper = new Call(O.INVOKESTATIC, owner, 'loadForWorld', '(' + args + 'Ljava/lang/Object;)' + future, false);
            method.instructions.set(cursor, wrapper);
            cursor = wrapper;
        }
        if (count !== 1) throw new Error('Missing native advancement resource load');
        method.maxStack++;
    }

    function initialLoad(node) {
        var load = find(node, 'load', 'm_214362_', starter), output = -1;
        for (var cursor = load.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (!(cursor instanceof Call) || String(cursor.owner) !== 'net/minecraft/server/WorldLoader$WorldDataSupplier'
                    || String(cursor.desc) !== '(Lnet/minecraft/server/WorldLoader$DataLoadContext;)' + loadResult) continue;
            var store = cursor.getNext();
            while (store !== null && store.getOpcode() < 0) store = store.getNext();
            if (!(store instanceof Var) || store.getOpcode() !== O.ASTORE || output !== -1) {
                throw new Error('Unexpected saved world data owner');
            }
            output = store.var;
        }
        if (output < 0) throw new Error('Missing saved world data owner');
        replaceResources(load, output);
        return node;
    }

    function reload(node) {
        var method = find(node, 'lambda$reloadResources$25', 'lambda$reloadResources$25',
            '(Lnet/minecraft/core/RegistryAccess$Frozen;Lcom/google/common/collect/ImmutableList;)Ljava/util/concurrent/CompletionStage;');
        if ((method.access & O.ACC_STATIC) !== 0) throw new Error('Resource reload requires the actual server object');
        replaceResources(method, 0);
        return node;
    }

    function manager(node) {
        var body = find(node, 'apply', 'm_5787_', apply);
        if (node.interfaces.contains(adapter)) throw new Error('Unexpected duplicate advancement manager adapter');
        node.interfaces.add(adapter);
        var guard = new Insns();
        guard.add(new Var(O.ALOAD, 0));
        guard.add(new Var(O.ALOAD, 1));
        guard.add(new Var(O.ALOAD, 2));
        guard.add(new Call(O.INVOKESTATIC, owner, 'forManager',
            '(' + nativeManager + 'Ljava/util/Map;' + resources + ')Ljava/util/Map;', false));
        guard.add(new Var(O.ASTORE, 1));
        body.instructions.insert(guard);
        body.maxStack = Math.max(body.maxStack, 3);
        var bridge = new Method(O.ACC_PUBLIC | O.ACC_FINAL | O.ACC_SYNTHETIC, 'mite$applyWorldAdvancements', apply, null, null);
        for (var i = 0; i < 4; i++) bridge.instructions.add(new Var(O.ALOAD, i));
        bridge.instructions.add(new Call(O.INVOKEVIRTUAL, node.name, body.name, apply, false));
        bridge.instructions.add(new Insn(O.RETURN));
        bridge.maxLocals = 4;
        bridge.maxStack = 4;
        node.methods.add(bridge);
        return node;
    }

    function worldStem(node) {
        var constructor = find(node, '<init>', '<init>',
            '(Lnet/minecraft/server/packs/resources/CloseableResourceManager;L' + reloaded
            + ';Lnet/minecraft/core/LayeredRegistryAccess;' + worldData + ')V');
        var exits = 0;
        for (var cursor = constructor.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() !== O.RETURN) continue;
            if (++exits !== 1) throw new Error('Unexpected world stem exits');
            var finish = new Insns();
            finish.add(new Var(O.ALOAD, 2));
            finish.add(new Var(O.ALOAD, 4));
            finish.add(new Var(O.ALOAD, 1));
            finish.add(new Call(O.INVOKESTATIC, owner, 'finishPreview', '(L' + reloaded + ';' + worldData + resources + ')V', false));
            constructor.instructions.insertBefore(cursor, finish);
        }
        if (exits !== 1) throw new Error('Missing world stem exit');
        constructor.maxStack = Math.max(constructor.maxStack, 3);
        return node;
    }

    return {
        world_advancement_load_048: {target: {type: 'CLASS', name: 'net.minecraft.server.WorldLoader'}, transformer: initialLoad},
        world_advancement_reload_048: {target: {type: 'CLASS', name: 'net.minecraft.server.MinecraftServer'}, transformer: reload},
        world_advancement_manager_048: {target: {type: 'CLASS', name: 'net.minecraft.server.ServerAdvancementManager'}, transformer: manager},
        world_advancement_finalize_048: {target: {type: 'CLASS', name: 'net.minecraft.server.WorldStem'}, transformer: worldStem}
    };
}


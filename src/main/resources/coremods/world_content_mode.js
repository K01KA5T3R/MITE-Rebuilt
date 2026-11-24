function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Field = Java.type('org.objectweb.asm.tree.FieldNode');
    var FieldInsn = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var screen = 'net/minecraft/client/gui/screens/worldselection/CreateWorldScreen';
    var rows = 'net/minecraft/client/gui/layouts/GridLayout$RowHelper';

    function state(node) {
        var access = 'dev/koikaster/mite/WorldContentMode$Access';
        var field = 'mite$worldOptions';
        if (!node.interfaces.add(access)) throw new Error('World content state is already present');
        node.fields.add(new Field(O.ACC_PRIVATE, field, 'I', null, null));

        var read = new Method(O.ACC_PUBLIC, 'mite$worldOptions', '()I', null, null);
        read.instructions.add(new Var(O.ALOAD, 0));
        read.instructions.add(new FieldInsn(O.GETFIELD, node.name, field, 'I'));
        read.instructions.add(new Insn(O.IRETURN));
        read.maxStack = 1;
        read.maxLocals = 1;
        node.methods.add(read);

        var write = new Method(O.ACC_PUBLIC, 'mite$readWorldOptions', '(I)V', null, null);
        write.instructions.add(new Var(O.ALOAD, 0));
        write.instructions.add(new Var(O.ILOAD, 1));
        write.instructions.add(new FieldInsn(O.PUTFIELD, node.name, field, 'I'));
        write.instructions.add(new Insn(O.RETURN));
        write.maxStack = 2;
        write.maxLocals = 2;
        node.methods.add(write);
        return node;
    }

    function clientLevelHandoff(node) {
        var listener = 'Lnet/minecraft/client/multiplayer/ClientPacketListener;';
        var data = 'Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;';
        var descriptor = '(' + listener + data + 'Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/Holder;'
            + 'IILjava/util/function/Supplier;Lnet/minecraft/client/renderer/LevelRenderer;ZJ)V';
        var inserted = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== '<init>' || String(method.desc) !== descriptor) continue;
            // Apply the settings already received for this connection before Forge publishes the new level.
            var handoff = new Insns();
            handoff.add(new Var(O.ALOAD, 1));
            handoff.add(new Var(O.ALOAD, 2));
            handoff.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClientWorldContentMode',
                'inheritLevelData', '(' + listener + data + ')V', false));
            method.instructions.insert(handoff);
            method.maxStack = Math.max(method.maxStack, 2);
            inserted++;
        }
        if (inserted !== 1) throw new Error('Expected one client level constructor, found ' + inserted);
        return node;
    }

    function gameTab(node) {
        var injected = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== '<init>' || String(method.desc) !== '(L' + screen + ';)V') continue;
            var rowLocal = -1;
            var code = method.instructions.toArray();
            for (var j = 0; j < code.length; j++) {
                var call = code[j];
                if (!(call instanceof Call)) continue;
                if (rowLocal === -1 && String(call.owner) === 'net/minecraft/client/gui/layouts/GridLayout'
                    && String(call.desc) === '(I)L' + rows + ';') {
                    var store = call.getNext();
                    while (store !== null && store.getOpcode() < 0) store = store.getNext();
                    if (!(store instanceof Var) || store.getOpcode() !== O.ASTORE) {
                        throw new Error('Expected the game tab layout to store its first row helper');
                    }
                    rowLocal = store.var;
                }
                if (rowLocal !== -1 && String(call.owner) === 'net/minecraft/SharedConstants'
                    && String(call.desc) === '()Lnet/minecraft/WorldVersion;') {
                    var add = new Insns();
                    add.add(new Var(O.ALOAD, rowLocal));
                    add.add(new Var(O.ALOAD, 1));
                    add.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClientWorldContentMode',
                        'addButton', '(L' + rows + ';L' + screen + ';)V', false));
                    method.instructions.insertBefore(call, add);
                    injected++;
                }
            }
            method.maxStack = Math.max(method.maxStack, 2);
        }
        if (injected !== 1) throw new Error('Expected one world content control, found ' + injected);
        return node;
    }

    function chunkMap(node) {
        var injected = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== '<init>') continue;
            var code = method.instructions.toArray();
            for (var j = 0; j < code.length; j++) {
                var call = code[j];
                if (!(call instanceof Call) || String(call.owner) !== 'net/minecraft/world/level/chunk/ChunkGenerator'
                    || String(call.desc) !== '(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;') continue;
                var seed = call.getPrevious();
                while (seed !== null && seed.getOpcode() < 0) seed = seed.getPrevious();
                var randomState = seed === null ? null : seed.getPrevious();
                while (randomState !== null && randomState.getOpcode() < 0) randomState = randomState.getPrevious();
                var receiver = randomState === null ? null : randomState.getPrevious();
                while (receiver !== null && receiver.getOpcode() < 0) receiver = receiver.getPrevious();
                if (!(seed instanceof Var) || seed.getOpcode() !== O.LLOAD || !(randomState instanceof FieldInsn)
                    || randomState.getOpcode() !== O.GETFIELD || !(receiver instanceof Var)
                    || receiver.getOpcode() !== O.ALOAD || receiver.var !== 0
                    || !String(method.desc).startsWith('(Lnet/minecraft/server/level/ServerLevel;')) {
                    throw new Error('Unexpected chunk map constructor structure lookup');
                }
                var filter = new Insns();
                filter.add(new Var(O.ALOAD, 1));
                filter.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentMode', 'structures',
                    '(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/core/HolderLookup;', false));
                method.instructions.insertBefore(receiver, filter);
                var remember = new Insns();
                remember.add(new Var(O.ALOAD, 1));
                remember.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentMode', 'structureState',
                    '(Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;', false));
                method.instructions.insert(call, remember);
                injected++;
            }
        }
        if (injected !== 1) throw new Error('Expected one world-specific structure lookup, found ' + injected);
        return node;
    }

    function creativeSlots(node) {
        var injected = 0;
        var packet = 'net/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket';
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) !== '(L' + packet + ';)V') continue;
            var code = method.instructions.toArray();
            for (var j = 0; j < code.length; j++) {
                var call = code[j];
                if (!(call instanceof Call) || String(call.owner) !== 'net/minecraft/network/protocol/PacketUtils'
                    || String(call.desc) !== '(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V') continue;
                var filter = new Insns();
                var allowed = new Label();
                filter.add(new Var(O.ALOAD, 1));
                filter.add(new Var(O.ALOAD, 0));
                filter.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentEvents', 'creativeStackAllowed',
                    '(L' + packet + ';L' + node.name + ';)Z', false));
                filter.add(new Jump(O.IFNE, allowed));
                filter.add(new Insn(O.RETURN));
                filter.add(allowed);
                method.instructions.insert(call, filter);
                method.maxStack = Math.max(method.maxStack, 2);
                injected++;
            }
        }
        if (injected !== 1) throw new Error('Expected one creative slot stack input, found ' + injected);
        return node;
    }

    function dimensions(node) {
        var injected = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) !== '(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V') continue;
            var code = method.instructions.toArray();
            for (var j = 0; j < code.length; j++) {
                var call = code[j];
                if (!(call instanceof Call) || String(call.owner) !== 'net/minecraft/core/Registry'
                    || String(call.desc) !== '()Ljava/util/Set;') continue;
                var filter = new Insns();
                filter.add(new Var(O.ALOAD, 0));
                filter.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentMode', 'dimensions',
                    '(Ljava/util/Set;Lnet/minecraft/server/MinecraftServer;)Ljava/util/Set;', false));
                method.instructions.insert(call, filter);
                method.maxStack = Math.max(method.maxStack, 2);
                injected++;
            }
        }
        if (injected !== 1) throw new Error('Expected one dimension construction list, found ' + injected);
        return node;
    }

    function placedFeature(node) {
        var injected = 0;
        var context = 'Lnet/minecraft/world/level/levelgen/placement/PlacementContext;';
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) !== '(' + context + 'Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z') continue;
            var original = new Label();
            var code = new Insns();
            code.add(new Var(O.ALOAD, 0));
            code.add(new Var(O.ALOAD, 1));
            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/WorldContentMode', 'allowsPlacement',
                '(L' + node.name + ';' + context + ')Z', false));
            code.add(new Jump(O.IFNE, original));
            code.add(new Insn(O.ICONST_0));
            code.add(new Insn(O.IRETURN));
            code.add(original);
            method.instructions.insert(code);
            method.maxStack = Math.max(method.maxStack, 2);
            injected++;
        }
        if (injected !== 1) throw new Error('Expected the shared placed feature entry, found ' + injected);
        return node;
    }

    function decorateChunk(node) {
        var world = 'Lnet/minecraft/world/level/WorldGenLevel;';
        var chunk = 'Lnet/minecraft/world/level/chunk/ChunkAccess;';
        var descriptor = '(' + world + chunk + 'Lnet/minecraft/world/level/StructureManager;)V';
        var injected = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) !== descriptor) continue;
            var code = new Insns();
            code.add(new Var(O.ALOAD, 1));
            code.add(new Var(O.ALOAD, 2));
            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicOreFeature',
                'decorate', '(' + world + chunk + ')V', false));
            method.instructions.insert(code);
            method.maxStack = Math.max(method.maxStack, 2);
            injected++;
        }
        if (injected !== 1) throw new Error('Expected one common chunk decoration method, found ' + injected);
        return node;
    }

    function strongholdRings(node) {
        state(node);
        var injected = 0;
        var biome = 'Lnet/minecraft/world/level/biome/BiomeSource;';
        var holder = 'Lnet/minecraft/core/Holder;';
        var future = 'Ljava/util/concurrent/CompletableFuture;';
        var field = null;
        for (var i = 0; i < node.fields.size(); i++) {
            var candidate = node.fields.get(i);
            if (String(candidate.desc) === biome) field = candidate.name;
        }
        if (field === null) throw new Error('Expected the active stronghold biome source');
        for (var j = 0; j < node.methods.size(); j++) {
            var method = node.methods.get(j);
            if (String(method.desc) !== '(' + holder +
                'Lnet/minecraft/world/level/levelgen/structure/placement/ConcentricRingsStructurePlacement;)' + future) continue;
            var original = new Label();
            var code = new Insns();
            code.add(new Var(O.ALOAD, 0));
            code.add(new Var(O.ALOAD, 1));
            code.add(new Var(O.ALOAD, 0));
            code.add(new FieldInsn(O.GETFIELD, node.name, field, biome));
            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicStrongholds', 'positions',
                '(L' + node.name + ';' + holder + biome + ')' + future, false));
            code.add(new Insn(O.DUP));
            code.add(new Jump(O.IFNULL, original));
            code.add(new Insn(O.ARETURN));
            code.add(original);
            code.add(new Insn(O.POP));
            method.instructions.insert(code);
            method.maxStack = Math.max(method.maxStack, 3);
            injected++;
        }
        if (injected !== 1) throw new Error('Expected the structure ring generator, found ' + injected);
        return node;
    }

    function experienceOrb(node) {
        node.interfaces.add('dev/koikaster/mite/WorldContentMode$OrbAgeAccess');
        var age = null;
        for (var i = 0; i < node.fields.size(); i++) {
            var field = node.fields.get(i);
            if (String(field.desc) === 'I' && (String(field.name) === 'age' || String(field.name) === 'f_20767_')) {
                age = field.name;
            }
        }
        if (age === null) throw new Error('Missing native orb age');
        var push = Java.type('org.objectweb.asm.tree.IntInsnNode');
        var extend = new Method(O.ACC_PUBLIC, 'mite$extendClassicDeathAge', '()V', null, null);
        extend.instructions.add(new Var(O.ALOAD, 0));
        extend.instructions.add(new push(O.SIPUSH, -18000));
        extend.instructions.add(new FieldInsn(O.PUTFIELD, node.name, age, 'I'));
        extend.instructions.add(new Insn(O.RETURN));
        extend.maxStack = 2;
        extend.maxLocals = 1;
        node.methods.add(extend);

        var orb = 'L' + node.name + ';';
        var inserted = 0;
        for (var j = 0; j < node.methods.size(); j++) {
            var method = node.methods.get(j);
            if (String(method.desc) !== '(' + orb + 'II)Z') continue;
            var nativePath = new Label();
            var code = new Insns();
            code.add(new Var(O.ALOAD, 0));
            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicDeathExperience',
                'ordinaryOrb', '(' + orb + ')Z', false));
            code.add(new Jump(O.IFNE, nativePath));
            code.add(new Insn(O.ICONST_0));
            code.add(new Insn(O.IRETURN));
            code.add(nativePath);
            method.instructions.insert(code);
            inserted++;
        }
        var actorCheck = 0;
        for (var k = 0; k < node.methods.size(); k++) {
            var instance = node.methods.get(k);
            if (String(instance.desc) !== '(' + orb + ')Z') continue;
            var continueWithTarget = new Label();
            var pre = new Insns();
            pre.add(new Var(O.ALOAD, 0));
            pre.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicDeathExperience',
                'ordinaryOrb', '(' + orb + ')Z', false));
            pre.add(new Jump(O.IFNE, continueWithTarget));
            pre.add(new Insn(O.ICONST_0));
            pre.add(new Insn(O.IRETURN));
            pre.add(continueWithTarget);
            instance.instructions.insert(pre);
            actorCheck++;
        }
        if (inserted !== 1 || actorCheck !== 1) throw new Error('Unexpected orb merge paths ' + inserted + '/' + actorCheck);
        return node;
    }


    var stockHooks = 'dev/koikaster/mite/WorldContentEvents';
    var living = 'Lnet/minecraft/world/entity/LivingEntity;';
    var stack = 'Lnet/minecraft/world/item/ItemStack;';
    var slot = 'Lnet/minecraft/world/entity/EquipmentSlot;';
    var modifiers = 'Lcom/google/common/collect/Multimap;';

    function stockRecipeLookup(node) {
        var lookups = [String(Java.type('net.minecraftforge.coremod.api.ASMAPI').mapMethod('m_44015_')),
            String(Java.type('net.minecraftforge.coremod.api.ASMAPI').mapMethod('m_220248_'))];
        var count = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (lookups.indexOf(String(method.name)) < 0) continue;
            var normal = new Label();
            var code = new Insns();
            code.add(new Var(O.ALOAD, 1));
            code.add(new Var(O.ALOAD, 2));
            code.add(new Var(O.ALOAD, 3));
            code.add(new Call(O.INVOKESTATIC, stockHooks, 'craftingAllowed',
                '(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z', false));
            code.add(new Jump(O.IFNE, normal));
            code.add(new Call(O.INVOKESTATIC, 'java/util/Optional', 'empty', '()Ljava/util/Optional;', false));
            code.add(new Insn(O.ARETURN));
            code.add(normal);
            method.instructions.insert(code);
            count++;
        }
        if (count !== 2) throw new Error('Unexpected legacy-stock recipe lookup count ' + count);
        return node;
    }

    function stockGlide(method) {
        var calls = method.instructions.toArray();
        var count = 0;
        for (var i = 0; i < calls.length; i++) {
            var instruction = calls[i];
            if (!(instruction instanceof Call) || String(instruction.owner) !== 'net/minecraft/world/item/ItemStack'
                    || String(instruction.name) !== 'canElytraFly' || String(instruction.desc) !== '(' + living + ')Z') continue;
            method.instructions.set(instruction, new Call(O.INVOKESTATIC, stockHooks,
                'canGlide', '(' + stack + living + ')Z', false));
            count++;
        }
        if (count !== 1) throw new Error('Unexpected legacy-stock glide call count ' + count);
    }

    function stockLiving(node) {
        var api = Java.type('net.minecraftforge.coremod.api.ASMAPI');
        var flying = api.mapMethod('m_21323_');
        var equipment = api.mapMethod('m_21319_');
        var stackModifiers = api.mapMethod('m_41638_');
        var glideCount = 0;
        var attributeCount = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) === String(flying) && String(method.desc) === '()V') {
                stockGlide(method);
                glideCount++;
            }
            if (String(method.name) !== String(equipment) || String(method.desc) !== '()Ljava/util/Map;') continue;
            var calls = method.instructions.toArray();
            var reads = [];
            for (var j = 0; j < calls.length; j++) {
                var instruction = calls[j];
                if (instruction instanceof Call && String(instruction.owner) === 'net/minecraft/world/item/ItemStack'
                        && String(instruction.name) === String(stackModifiers) && String(instruction.desc) === '(' + slot + ')' + modifiers) reads.push(instruction);
            }
            if (reads.length !== 2) throw new Error('Unexpected legacy-stock equipment attribute read count ' + reads.length);
            // The first read removes the old modifiers. Only the native addition reads active effects.
            method.instructions.insertBefore(reads[1], new Var(O.ALOAD, 0));
            method.instructions.set(reads[1], new Call(O.INVOKESTATIC, stockHooks, 'equipmentModifiers',
                '(' + stack + slot + living + ')' + modifiers, false));
            attributeCount++;
        }
        if (glideCount !== 1 || attributeCount !== 1) throw new Error('Expected legacy-stock living equipment hooks');
        return node;
    }

    function stockPlayer(node) {
        var api = Java.type('net.minecraftforge.coremod.api.ASMAPI');
        var check = String(api.mapMethod('m_36319_'));
        var projectile = String(api.mapMethod('m_6298_'));
        var predicates = [String(api.mapMethod('m_6437_')), String(api.mapMethod('m_6442_'))];
        var predicate = 'Ljava/util/function/Predicate;';
        var player = 'Lnet/minecraft/world/entity/player/Player;';
        var glide = 0;
        var ammo = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) === check && String(method.desc) === '()Z') {
                stockGlide(method);
                glide++;
            }
            if (String(method.name) !== projectile || String(method.desc) !== '(' + stack + ')' + stack) continue;
            var calls = method.instructions.toArray();
            for (var j = 0; j < calls.length; j++) {
                var instruction = calls[j];
                if (!(instruction instanceof Call) || String(instruction.owner) !== 'net/minecraft/world/item/ProjectileWeaponItem'
                        || predicates.indexOf(String(instruction.name)) < 0 || String(instruction.desc) !== '()' + predicate) continue;
                var code = new Insns();
                code.add(new Var(O.ALOAD, 0));
                code.add(new Call(O.INVOKESTATIC, stockHooks, 'projectilePredicate', '(' + predicate + player + ')' + predicate, false));
                method.instructions.insert(instruction, code);
                ammo++;
            }
        }
        if (glide !== 1 || ammo !== 2) throw new Error('Expected legacy-stock player flight and projectile entries');
        return node;
    }

    function stockEnchantments(node) {
        var api = Java.type('net.minecraftforge.coremod.api.ASMAPI');
        var lookup = String(api.mapMethod('m_44684_'));
        var equipmentItem = String(api.mapMethod('m_6844_'));
        var found = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== lookup || String(method.desc) !== '(' + living + ')Ljava/util/Map;') continue;
            var calls = method.instructions.toArray();
            for (var j = 0; j < calls.length; j++) {
                var instruction = calls[j];
                if (!(instruction instanceof Call) || String(instruction.owner) !== 'net/minecraft/world/entity/LivingEntity'
                        || String(instruction.name) !== equipmentItem || String(instruction.desc) !== '(' + slot + ')' + stack) continue;
                var code = new Insns();
                code.add(new Var(O.ALOAD, 1));
                code.add(new Call(O.INVOKESTATIC, stockHooks, 'activeEquipment', '(' + stack + living + ')' + stack, false));
                method.instructions.insert(instruction, code);
                found++;
            }
        }
        if (found !== 1) throw new Error('Expected one active enchantment equipment lookup');
        return node;
    }

    function catSpawner(node) {
        var api = Java.type('net.minecraftforge.coremod.api.ASMAPI');
        var tick = String(api.mapMethod('m_7995_'));
        var level = 'Lnet/minecraft/server/level/ServerLevel;';
        var found = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== tick || String(method.desc) !== '(' + level + 'ZZ)I') continue;
            var body = new Insns();
            var original = new Label();
            body.add(new Var(O.ALOAD, 1));
            body.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicCats', 'skipWildSpawner', '(' + level + ')Z', false));
            body.add(new Jump(O.IFEQ, original));
            body.add(new Insn(O.ICONST_0));
            body.add(new Insn(O.IRETURN));
            body.add(original);
            body.add(new Frame(O.F_SAME, 0, null, 0, null));
            method.instructions.insert(body);
            method.maxStack = Math.max(method.maxStack, 1);
            found++;
        }
        if (found !== 1) throw new Error('Expected exactly one native village and hut cat spawner tick');
        return node;
    }

    return {
        mite_content_mode_creation: {target: {type: 'CLASS', name: screen.replace(/\//g, '.')}, transformer: state},
        mite_content_mode_settings: {target: {type: 'CLASS', name: 'net.minecraft.world.level.LevelSettings'}, transformer: state},
        mite_content_mode_primary: {target: {type: 'CLASS', name: 'net.minecraft.world.level.storage.PrimaryLevelData'}, transformer: state},
        mite_content_mode_client: {target: {type: 'CLASS', name: 'net.minecraft.client.multiplayer.ClientLevel$ClientLevelData'}, transformer: state},
        mite_content_mode_client_level: {target: {type: 'CLASS', name: 'net.minecraft.client.multiplayer.ClientLevel'}, transformer: clientLevelHandoff},
        mite_content_mode_game_tab: {target: {type: 'CLASS', name: 'net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab'}, transformer: gameTab},
        mite_content_mode_chunk_map: {target: {type: 'CLASS', name: 'net.minecraft.server.level.ChunkMap'}, transformer: chunkMap},
        mite_content_mode_creative_slots: {target: {type: 'CLASS', name: 'net.minecraft.server.network.ServerGamePacketListenerImpl'}, transformer: creativeSlots},
        mite_content_mode_dimensions: {target: {type: 'CLASS', name: 'net.minecraft.server.MinecraftServer'}, transformer: dimensions},
        mite_content_mode_placed_feature: {target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.placement.PlacedFeature'}, transformer: placedFeature},
        mite_content_mode_chunk_decoration: {target: {type: 'CLASS', name: 'net.minecraft.world.level.chunk.ChunkGenerator'}, transformer: decorateChunk},
        mite_content_mode_stronghold_rings: {target: {type: 'CLASS', name: 'net.minecraft.world.level.chunk.ChunkGeneratorStructureState'}, transformer: strongholdRings},
        mite_content_mode_experience_orb: {target: {type: 'CLASS', name: 'net.minecraft.world.entity.ExperienceOrb'}, transformer: experienceOrb},
        mite_content_mode_stock_recipes: {target: {type: 'CLASS', name: 'net.minecraft.world.item.crafting.RecipeManager'}, transformer: stockRecipeLookup},
        mite_content_mode_stock_equipment: {target: {type: 'CLASS', name: 'net.minecraft.world.entity.LivingEntity'}, transformer: stockLiving},
        mite_content_mode_stock_player: {target: {type: 'CLASS', name: 'net.minecraft.world.entity.player.Player'}, transformer: stockPlayer},
        mite_content_mode_stock_enchantments: {target: {type: 'CLASS', name: 'net.minecraft.world.item.enchantment.Enchantment'}, transformer: stockEnchantments},
        mite_content_mode_cat_spawner: {target: {type: 'CLASS', name: 'net.minecraft.world.entity.npc.CatSpawner'}, transformer: catSpawner}
    };
}

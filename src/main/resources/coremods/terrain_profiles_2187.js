function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var helper = 'dev/koikaster/mite/TerrainProfiles2187';
    var compound = 'Lnet/minecraft/nbt/CompoundTag;';
    var registry = 'Lnet/minecraft/core/Registry;';
    var key = 'Lnet/minecraft/resources/ResourceKey;';
    return {
        mite_pin_terrain_on_save_read: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.storage.LevelStorageSource'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/world/level/WorldDataConfiguration;' +
                        registry + 'Lcom/mojang/serialization/Lifecycle;Ljava/nio/file/Path;Lcom/mojang/datafixers/DataFixer;)Lcom/mojang/datafixers/util/Pair;') continue;
                    for (var iter = method.instructions.iterator(); iter.hasNext();) {
                        var call = iter.next();
                        if (!(call instanceof M) || call.getOpcode() !== O.INVOKESTATIC
                            || String(call.owner) !== 'net/minecraft/nbt/NbtIo' || String(call.desc) !== '(Ljava/io/File;)' + compound) continue;
                        method.instructions.insert(call, new M(O.INVOKESTATIC, helper, 'pin', '(' + compound + ')' + compound, false));
                        count++;
                    }
                }
                if (count !== 1) throw new Error('Expected one raw world metadata read, got ' + count);
                return node;
            }
        },
        mite_preserve_pinned_level_stem: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.WorldDimensions'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '(' + registry + 'Ljava/util/List;' + key + ')V') continue;
                    var insns = method.instructions.toArray();
                    for (var j = 0; j < insns.length; j++) {
                        var call = insns[j];
                        if (!(call instanceof M) || call.getOpcode() !== O.INVOKEINTERFACE || String(call.owner) !== 'net/minecraft/core/Registry'
                            || String(call.desc) !== '(' + key + ')Ljava/util/Optional;') continue;
                        method.instructions.insertBefore(call, new V(O.ALOAD, 0));
                        method.instructions.set(call, new M(O.INVOKESTATIC, helper, 'packOrPinned',
                            '(' + registry + key + 'Lnet/minecraft/world/level/levelgen/WorldDimensions;)Ljava/util/Optional;', false));
                        method.maxStack = Math.max(3, method.maxStack);
                        count++;
                    }
                }
                if (count !== 1) throw new Error('Expected one world dimension merge site, got ' + count);
                return node;
            }
        }
    };
}

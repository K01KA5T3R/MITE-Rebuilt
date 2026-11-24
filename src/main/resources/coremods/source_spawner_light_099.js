function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

    var owner = 'net/minecraft/world/entity/PathfinderMob';
    var checkDescriptor = '(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/MobSpawnType;)Z';
    var weightDescriptor = '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/LevelReader;)F';

    return {
        mite_source_spawner_light_099: {
            target: {type: 'CLASS', name: 'net.minecraft.world.entity.PathfinderMob'},
            transformer: function(node) {
                var matches = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'checkSpawnRules' && name !== 'm_5545_') ||
                            String(method.desc) !== checkDescriptor) continue;

                    for (var iterator = method.instructions.iterator(); iterator.hasNext();) {
                        var call = iterator.next();
                        if (!(call instanceof MethodInsnNode) || call.getOpcode() !== Opcodes.INVOKEVIRTUAL ||
                                String(call.owner) !== owner || String(call.desc) !== weightDescriptor ||
                                (String(call.name) !== 'getWalkTargetValue' && String(call.name) !== 'm_5610_')) continue;

                        var adjustment = new InsnList();
                        adjustment.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        adjustment.add(new VarInsnNode(Opcodes.ALOAD, 2));
                        adjustment.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'dev/koikaster/mite/SourceSpawnerLight',
                            'nativeWalkWeight', '(FLjava/lang/Object;Ljava/lang/Object;)F', false));
                        method.instructions.insert(call, adjustment);
                        method.maxStack = Math.max(method.maxStack, 3);
                        matches++;
                    }
                }
                if (matches !== 1) throw new Error('Source spawner expected one PathfinderMob weight call, found ' + matches);
                return node;
            }
        }
    };
}

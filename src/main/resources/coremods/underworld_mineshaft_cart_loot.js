function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var world = 'Lnet/minecraft/world/level/WorldGenLevel;';
    var cart = 'net/minecraft/world/entity/vehicle/MinecartChest';
    var key = 'Lnet/minecraft/resources/ResourceLocation;';
    var createChest = '(' + world + 'Lnet/minecraft/world/level/levelgen/structure/BoundingBox;' +
        'Lnet/minecraft/util/RandomSource;III' + key + ')Z';
    return {
        underworld_natural_minecart_loot: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces$MineShaftCorridor'},
            transformer: function(node) {
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== createChest) continue;
                    var setter = null;
                    var code = method.instructions.toArray();
                    for (var j = 0; j < code.length; j++) {
                        var call = code[j];
                        if (!(call instanceof MethodInsnNode) || call.getOpcode() !== Opcodes.INVOKEVIRTUAL ||
                                String(call.owner) !== cart || String(call.desc) !== '(' + key + 'J)V') continue;
                        if (setter !== null) throw new Error('Multiple native mineshaft chest loot assignments');
                        setter = call;
                    }
                    if (setter === null) throw new Error('Native mineshaft chest loot assignment missing');
                    var loadWorld = new InsnList();
                    loadWorld.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    method.instructions.insertBefore(setter, loadWorld);
                    method.instructions.set(setter, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'dev/koikaster/mite/UnderworldExplorationRewards', 'setMineshaftLoot',
                        '(L' + cart + ';' + key + 'J' + world + ')V', false));
                    method.maxStack = Math.max(method.maxStack, 5);
                    return node;
                }
                throw new Error('Native mineshaft chest generation method missing');
            }
        }
    };
}

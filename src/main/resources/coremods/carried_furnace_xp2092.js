function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var input = '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;';
    return {restore_mite_carried_furnace_xp2092: {
        target: {type: 'CLASS', name: 'net.minecraft.world.item.BlockItem'},
        transformer: function(node) {
            var found = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                var name = String(method.name);
                if ((name !== 'updateCustomBlockEntityTag' && name !== 'm_7274_')
                    || String(method.desc) !== input + 'Lnet/minecraft/world/level/block/state/BlockState;)Z') continue;
                var original = method.instructions.toArray();
                for (var j = 0; j < original.length; j++) {
                    if (original[j].getOpcode() !== O.IRETURN) continue;
                    var hook = new L();
                    for (var k = 1; k <= 4; k++) hook.add(new V(O.ALOAD, k));
                    hook.add(new M(O.INVOKESTATIC, 'dev/koikaster/mite/FurnaceExperience', 'restorePlaced', input + ')V', false));
                    method.instructions.insertBefore(original[j], hook);
                    method.maxStack = Math.max(method.maxStack, 5);
                    found++;
                }
            }
            if (found !== 1) throw new Error('Expected one native furnace placement callback, got ' + found);
            return node;
        }
    }};
}

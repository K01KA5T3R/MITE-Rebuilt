function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    return {'mite_modern_music_pool_lookup': {
        target: {type: 'CLASS', name: 'net.minecraft.client.sounds.SoundManager'},
        transformer: function(node) {
            var matches = 0;
            var signature = '(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/sounds/WeighedSoundEvents;';
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                if (String(method.desc) !== signature || (method.access & O.ACC_STATIC) !== 0) continue;
                var prefix = new L();
                prefix.add(new V(O.ALOAD, 1));
                prefix.add(new M(O.INVOKESTATIC, 'dev/koikaster/mite/ModernMusicClient', 'poolKey',
                    '(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceLocation;', false));
                prefix.add(new V(O.ASTORE, 1));
                method.instructions.insert(prefix);
                method.maxStack = Math.max(method.maxStack, 1);
                matches++;
            }
            if (matches !== 1) throw new Error('Unexpected SoundManager event lookup count ' + matches);
            return node;
        }
    }};
}

function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var helper = 'dev/koikaster/mite/DeepDarkRemoval';
    var chunk = 'Lnet/minecraft/world/level/chunk/ChunkAccess;';
    var resolver = 'Lnet/minecraft/world/level/biome/BiomeResolver;';
    var sampler = 'Lnet/minecraft/world/level/biome/Climate$Sampler;';
    var consumer = 'Ljava/util/function/Consumer;';

    function install(node, named, mapped, descriptor, hook) {
        var sites = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            var name = String(method.name);
            if ((name !== named && name !== mapped) || String(method.desc) !== descriptor) continue;
            method.instructions.insert(hook);
            method.maxStack = Math.max(method.maxStack, 2);
            sites++;
        }
        if (sites !== 1) throw new Error('Expected one Deep Dark boundary at ' + named + ', found ' + sites);
        return node;
    }

    return {
        deep_dark_remove_climate: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.biome.OverworldBiomeBuilder'},
            transformer: function (node) {
                var hook = new List();
                hook.add(new Var(O.ALOAD, 1));
                hook.add(new Call(O.INVOKESTATIC, helper, 'climate', '(' + consumer + ')' + consumer, false));
                hook.add(new Var(O.ASTORE, 1));
                return install(node, 'addBiomes', 'm_187175_', '(' + consumer + ')V', hook);
            }
        },
        deep_dark_reject_generated_palette: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.chunk.ChunkAccess'},
            transformer: function (node) {
                var hook = new List();
                hook.add(new Var(O.ALOAD, 0));
                hook.add(new Var(O.ALOAD, 1));
                hook.add(new Call(O.INVOKESTATIC, helper, 'forGeneration', '(' + chunk + resolver + ')' + resolver, false));
                hook.add(new Var(O.ASTORE, 1));
                return install(node, 'fillBiomesFromNoise', 'm_183442_', '(' + resolver + sampler + ')V', hook);
            }
        }
    };
}

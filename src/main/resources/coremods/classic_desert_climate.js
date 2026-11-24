function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var consumer = 'Ljava/util/function/Consumer;';
    var parameter = 'Lnet/minecraft/world/level/biome/Climate$Parameter;';
    var descriptor = '(' + consumer + parameter + parameter + parameter + parameter + parameter +
        'FLnet/minecraft/resources/ResourceKey;)V';
    return {
        classic_desert_climate: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.biome.OverworldBiomeBuilder'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'addSurfaceBiome' && name !== 'm_187180_') || String(method.desc) !== descriptor) continue;
                    var hook = new List();
                    hook.add(new Var(O.ALOAD, 1));
                    hook.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ClassicDesertClimate', 'balanceSurface',
                        '(' + consumer + ')' + consumer, false));
                    hook.add(new Var(O.ASTORE, 1));
                    method.instructions.insert(hook);
                    count++;
                }
                if (count !== 1) throw new Error('Expected one Overworld surface biome emitter, found ' + count);
                return node;
            }
        }
    };
}

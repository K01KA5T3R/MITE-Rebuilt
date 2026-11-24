function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var InvokeDynamic = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
    var Handle = Java.type('org.objectweb.asm.Handle');
    var monsters = 'net/minecraft/world/entity/monster/';
    var descriptor = '(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V';
    return {
        mite_giant_boss_factory: {
            target: {type: 'CLASS', name: 'net.minecraft.world.entity.EntityType'},
            transformer: function(node) {
                var giants = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    if (String(method.name) !== '<clinit>') continue;
                    for (var insns = method.instructions.iterator(); insns.hasNext();) {
                        var insn = insns.next();
                        if (!(insn instanceof InvokeDynamic)) continue;
                        for (var j = 0; j < insn.bsmArgs.length; ++j) {
                            var factory = insn.bsmArgs[j];
                            if (!(factory instanceof Handle)) continue;
                            var owner = String(factory.getOwner());
                            if (owner !== monsters + 'Giant') continue;
                            ++giants;
                            if (factory.getTag() !== O.H_NEWINVOKESPECIAL || String(factory.getDesc()) !== descriptor) {
                                throw new Error('Unexpected boss factory ' + owner);
                            }
                            insn.bsmArgs[j] = new Handle(factory.getTag(), 'dev/koikaster/mite/SourceGiantZombieBoss',
                                factory.getName(), factory.getDesc(), factory.isInterface());
                        }
                    }
                }
                if (giants !== 1) throw new Error('Giant factory sites ' + giants);
                return node;
            }
        }
    };
}

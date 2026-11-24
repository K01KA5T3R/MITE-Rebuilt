function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var InvokeDynamic = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
    var Handle = Java.type('org.objectweb.asm.Handle');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var descriptor = '(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V';
    var world = 'Lnet/minecraft/world/level/WorldGenLevel;';
    var box = 'Lnet/minecraft/world/level/levelgen/structure/BoundingBox;';
    return {
        mite_elder_guardian_factory: {
            target: {type: 'CLASS', name: 'net.minecraft.world.entity.EntityType'},
            transformer: function(node) {
                var changed = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    if (String(method.name) !== '<clinit>') continue;
                    for (var insns = method.instructions.iterator(); insns.hasNext();) {
                        var insn = insns.next();
                        if (!(insn instanceof InvokeDynamic)) continue;
                        for (var j = 0; j < insn.bsmArgs.length; ++j) {
                            var factory = insn.bsmArgs[j];
                            if (!(factory instanceof Handle) || String(factory.getOwner()) !== 'net/minecraft/world/entity/monster/ElderGuardian') continue;
                            if (factory.getTag() !== O.H_NEWINVOKESPECIAL || String(factory.getDesc()) !== descriptor) {
                                throw new Error('Unexpected ElderGuardian factory');
                            }
                            insn.bsmArgs[j] = new Handle(factory.getTag(), 'dev/koikaster/mite/SourceElderGuardianBoss',
                                factory.getName(), factory.getDesc(), factory.isInterface());
                            ++changed;
                        }
                    }
                }
                if (changed !== 1) throw new Error('ElderGuardian factories ' + changed);
                return node;
            }
        },
        mite_ocean_monument_guardians: {
            target: {type: 'CLASS', name: 'net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece'},
            transformer: function(node) {
                var changed = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'spawnElder' && name !== 'm_247483_') || String(method.desc) !== '(' + world + box + 'III)V') continue;
                    var worldPos = null;
                    for (var it = method.instructions.iterator(); it.hasNext();) {
                        var candidate = it.next();
                        if (candidate.getOpcode() === O.INVOKEVIRTUAL
                            && (String(candidate.name) === 'getWorldPos' || String(candidate.name) === 'm_163582_')
                            && String(candidate.desc) === '(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;') worldPos = candidate;
                    }
                    if (worldPos === null) throw new Error('Missing original Ocean Monument position conversion');
                    method.instructions.clear();
                    method.tryCatchBlocks.clear();
                    if (method.localVariables !== null) method.localVariables.clear();
                    method.instructions.add(new Var(O.ALOAD, 1));
                    method.instructions.add(new Var(O.ALOAD, 2));
                    method.instructions.add(new Var(O.ALOAD, 0));
                    method.instructions.add(new Var(O.ILOAD, 3));
                    method.instructions.add(new Var(O.ILOAD, 4));
                    method.instructions.add(new Var(O.ILOAD, 5));
                    method.instructions.add(new Call(O.INVOKEVIRTUAL, worldPos.owner, worldPos.name, worldPos.desc, false));
                    method.instructions.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/ElderRitualContent', 'populateMonument',
                        '(' + world + box + 'Lnet/minecraft/core/BlockPos;)V', false));
                    method.instructions.add(new Insn(O.RETURN));
                    method.maxStack = 6;
                    ++changed;
                }
                if (changed !== 1) throw new Error('Ocean Monument guardian spawn sites ' + changed);
                return node;
            }
        }
    };
}

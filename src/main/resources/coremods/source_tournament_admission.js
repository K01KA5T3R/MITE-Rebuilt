function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsn = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var desc = '(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;';
    return {
        'source_tournament_admission': {
            target: {type: 'CLASS', name: 'net.minecraft.server.players.PlayerList'},
            transformer: function (node) {
                var found = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'canPlayerLogin' && name !== 'm_6418_') || String(method.desc) !== desc) continue;
                    found++;
                    var count = 0;
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; j++) {
                        var insn = instructions[j];
                        if (insn.getOpcode() !== O.ARETURN) continue;
                        var before = new InsnList();
                        before.add(new VarInsn(O.ALOAD, 0));
                        before.add(new VarInsn(O.ALOAD, 2));
                        before.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/SourceTournament', 'admission',
                            '(Lnet/minecraft/network/chat/Component;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/network/chat/Component;', false));
                        method.instructions.insertBefore(insn, before);
                        count++;
                    }
                    if (count === 0) throw new Error('No vanilla player admission result');
                    method.maxStack = Math.max(method.maxStack, 3);
                }
                if (found !== 1) throw new Error('Expected one vanilla player admission method, got ' + found);
                return node;
            }
        }
    };
}


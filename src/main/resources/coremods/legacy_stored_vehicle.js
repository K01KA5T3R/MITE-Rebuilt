function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var level = 'net/minecraft/server/level/ServerLevel';
    var entity = 'net/minecraft/world/entity/Entity';

    return {
        legacy_stored_vehicle: {
            target: {type: 'CLASS', name: 'net.minecraft.server.players.PlayerList'},
            transformer: function (node) {
                var matches = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '(L' + level + ';L' + entity + ';)L' + entity + ';') continue;
                    var calls = method.instructions.toArray();
                    for (var j = 0; j < calls.length; j++) {
                        var call = calls[j];
                        if (!(call instanceof Call) || call.getOpcode() !== O.INVOKEVIRTUAL || String(call.owner) !== level
                                || !/^(m_8847_|addWithUUID)$/.test(String(call.name))
                                || String(call.desc) !== '(L' + entity + ';)Z') continue;
                        method.instructions.set(call, new Call(O.INVOKESTATIC, 'dev/koikaster/mite/LegacyStoredVehicle',
                            'restore', '(L' + level + ';L' + entity + ';)Z', false));
                        matches++;
                    }
                }
                if (matches !== 1) throw new Error('Expected one saved player vehicle restoration, found ' + matches);
                return node;
            }
        }
    };
}

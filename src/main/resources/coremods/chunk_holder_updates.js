function initializeCoreMod() {
    var A = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var O = Java.type('org.objectweb.asm.Opcodes');
    var C = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Dynamic = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
    var methodDescriptor = '(Lnet/minecraft/server/level/ChunkMap;)Z';
    var setDescriptor = 'Ljava/util/Set;';
    var updateDescriptor = '(Ljava/util/function/Consumer;)V';

    function previous(instruction) {
        var node = instruction.getPrevious();
        while (node !== null && node.getOpcode() < 0) node = node.getPrevious();
        return node;
    }

    function loads(node, variable) {
        return node instanceof V && node.getOpcode() === O.ALOAD && node.var === variable;
    }

    return {
        holder_update_batch: {
            target: {type: 'CLASS', name: 'net.minecraft.server.level.DistanceManager'},
            transformer: function(node) {
                var method = null;
                var mapped = String(A.mapMethod('m_140805_'));
                for (var i = 0; i < node.methods.size(); i++) {
                    var entry = node.methods.get(i);
                    var name = String(entry.name);
                    if (String(entry.desc) === methodDescriptor &&
                            (name === mapped || name === 'm_140805_' || name === 'runAllUpdates')) {
                        if (method !== null) throw new Error('Duplicate native chunk distance update');
                        method = entry;
                    }
                }
                if (method === null) throw new Error('Missing native chunk distance update');

                var iteration = null;
                var clear = null;
                var code = method.instructions.toArray();
                for (var j = 0; j < code.length; j++) {
                    var instruction = code[j];
                    if (!(instruction instanceof C) || instruction.getOpcode() !== O.INVOKEINTERFACE ||
                            String(instruction.owner) !== 'java/util/Set') continue;
                    if (String(instruction.name) === 'forEach' && String(instruction.desc) === updateDescriptor) {
                        if (iteration !== null) throw new Error('Duplicate native chunk holder iteration');
                        iteration = instruction;
                    } else if (String(instruction.name) === 'clear' && String(instruction.desc) === '()V') {
                        if (clear !== null) throw new Error('Duplicate native chunk holder clear');
                        clear = instruction;
                    }
                }

                if (iteration === null || clear === null) throw new Error('Missing native chunk holder operations');
                var consumer = previous(iteration);
                var chunkMap = previous(consumer);
                var manager = previous(chunkMap);
                var iterationField = previous(manager);
                var clearField = previous(clear);
                var clearOwner = previous(clearField);
                if (!(consumer instanceof Dynamic) || !loads(chunkMap, 1) || !loads(manager, 0) ||
                        !(iterationField instanceof F) || iterationField.getOpcode() !== O.GETFIELD ||
                        !(clearField instanceof F) || clearField.getOpcode() !== O.GETFIELD ||
                        String(iterationField.desc) !== setDescriptor || String(clearField.desc) !== setDescriptor ||
                        String(iterationField.owner) !== String(node.name) ||
                        String(clearField.owner) !== String(node.name) ||
                        String(iterationField.name) !== String(clearField.name) || !loads(clearOwner, 0) ||
                        previous(clearOwner) !== iteration) {
                    throw new Error('Native chunk holder operations do not share the expected pending set');
                }

                method.instructions.set(iteration, new C(O.INVOKESTATIC, 'dev/koikaster/mite/ChunkHolderUpdates',
                    'apply', '(Ljava/util/Set;Ljava/util/function/Consumer;)V', false));
                method.instructions.remove(clearOwner);
                method.instructions.remove(clearField);
                method.instructions.remove(clear);
                return node;
            }
        }
    };
}
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var K = Java.type('org.objectweb.asm.tree.LdcInsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var helper = 'dev/koikaster/mite/ClientRainSpawn2160';
    var client = 'Lnet/minecraft/client/Minecraft;';
    var level = 'net/minecraft/client/multiplayer/ClientLevel';
    var state = 'Lnet/minecraft/world/level/block/state/BlockState;';
    var particle = 'Lnet/minecraft/core/particles/ParticleOptions;';

    return {
        source_rain_spawn2160: {
            target: {type: 'CLASS', name: 'net.minecraft.client.renderer.LevelRenderer'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if ((String(method.name) !== 'tickRain' && String(method.name) !== 'm_109693_')
                        || String(method.desc) !== '(Lnet/minecraft/client/Camera;)V') continue;

                    var instructions = method.instructions.toArray();
                    var real = [];
                    for (var j = 0; j < instructions.length; j++) {
                        if (instructions[j].getOpcode() >= 0) real.push(instructions[j]);
                    }
                    var rainHead = null, clientField = null, tickField = null, stateSlot = -1, emit = null;
                    var soundHead = null, soundExit = null, soundCounter = null;
                    for (var j = 0; j < real.length; j++) {
                        var insn = real[j];
                        if (insn instanceof M && String(insn.owner) === level && String(insn.desc) === '(F)F'
                            && (String(insn.name) === 'getRainLevel' || String(insn.name) === 'm_46722_')) {
                            if (rainHead !== null || j < 4 || real[j-4].getOpcode() !== O.ALOAD || real[j-4].var !== 0
                                || real[j-3].getOpcode() !== O.GETFIELD || String(real[j-3].owner) !== String(node.name)
                                || String(real[j-3].desc) !== client || real[j-1].getOpcode() !== O.FCONST_1) {
                                throw new Error('Unexpected initial rain strength instructions');
                            }
                            rainHead = real[j-4];
                            clientField = real[j-3];
                        } else if (insn instanceof K && Number(insn.cst) === 312987231 && j >= 2
                            && real[j-1].getOpcode() === O.I2L && real[j-2].getOpcode() === O.GETFIELD
                            && String(real[j-2].owner) === String(node.name) && String(real[j-2].desc) === 'I') {
                            if (tickField !== null) throw new Error('Duplicate rain tick source');
                            tickField = real[j-2];
                        } else if (insn instanceof M && String(insn.desc) === '(Lnet/minecraft/core/BlockPos;)' + state) {
                            if (stateSlot !== -1 || real[j+1].getOpcode() !== O.ASTORE) throw new Error('Unexpected rain block state');
                            stateSlot = real[j+1].var;
                        } else if (insn instanceof M && insn.getOpcode() === O.INVOKEVIRTUAL
                            && String(insn.owner) === level && String(insn.desc) === '(' + particle + 'DDDDDD)V') {
                            if (emit !== null) throw new Error('Duplicate rain particle emitter');
                            emit = insn;
                        } else if (insn instanceof J && insn.getOpcode() === O.IFNULL && j > 0 && j + 11 < real.length
                            && real[j-1].getOpcode() === O.ALOAD && real[j+1].getOpcode() === O.ALOAD
                            && real[j+2].getOpcode() === O.ICONST_3 && real[j+3] instanceof M
                            && String(real[j+3].owner) === 'net/minecraft/util/RandomSource' && String(real[j+3].desc) === '(I)I'
                            && real[j+4].getOpcode() === O.ALOAD && real[j+4].var === 0 && real[j+5].getOpcode() === O.DUP
                            && real[j+6].getOpcode() === O.GETFIELD && String(real[j+6].owner) === String(node.name)
                            && String(real[j+6].desc) === 'I' && real[j+10].getOpcode() === O.PUTFIELD
                            && String(real[j+10].name) === String(real[j+6].name) && real[j+11].getOpcode() === O.IF_ICMPGE
                            && real[j+11].label === insn.label) {
                            if (soundHead !== null) throw new Error('Duplicate native rain sound tail');
                            soundHead = real[j-1];
                            soundExit = insn.label;
                            soundCounter = real[j+6];
                        }
                    }
                    if (rainHead === null || tickField === null || stateSlot < 0 || emit === null || soundHead === null) {
                        throw new Error('Missing native rain visual or tick site');
                    }

                    // The vanilla rain-strength read follows Forge's dimension effect early return.
                    var start = new L();
                    start.add(new V(O.ALOAD, 0));
                    start.add(new V(O.ALOAD, 0));
                    start.add(new F(O.GETFIELD, node.name, clientField.name, client));
                    start.add(new V(O.ALOAD, 0));
                    start.add(new F(O.GETFIELD, node.name, tickField.name, 'I'));
                    start.add(new V(O.ALOAD, 0));
                    start.add(new F(O.GETFIELD, node.name, soundCounter.name, 'I'));
                    start.add(new M(O.INVOKESTATIC, helper, 'sample', '(' + client + 'II)I', false));
                    start.add(new F(O.PUTFIELD, node.name, soundCounter.name, 'I'));
                    method.instructions.insertBefore(rainHead, start);
                    var audio = new L();
                    audio.add(new V(O.ALOAD, 0));
                    audio.add(new F(O.GETFIELD, node.name, clientField.name, client));
                    audio.add(new M(O.INVOKESTATIC, helper, 'ownsAudio', '(' + client + ')Z', false));
                    audio.add(new J(O.IFNE, soundExit));
                    method.instructions.insertBefore(soundHead, audio);
                    method.instructions.insertBefore(emit, new V(O.ALOAD, stateSlot));
                    method.instructions.set(emit, new M(O.INVOKESTATIC, helper, 'nativeParticle',
                        '(L' + level + ';' + particle + 'DDDDDD' + state + ')V', false));
                    method.maxStack = Math.max(method.maxStack, 15);
                    count++;
                }
                if (count !== 1) throw new Error('Expected exactly one native rain tick, got ' + count);
                return node;
            }
        }
    };
}

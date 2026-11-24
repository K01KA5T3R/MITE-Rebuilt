function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var I = Java.type('org.objectweb.asm.tree.InsnNode');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var C = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var server = 'dev/koikaster/mite/CooperativeMining2133';
    var client = 'dev/koikaster/mite/ClientCooperativeMining2133';
    var obj = 'Ljava/lang/Object;';
    var pos = 'Lnet/minecraft/core/BlockPos;';
    var state = 'Lnet/minecraft/world/level/block/state/BlockState;';
    var player = 'Lnet/minecraft/server/level/ServerPlayer;';
    var action = 'Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;';
    var blockRate = '(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;' + pos + ')F';

    function matches(method, official, srg, desc) {
        return (String(method.name) === official || String(method.name) === srg) && String(method.desc) === desc;
    }

    function field(node, official, srg, desc) {
        for (var i = 0; i < node.fields.size(); i++) {
            var f = node.fields.get(i);
            if (matches(f, official, srg, desc)) return String(f.name);
        }
        throw new Error('Cooperative mining missing field ' + official);
    }

    function load(list, opcode, slot) {
        list.add(new V(opcode, slot));
    }

    function get(list, node, name, desc) {
        load(list, O.ALOAD, 0);
        list.add(new F(O.GETFIELD, String(node.name), name, desc));
    }

    function call(list, owner, name, desc) {
        list.add(new C(O.INVOKESTATIC, owner, name, desc, false));
    }

    function previous(insn) {
        var cursor = insn.getPrevious();
        while (cursor !== null && cursor.getOpcode() < 0) cursor = cursor.getPrevious();
        return cursor;
    }

    function next(insn) {
        var cursor = insn.getNext();
        while (cursor !== null && cursor.getOpcode() < 0) cursor = cursor.getNext();
        return cursor;
    }

    function elapsedProduct(insn) {
        if (insn.getOpcode() !== O.FMUL) return false;
        var current = insn;
        var prefix = [O.I2F, O.IADD, O.ICONST_1, O.ILOAD];
        for (var i = 0; i < prefix.length; i++) {
            current = previous(current);
            if (current === null || current.getOpcode() !== prefix[i]) return false;
        }
        return true;
    }

    function nativeRate(insn) {
        return insn.getOpcode() === O.INVOKEVIRTUAL
            && String(insn.owner) === 'net/minecraft/world/level/block/state/BlockState'
            && matches(insn, 'getDestroyProgress', 'm_60625_', blockRate);
    }

    return {
        'cooperative_server2133': {
            target: {type: 'CLASS', name: 'net.minecraft.server.level.ServerPlayerGameMode'},
            transformer: function(node) {
                var owner = field(node, 'player', 'f_9245_', player);
                var active = field(node, 'isDestroyingBlock', 'f_9249_', 'Z');
                var activePos = field(node, 'destroyPos', 'f_9251_', pos);
                var delayed = field(node, 'hasDelayedDestroy', 'f_9253_', 'Z');
                var delayedPos = field(node, 'delayedDestroyPos', 'f_9254_', pos);
                var accepts = 0, stops = 0, ticks = 0, actionSyncs = 0, tickSyncs = 0;

                function sync(method, insn) {
                    var list = new L();
                    get(list, node, owner, player);
                    get(list, node, active, 'Z');
                    get(list, node, activePos, pos);
                    get(list, node, delayed, 'Z');
                    get(list, node, delayedPos, pos);
                    call(list, server, 'sync', '(' + obj + 'Z' + obj + 'Z' + obj + ')V');
                    method.instructions.insertBefore(insn, list);
                }

                for (var i = 0; i < node.methods.size(); i++) {
                    var m = node.methods.get(i), all = m.instructions.toArray();
                    if (matches(m, 'handleBlockBreakAction', 'm_214168_', '(' + pos + action + 'Lnet/minecraft/core/Direction;II)V')) {
                        for (var j = 0; j < all.length; j++) {
                            var insn = all[j];
                            if (insn.getOpcode() === O.PUTFIELD && String(insn.owner) === String(node.name)
                                && String(insn.name) === activePos && String(insn.desc) === pos) {
                                var rate = next(insn);
                                if (rate === null || rate.getOpcode() !== O.FLOAD) throw new Error('Cooperative mining START rate missing');
                                var list = new L();
                                get(list, node, owner, player);
                                load(list, O.ALOAD, 1);
                                load(list, O.ILOAD, 5);
                                load(list, O.FLOAD, rate.var);
                                call(list, server, 'accepted', '(' + obj + obj + 'IF)V');
                                m.instructions.insert(insn, list);
                                accepts++;
                            } else if (elapsedProduct(insn)) {
                                var list = new L();
                                get(list, node, owner, player);
                                load(list, O.ALOAD, 1);
                                call(list, server, 'stopProgress', '(F' + obj + obj + ')F');
                                m.instructions.insert(insn, list);
                                stops++;
                            } else if (insn.getOpcode() === O.RETURN) {
                                sync(m, insn);
                                actionSyncs++;
                            }
                        }
                    } else if (matches(m, 'incrementDestroyProgress', 'm_9276_', '(' + state + pos + 'I)F')) {
                        var product = null, rate = null;
                        for (var j = 0; j < all.length; j++) {
                            if (elapsedProduct(all[j])) {
                                if (product !== null) throw new Error('Duplicate cooperative mining tick product');
                                product = all[j];
                            }
                            if (nativeRate(all[j])) {
                                if (rate !== null) throw new Error('Duplicate cooperative mining tick rate');
                                rate = all[j];
                            }
                        }
                        if (product === null || rate === null || previous(previous(previous(previous(previous(product))))) !== rate) {
                            throw new Error('Cooperative mining tick rate does not feed the elapsed product');
                        }
                        var slot = m.maxLocals;
                        m.maxLocals = slot + 1;
                        var save = new L();
                        save.add(new I(O.DUP));
                        load(save, O.FSTORE, slot);
                        m.instructions.insert(rate, save);
                        var list = new L();
                        load(list, O.FLOAD, slot);
                        get(list, node, owner, player);
                        load(list, O.ALOAD, 1);
                        load(list, O.ALOAD, 2);
                        get(list, node, delayed, 'Z');
                        call(list, server, 'tickProgress', '(FF' + obj + obj + obj + 'Z)F');
                        m.instructions.insert(product, list);
                        ticks++;
                    } else if (matches(m, 'tick', 'm_7712_', '()V')) {
                        for (var j = 0; j < all.length; j++) {
                            if (all[j].getOpcode() === O.RETURN) {
                                sync(m, all[j]);
                                tickSyncs++;
                            }
                        }
                    } else {
                        continue;
                    }
                    m.maxStack = Math.max(16, m.maxStack);
                }
                if (accepts !== 1 || stops !== 1 || ticks !== 1 || actionSyncs < 2 || tickSyncs < 1) {
                    throw new Error('Cooperative server sites ' + [accepts, stops, ticks, actionSyncs, tickSyncs]);
                }
                return node;
            }
        },
        'cooperative_client2133': {
            target: {type: 'CLASS', name: 'net.minecraft.client.multiplayer.MultiPlayerGameMode'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var m = node.methods.get(i);
                    if (!matches(m, 'continueDestroyBlock', 'm_105283_', '(' + pos + 'Lnet/minecraft/core/Direction;)Z')) continue;
                    var all = m.instructions.toArray();
                    for (var j = 0; j < all.length; j++) {
                        var insn = all[j];
                        if (!nativeRate(insn) || next(insn).getOpcode() !== O.FADD) continue;
                        var list = new L();
                        load(list, O.ALOAD, 1);
                        call(list, client, 'progress', '(F' + obj + ')F');
                        m.instructions.insert(insn, list);
                        m.maxStack = Math.max(16, m.maxStack);
                        count++;
                    }
                }
                if (count !== 1) throw new Error('Cooperative client progress sites ' + count);
                return node;
            }
        },
        'cooperative_outgoing2133': {
            target: {type: 'CLASS', name: 'net.minecraft.client.multiplayer.ClientPacketListener'},
            transformer: function(node) {
                var count = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var m = node.methods.get(i);
                    if (!matches(m, 'send', 'm_104955_', '(Lnet/minecraft/network/protocol/Packet;)V')) continue;
                    var list = new L();
                    load(list, O.ALOAD, 1);
                    call(list, client, 'outgoing', '(' + obj + ')V');
                    m.instructions.insert(list);
                    m.maxStack = Math.max(16, m.maxStack);
                    count++;
                }
                if (count !== 1) throw new Error('Cooperative client packet sites ' + count);
                return node;
            }
        }
    };
}


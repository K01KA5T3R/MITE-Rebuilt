function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var policy = 'dev/koikaster/mite/ModernOffhand';
    var playerType = 'Lnet/minecraft/world/entity/player/Player;';
    var item = 'Lnet/minecraft/world/item/ItemStack;';
    var livingType = 'Lnet/minecraft/world/entity/LivingEntity;';
    var handType = 'Lnet/minecraft/world/InteractionHand;';

    function slot(node) {
        var playerField = null;
        for (var i = 0; i < node.fields.size(); i++) {
            var field = node.fields.get(i);
            if (String(field.desc) !== playerType || (field.access & O.ACC_SYNTHETIC) === 0) continue;
            if (playerField !== null) throw new Error('Offhand slot has multiple captured players');
            playerField = String(field.name);
        }
        if (playerField === null) throw new Error('Offhand slot has no captured player');
        var desc = '(' + item + ')Z';
        var srg = false;
        for (var j = 0; j < node.methods.size(); j++) {
            var method = node.methods.get(j);
            var name = String(method.name);
            if (name.indexOf('m_') === 0) srg = true;
            if (String(method.desc) === desc && (name === 'mayPlace' || name === 'm_5857_')) {
                throw new Error('Offhand slot unexpectedly already overrides mayPlace');
            }
        }
        var added = new Method(O.ACC_PUBLIC, srg ? 'm_5857_' : 'mayPlace', desc, null, null);
        added.instructions.add(new Var(O.ALOAD, 0));
        added.instructions.add(new Field(O.GETFIELD, node.name, playerField, playerType));
        added.instructions.add(new Var(O.ALOAD, 1));
        added.instructions.add(new Call(O.INVOKESTATIC, policy, 'allowed', '(' + playerType + item + ')Z', false));
        added.instructions.add(new Insn(O.IRETURN));
        added.maxStack = 2;
        added.maxLocals = 2;
        node.methods.add(added);
        return node;
    }

    function swap(node) {
        var serverPlayer = 'Lnet/minecraft/server/level/ServerPlayer;';
        var playerField = null;
        for (var i = 0; i < node.fields.size(); i++) {
            var field = node.fields.get(i);
            var name = String(field.name);
            if (String(field.desc) === serverPlayer && (name === 'player' || name === 'f_9743_')) playerField = name;
        }
        if (playerField === null) throw new Error('Swap player field missing');
        var count = 0;
        for (var j = 0; j < node.methods.size(); j++) {
            var method = node.methods.get(j);
            var methodName = String(method.name);
            if (String(method.desc) !== '(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V'
                    || (methodName !== 'handlePlayerAction' && methodName !== 'm_7502_')) continue;
            var instructions = method.instructions.toArray();
            for (var k = 0; k < instructions.length; k++) {
                var nodeCall = instructions[k];
                if (nodeCall.getOpcode() !== O.INVOKESTATIC
                        || String(nodeCall.owner) !== 'net/minecraft/network/protocol/PacketUtils'
                        || (String(nodeCall.name) !== 'ensureRunningOnSameThread' && String(nodeCall.name) !== 'm_131359_')) continue;
                var added = new List();
                var next = new Label();
                added.add(new Var(O.ALOAD, 0));
                added.add(new Field(O.GETFIELD, node.name, playerField, serverPlayer));
                added.add(new Var(O.ALOAD, 1));
                added.add(new Call(O.INVOKESTATIC, policy, 'rejectSwap', '(Ljava/lang/Object;Ljava/lang/Object;)Z', false));
                added.add(new Jump(O.IFEQ, next));
                added.add(new Insn(O.RETURN));
                added.add(next);
                method.instructions.insert(nodeCall, added);
                method.maxStack = Math.max(method.maxStack, 2);
                count++;
            }
        }
        if (count !== 1) throw new Error('Expected one main-thread-guarded swap entry, got ' + count);
        return node;
    }

    function startUsing(node) {
        var count = 0;
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            var name = String(method.name);
            if (String(method.desc) !== '(' + handType + ')V'
                    || (name !== 'startUsingItem' && name !== 'm_6672_')) continue;
            var added = new List();
            var next = new Label();
            added.add(new Var(O.ALOAD, 0));
            added.add(new Var(O.ALOAD, 1));
            added.add(new Call(O.INVOKESTATIC, policy, 'canUseHand', '(' + livingType + handType + ')Z', false));
            added.add(new Jump(O.IFNE, next));
            added.add(new Insn(O.RETURN));
            added.add(next);
            added.add(new Frame(O.F_SAME, 0, null, 0, null));
            method.instructions.insert(added);
            method.maxStack = Math.max(method.maxStack, 2);
            count++;
        }
        if (count !== 1) throw new Error('Expected one actual item-use hand parameter entry, got ' + count);
        return node;
    }

    return {
        mite_modern_offhand_slot_040: {
            target: {type: 'CLASS', name: 'net.minecraft.world.inventory.InventoryMenu$2'},
            transformer: slot
        },
        mite_modern_offhand_swap_040: {
            target: {type: 'CLASS', name: 'net.minecraft.server.network.ServerGamePacketListenerImpl'},
            transformer: swap
        },
        mite_modern_offhand_use_045: {
            target: {type: 'CLASS', name: 'net.minecraft.world.entity.LivingEntity'},
            transformer: startUsing
        }
    };
}


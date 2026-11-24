/** Apply the already saved world choice to native progression presentation. */
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Insns = Java.type('org.objectweb.asm.tree.InsnList');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Type = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var Ldc = Java.type('org.objectweb.asm.tree.LdcInsnNode');
    var owner = 'dev/koikaster/mite/ClientModernAdvancement548';
    var grid = 'net/minecraft/client/gui/layouts/GridLayout';
    var rows = grid + '$RowHelper';
    var options = 'net/minecraft/client/Options';
    var keyArray = '[Lnet/minecraft/client/KeyMapping;';
    var layoutChild = '(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;';

    function method(node, named, srg, descriptor) {
        var found = null;
        for (var i = 0; i < node.methods.size(); i++) {
            var candidate = node.methods.get(i), name = String(candidate.name);
            if (String(candidate.desc) !== descriptor || (name !== named && name !== srg)) continue;
            if (found !== null) throw new Error('Duplicate advancement owner ' + named);
            found = candidate;
        }
        if (found === null) throw new Error('Missing advancement owner ' + named);
        return found;
    }

    function nextInstruction(node) {
        var next = node.getNext();
        while (next !== null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    function statsField(pause) {
        var init = method(pause, '<clinit>', '<clinit>', '()V'), found = null;
        for (var cursor = init.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (!(cursor instanceof Ldc) || String(cursor.cst) !== 'gui.stats') continue;
            var translation = nextInstruction(cursor), store = nextInstruction(translation);
            if (!(translation instanceof Call) || !(store instanceof Field) || store.getOpcode() !== O.PUTSTATIC
                || String(store.owner) !== String(pause.name) || String(store.desc) !== 'Lnet/minecraft/network/chat/Component;'
                || found !== null) throw new Error('Unexpected PauseScreen statistics field');
            found = String(store.name);
        }
        if (found === null) throw new Error('Missing PauseScreen statistics field');
        return found;
    }

    function pauseMenu(node) {
        var menu = method(node, 'createPauseMenu', 'm_96338_', '()V'), stats = statsField(node);
        var row = -1, after = null, foundStats = false;
        for (var cursor = menu.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (cursor instanceof Call && String(cursor.owner) === grid && String(cursor.desc) === '(I)L' + rows + ';') {
                var store = nextInstruction(cursor);
                if (!(store instanceof Var) || store.getOpcode() !== O.ASTORE || row !== -1) {
                    throw new Error('Unexpected PauseScreen row owner');
                }
                row = store.var;
            }
            if (cursor instanceof Field && cursor.getOpcode() === O.GETSTATIC && String(cursor.owner) === String(node.name)
                    && String(cursor.name) === stats) {
                if (foundStats || after !== null) throw new Error('Unexpected repeated PauseScreen statistics reference');
                foundStats = true;
            }
            if (foundStats && cursor instanceof Call && String(cursor.owner) === rows && String(cursor.desc) === layoutChild) {
                after = nextInstruction(cursor);
                if (after === null || after.getOpcode() !== O.POP) throw new Error('Unexpected statistics button result');
                foundStats = false;
            }
        }
        if (row < 0 || after === null) throw new Error('Missing PauseScreen statistics row');
        var bridge = new Insns();
        bridge.add(new Var(O.ALOAD, row));
        bridge.add(new Call(O.INVOKESTATIC, owner, 'addMiteAchievementRow', '(L' + rows + ';)V', false));
        menu.instructions.insert(after, bridge);
        menu.maxStack = Math.max(menu.maxStack, 1);
        return node;
    }

    function keyBindings(node) {
        var selection = method(node, '<init>', '<init>',
            '(Lnet/minecraft/client/gui/screens/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V');
        var matched = 0;
        for (var cursor = selection.instructions.getFirst(); cursor !== null; cursor = cursor.getNext()) {
            if (!(cursor instanceof Field) || cursor.getOpcode() !== O.GETFIELD || String(cursor.owner) !== options
                    || String(cursor.desc) !== keyArray) continue;
            if (++matched !== 1 || (String(cursor.name) !== 'keyMappings' && String(cursor.name) !== 'f_92059_')) {
                throw new Error('Unexpected controls key mapping source');
            }
            selection.instructions.insertBefore(cursor, new Insn(O.DUP));
            selection.instructions.insert(cursor, new Call(O.INVOKESTATIC, owner + '$Setup', 'shownKeyMappings',
                '(L' + options + ';' + keyArray + ')' + keyArray, false));
        }
        if (matched !== 1) throw new Error('Missing controls key mapping source');
        selection.maxStack++;
        return node;
    }

    function toastAdmission(node) {
        var addToast = method(node, 'addToast', 'm_94922_', '(Lnet/minecraft/client/gui/components/toasts/Toast;)V');
        var bridge = new Insns(), continueNative = new Label();
        bridge.add(new Var(O.ALOAD, 1));
        bridge.add(new Type(O.INSTANCEOF, 'net/minecraft/client/gui/components/toasts/AdvancementToast'));
        bridge.add(new Jump(O.IFEQ, continueNative));
        bridge.add(new Call(O.INVOKESTATIC, owner, 'showModernProgress', '()Z', false));
        bridge.add(new Jump(O.IFNE, continueNative));
        bridge.add(new Insn(O.RETURN));
        bridge.add(continueNative);
        addToast.instructions.insert(bridge);
        addToast.maxStack = Math.max(addToast.maxStack, 1);
        return node;
    }

    return {
        world_advancement_pause_menu_048: {target: {type: 'CLASS', name: 'net.minecraft.client.gui.screens.PauseScreen'}, transformer: pauseMenu},
        world_advancement_key_bindings_048: {target: {type: 'CLASS', name: 'net.minecraft.client.gui.screens.controls.KeyBindsList'}, transformer: keyBindings},
        world_advancement_toast_048: {target: {type: 'CLASS', name: 'net.minecraft.client.gui.components.toasts.ToastComponent'}, transformer: toastAdmission}
    };
}


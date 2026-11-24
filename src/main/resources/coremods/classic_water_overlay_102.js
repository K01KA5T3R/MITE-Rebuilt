// Keep the native drawing, overlay events, and water-alpha hook; restore the classic layer order.
function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var C = Java.type('org.objectweb.asm.tree.LdcInsnNode');
    var L = Java.type('org.objectweb.asm.tree.InsnList');

    var minecraft = 'net/minecraft/client/Minecraft';
    var player = 'Lnet/minecraft/client/player/LocalPlayer;';
    var screen = 'net/minecraft/client/renderer/ScreenEffectRenderer';
    var hooks = 'net/minecraftforge/client/ForgeHooksClient';
    var draw = '(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V';

    function previous(node) {
        if (node === null) return null;
        for (var n = node.getPrevious(); n !== null; n = n.getPrevious()) if (n.getOpcode() >= 0) return n;
        return null;
    }

    function next(node) {
        if (node === null) return null;
        for (var n = node.getNext(); n !== null; n = n.getNext()) if (n.getOpcode() >= 0) return n;
        return null;
    }

    function local(node, opcode, variable) {
        return node instanceof V && node.getOpcode() === opcode && node.var === variable;
    }

    function clientPlayerField(node) {
        return node instanceof F && node.getOpcode() === O.GETFIELD
            && String(node.owner) === minecraft && String(node.desc) === player;
    }

    function sameField(first, second) {
        return clientPlayerField(first) && clientPlayerField(second) && String(first.name) === String(second.name);
    }

    function booleanCall(node) {
        return node instanceof M && node.getOpcode() === O.INVOKEVIRTUAL
            && String(node.owner) === 'net/minecraft/client/player/LocalPlayer' && String(node.desc) === '()Z';
    }

    function guard(node, opcode) {
        return node instanceof J && node.getOpcode() === opcode;
    }

    function copyCall(call) {
        return new M(call.getOpcode(), call.owner, call.name, call.desc, call.itf);
    }

    function restoreWaterAlpha(node) {
        var anchors = [];
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.name) !== 'renderFluid'
                    || String(method.desc) !== '(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;)V'
                    || (method.access & O.ACC_STATIC) === 0) continue;
            for (var n = method.instructions.getFirst(); n !== null; n = n.getNext()) {
                if (!(n instanceof M) || String(n.owner) !== 'com/mojang/blaze3d/systems/RenderSystem'
                        || String(n.name) !== 'setShaderColor' || String(n.desc) !== '(FFFF)V') continue;
                var before = n.getPrevious();
                if (before instanceof C && Math.abs(Number(before.cst) - 0.1) < 0.000001) anchors.push({method: method, call: n});
            }
        }
        if (anchors.length !== 1) {
            return;
        }
        var target = anchors[0], code = new L();
        code.add(new V(O.ALOAD, 2));
        code.add(new M(O.INVOKESTATIC, 'dev/koikaster/mite/visual/ClassicWaterOverlay', 'alpha', '(FLjava/lang/Object;)F', false));
        target.method.instructions.insertBefore(target.call, code);
        target.method.maxStack += 1;
    }

    function locateFire(method) {
        var found = Object.create(null);
        found.renderFireOverlay = [];
        found.renderWaterOverlay = [];
        found.renderBlockOverlay = [];
        for (var n = method.instructions.getFirst(); n !== null; n = n.getNext()) {
            if (!(n instanceof M) || n.getOpcode() !== O.INVOKESTATIC || String(n.owner) !== hooks) continue;
            var list = found[String(n.name)];
            if (list) list.push(n);
        }
        if (found.renderFireOverlay.length !== 1 || found.renderWaterOverlay.length !== 1
                || found.renderBlockOverlay.length !== 1) return null;

        var originalFirst = method.instructions.getFirst();
        if (originalFirst === null) return null;
        var first = originalFirst.getOpcode() >= 0 ? originalFirst : next(originalFirst);
        var field = next(first);
        var store = next(field);
        if (!local(first, O.ALOAD, 0) || !clientPlayerField(field)
                || !(store instanceof V) || store.getOpcode() !== O.ASTORE || store.var < 2) return null;

        var hook = found.renderFireOverlay[0];
        var poseArg = previous(hook), playerArg = previous(poseArg);
        var fireJump = previous(playerArg), onFire = previous(fireJump);
        var firePlayer = previous(onFire), start = previous(firePlayer);
        if (!local(poseArg, O.ALOAD, 1) || !local(playerArg, O.ALOAD, store.var)
                || !guard(fireJump, O.IFEQ) || !booleanCall(onFire) || !sameField(field, firePlayer)
                || !local(start, O.ALOAD, 0)) return null;

        var canceled = next(hook), nativeMc = next(canceled), nativePose = next(nativeMc), nativeFire = next(nativePose);
        if (!guard(canceled, O.IFNE) || canceled.label !== fireJump.label
                || !local(nativeMc, O.ALOAD, 0) || !local(nativePose, O.ALOAD, 1)
                || !(nativeFire instanceof M) || nativeFire.getOpcode() !== O.INVOKESTATIC
                || String(nativeFire.owner) !== screen || String(nativeFire.desc) !== draw
                || next(nativeFire) === null || next(nativeFire) !== next(canceled.label)
                || next(nativeFire).getOpcode() !== O.RETURN) return null;

        var spectators = [];
        for (var cursor = store.getNext(); cursor !== null && cursor !== start; cursor = cursor.getNext()) {
            if (!guard(cursor, O.IFNE) || cursor.label !== canceled.label) continue;
            var candidate = previous(cursor);
            if (booleanCall(candidate) && sameField(field, previous(candidate))
                    && local(previous(previous(candidate)), O.ALOAD, 0)) spectators.push(candidate);
        }
        if (spectators.length !== 1) return null;
        return {store: store, start: start, end: canceled.label, spectator: spectators[0],
            onFire: onFire, forgeFire: hook, nativeFire: nativeFire};
    }

    function restoreLayerOrder(node) {
        var matches = [];
        for (var i = 0; i < node.methods.size(); i++) {
            var method = node.methods.get(i);
            if (String(method.desc) !== draw || (method.access & O.ACC_STATIC) === 0
                    || (method.access & O.ACC_PUBLIC) === 0) continue;
            var fire = locateFire(method);
            if (fire !== null) matches.push({method: method, fire: fire});
        }
        if (matches.length !== 1) {
            return;
        }

        var method = matches[0].method, fire = matches[0].fire;
        var early = new L(), originalBlock = new Label();
        var mode = method.maxLocals;
        var actor = fire.store.var;
        early.add(new V(O.ALOAD, actor));
        early.add(new M(O.INVOKESTATIC, 'dev/koikaster/mite/visual/ClassicScreenOverlay', 'earlyFire', '(Ljava/lang/Object;)Z', false));
        early.add(new V(O.ISTORE, mode));
        early.add(new V(O.ILOAD, mode));
        early.add(new J(O.IFEQ, originalBlock));
        early.add(new V(O.ALOAD, actor));
        early.add(copyCall(fire.spectator));
        early.add(new J(O.IFNE, originalBlock));
        early.add(new V(O.ALOAD, actor));
        early.add(copyCall(fire.onFire));
        early.add(new J(O.IFEQ, originalBlock));
        early.add(new V(O.ALOAD, actor));
        early.add(new V(O.ALOAD, 1));
        early.add(copyCall(fire.forgeFire));
        early.add(new J(O.IFNE, originalBlock));
        early.add(new V(O.ALOAD, 0));
        early.add(new V(O.ALOAD, 1));
        early.add(copyCall(fire.nativeFire));
        early.add(originalBlock);

        var late = new L();
        late.add(new V(O.ILOAD, mode));
        late.add(new J(O.IFNE, fire.end));
        method.instructions.insert(fire.store, early);
        method.instructions.insertBefore(fire.start, late);
        method.maxLocals = mode + 1;
        method.maxStack = Math.max(method.maxStack, 2);
    }

    return {classic_water_overlay_102: {target: {type: 'CLASS', name: 'net.minecraft.client.renderer.ScreenEffectRenderer'},
        transformer: function(node) {
            restoreWaterAlpha(node);
            restoreLayerOrder(node);
            return node;
        }}};
}

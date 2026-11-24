function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var itemStack = 'net/minecraft/world/item/ItemStack';
    return {
        cartography_batch2254: {
            target: {type: 'CLASS', name: 'net.minecraft.world.inventory.CartographyTableMenu'},
            transformer: function (node) {
                var match = null;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '(Lnet/minecraft/world/entity/player/Player;I)L' + itemStack + ';') continue;
                    if (match !== null) throw new Error('Multiple cartography quick-move methods');
                    match = method;
                }
                if (match === null) throw new Error('Cartography quick-move method missing');
                var first = match.instructions.getFirst();
                while (first !== null && first.getOpcode() < 0) first = first.getNext();
                if (first === null || first.getOpcode() !== O.GETSTATIC || String(first.owner) !== itemStack
                    || String(first.desc) !== 'L' + itemStack + ';') {
                    throw new Error('Cartography quick move must begin with the native empty stack');
                }
                var proceed = new Label();
                var guard = new InsnList();
                guard.add(new Var(O.ALOAD, 0));
                guard.add(new Var(O.ALOAD, 1));
                guard.add(new Var(O.ILOAD, 2));
                guard.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/SourceCraftBatch1722', 'cartographyShiftBlocked',
                    '(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Player;I)Z', false));
                guard.add(new Jump(O.IFEQ, proceed));
                guard.add(new Field(O.GETSTATIC, itemStack, first.name, first.desc));
                guard.add(new Insn(O.ARETURN));
                guard.add(proceed);
                match.instructions.insertBefore(first, guard);
                match.maxStack = Math.max(match.maxStack, 3);
                return node;
            }
        }
    };
}

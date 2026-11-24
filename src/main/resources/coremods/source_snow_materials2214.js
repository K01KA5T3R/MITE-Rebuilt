function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var I = Java.type('org.objectweb.asm.tree.InsnNode');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var C = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var N = Java.type('org.objectweb.asm.tree.LabelNode');
    var F = Java.type('org.objectweb.asm.tree.FrameNode');
    var result = 'net/minecraft/world/InteractionResult';
    return {
        source_snow_merge2214: {
            target: {type: 'CLASS', name: 'net.minecraft.world.item.BlockItem'},
            transformer: function(node) {
                var found = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var name = String(method.name);
                    if ((name !== 'place' && name !== 'm_40576_') ||
                            String(method.desc) !== '(Lnet/minecraft/world/item/context/BlockPlaceContext;)L' + result + ';') continue;
                    var code = new L();
                    var vanilla = new N();
                    code.add(new V(O.ALOAD, 1));
                    code.add(new C(O.INVOKESTATIC, 'dev/koikaster/mite/SourceSnowMaterials', 'tryMerge',
                        '(Ljava/lang/Object;)L' + result + ';', false));
                    code.add(new I(O.DUP));
                    code.add(new J(O.IFNULL, vanilla));
                    code.add(new I(O.ARETURN));
                    code.add(vanilla);
                    code.add(new F(O.F_SAME1, 0, null, 1, [result]));
                    code.add(new I(O.POP));
                    method.instructions.insert(code);
                    method.maxStack = Math.max(method.maxStack, 2);
                    found++;
                }
                if (found !== 1) throw new Error('Source snow placement expected one BlockItem.place method, got ' + found);
                return node;
            }
        }
    };
}

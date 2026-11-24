function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var F = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var I = Java.type('org.objectweb.asm.tree.InsnNode');
    var B = Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame = Java.type('org.objectweb.asm.tree.FrameNode');
    var owner = 'net.minecraft.client.renderer.entity.ItemEntityRenderer';
    var itemRenderer = 'Lnet/minecraft/client/renderer/entity/ItemRenderer;';
    var entityRenderer = 'net/minecraft/client/renderer/entity/EntityRenderer';
    var argumentsTail = 'FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V';
    var obj = 'Ljava/lang/Object;';
    var helper = 'dev/koikaster/mite/visual/ClassicGroundItemRenderer2140';

    function load(list, opcode, index) { list.add(new V(opcode, index)); }

    return {
        'classic_ground_item_render2140': {
            target: {type: 'CLASS', name: owner},
            transformer: function(node) {
                var item = null, changed = 0;
                for (var i = 0; i < node.fields.size(); i++) {
                    var field = node.fields.get(i);
                    if (String(field.desc) !== itemRenderer) continue;
                    if (item !== null) throw new Error('Duplicate native dropped-item renderer field');
                    item = String(field.name);
                }
                if (item === null) throw new Error('Native dropped-item renderer field missing');
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    if (String(method.desc) !== '(Lnet/minecraft/world/entity/item/ItemEntity;' + argumentsTail) continue;
                    var instructions = method.instructions.toArray(), base = null;
                    for (var j = 0; j < instructions.length; j++) {
                        var instruction = instructions[j];
                        if (instruction.getOpcode() === O.INVOKESPECIAL && String(instruction.owner) === entityRenderer
                            && String(instruction.desc) === '(Lnet/minecraft/world/entity/Entity;' + argumentsTail) {
                            if (base !== null) throw new Error('Duplicate native dropped-item base render');
                            base = instruction;
                        }
                    }
                    if (base === null) throw new Error('Native dropped-item base render missing');
                    var code = new L(), nativePath = new B();
                    load(code, O.ALOAD, 0);
                    load(code, O.ALOAD, 0);
                    code.add(new F(O.GETFIELD, String(node.name), item, itemRenderer));
                    load(code, O.ALOAD, 1);
                    load(code, O.FLOAD, 3);
                    load(code, O.ALOAD, 4);
                    load(code, O.ALOAD, 5);
                    load(code, O.ILOAD, 6);
                    code.add(new M(O.INVOKESTATIC, helper, 'render', '(' + obj + obj + obj + 'F' + obj + obj + 'I)Z', false));
                    code.add(new J(O.IFEQ, nativePath));
                    // Preserve the native superclass call, including the Forge nameplate event.
                    load(code, O.ALOAD, 0);
                    load(code, O.ALOAD, 1);
                    load(code, O.FLOAD, 2);
                    load(code, O.FLOAD, 3);
                    load(code, O.ALOAD, 4);
                    load(code, O.ALOAD, 5);
                    load(code, O.ILOAD, 6);
                    code.add(new M(O.INVOKESPECIAL, String(base.owner), String(base.name), String(base.desc), false));
                    code.add(new I(O.RETURN));
                    code.add(nativePath);
                    code.add(new Frame(O.F_SAME, 0, null, 0, null));
                    method.instructions.insert(code);
                    method.maxStack = Math.max(method.maxStack, 7);
                    changed++;
                }
                if (changed !== 1) throw new Error('Native dropped-item render sites ' + changed);
                return node;
            }
        }
    };
}


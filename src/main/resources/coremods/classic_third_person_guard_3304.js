function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var A = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var arm = 'Lnet/minecraft/world/entity/HumanoidArm;';
    var pose = 'Lcom/mojang/blaze3d/vertex/PoseStack;';
    var descriptor = '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;'
        + 'Lnet/minecraft/world/item/ItemDisplayContext;' + arm + pose
        + 'Lnet/minecraft/client/renderer/MultiBufferSource;I)V';

    return {mite_classic_third_person_guard_3304: {
        target: {type: 'CLASS', name: 'net.minecraft.client.renderer.entity.layers.ItemInHandLayer'},
        transformer: function(node) {
            var method = null;
            for (var i = 0; i < node.methods.size(); ++i) {
                var m = node.methods.get(i);
                var name = String(m.name);
                if (String(m.desc) !== descriptor || (name !== 'renderArmWithItem'
                        && name !== String(A.mapMethod('m_117184_')))) continue;
                if (method !== null) return node;
                method = m;
            }
            if (method === null) return node;

            var grip = null;
            for (var it = method.instructions.iterator(); it.hasNext();) {
                var op = it.next();
                if (op.getOpcode() !== O.INVOKEINTERFACE || String(op.owner) !== 'net/minecraft/client/model/ArmedModel'
                        || String(op.desc) !== '(' + arm + pose + ')V'
                        || (String(op.name) !== 'translateToHand' && String(op.name) !== String(A.mapMethod('m_6002_')))) continue;
                if (grip !== null) return node;
                grip = op;
            }
            if (grip === null) return node;

            var code = new InsnList();
            var slots = [1, 2, 4, 5];
            for (var j = 0; j < slots.length; ++j) code.add(new Var(O.ALOAD, slots[j]));
            code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/visual/ClassicThirdPersonGuard',
                'apply', '(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V', false));
            method.instructions.insert(grip, code);
            method.maxStack = Math.max(method.maxStack, 4);
            return node;
        }
    }};
}

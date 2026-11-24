function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Load = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var stack = 'Lnet/minecraft/world/item/ItemStack;';
    var actor = 'Lnet/minecraft/world/entity/LivingEntity;';
    var food = 'Lnet/minecraft/world/food/FoodProperties;';
    return {source_food_effects2226: {
        target: {type: 'CLASS', name: 'net.minecraft.world.entity.LivingEntity'},
        transformer: function(node) {
            var anchors = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                var name = String(method.name);
                if ((name !== 'addEatEffect' && name !== 'm_21063_') ||
                    String(method.desc) !== '(' + stack + 'Lnet/minecraft/world/level/Level;' + actor + ')V') continue;
                var instructions = method.instructions.toArray();
                for (var j = 0; j < instructions.length; j++) {
                    var insn = instructions[j];
                    if (insn.getOpcode() !== O.INVOKEVIRTUAL || String(insn.owner) !== 'net/minecraft/world/item/ItemStack' ||
                        String(insn.name) !== 'getFoodProperties' || String(insn.desc) !== '(' + actor + ')' + food) continue;
                    var code = new List();
                    code.add(new Load(O.ALOAD, 1));
                    code.add(new Load(O.ALOAD, 3));
                    code.add(new Call(O.INVOKESTATIC, 'dev/koikaster/mite/Diet', 'foodEffectProperties',
                        '(' + food + 'Ljava/lang/Object;Ljava/lang/Object;)' + food, false));
                    method.instructions.insert(insn, code);
                    method.maxStack = Math.max(3, method.maxStack);
                    anchors++;
                }
            }
            if (anchors !== 1) throw new Error('Expected one native eating effects lookup, got ' + anchors);
            return node;
        }
    }};
}

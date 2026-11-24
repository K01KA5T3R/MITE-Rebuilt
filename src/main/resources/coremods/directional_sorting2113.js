function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var L = Java.type('org.objectweb.asm.tree.InsnList');
    var V = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var I = Java.type('org.objectweb.asm.tree.InsnNode');
    var J = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var N = Java.type('org.objectweb.asm.tree.LabelNode');
    var F = Java.type('org.objectweb.asm.tree.FrameNode');
    var HELP = 'dev/koikaster/mite/DirectionalSorting2113';

    function method(node, names, descriptor) {
        var found = null;
        for (var i = 0; i < node.methods.size(); i++) {
            var value = node.methods.get(i);
            if (names.indexOf(String(value.name)) < 0 || String(value.desc) !== descriptor) continue;
            if (found !== null) throw new Error('Duplicate directional sorting method ' + names);
            found = value;
        }
        if (found === null) throw new Error('Missing directional sorting method ' + names);
        return found;
    }

    return {
        sort_menu_button2113: {
            target: {type:'CLASS',name:'net.minecraft.world.inventory.AbstractContainerMenu'},
            transformer: function(node) {
                var click = method(node,['clickMenuButton','m_6366_'],'(Lnet/minecraft/world/entity/player/Player;I)Z');
                var real = [];
                for (var insn = click.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
                    if (insn.getOpcode() >= 0) real.push(insn);
                }
                if (real.length !== 2 || real[0].getOpcode() !== O.ICONST_0 || real[1].getOpcode() !== O.IRETURN) {
                    throw new Error('Unexpected native menu button default');
                }
                var code = new L();
                code.add(new V(O.ALOAD,0));
                code.add(new V(O.ALOAD,1));
                code.add(new V(O.ILOAD,2));
                code.add(new M(O.INVOKESTATIC,HELP,'click','(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Player;I)Z',false));
                click.instructions.insertBefore(real[0],code);
                click.instructions.remove(real[0]);
                click.maxStack = Math.max(click.maxStack,3);
                return node;
            }
        },
        remove_sort_shortcut2113: {
            target: {type:'CLASS',name:'cpw.mods.inventorysorter.KeyHandler'},
            transformer: function(node) {
                var constructor = method(node,['<init>'],'()V');
                var count = 0;
                for (var insn = constructor.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
                    if (!(insn instanceof M) || insn.getOpcode() !== O.INVOKESTATIC || String(insn.owner) !== 'java/util/stream/Stream'
                        || String(insn.name) !== 'of' || String(insn.desc) !== '([Ljava/lang/Object;)Ljava/util/stream/Stream;') continue;
                    insn.owner = HELP;
                    insn.name = 'shortcutActions';
                    insn.itf = false;
                    count++;
                }
                if (count !== 1) throw new Error('Expected one embedded sort shortcut registration');
                return node;
            }
        },
        ignore_legacy_sort_request2113: {
            target: {type:'CLASS',name:'cpw.mods.inventorysorter.Action'},
            transformer: function(node) {
                var execute = method(node,['execute'],'(Lcpw/mods/inventorysorter/ContainerContext;)V');
                var nativePath = new N();
                var code = new L();
                code.add(new V(O.ALOAD,0));
                code.add(new M(O.INVOKESTATIC,HELP,'legacySortAction','(Ljava/lang/Object;)Z',false));
                code.add(new J(O.IFEQ,nativePath));
                code.add(new I(O.RETURN));
                code.add(nativePath);
                code.add(new F(O.F_SAME,0,null,0,null));
                execute.instructions.insert(code);
                execute.maxStack = Math.max(execute.maxStack,1);
                return node;
            }
        }
    };
}

function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var vanilla = 'net/minecraft/world/item/CreativeModeTabs';
    var forge = 'net/minecraftforge/common/CreativeModeTabRegistry';
    var hook = 'dev/koikaster/mite/ClassicCreativeTabs';
    var tab = 'Lnet/minecraft/world/item/CreativeModeTab;';
    var list = '()Ljava/util/List;';

    function same(insn, opcode, owner, official, srg, desc) {
        return insn.getOpcode() === opcode && String(insn.owner) === owner && String(insn.desc) === desc
            && (String(insn.name) === official || (srg !== null && String(insn.name) === ASMAPI.mapMethod(srg)));
    }

    function invoke(method, insn, target, desc) {
        method.instructions.set(insn, new Call(O.INVOKESTATIC, hook, target, desc, false));
    }

    return {
        mite_classic_creative_screen: {
            target: {type: 'CLASS', name: 'net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen'},
            transformer: function(node) {
                var layout = 0, refresh = 0, empty = 0, tooltip = 0, defaults = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i), desc = String(method.desc);
                    var isRefresh = desc === '(Lnet/minecraft/world/flag/FeatureFlagSet;ZLnet/minecraft/core/HolderLookup$Provider;)V';
                    var isTooltip = desc === '(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;';
                    var isInit = String(method.name) === 'init' || String(method.name) === ASMAPI.mapMethod('m_7856_');
                    var insns = method.instructions.toArray();
                    for (var j = 0; j < insns.length; ++j) {
                        var insn = insns[j];
                        if (isInit && same(insn, O.INVOKESTATIC, forge, 'getSortedCreativeModeTabs', null, list)) {
                            invoke(method, insn, 'pages', list); ++layout;
                        } else if (isRefresh && same(insn, O.INVOKESTATIC, vanilla, 'allTabs', 'm_257478_', list)) {
                            invoke(method, insn, 'refreshedTabs', list); ++refresh;
                        } else if (isRefresh && same(insn, O.INVOKEINTERFACE, 'java/util/Collection', 'isEmpty', null, '()Z')) {
                            var previous = insn.getPrevious();
                            while (previous !== null && previous.getOpcode() < 0) previous = previous.getPrevious();
                            if (previous.getOpcode() !== O.ALOAD || previous.var !== 6) throw new Error('Creative refresh collection changed');
                            method.instructions.insertBefore(insn, new Var(O.ALOAD, 5));
                            invoke(method, insn, 'emptyAfterRefresh', '(Ljava/util/Collection;' + tab + ')Z');
                            method.maxStack = Math.max(method.maxStack, 3);
                            ++empty;
                        } else if (isTooltip && same(insn, O.INVOKESTATIC, vanilla, 'tabs', 'm_257388_', list)) {
                            invoke(method, insn, 'shownTabs', list); ++tooltip;
                        } else if ((isInit || isRefresh) && same(insn, O.INVOKESTATIC, vanilla, 'getDefaultTab', 'm_257543_', '()' + tab)) {
                            invoke(method, insn, 'defaultTab', '()' + tab); ++defaults;
                        }
                    }
                }
                if (layout !== 1 || refresh !== 1 || empty !== 1 || tooltip !== 1 || defaults !== 3)
                    throw new Error('Unexpected classic creative screen sites ' + [layout, refresh, empty, tooltip, defaults]);
                return node;
            }
        },
        mite_classic_creative_page: {
            target: {type: 'CLASS', name: 'net.minecraftforge.client.gui.CreativeTabsScreenPage'},
            transformer: function(node) {
                var defaults = 0;
                for (var i = 0; i < node.methods.size(); ++i) {
                    var method = node.methods.get(i), insns = method.instructions.toArray();
                    for (var j = 0; j < insns.length; ++j) {
                        if (!same(insns[j], O.INVOKESTATIC, forge, 'getDefaultTabs', null, list)) continue;
                        invoke(method, insns[j], 'defaults', list);
                        ++defaults;
                    }
                }
                if (defaults !== 5) throw new Error('Unexpected shared creative page sites ' + defaults);
                return node;
            }
        }
    };
}

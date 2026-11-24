function initializeCoreMod() {
    var O = Java.type('org.objectweb.asm.Opcodes');
    var Method = Java.type('org.objectweb.asm.tree.MethodNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Field = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var Insn = Java.type('org.objectweb.asm.tree.InsnNode');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Label = Java.type('org.objectweb.asm.tree.LabelNode');
    var Jump = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var H = 'dev/koikaster/mite/SourcePaintings2050';
    var P = 'net/minecraft/world/entity/decoration/Painting';
    var E = 'net/minecraft/world/entity/Entity';
    var RNG = 'Lnet/minecraft/util/RandomSource;';
    var CREATE = '(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Ljava/util/Optional;';
    var HOVER = '(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V';
    function named(m, a, b) { return String(m.name) === a || String(m.name) === b; }
    return {
        source_painting_backing2050: {
            target: {type:'CLASS',name:'net.minecraft.world.entity.decoration.HangingEntity'},
            transformer: function(node) {
                var count=0;
                for (var i=0; i<node.methods.size(); i++) {
                    var m=node.methods.get(i);
                    if (!named(m,'survives','m_7088_') || String(m.desc)!=='()Z') continue;
                    var code=new List(), proceed=new Label();
                    code.add(new Var(O.ALOAD,0));
                    code.add(new Call(O.INVOKESTATIC,H,'hasOriginalBacking',
                        '(Lnet/minecraft/world/entity/decoration/HangingEntity;)Z',false));
                    code.add(new Jump(O.IFNE,proceed));
                    code.add(new Insn(O.ICONST_0));
                    code.add(new Insn(O.IRETURN));
                    code.add(proceed);
                    m.instructions.insert(code);
                    m.maxStack=Math.max(1,m.maxStack);
                    count++;
                }
                if (count!==1) throw new Error('Unexpected hanging support method count '+count);
                return node;
            }
        },
        source_painting_random2050: {
            target: {type:'CLASS',name:'net.minecraft.world.entity.decoration.Painting'},
            transformer: function(node) {
                var field = null;
                for (var i=0; i<node.methods.size(); i++) {
                    var m=node.methods.get(i);
                    if (!named(m,'create','m_218887_') || String(m.desc)!==CREATE) continue;
                    var insns=m.instructions.toArray();
                    for (var j=0; j<insns.length; j++) {
                        var x=insns[j];
                        if (!(x instanceof Field) || x.getOpcode()!==O.GETFIELD || String(x.desc)!==RNG ||
                            (String(x.owner)!==P && String(x.owner)!==E) ||
                            (String(x.name)!=='random' && String(x.name)!=='f_19796_')) continue;
                        if (field!==null) throw new Error('Duplicate native painting RNG');
                        field=x;
                    }
                }
                if (field===null || node.interfaces.contains(H+'$EntityRandom')) throw new Error('Native painting RNG missing or already bridged');
                node.interfaces.add(H+'$EntityRandom');
                var bridge=new Method(O.ACC_PUBLIC|O.ACC_FINAL|O.ACC_SYNTHETIC,'mite$paintingRandom2050','()'+RNG,null,null);
                bridge.instructions.add(new Var(O.ALOAD,0));
                bridge.instructions.add(new Field(O.GETFIELD,String(field.owner),String(field.name),RNG));
                bridge.instructions.add(new Insn(O.ARETURN));
                bridge.maxStack=1;
                bridge.maxLocals=1;
                node.methods.add(bridge);
                return node;
            }
        },
        source_painting_item2050: {
            target: {type:'CLASS',name:'net.minecraft.world.item.HangingEntityItem'},
            transformer: function(node) {
                var use=0, hover=0;
                for (var i=0; i<node.methods.size(); i++) {
                    var m=node.methods.get(i);
                    if (named(m,'useOn','m_6225_') &&
                        String(m.desc)==='(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;') {
                        var all=m.instructions.toArray();
                        for (var j=0; j<all.length; j++) {
                            var x=all[j];
                            if (!(x instanceof Call) || x.getOpcode()!==O.INVOKESTATIC ||
                                String(x.owner)!==P || !named(x,'create','m_218887_') || String(x.desc)!==CREATE) continue;
                            m.instructions.set(x,new Call(O.INVOKESTATIC,H,'createForItem',CREATE,false));
                            use++;
                        }
                    }
                    if (named(m,'appendHoverText','m_7373_') && String(m.desc)===HOVER) {
                        var head=new List(), fallthrough=new Label();
                        head.add(new Var(O.ALOAD,1));
                        head.add(new Var(O.ALOAD,3));
                        head.add(new Call(O.INVOKESTATIC,H,'appendTaggedTooltip',
                            '(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Z',false));
                        head.add(new Jump(O.IFEQ,fallthrough));
                        head.add(new Insn(O.RETURN));
                        head.add(fallthrough);
                        m.instructions.insert(head);
                        m.maxStack=Math.max(2,m.maxStack);
                        hover++;
                    }
                }
                if (use!==1 || hover!==1) throw new Error('Unexpected painting item hook counts '+use+'/'+hover);
                return node;
            }
        }
    };
}

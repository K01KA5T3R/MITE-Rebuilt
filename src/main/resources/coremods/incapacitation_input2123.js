function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes');
    var L=Java.type('org.objectweb.asm.tree.InsnList');
    var V=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var D=Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var M=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var J=Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var I=Java.type('org.objectweb.asm.tree.InsnNode');
    var N=Java.type('org.objectweb.asm.tree.LabelNode');
    var F=Java.type('org.objectweb.asm.tree.FrameNode');
    var playerType='Lnet/minecraft/server/level/ServerPlayer;';
    var guarded=['PlayerAction','UseItem','UseItemOn','Interact','ContainerClick','ContainerButtonClick','SetCarriedItem','Swing',
        'MoveVehicle','PlayerCommand','PickItem','PlayerInput'];

    return {incapacitated_native_actions2123:{
        target:{type:'CLASS',name:'net.minecraft.server.network.ServerGamePacketListenerImpl'},
        transformer:function(owner){
            var playerField=null;
            for(var i=0;i<owner.fields.size();i++){
                var field=owner.fields.get(i);
                if(String(field.desc)!==playerType)continue;
                if(playerField!==null)throw new Error('Ambiguous server player owner');
                playerField=String(field.name);
            }
            if(playerField===null)throw new Error('Missing server player owner');
            for(var k=0;k<guarded.length;k++){
                var descriptor='(Lnet/minecraft/network/protocol/game/Serverbound'+guarded[k]+'Packet;)V';
                var handlers=0;
                for(var i=0;i<owner.methods.size();i++){
                    var method=owner.methods.get(i);
                    if(String(method.desc)!==descriptor)continue;
                    var handoff=null;
                    for(var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()){
                        if(!(insn instanceof M)||insn.getOpcode()!==O.INVOKESTATIC||String(insn.owner)!=='net/minecraft/network/protocol/PacketUtils')continue;
                        if(handoff!==null)throw new Error('Multiple thread handoffs for '+descriptor);
                        handoff=insn;
                    }
                    if(handoff===null)throw new Error('Missing thread handoff for '+descriptor);
                    var next=new N();
                    var code=new L();
                    code.add(new V(O.ALOAD,0));
                    code.add(new D(O.GETFIELD,String(owner.name),playerField,playerType));
                    code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/Incapacitation2123','incapacitated','('+playerType+')Z',false));
                    code.add(new J(O.IFEQ,next));
                    code.add(new I(O.RETURN));
                    code.add(next);
                    code.add(new F(O.F_SAME,0,null,0,null));
                    method.instructions.insert(handoff,code);
                    method.maxStack=Math.max(1,method.maxStack);
                    handlers++;
                }
                if(handlers!==1)throw new Error('Expected one packet handler for '+descriptor);
            }
            return owner;
        }
    }};
}

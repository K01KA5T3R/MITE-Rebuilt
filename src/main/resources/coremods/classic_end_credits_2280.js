function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes');
 return {mite_classic_end_credits_2280:{
  target:{type:'CLASS',name:'net.minecraft.client.multiplayer.ClientPacketListener'},
  transformer:function(node){
   var original='net/minecraft/client/gui/screens/WinScreen';
   var classic='dev/koikaster/mite/ClassicEndCreditsScreen';
   var allocations=0,constructors=0;
   for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);
    if((String(m.name)!=='handleGameEvent'&&String(m.name)!=='m_7616_')
      ||String(m.desc)!=='(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V')continue;
    for(var insn=m.instructions.getFirst();insn!==null;insn=insn.getNext()){
     if(insn.getOpcode()===O.NEW&&String(insn.desc)===original){
      insn.desc=classic;allocations++;
     }else if(insn.getOpcode()===O.INVOKESPECIAL&&String(insn.owner)===original
       &&String(insn.name)==='<init>'&&String(insn.desc)==='(ZLjava/lang/Runnable;)V'){
      insn.owner=classic;constructors++;
     }
    }
   }
   if(allocations!==1||constructors!==1)throw new Error('2280 WinScreen allocation/constructor mismatch');
   return node;
  }
 }};
}

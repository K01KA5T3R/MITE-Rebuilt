/** Retain native movement, water sound and game events; remove only the later muffled layer. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 function previous(n){do{n=n.getPrevious();}while(n!==null&&n.getOpcode()<0);return n;}
 return {source_player_water_step1366:{target:{type:'CLASS',name:'net.minecraft.world.entity.player.Player'},transformer:function(node){var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V'||(String(m.name)!=='playStepSound'&&String(m.name)!==String(A.mapMethod('m_7355_'))))continue;
   var code=m.instructions.toArray();for(var j=0;j<code.length;j++){var c=code[j];if(!(c instanceof C)||c.getOpcode()!==O.INVOKEVIRTUAL||String(c.owner)!=='net/minecraft/world/entity/player/Player'||String(c.name)!=='playMuffledStepSound'||String(c.desc)!=='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V')continue;
    var pos=previous(c),state=previous(pos),self=previous(state),swim=previous(self);if(!(pos instanceof V)||pos.getOpcode()!==O.ALOAD||pos.var!==1||!(state instanceof V)||state.getOpcode()!==O.ALOAD||state.var!==2||!(self instanceof V)||self.getOpcode()!==O.ALOAD||self.var!==0||!(swim instanceof C)||String(swim.owner)!=='net/minecraft/world/entity/player/Player'||String(swim.desc)!=='()V'||(String(swim.name)!=='waterSwimSound'&&String(swim.name)!==String(A.mapMethod('m_280447_'))))throw new Error('Unexpected player water step layer');
    m.instructions.remove(pos);m.instructions.remove(state);m.instructions.remove(self);m.instructions.remove(c);count++;
   }
  }if(count!==1)throw new Error('Expected one player muffled water layer, got '+count);return node;
 }}};
}

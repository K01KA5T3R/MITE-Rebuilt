function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes');
 return {mite_source_animal_spawn_phase:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerChunkCache'},transformer:function(node){
  var count=0;for(var i=0;i<node.methods.size();i++){var method=node.methods.get(i);if((String(method.name)!=='tickChunks'&&String(method.name)!=='m_8490_')||String(method.desc)!=='()V')continue;
   for(var it=method.instructions.iterator();it.hasNext();){var x=it.next();if(String(x.owner)!=='net/minecraft/world/level/storage/LevelData'||String(x.desc)!=='()J'||(String(x.name)!=='getGameTime'&&String(x.name)!=='m_6793_'))continue;
    var next=x.getNext();while(next!==null&&next.getOpcode()<0)next=next.getNext();if(next===null||String(next.cst)!=='400')continue;var remainder=next.getNext();while(remainder!==null&&remainder.getOpcode()<0)remainder=remainder.getNext();if(remainder===null||remainder.getOpcode()!==O.LREM)continue;
    x.name=String(x.name)==='getGameTime'?'getDayTime':'m_6792_';count++;
   }
  }if(count!==1)throw new Error('Native animal dayTime400 phase sites '+count);return node;
 }}};
}

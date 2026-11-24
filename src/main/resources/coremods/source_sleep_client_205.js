function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 return {source_sleep_rain_volume205:{target:{type:'CLASS',name:'net.minecraft.client.renderer.LevelRenderer'},transformer:function(node){var count=0;
  for(var n=0;n<node.methods.size();n++){var m=node.methods.get(n);if((String(m.name)!=='tickRain'&&String(m.name)!=='m_109693_')||String(m.desc)!=='(Lnet/minecraft/client/Camera;)V')continue;
   var a=m.instructions.toArray();for(var k=0;k<a.length;k++){var x=a[k];if(x instanceof C&&x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/client/multiplayer/ClientLevel'&&String(x.desc)==='(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V'){
    m.instructions.set(x,new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceSleepClient','rainSound','(Ljava/lang/Object;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V',false));count++;
   }}
  }if(count!==2)throw new Error('Source sleep rain-volume consumers '+count);return node;
 }}};
}

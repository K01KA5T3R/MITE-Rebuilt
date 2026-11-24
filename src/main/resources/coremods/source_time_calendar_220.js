// Observe completed authoritative clock operations; no native clock is replaced.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList');
 var helper='dev/koikaster/mite/SourceFastForwardTimeline220';
 return {
  source_calendar_after_clock220:{target:{type:'CLASS',name:'dev.koikaster.mite.SourceDimensionClock'},transformer:function(node){
   var methods=0,count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='tick'||String(m.desc)!=='(Ljava/lang/Object;)Z')continue;methods++;var code=m.instructions.toArray();for(var j=0;j<code.length;j++)if(code[j].getOpcode()===O.IRETURN){var extra=new L();extra.add(new V(O.ALOAD,0));extra.add(new C(O.INVOKESTATIC,helper,'afterClock','(ZLjava/lang/Object;)Z',false));m.instructions.insertBefore(code[j],extra);count++;}m.maxStack=Math.max(m.maxStack,2);}
   if(methods!==1||count!==3)throw new Error('Exact dimension-clock return boundaries required: '+methods+'/'+count);return node;
  }},
  source_calendar_after_set220:{target:{type:'CLASS',name:'dev.koikaster.mite.SourceDimensionLevelData'},transformer:function(node){
   var methods=0,count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)!=='setDayTime'&&String(m.name)!=='m_6247_')||String(m.desc)!=='(J)V')continue;methods++;var code=m.instructions.toArray();for(var j=0;j<code.length;j++)if(code[j].getOpcode()===O.RETURN){var extra=new L();extra.add(new V(O.ALOAD,0));extra.add(new C(O.INVOKESTATIC,helper,'calendarChanged','(Ljava/lang/Object;)V',false));m.instructions.insertBefore(code[j],extra);count++;}m.maxStack=Math.max(m.maxStack,1);}
   if(methods!==1||count!==1)throw new Error('Exact native calendar-set return boundary required: '+methods+'/'+count);return node;
  }}
 };
}

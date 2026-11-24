function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),
     C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),
     B=Java.type('org.objectweb.asm.tree.LabelNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),
     V=Java.type('org.objectweb.asm.tree.VarInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode');
 var instance='net/minecraft/client/resources/sounds/SoundInstance';
 var sample='Lnet/minecraft/client/resources/sounds/Sound;';
 var bridge='dev/koikaster/mite/ClassicMusicClient';

 function matches(node, official, srg, descriptor){
  return (String(node.name)===official||String(node.name)===srg)&&String(node.desc)===descriptor;
 }
 function adjacent(node, backwards){
  var cursor=backwards?node.getPrevious():node.getNext();
  while(cursor!==null&&cursor.getOpcode()<0)cursor=backwards?cursor.getPrevious():cursor.getNext();
  return cursor;
 }
 function matchesLocal(node, opcode, slot){
  return node instanceof V&&node.getOpcode()===opcode&&node.var===slot;
 }
 function emptySampleBranch(start, selected, official, srg, opcode){
  var load=adjacent(start,false);
  if(!matchesLocal(load,O.ALOAD,selected))throw new Error('Missing selected Sound load in the native empty check');
  var field=adjacent(load,false);
  if(!(field instanceof F)||field.getOpcode()!==O.GETSTATIC||String(field.owner)!=='net/minecraft/client/sounds/SoundManager'||
      !matches(field,official,srg,sample))throw new Error('Missing native empty Sound field '+official);
  var branch=adjacent(field,false);
  if(!(branch instanceof J)||branch.getOpcode()!==opcode)throw new Error('Unexpected native empty Sound branch '+official);
  return branch;
 }
 function guardSelectedSample(node){
  var methods=[];
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i);
   if(matches(m,'play','m_120312_','(L'+instance+';)V'))methods.push(m);
  }
  if(methods.length!==1)throw new Error('Expected one SoundEngine playback entry, found '+methods.length);
  var play=methods[0],resolve=null,readSample=null,readVolume=null;
  for(var iterator=play.instructions.iterator();iterator.hasNext();){
   var insn=iterator.next();
   if(!(insn instanceof C)||insn.getOpcode()!==O.INVOKEINTERFACE||String(insn.owner)!==instance)continue;
   if(matches(insn,'resolve','m_6775_','(Lnet/minecraft/client/sounds/SoundManager;)Lnet/minecraft/client/sounds/WeighedSoundEvents;')){
    if(resolve!==null)throw new Error('Unexpected duplicate native Sound resolution');
    resolve=insn;
   }else if(matches(insn,'getSound','m_5891_','()'+sample)){
    if(resolve===null||readSample!==null)throw new Error('Unexpected selected Sound read order');
    readSample=insn;
   }else if(matches(insn,'getVolume','m_7769_','()F')){
    if(readSample===null||readVolume!==null)throw new Error('Unexpected native Sound volume read order');
    readVolume=insn;
   }
  }
  if(readSample===null||readVolume===null)throw new Error('Missing resolved Sound locals in native playback');
  var actual=adjacent(readSample,true),selected=adjacent(readSample,false),volume=adjacent(readVolume,true);
  if(!(actual instanceof V)||actual.getOpcode()!==O.ALOAD||
      !(selected instanceof V)||selected.getOpcode()!==O.ASTORE||
      !matchesLocal(volume,O.ALOAD,actual.var))throw new Error('Unexpected resolved Sound stack shape');

  var silent=emptySampleBranch(selected,selected.var,'INTENTIONALLY_EMPTY_SOUND','f_271451_',O.IF_ACMPEQ);
  var missing=emptySampleBranch(silent,selected.var,'EMPTY_SOUND','f_120344_',O.IF_ACMPNE);
  var resume=adjacent(missing.label,false),exit=adjacent(silent.label,false);
  if(resume===null||!resume.equals(volume)||exit===null||exit.getOpcode()!==O.RETURN)
   throw new Error('Unexpected native Sound playback continuation');

  var gate=new L();
  gate.add(new V(O.ALOAD,actual.var));
  gate.add(new V(O.ALOAD,selected.var));
  gate.add(new C(O.INVOKESTATIC,bridge,'allowsResolved','(Ljava/lang/Object;Ljava/lang/Object;)Z',false));
  gate.add(new J(O.IFEQ,silent.label));
  play.instructions.insertBefore(volume,gate);
  play.maxStack=Math.max(play.maxStack,2);
  return node;
 }
 return {'mite_classic_music':{target:{type:'CLASS',name:'net.minecraft.client.sounds.MusicManager'},transformer:function(node){
  var count=0;
  for(var i=0;i<node.methods.size();i++)for(var insn=node.methods.get(i).instructions.getFirst();insn!==null;insn=insn.getNext()){
   if(insn.getOpcode()===O.INVOKEVIRTUAL&&String(insn.owner)==='net/minecraft/client/Minecraft'&&String(insn.desc)==='()Lnet/minecraft/sounds/Music;'){
    insn.setOpcode(O.INVOKESTATIC);insn.owner='dev/koikaster/mite/ClassicMusicClient';insn.name='situationalMusic';insn.desc='(Ljava/lang/Object;)Lnet/minecraft/sounds/Music;';insn.itf=false;count++;
   }
  }
  if(count!==1)throw new Error('Unexpected MusicManager situational selector count '+count);
  var guards=0;
  for(var j=0;j<node.methods.size();j++){
   var m=node.methods.get(j);
   if((String(m.name)!=='tick'&&String(m.name)!=='m_120183_')||String(m.desc)!=='()V')continue;
   var resume=new B(),guard=new L();
   guard.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/ClassicMusicClient','blocksOrdinaryMusic','()Z',false));
   guard.add(new J(O.IFEQ,resume));guard.add(new I(O.RETURN));guard.add(resume);
   m.instructions.insert(guard);guards++;
  }
  if(guards!==1)throw new Error('Unexpected MusicManager tick count '+guards);return node;
 }},
 'mite_classic_resolved_music':{
  target:{type:'CLASS',name:'net.minecraft.client.sounds.SoundEngine'},
  transformer:guardSelectedSample
 }};
}

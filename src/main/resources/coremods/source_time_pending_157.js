// Lift the literal native scheduled-block/fluid span after historical catch-up.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 function next(n){do{n=n.getNext();}while(n!==null&&n.getOpcode()<0);return n;}
 function previous(n){do{n=n.getPrevious();}while(n!==null&&n.getOpcode()<0);return n;}
 return {source_pending_after_forward_157:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerLevel'},transformer:function(node){
  var owner=String(node.name),flag='mite$pendingRandomCatchup',time='mite$pendingRandomCatchupTime',method='mite$runPendingBlockUpdates';
  for(var i=0;i<node.fields.size();i++)if(String(node.fields.get(i).name)===flag||String(node.fields.get(i).name)===time)throw new Error('Pending bridge field collision');
  var tick=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)===method)throw new Error('Pending bridge method collision');if((String(m.name)==='tick'||String(m.name)===String(A.mapMethod('m_8793_')))&&String(m.desc)==='(Ljava/util/function/BooleanSupplier;)V')tick=m;}
  if(tick===null)throw new Error('Exact native level tick required');
  var calls=[],getProfiler=null,code=tick.instructions.toArray();for(var i=0;i<code.length;i++){var n=code[i];if(n instanceof C&&String(n.owner)==='net/minecraft/world/ticks/LevelTicks'&&String(n.desc)==='(JILjava/util/function/BiConsumer;)V')calls.push(n);if(n instanceof C&&String(n.desc)==='()Lnet/minecraft/util/profiling/ProfilerFiller;')getProfiler=n;}
  if(calls.length!==2||getProfiler===null)throw new Error('Exact native block/fluid scheduled calls required');
  var store=previous(calls[0]);while(store!==null&&!(store instanceof V&&store.getOpcode()===O.LSTORE))store=previous(store);
  if(store===null)throw new Error('Native pending game-time local missing');var read=previous(store);if(!(read instanceof C)||String(read.desc)!=='()J'||(String(read.name)!=='getGameTime'&&String(read.name)!==String(A.mapMethod('m_46467_'))))throw new Error('Native scheduling time capture must stay intact');
  var start=next(store),last=next(next(calls[1]));if(!(start instanceof V)||start.getOpcode()!==O.ALOAD||!(last instanceof C)||String(last.owner)!=='net/minecraft/util/profiling/ProfilerFiller'||String(last.desc)!=='()V')throw new Error('Exact native profiler-bounded scheduled span required');
  var profilerLocal=start.var,timeLocal=store.var,real=[],end=last.getNext();for(var n=start;n!==end;n=n.getNext()){if(n instanceof Frame)throw new Error('Scheduled span must be straight-line native code');if(n.getOpcode()<0)continue;if(n instanceof Jump||n.getOpcode()===O.ATHROW||n.getOpcode()===O.RETURN)throw new Error('No branch may leave the scheduled span');if(n instanceof V&&n.var!==0&&n.var!==profilerLocal&&n.var!==timeLocal)throw new Error('Unexpected native pending local');real.push(n);}
  if(real.length!==22)throw new Error('Exact22 native scheduled instructions required: '+real.length);
  node.fields.add(new Field(O.ACC_PRIVATE|O.ACC_TRANSIENT,flag,'Z',null,null));node.fields.add(new Field(O.ACC_PRIVATE|O.ACC_TRANSIENT,time,'J',null,null));
  var moved=new M(O.ACC_PUBLIC,method,'()V',null,null),a=moved.instructions,done=new Label();a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,owner,flag,'Z'));a.add(new Jump(O.IFEQ,done));a.add(new V(O.ALOAD,0));a.add(new I(O.ICONST_0));a.add(new F(O.PUTFIELD,owner,flag,'Z'));
  a.add(new V(O.ALOAD,0));a.add(new C(getProfiler.getOpcode(),String(getProfiler.owner),String(getProfiler.name),String(getProfiler.desc),getProfiler.itf));a.add(new V(O.ASTORE,profilerLocal));a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,owner,time,'J'));a.add(new V(O.LSTORE,timeLocal));
  for(var i=0;i<real.length;i++)a.add(real[i].clone(new java.util.HashMap()));a.add(new I(O.RETURN));a.add(done);a.add(new Frame(O.F_SAME,0,null,0,null));a.add(new I(O.RETURN));moved.maxStack=tick.maxStack;moved.maxLocals=Math.max(profilerLocal+1,timeLocal+2);node.methods.add(moved);node.interfaces.add('dev/koikaster/mite/SourcePendingTickAccess');
  var queue=new L();queue.add(new V(O.ALOAD,0));queue.add(new V(O.LLOAD,timeLocal));queue.add(new F(O.PUTFIELD,owner,time,'J'));queue.add(new V(O.ALOAD,0));queue.add(new I(O.ICONST_1));queue.add(new F(O.PUTFIELD,owner,flag,'Z'));tick.instructions.insert(store,queue);
  // Keep native labels,frames and debug scopes in the original method.
  for(var i=0;i<real.length;i++)tick.instructions.remove(real[i]);
  var reset=new L();reset.add(new V(O.ALOAD,0));reset.add(new I(O.ICONST_0));reset.add(new F(O.PUTFIELD,owner,flag,'Z'));tick.instructions.insert(reset);tick.maxStack=Math.max(tick.maxStack,3);return node;
 }}};
}

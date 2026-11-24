function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),T=Java.type('org.objectweb.asm.tree.TypeInsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),L=Java.type('org.objectweb.asm.tree.LabelNode'),Frame=Java.type('org.objectweb.asm.tree.FrameNode');
 var P='dev/koikaster/mite/SourcePhasePositionUpdates',E='net/minecraft/world/entity/Entity',S='net/minecraft/server/level/ServerEntity',K='net/minecraft/server/level/ChunkMap',D=K+'$TrackedEntity';
 function field(node,official,srg,desc){var match=null;for(var x=0;x<node.fields.size();x++){var f=node.fields.get(x);if((String(f.name)===official||String(f.name)===srg)&&String(f.desc)===desc){if(match!==null)throw new Error('Ambiguous Phase field '+official);match=String(f.name);}}if(match===null)throw new Error('Missing Phase field '+official);return match;}
 function fresh(node,iface,desc){if(node.interfaces.contains(iface))throw new Error('Duplicate Phase bridge');for(var x=0;x<node.methods.size();x++)if(String(node.methods.get(x).name)==='mite$phasePosition')throw new Error('Duplicate Phase bridge method');node.interfaces.add(iface);var m=new M(O.ACC_PUBLIC|O.ACC_FINAL,'mite$phasePosition',desc,null,null);node.methods.add(m);return m;}
 return {
  mite_phase_tracker_lookup:{target:{type:'CLASS',name:'net.minecraft.server.level.ChunkMap'},transformer:function(node){
   var map=field(node,'entityMap','f_140150_','Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;'),srg=map!=='entityMap';
   var m=fresh(node,P+'$Lookup','(L'+E+';)V'),a=m.instructions,end=new L();
   a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,K,map,'Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;'));a.add(new V(O.ALOAD,1));a.add(new C(O.INVOKEVIRTUAL,E,srg?'m_19879_':'getId','()I',false));a.add(new C(O.INVOKEINTERFACE,'it/unimi/dsi/fastutil/ints/Int2ObjectMap','get','(I)Ljava/lang/Object;',true));a.add(new T(O.CHECKCAST,D));a.add(new V(O.ASTORE,2));a.add(new V(O.ALOAD,2));a.add(new J(O.IFNULL,end));
   a.add(new V(O.ALOAD,2));a.add(new F(O.GETFIELD,D,srg?'f_140471_':'serverEntity','L'+S+';'));a.add(new T(O.CHECKCAST,P+'$Tracked'));a.add(new C(O.INVOKEINTERFACE,P+'$Tracked','mite$phasePosition','()V',true));
   a.add(end);var frame=new Frame(O.F_APPEND,0,null,0,null);frame.local.add(D);a.add(frame);a.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=3;return node;
  }},
  mite_phase_tracker_position:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerEntity'},transformer:function(node){
   var entity=field(node,'entity','f_8510_','L'+E+';'),codec=field(node,'positionCodec','f_214995_','Lnet/minecraft/network/protocol/game/VecDeltaCodec;'),broadcast=field(node,'broadcast','f_8513_','Ljava/util/function/Consumer;');
   var yaw=field(node,'yRotp','f_8517_','I'),pitch=field(node,'xRotp','f_8518_','I'),ground=field(node,'wasOnGround','f_8525_','Z'),delay=field(node,'teleportDelay','f_8522_','I'),srg=entity!=='entity';
   var m=fresh(node,P+'$Tracked','()V'),a=m.instructions;
   a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,S,entity,'L'+E+';'));a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,S,codec,'Lnet/minecraft/network/protocol/game/VecDeltaCodec;'));a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,S,broadcast,'Ljava/util/function/Consumer;'));a.add(new C(O.INVOKESTATIC,P,'synchronize','(L'+E+';Lnet/minecraft/network/protocol/game/VecDeltaCodec;Ljava/util/function/Consumer;)V',false));
   function rotation(target,getter){a.add(new V(O.ALOAD,0));a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,S,entity,'L'+E+';'));a.add(new C(O.INVOKEVIRTUAL,E,getter,'()F',false));a.add(new C(O.INVOKESTATIC,P,'rotation','(F)I',false));a.add(new F(O.PUTFIELD,S,target,'I'));}
   rotation(yaw,srg?'m_146908_':'getYRot');rotation(pitch,srg?'m_146909_':'getXRot');
   a.add(new V(O.ALOAD,0));a.add(new V(O.ALOAD,0));a.add(new F(O.GETFIELD,S,entity,'L'+E+';'));a.add(new C(O.INVOKEVIRTUAL,E,srg?'m_20096_':'onGround','()Z',false));a.add(new F(O.PUTFIELD,S,ground,'Z'));a.add(new V(O.ALOAD,0));a.add(new I(O.ICONST_0));a.add(new F(O.PUTFIELD,S,delay,'I'));a.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=1;return node;
  }}
 };
}

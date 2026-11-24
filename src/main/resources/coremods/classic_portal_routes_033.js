function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Method=Java.type('org.objectweb.asm.tree.MethodNode'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),IntInsn=Java.type('org.objectweb.asm.tree.IntInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var P='dev/koikaster/mite/';
 function match(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function field(node,a,b,d){for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if((String(f.name)===a||String(f.name)===b)&&String(f.desc)===d)return String(f.name);}throw new Error('Missing portal field '+a);}
 function getter(node,method,a,b,d){var name=field(node,a,b,d),m=new Method(O.ACC_PUBLIC,method,'()'+d,null,null);m.instructions.add(new Var(O.ALOAD,0));m.instructions.add(new Field(O.GETFIELD,node.name,name,d));m.instructions.add(new Insn(d==='I'?O.IRETURN:O.ARETURN));m.maxStack=1;m.maxLocals=1;node.methods.add(m);}
 function clearContact(node){
  var m=new Method(O.ACC_PUBLIC,'mite$clearPortalContact','()V',null,null);
  m.instructions.add(new Var(O.ALOAD,0));
  m.instructions.add(new Insn(O.ICONST_0));
  m.instructions.add(new Field(O.PUTFIELD,node.name,field(node,'isInsidePortal','f_19817_','Z'),'Z'));
  m.instructions.add(new Var(O.ALOAD,0));
  m.instructions.add(new Insn(O.ICONST_0));
  m.instructions.add(new Field(O.PUTFIELD,node.name,field(node,'portalTime','f_19818_','I'),'I'));
  m.instructions.add(new Insn(O.RETURN));
  m.maxStack=2;m.maxLocals=1;node.methods.add(m);
 }
 function replace(m,owner,name,desc,args,result){m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();for(var i=0;i<args.length;i++)m.instructions.add(new Var(O.ALOAD,args[i]));m.instructions.add(new Call(O.INVOKESTATIC,P+owner,name,desc,false));m.instructions.add(new Insn(result));m.maxStack=Math.max(8,args.length);}
 return {
  mite_portal_block_state_and_contact_033:{target:{type:'CLASS',name:'net.minecraft.world.level.block.NetherPortalBlock'},transformer:function(node){var state=0,contact=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(match(m,'createBlockStateDefinition','m_7926_','(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V')){var c=new List();c.add(new Var(O.ALOAD,1));c.add(new Call(O.INVOKESTATIC,P+'SourcePortalState','addProperty','(Ljava/lang/Object;)V',false));m.instructions.insert(c);m.maxStack=Math.max(8,m.maxStack);state++;}else if(match(m,'entityInside','m_7892_','(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V')){replace(m,'SourcePortalTravel','contact','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',[1,2,3,4],O.RETURN);contact++;}}if(state!==1||contact!==1)throw new Error('Portal block sites '+state+'/'+contact);return node;}},
  mite_portal_shape_and_creation_033:{target:{type:'CLASS',name:'net.minecraft.world.level.portal.PortalShape'},transformer:function(node){node.interfaces.add(P+'SourcePortalShapeAccess');getter(node,'mite$portalLevel','level','f_77686_','Lnet/minecraft/world/level/LevelAccessor;');getter(node,'mite$portalBottom','bottomLeft','f_77690_','Lnet/minecraft/core/BlockPos;');getter(node,'mite$portalRight','rightDir','f_77688_','Lnet/minecraft/core/Direction;');getter(node,'mite$portalAxis','axis','f_77687_','Lnet/minecraft/core/Direction$Axis;');getter(node,'mite$portalWidth','width','f_77692_','I');getter(node,'mite$portalHeight','height','f_77691_','I');var valid=0,create=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(match(m,'isValid','m_77698_','()Z')){replace(m,'SourcePortalFrames','valid','(Ljava/lang/Object;)Z',[0],O.IRETURN);valid++;}else if(match(m,'createPortalBlocks','m_77743_','()V')){replace(m,'SourcePortalFrames','create','(Ljava/lang/Object;)V',[0],O.RETURN);create++;}}if(valid!==1||create!==1)throw new Error('Portal shape sites '+valid+'/'+create);return node;}},
  mite_portal_entity_route_033:{target:{type:'CLASS',name:'net.minecraft.world.entity.Entity'},transformer:function(node){
   node.interfaces.add(P+'SourcePortalEntityAccess');
   getter(node,'mite$portalEntrance','portalEntrancePos','f_19819_','Lnet/minecraft/core/BlockPos;');
   getter(node,'mite$portalWarmup','portalTime','f_19818_','I');
   clearContact(node);
   var route=0,delay=0,contact=0,warmup=0;
   for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);
    if(match(m,'handleNetherPortal','m_20157_','()V')){
     for(var it=m.instructions.iterator();it.hasNext();){
      var x=it.next();
      if(x.getOpcode()===O.GETFIELD&&String(x.owner)===String(node.name)&&String(x.desc)==='Z'
          &&(String(x.name)==='isInsidePortal'||String(x.name)==='f_19817_')){
       var gate=new List();
       gate.add(new Var(O.ALOAD,0));
       gate.add(new Call(O.INVOKESTATIC,P+'SourcePortalTravel','allowContact','(ZLnet/minecraft/world/entity/Entity;)Z',false));
       m.instructions.insert(x,gate);
       contact++;
      }
      if(x.getOpcode()!==O.INVOKEVIRTUAL||(String(x.name)!=='changeDimension'&&String(x.name)!=='m_5489_')
          ||String(x.desc)!=='(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;')continue;
      x.setOpcode(O.INVOKESTATIC);
      x.owner=P+'SourcePortalTravel';
      x.name='change';
      x.desc='(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;';
      x.itf=false;
      route++;
     }
     m.maxStack++;
    }else if(match(m,'handleInsidePortal','m_20221_','(Lnet/minecraft/core/BlockPos;)V')){
     var inside=field(node,'isInsidePortal','f_19817_','Z');
     for(var j=0,body=m.instructions.toArray();j<body.length;j++){
      var x=body[j];
      if(x.getOpcode()!==O.PUTFIELD||String(x.owner)!==String(node.name)||String(x.name)!==inside||String(x.desc)!=='Z')continue;
      var warm=new List();
      warm.add(new Var(O.ALOAD,0));
      warm.add(new Call(O.INVOKESTATIC,P+'SourcePortalTravel','trackWarmup','(Lnet/minecraft/world/entity/Entity;)V',false));
      m.instructions.insert(x,warm);
      warmup++;
     }
     m.maxStack=Math.max(m.maxStack,1);
    }else if(match(m,'getDimensionChangingDelay','m_6045_','()I')){
     m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();
     m.instructions.add(new IntInsn(O.SIPUSH,900));m.instructions.add(new Insn(O.IRETURN));m.maxStack=1;
     delay++;
    }
   }
   if(route!==1||delay!==1||contact!==1||warmup!==1)throw new Error('Portal native route/delay/contact/warmup '+route+'/'+delay+'/'+contact+'/'+warmup);
   return node;
  }}
 };
}

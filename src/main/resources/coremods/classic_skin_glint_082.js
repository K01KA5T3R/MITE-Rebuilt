function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode');
 function name(m,official,srg){return String(m.name)===official||String(m.name)===String(A.mapMethod(srg));}
 return {
  mite_classic_fallback_player_082:{target:{type:'CLASS',name:'net.minecraft.client.resources.DefaultPlayerSkin'},transformer:function(node){
   var methods=[];for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i),d=String(m.desc);if(d==='()Lnet/minecraft/resources/ResourceLocation;'&&name(m,'getDefaultSkin','m_118626_')||d==='(Ljava/util/UUID;)Lnet/minecraft/resources/ResourceLocation;'&&name(m,'getDefaultSkin','m_118627_')||d==='(Ljava/util/UUID;)Ljava/lang/String;'&&name(m,'getSkinModelName','m_118629_'))methods.push(m);}
   if(methods.length!==3)return node;
   for(var i=0;i<methods.length;i++){var m=methods[i],model=String(m.desc)==='(Ljava/util/UUID;)Ljava/lang/String;',end=new Label(),code=new List();code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicPlayerAppearance',model?'fallbackModel':'fallbackTexture',model?'()Ljava/lang/String;':'()Lnet/minecraft/resources/ResourceLocation;',false));code.add(new Insn(O.DUP));code.add(new Jump(O.IFNULL,end));code.add(new Insn(O.ARETURN));code.add(end);code.add(new Insn(O.POP));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,2);}return node;
  }},
  mite_classic_dual_glint_082:{target:{type:'CLASS',name:'net.minecraft.client.renderer.entity.ItemRenderer'},transformer:function(node){
   var methods=[],desc='(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;';
   for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!==desc)continue;var factory=name(m,'getFoilBuffer','m_115211_')?0:name(m,'getFoilBufferDirect','m_115222_')?1:name(m,'getArmorFoilBuffer','m_115184_')?2:-1;if(factory>=0)methods.push({method:m,factory:factory});}
   if(methods.length!==3)return node;
   for(var i=0;i<methods.length;i++){var m=methods[i].method,returns=[];for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.ARETURN)returns.push(x);}
    for(var j=0;j<returns.length;j++){var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,1));code.add(new Var(O.ILOAD,2));code.add(new Var(O.ILOAD,3));code.add(new Insn(O.ICONST_0+methods[i].factory));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicGlint','consumer','(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZI)Lcom/mojang/blaze3d/vertex/VertexConsumer;',false));m.instructions.insertBefore(returns[j],code);}m.maxStack=Math.max(m.maxStack,6);
   }return node;
  }}
 };
}

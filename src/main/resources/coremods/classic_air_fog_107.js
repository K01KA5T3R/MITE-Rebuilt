// Set optional air fog uniforms only after the native shader has been applied.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 return {classic_air_fog_107:{target:{type:'CLASS',name:'net.minecraft.client.renderer.ShaderInstance'},transformer:function(node){
  var methods=[];
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i),name=String(m.name);
   if((name==='apply'||name===String(A.mapMethod('m_173363_')))&&String(m.desc)==='()V'&&(m.access&O.ACC_STATIC)===0)methods.push(m);
  }
  if(methods.length!==1){A.log('WARN','Classic air fog: unsupported shader binding; keeping native fog.');return node;}
  var method=methods[0],returns=[];
  for(var n=method.instructions.getFirst();n!==null;n=n.getNext())if(n.getOpcode()===O.RETURN)returns.push(n);
  if(returns.length!==1){A.log('WARN','Classic air fog: unsupported shader return; keeping native fog.');return node;}
  var code=new L();code.add(new V(O.ALOAD,0));code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicAirFogShader','applied','(Ljava/lang/Object;)V',false));method.instructions.insertBefore(returns[0],code);method.maxStack=Math.max(method.maxStack,1);return node;
 }} };
}

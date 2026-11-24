// Adapt only the block atlas mip request. Native blur handling and GL state updates stay intact.
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),M=Java.type('org.objectweb.asm.tree.MethodInsnNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 return {classic_texture_sampling_103:{target:{type:'CLASS',name:'net.minecraft.client.renderer.texture.AbstractTexture'},transformer:function(node){
  var methods=[];
  for(var i=0;i<node.methods.size();i++){
   var m=node.methods.get(i),name=String(m.name);
   if((name==='setFilter'||name===String(A.mapMethod('m_117960_')))&&String(m.desc)==='(ZZ)V'&&(m.access&O.ACC_STATIC)===0)methods.push(m);
  }
  if(methods.length!==1){A.log('WARN','Classic textures: unsupported texture filter; keeping native sampling.');return node;}
  var code=new L();code.add(new V(O.ILOAD,2));code.add(new V(O.ALOAD,0));code.add(new M(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicTextureSampling','mipmap','(ZLjava/lang/Object;)Z',false));code.add(new V(O.ISTORE,2));methods[0].instructions.insert(code);methods[0].maxStack=Math.max(methods[0].maxStack,2);return node;
 }} };
}

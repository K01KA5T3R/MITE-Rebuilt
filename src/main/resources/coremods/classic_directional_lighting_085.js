function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode');
 return {mite_classic_nether_diffuse_085:{target:{type:'CLASS',name:'com.mojang.blaze3d.platform.Lighting'},transformer:function(node){
  var found=null;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)==='(Lorg/joml/Matrix4f;)V'&&(String(m.name)==='setupNetherLevel'||String(m.name)===String(A.mapMethod('m_252995_')))){if(found!==null)return node;found=m;}}
  if(found===null)return node;var code=new List(),body=new Label();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,'dev/koikaster/mite/visual/ClassicDirectionalLighting','setupLegacyNether','(Lorg/joml/Matrix4f;)Z',false));code.add(new Jump(O.IFEQ,body));code.add(new Insn(O.RETURN));code.add(body);found.instructions.insert(code);found.maxStack=Math.max(found.maxStack,1);return node;
 }}};
}

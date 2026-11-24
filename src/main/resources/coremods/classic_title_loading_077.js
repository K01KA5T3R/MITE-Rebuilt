function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var Insn=Java.type('org.objectweb.asm.tree.InsnNode'),List=Java.type('org.objectweb.asm.tree.InsnList'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
 var Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode');
 var renderDesc='(Lnet/minecraft/client/gui/GuiGraphics;IIF)V',loading='net/minecraft/client/gui/screens/LoadingOverlay',client='dev/koikaster/mite/ClientResourceLoading';
 function render(m){return String(m.desc)===renderDesc&&(String(m.name)==='render'||String(m.name)===String(A.mapMethod('m_88315_')));}
 function clear(m){m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();}
 return {
  mite_no_resource_loading_screen_077:{target:{type:'CLASS',name:'net.minecraft.client.gui.screens.LoadingOverlay'},transformer:function(node){
   var minecraft=null,reload=null,finish=null,count=0;
   for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i),d=String(f.desc);if(d==='Lnet/minecraft/client/Minecraft;')minecraft=f;if(d==='Lnet/minecraft/server/packs/resources/ReloadInstance;')reload=f;if(d==='Ljava/util/function/Consumer;')finish=f;}
   if(minecraft===null||reload===null||finish===null)throw new Error('077 loading lifecycle fields changed');
   for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);if(!render(m))continue;clear(m);var code=m.instructions;code.add(new Var(O.ALOAD,0));
    var fields=[minecraft,reload,finish];for(var k=0;k<fields.length;k++){code.add(new Var(O.ALOAD,0));code.add(new Field(O.GETFIELD,loading,String(fields[k].name),String(fields[k].desc)));}
    code.add(new Var(O.ALOAD,1));code.add(new Var(O.ILOAD,2));code.add(new Var(O.ILOAD,3));code.add(new Var(O.FLOAD,4));
    code.add(new Call(O.INVOKESTATIC,client,'render','(Lnet/minecraft/client/gui/screens/LoadingOverlay;Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Lnet/minecraft/client/gui/GuiGraphics;IIF)V',false));
    code.add(new Insn(O.RETURN));m.maxStack=8;m.maxLocals=5;count++;
   }
   if(count!==1)throw new Error('Expected one resource lifecycle render');return node;
  }},
  mite_no_forge_loading_screen_077:{target:{type:'CLASS',name:'net.minecraftforge.client.loading.ForgeLoadingOverlay'},transformer:function(node){
   var logo=0,renderer=0;
   for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);
    if(String(m.name)==='<init>')for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraftforge/fml/earlydisplay/DisplayWindow'&&String(x.name)==='addMojangTexture'){m.instructions.set(x,new Insn(O.POP2));logo++;}}
    if(!render(m))continue;clear(m);var code=m.instructions,end=new Label();
    code.add(new Var(O.ALOAD,0));code.add(new Var(O.ALOAD,1));code.add(new Var(O.ILOAD,2));code.add(new Var(O.ILOAD,3));code.add(new Var(O.FLOAD,4));code.add(new Call(O.INVOKESPECIAL,loading,String(m.name),renderDesc,false));
    code.add(new Var(O.ALOAD,0));code.add(new Field(O.GETFIELD,String(node.name),'minecraft','Lnet/minecraft/client/Minecraft;'));code.add(new Call(O.INVOKEVIRTUAL,'net/minecraft/client/Minecraft',A.mapMethod('m_91265_'),'()Lnet/minecraft/client/gui/screens/Overlay;',false));
    code.add(new Var(O.ALOAD,0));code.add(new Jump(O.IF_ACMPEQ,end));
    code.add(new Var(O.ALOAD,0));code.add(new Field(O.GETFIELD,String(node.name),'progress','Lnet/minecraftforge/fml/loading/progress/ProgressMeter;'));code.add(new Call(O.INVOKEVIRTUAL,'net/minecraftforge/fml/loading/progress/ProgressMeter','complete','()V',false));
    code.add(new Var(O.ALOAD,0));code.add(new Field(O.GETFIELD,String(node.name),'displayWindow','Lnet/minecraftforge/fml/earlydisplay/DisplayWindow;'));code.add(new Call(O.INVOKEVIRTUAL,'net/minecraftforge/fml/earlydisplay/DisplayWindow','close','()V',false));
    code.add(end);code.add(new Insn(O.RETURN));m.maxStack=5;m.maxLocals=5;renderer++;
   }
   if(logo!==1||renderer!==1)throw new Error('077 Forge lifecycle shape changed');return node;
  }},
  mite_publish_only_ready_window_077:{target:{type:'CLASS',name:'com.mojang.blaze3d.platform.Window'},transformer:function(node){
   var hidden=0,shown=0;
   for(var i=0;i<node.methods.size();i++){
    var m=node.methods.get(i);
    if(String(m.name)==='<init>')for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.PUTFIELD&&String(x.owner)===String(node.name)&&String(x.desc)==='J'){
     var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,client,'hideUntilReady','(Lcom/mojang/blaze3d/platform/Window;)V',false));m.instructions.insert(x,code);hidden++;
    }}
    if(String(m.desc)!=='()V'||!(String(m.name)==='updateDisplay'||String(m.name)===String(A.mapMethod('m_85435_'))))continue;
    for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.RETURN){var code=new List();code.add(new Var(O.ALOAD,0));code.add(new Call(O.INVOKESTATIC,client,'showWhenReady','(Lcom/mojang/blaze3d/platform/Window;)V',false));m.instructions.insertBefore(x,code);shown++;}}
   }
   if(hidden!==1||shown!==1)throw new Error('077 window publication shape changed');return node;
  }}
 };
}

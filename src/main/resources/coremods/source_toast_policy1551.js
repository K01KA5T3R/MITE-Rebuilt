/** Keep operational notifications while removing automatic modern reminders. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),J=Java.type('org.objectweb.asm.tree.JumpInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),N=Java.type('org.objectweb.asm.tree.LabelNode'),F=Java.type('org.objectweb.asm.tree.FrameNode');
 return {source_toast_policy1551:{target:{type:'CLASS',name:'net.minecraft.client.gui.components.toasts.ToastComponent'},transformer:function(node){
  var hits=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)!=='addToast'&&String(m.name)!=='m_94922_')||String(m.desc)!=='(Lnet/minecraft/client/gui/components/toasts/Toast;)V')continue;
   var l=new L(),next=new N();l.add(new V(O.ALOAD,1));l.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/ClientToastPolicy1551','blocked','(Lnet/minecraft/client/gui/components/toasts/Toast;)Z',false));l.add(new J(O.IFEQ,next));l.add(new I(O.RETURN));l.add(next);l.add(new F(O.F_SAME,0,null,0,null));m.instructions.insert(l);m.maxStack=Math.max(m.maxStack,1);hits++;
  }if(hits!==1)throw new Error('Toast1551 expected one admission owner: '+hits);return node;
 }}};}

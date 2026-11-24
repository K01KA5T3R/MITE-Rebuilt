function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),List=Java.type('org.objectweb.asm.tree.InsnList'),Var=Java.type('org.objectweb.asm.tree.VarInsnNode'),Call=Java.type('org.objectweb.asm.tree.MethodInsnNode'),Insn=Java.type('org.objectweb.asm.tree.InsnNode'),Label=Java.type('org.objectweb.asm.tree.LabelNode'),Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode'),Field=Java.type('org.objectweb.asm.tree.FieldInsnNode');
 var H='dev/koikaster/mite/EnchantmentEffects',obj='Ljava/lang/Object;';
 function match(m,a,b,d){return (String(m.name)===a||String(m.name)===b)&&String(m.desc)===d;}
 function load(c,i){c.add(new Var(O.ALOAD,i));}function call(c,n,d){c.add(new Call(O.INVOKESTATIC,H,n,d,false));}
 function returns(m,opcode){var result=[];for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===opcode)result.push(x);}return result;}
 function protectBoneSweep(m){
  var eventCall=null,mineCall=null;
  var eventDescriptor='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)I';
  var mineDescriptor='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V';
  for(var it=m.instructions.iterator();it.hasNext();){
   var x=it.next();
   if(x.getOpcode()===O.INVOKESTATIC&&String(x.owner)==='net/minecraftforge/common/ForgeHooks'
      &&String(x.name)==='onBlockBreakEvent'&&String(x.desc)===eventDescriptor){
    if(eventCall!==null)throw new Error('Multiple Forge block break event calls');
    eventCall=x;
   }
   if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/item/ItemStack'
      &&String(x.desc)===mineDescriptor){
    if(mineCall!==null)throw new Error('Multiple native mining calls');
    mineCall=x;
   }
  }
  if(eventCall===null||mineCall===null)throw new Error('Missing native bone-pickaxe guard sites');
  var afterEvent=new List();call(afterEvent,'checkBoneSweepAfterEvent','(I)I');
  m.instructions.insert(eventCall,afterEvent);

  // Preserve the original virtual call and all five of its reference operands.
  var stack=m.maxLocals,level=stack+1,state=stack+2,pos=stack+3,player=stack+4;
  m.maxLocals+=5;
  var beforeMine=new List(),allowed=new Label();
  for(var local=player;local>=stack;local--)beforeMine.add(new Var(O.ASTORE,local));
  load(beforeMine,stack);load(beforeMine,level);load(beforeMine,pos);load(beforeMine,player);
  call(beforeMine,'mayUseBoneSweepTool','('+obj+obj+obj+obj+')Z');
  beforeMine.add(new Jump(O.IFNE,allowed));
  beforeMine.add(new Insn(O.ICONST_0));
  beforeMine.add(new Insn(O.IRETURN));
  beforeMine.add(allowed);
  for(var local=stack;local<=player;local++)load(beforeMine,local);
  m.instructions.insertBefore(mineCall,beforeMine);
 }
 return {
  'mite_source_enchant_potion_movement':{target:{type:'CLASS',name:'net.minecraft.world.effect.MobEffect'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(!match(m,'addAttributeModifiers','m_6385_','(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V')&&!match(m,'removeAttributeModifiers','m_6386_','(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V'))continue;var exits=returns(m,O.RETURN);for(var j=0;j<exits.length;j++){var c=new List();load(c,1);call(c,'refreshMovement','('+obj+')V');m.instructions.insertBefore(exits[j],c);count++;}m.maxStack=Math.max(16,m.maxStack);}if(count!==2)throw new Error('Unexpected potion attribute exits '+count);return node;}},
  'mite_source_enchant_tilling':{target:{type:'CLASS',name:'net.minecraft.world.item.HoeItem'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(!match(m,'useOn','m_6225_','(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;'))continue;var exits=returns(m,O.ARETURN),local=m.maxLocals++,entry=new List(),allowed=new Label();load(entry,1);call(entry,'denyTilling','('+obj+')Z');entry.add(new Jump(O.IFEQ,allowed));entry.add(new Field(O.GETSTATIC,'net/minecraft/world/InteractionResult',String(m.name)==='m_6225_'?'FAIL':'FAIL','Lnet/minecraft/world/InteractionResult;'));entry.add(new Insn(O.ARETURN));entry.add(allowed);load(entry,1);call(entry,'beforeTill','('+obj+')'+obj);entry.add(new Var(O.ASTORE,local));m.instructions.insert(entry);for(var j=0;j<exits.length;j++){var c=new List();load(c,local);load(c,1);call(c,'afterTill','('+obj+obj+obj+')Lnet/minecraft/world/InteractionResult;');m.instructions.insertBefore(exits[j],c);}m.maxStack=Math.max(16,m.maxStack);count++;}if(count!==1)throw new Error('Unexpected tilling source method '+count);return node;}},
  'mite_source_enchant_break_actions':{
   target:{type:'CLASS',name:'net.minecraft.server.level.ServerPlayerGameMode'},
   transformer:function(node){
    var player=null,count=0;
    for(var i=0;i<node.fields.size();i++){
     var f=node.fields.get(i);
     if(String(f.desc)==='Lnet/minecraft/server/level/ServerPlayer;'){
      if(player!==null)throw new Error('Ambiguous game mode player field');
      player=String(f.name);
     }
    }
    if(player===null)throw new Error('No game mode player field');
    for(var i=0;i<node.methods.size();i++){
     var m=node.methods.get(i);
     if(!match(m,'destroyBlock','m_9280_','(Lnet/minecraft/core/BlockPos;)Z'))continue;
     protectBoneSweep(m);
     var entry=new List();
     load(entry,0);entry.add(new Field(O.GETFIELD,String(node.name),player,'Lnet/minecraft/server/level/ServerPlayer;'));
     load(entry,1);call(entry,'beforeDestroy','('+obj+obj+')V');m.instructions.insert(entry);
     var exits=returns(m,O.IRETURN);
     for(var j=0;j<exits.length;j++){
      var c=new List();call(c,'afterDestroy','(Z)Z');m.instructions.insertBefore(exits[j],c);
     }
     m.maxStack=Math.max(16,m.maxStack);
     count++;
    }
    if(count!==1)throw new Error('Unexpected destroy-block source method '+count);
    return node;
   }
  }
 };
}

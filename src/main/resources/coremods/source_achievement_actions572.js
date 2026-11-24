/** Keep native action bodies; expose their actual committed/removed results. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode'),M=Java.type('org.objectweb.asm.tree.MethodNode'),N=Java.type('org.objectweb.asm.tree.IntInsnNode');
 var lifetime='dev/koikaster/mite/SourceSpawnerLifetime',living='Lnet/minecraft/world/entity/LivingEntity;',damage='Lnet/minecraft/world/damagesource/DamageSource;';
 var actions='dev/koikaster/mite/SourceAchievementActions572',motion='dev/koikaster/mite/SourceAchievementMotion572',potion='dev/koikaster/mite/SourceAchievementPotion572';
 function find(n,a,b,d){for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i);if((String(m.name)===a||String(m.name)===b)&&String(m.desc)===d)return m;}throw new Error('Source572 missing '+a+d);}
 function ret(m,op,make){var count=0;for(var x=m.instructions.getFirst();x!==null;){var next=x.getNext();if(x.getOpcode()===op){m.instructions.insertBefore(x,make());count++;}x=next;}if(!count)throw new Error('Source572 missing return');return count;}
 function call(owner,name,desc){return new C(O.INVOKESTATIC,owner,name,desc,false);}
 function spawnerCredit(owner,method,field,opening){
  var count=0;
  for(var x=method.instructions.getFirst();x!==null;){
   var next=x.getNext();
   if(x.getOpcode()===O.PUTFIELD&&String(x.owner)===String(owner)&&String(x.name)===field&&String(x.desc)==='I'){
    var value=x.getPrevious();
    while(value!==null&&value.getOpcode()<0)value=value.getPrevious();
    var expected=opening ? value instanceof N && value.getOpcode()===O.BIPUSH && Number(value.operand)===100
                         : value!==null && value.getOpcode()===O.ISUB;
    if(!expected)throw new Error('Unexpected R195 spawner credit write in '+method.name);
    var input=new L();
    input.add(new V(O.ALOAD,0));
    if(opening)input.add(new V(O.ALOAD,1));
    input.add(call(lifetime,opening?'recordHurt':'tickCredit','('+living+(opening?damage:'')+')V'));
    method.instructions.insert(x,input);
    count++;
   }
   x=next;
  }
  if(count!==(opening?2:1))throw new Error('R195 spawner credit anchors '+method.name+' '+count);
  method.maxStack=Math.max(method.maxStack,opening?2:1);
 }
 return {
  source_achievement_server_actions572:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerPlayer'},transformer:function(n){
   var m=find(n,'awardKillScore','m_5993_','(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/world/damagesource/DamageSource;)V');ret(m,O.RETURN,function(){var l=new L();l.add(new V(O.ALOAD,0));l.add(new V(O.ALOAD,1));l.add(new V(O.ALOAD,3));l.add(call(actions,'killed','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V'));return l;});m.maxStack=Math.max(m.maxStack,3);
   m=find(n,'changeDimension','changeDimension','(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;');ret(m,O.ARETURN,function(){var l=new L();for(var i=0;i<=2;i++)l.add(new V(O.ALOAD,i));l.add(call(actions,'dimensionReturn','(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;'));return l;});m.maxStack=Math.max(m.maxStack,4);return n;
  }},
  source_achievement_committed_death572:{target:{type:'CLASS',name:'net.minecraft.world.entity.LivingEntity'},transformer:function(n){
   var m=find(n,'die','m_6667_','(Lnet/minecraft/world/damagesource/DamageSource;)V'),count=0;
   for(var x=m.instructions.getFirst();x!==null;){var next=x.getNext();if(x.getOpcode()===O.PUTFIELD&&(String(x.name)==='dead'||String(x.name)==='f_20890_')&&String(x.desc)==='Z'){var before=x.getPrevious();while(before!==null&&before.getOpcode()<0)before=before.getPrevious();if(before===null||before.getOpcode()!==O.ICONST_1)throw new Error('Source572 dead commit');var l=new L();l.add(new V(O.ALOAD,0));l.add(new V(O.ALOAD,1));l.add(call(actions,'skeletonDeath','(Ljava/lang/Object;Ljava/lang/Object;)V'));
   l.add(new V(O.ALOAD,0));l.add(call(lifetime,'committedDeath','('+living+')V'));m.instructions.insert(x,l);count++;}x=next;}if(count!==1)throw new Error('Source572 committed death count '+count);m.maxStack=Math.max(m.maxStack,2);
   var hurt=find(n,'hurt','m_6469_','('+damage+'F)Z');
   var field=String(hurt.name)==='hurt'?'lastHurtByPlayerTime':'f_20889_';
   spawnerCredit(n.name,hurt,field,true);
   spawnerCredit(n.name,find(n,'baseTick','m_6075_','()V'),field,false);
   return n;
  }},
  source_achievement_rail572:{target:{type:'CLASS',name:'net.minecraft.world.entity.player.Player'},transformer:function(n){
   var m=find(n,'checkRidingStatistics','m_36387_','(DDD)V'),l=new L();l.add(new V(O.ALOAD,0));for(var i=1;i<=5;i+=2)l.add(new V(O.DLOAD,i));l.add(call(motion,'rail','(Ljava/lang/Object;DDD)V'));m.instructions.insert(l);m.maxStack=Math.max(m.maxStack,7);return n;
  }},
  source_achievement_pig_fall572:{target:{type:'CLASS',name:'net.minecraft.world.entity.animal.Pig'},transformer:function(n){
   var desc='(FFLnet/minecraft/world/damagesource/DamageSource;)Z';for(var j=0;j<n.methods.size();j++){var old=n.methods.get(j);if((String(old.name)==='causeFallDamage'||String(old.name)==='m_142535_')&&String(old.desc)===desc)throw new Error('Source572 unexpected Pig override');}
   var mapped=String(find(n,'boost','m_6746_','()Z').name)!=='boost',name=mapped?'m_142535_':'causeFallDamage',m=new M(O.ACC_PUBLIC,name,desc,null,null);
   m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.FLOAD,1));m.instructions.add(new V(O.FLOAD,2));m.instructions.add(new V(O.ALOAD,3));m.instructions.add(new C(O.INVOKESPECIAL,String(n.superName),name,desc,false));m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.FLOAD,1));m.instructions.add(call(actions,'pigFall','(ZLjava/lang/Object;F)Z'));m.instructions.add(new I(O.IRETURN));m.maxStack=4;m.maxLocals=4;n.methods.add(m);return n;
  }},
  source_achievement_potion572:{target:{type:'CLASS',name:'net.minecraft.world.inventory.BrewingStandMenu$PotionSlot'},transformer:function(n){
   var m=find(n,'onTake','m_142406_','(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V'),mapped=String(m.name)!=='onTake',l=new L();for(var j=0;j<3;j++)l.add(new V(O.ALOAD,j));l.add(call(potion,'take','(Ljava/lang/Object;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;)V'));m.instructions.insert(l);m.maxStack=Math.max(m.maxStack,3);
   var remove=mapped?'m_6201_':'remove',quick=mapped?'m_7169_':'onQuickCraft';
   for(var i=0;i<n.methods.size();i++){var existing=n.methods.get(i);if((String(existing.name)===remove&&String(existing.desc)==='(I)Lnet/minecraft/world/item/ItemStack;')||(String(existing.name)===quick&&String(existing.desc)==='(Lnet/minecraft/world/item/ItemStack;I)V'))throw new Error('Source572 unexpected potion extraction override');}
   m=new M(O.ACC_PUBLIC,remove,'(I)Lnet/minecraft/world/item/ItemStack;',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ILOAD,1));m.instructions.add(new C(O.INVOKESPECIAL,String(n.superName),remove,String(m.desc),false));m.instructions.add(new V(O.ALOAD,0));m.instructions.add(call(potion,'removed','(Lnet/minecraft/world/item/ItemStack;Ljava/lang/Object;)Lnet/minecraft/world/item/ItemStack;'));m.instructions.add(new I(O.ARETURN));m.maxStack=2;m.maxLocals=2;n.methods.add(m);
   m=new M(O.ACC_PROTECTED,quick,'(Lnet/minecraft/world/item/ItemStack;I)V',null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new V(O.ILOAD,2));m.instructions.add(call(potion,'quick','(Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;I)V'));m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new V(O.ILOAD,2));m.instructions.add(new C(O.INVOKESPECIAL,String(n.superName),quick,String(m.desc),false));m.instructions.add(new I(O.RETURN));m.maxStack=3;m.maxLocals=3;n.methods.add(m);return n;
  }}
 };
}

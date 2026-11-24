/** Only legacy world decoding and player-load inputs; no content is registered. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),A=Java.type('net.minecraftforge.coremod.api.ASMAPI');
 var H='dev/koikaster/mite/RemovedLegacyContent';
 function named(m,n,s,d){return String(m.desc)===d&&(String(m.name)===n||String(m.name)===String(A.mapMethod(s)));}
 function player(node,single){var count=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!==(single?'(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;':'(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/nbt/CompoundTag;'))continue;
   var code=m.instructions.toArray();for(var j=0;j<code.length;j++){var c=code[j];if(!(c instanceof C)||c.getOpcode()!==O.INVOKEVIRTUAL||String(c.desc)!=='(Lnet/minecraft/nbt/CompoundTag;)V'||(String(c.name)!=='load'&&String(c.name)!==String(A.mapMethod('m_20258_'))))continue;
    if(String(c.owner)!==(single?'net/minecraft/server/level/ServerPlayer':'net/minecraft/world/entity/player/Player'))throw new Error('Unexpected player load owner '+c.owner);
    var l=new L();l.add(new V(O.ALOAD,1));l.add(new C(O.INVOKESTATIC,H,'player','(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/Object;)Lnet/minecraft/nbt/CompoundTag;',false));m.instructions.insertBefore(c,l);m.maxStack+=1;count++;
   }
  }if(count!==1)throw new Error('Legacy player load sites '+node.name+'/'+count);return node;
 }
 return {
  removed_world_settings1358:{target:{type:'CLASS',name:'net.minecraft.world.level.storage.LevelStorageSource'},transformer:function(node){var count=0;for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.desc)!=='(Lcom/mojang/serialization/Dynamic;Lcom/mojang/datafixers/DataFixer;I)Lcom/mojang/serialization/DataResult;')continue;var code=new L();code.add(new V(O.ALOAD,0));code.add(new C(O.INVOKESTATIC,H,'world','(Lcom/mojang/serialization/Dynamic;)Lcom/mojang/serialization/Dynamic;',false));code.add(new V(O.ASTORE,0));m.instructions.insert(code);m.maxStack=Math.max(m.maxStack,1);count++;}if(count!==1)throw new Error('Legacy world settings decoder '+count);return node;}},
  removed_singleplayer_load1358:{target:{type:'CLASS',name:'net.minecraft.server.players.PlayerList'},transformer:function(node){return player(node,true);}},
  removed_player_file_load1358:{target:{type:'CLASS',name:'net.minecraft.world.level.storage.PlayerDataStorage'},transformer:function(node){return player(node,false);}}
 };
}

/** Preserve native bodies; replace only the three factories and exact
 * successful harvest, committed placement, and explosion callback sites. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),F=Java.type('org.objectweb.asm.tree.FieldInsnNode');
 var helper='dev/koikaster/mite/SourceSoilControl271';
 function method(node,normal,srg,desc){for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===normal||String(m.name)===srg)&&String(m.desc)===desc)return m;}throw new Error('Soil271 missing '+normal);}
 function field(node,desc){var found=null;for(var i=0;i<node.fields.size();i++){var f=node.fields.get(i);if(String(f.desc)!==desc)continue;if(found!==null)throw new Error('Soil271 ambiguous native field '+desc);found=String(f.name);}if(found===null)throw new Error('Soil271 missing field '+desc);return found;}
 return {
  source_soil_factories271:{target:{type:'CLASS',name:'net.minecraft.world.level.block.Blocks'},transformer:function(node){
   var m=method(node,'<clinit>','<clinit>','()V'),count=0;
   for(var x=m.instructions.getFirst();x!==null;x=x.getNext()){
    if(x.getOpcode()!==O.LDC)continue;var id=String(x.cst);if(id!=='dirt'&&id!=='coarse_dirt'&&id!=='soul_sand')continue;
    var old=id==='soul_sand'?'net/minecraft/world/level/block/SoulSandBlock':'net/minecraft/world/level/block/Block',replacement=id==='soul_sand'?'dev/koikaster/mite/SourceSoulSandBlock271':'dev/koikaster/mite/SourceSoilBlock271',allocation=x.getNext();
    while(allocation!==null&&allocation.getOpcode()<0)allocation=allocation.getNext();if(allocation===null||allocation.getOpcode()!==O.NEW||String(allocation.desc)!==old)throw new Error('Soil271 exact factory allocation '+id);
    var ctor=allocation.getNext();while(ctor!==null&&!(ctor.getOpcode()===O.INVOKESPECIAL&&String(ctor.owner)===old&&String(ctor.name)==='<init>'))ctor=ctor.getNext();if(ctor===null||String(ctor.desc)!=='(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V')throw new Error('Soil271 constructor '+id);
    allocation.desc=replacement;ctor.owner=replacement;count++;
   }if(count!==3)throw new Error('Soil271 factory count '+count);return node;
  }},
  source_soil_harvest271:{target:{type:'CLASS',name:'net.minecraft.server.level.ServerPlayerGameMode'},transformer:function(node){
   var m=method(node,'destroyBlock','m_9280_','(Lnet/minecraft/core/BlockPos;)Z'),level=field(node,'Lnet/minecraft/server/level/ServerLevel;'),player=field(node,'Lnet/minecraft/server/level/ServerPlayer;'),last=null,removed=0;
   for(var x=m.instructions.getFirst();x!==null;x=x.getNext()){
    if(x.getOpcode()===O.IRETURN)last=x;
    if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)===String(node.name)&&String(x.desc)==='(Lnet/minecraft/core/BlockPos;Z)Z'){
     var next=x.getNext();while(next!==null&&next.getOpcode()<0)next=next.getNext();if(next!==null&&next.getOpcode()===O.ISTORE&&next.var===9)removed++;
    }
   }if(last===null||removed!==1)throw new Error('Soil271 exact survival removed flag');
   var code=new L();code.add(new V(O.ALOAD,0));code.add(new F(O.GETFIELD,String(node.name),level,'Lnet/minecraft/server/level/ServerLevel;'));code.add(new V(O.ALOAD,0));code.add(new F(O.GETFIELD,String(node.name),player,'Lnet/minecraft/server/level/ServerPlayer;'));code.add(new V(O.ALOAD,1));code.add(new V(O.ALOAD,2));code.add(new V(O.ILOAD,9));code.add(new C(O.INVOKESTATIC,helper,'afterHarvest','(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)I',false));m.instructions.insertBefore(last,code);m.maxStack=Math.max(m.maxStack,6);return node;
  }},
  source_soil_committed_place271:{target:{type:'CLASS',name:'net.minecraftforge.common.ForgeHooks'},transformer:function(node){
   var m=method(node,'onPlaceItemIntoWorld','onPlaceItemIntoWorld','(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;'),count=0;
   for(var x=m.instructions.getFirst();x!==null;x=x.getNext())if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/level/block/state/BlockState'&&(String(x.name)==='onPlace'||String(x.name)==='m_60696_')&&String(x.desc)==='(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V'){
    var code=new L();code.add(new V(O.ALOAD,3));m.instructions.insertBefore(x,code);x.setOpcode(O.INVOKESTATIC);x.owner=helper;x.name='committedPlace';x.desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/world/entity/player/Player;)V';x.itf=false;count++;
   }if(count!==1)throw new Error('Soil271 exact committed placement site '+count);m.maxStack+=1;return node;
  }},
  source_soil_explosion271:{target:{type:'CLASS',name:'net.minecraft.world.level.Explosion'},transformer:function(node){
   var m=method(node,'finalizeExplosion','m_46075_','(Z)V'),count=0;
   for(var x=m.instructions.getFirst();x!==null;x=x.getNext())if(x.getOpcode()===O.INVOKEVIRTUAL&&String(x.owner)==='net/minecraft/world/level/block/state/BlockState'&&String(x.name)==='onBlockExploded'&&String(x.desc)==='(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V'){
    x.setOpcode(O.INVOKESTATIC);x.owner=helper;x.name='exploded';x.desc='(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V';x.itf=false;count++;
   }
   if(count!==1)throw new Error('Soil271 exact explosion site '+count);
   var loot=0,xp=0,ore='dev/koikaster/mite/SourceOreBlast';
   var lootDesc='(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;';
   var xpDesc='(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V';
   for(var call=m.instructions.getFirst();call!==null;call=call.getNext()){
    if(call.getOpcode()!==O.INVOKEVIRTUAL||String(call.owner)!=='net/minecraft/world/level/block/state/BlockState')continue;
    var name=String(call.name),desc=String(call.desc);
    if((name==='getDrops'||name==='m_287290_')&&desc===lootDesc){call.name='drops';loot++;}
    else if((name==='spawnAfterBreak'||name==='m_222967_')&&desc===xpDesc){call.name='afterBreak';xp++;}
    else continue;
    call.setOpcode(O.INVOKESTATIC);call.owner=ore;call.desc='(Lnet/minecraft/world/level/block/state/BlockState;'+desc.substring(1);call.itf=false;
   }
   if(loot!==1||xp!==1)throw new Error('Ore blast expected one loot and one experience site '+loot+'/'+xp);
   return node;
  }}
 };
}

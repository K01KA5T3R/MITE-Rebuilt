/** Relighting is owned by the one-charcoal interaction, not native ignition items/projectiles. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 return {campfire_fuel_046:{target:{type:'CLASS',name:'net.minecraft.world.level.block.CampfireBlock'},transformer:function(n){var light=0,projectile=0;for(var i=0;i<n.methods.size();i++){var m=n.methods.get(i),name=String(m.name),value=-1;
  if((name==='canLight'||name==='m_51321_')&&String(m.desc)==='(Lnet/minecraft/world/level/block/state/BlockState;)Z'){light++;value=O.IRETURN;}
  else if((name==='onProjectileHit'||name==='m_5581_')&&String(m.desc)==='(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)V'){projectile++;value=O.RETURN;}
  if(value<0)continue;m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!==null)m.localVariables.clear();if(value===O.IRETURN)m.instructions.add(new I(O.ICONST_0));m.instructions.add(new I(value));m.maxStack=value===O.IRETURN?1:0;
 }if(light!==1||projectile!==1)throw new Error('Campfire ignition sites '+light+'/'+projectile);return n;}}};
}

/** Only three source reference-book chest owners and actual item destruction. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes'),M=Java.type('org.objectweb.asm.tree.MethodNode'),L=Java.type('org.objectweb.asm.tree.InsnList'),V=Java.type('org.objectweb.asm.tree.VarInsnNode'),C=Java.type('org.objectweb.asm.tree.MethodInsnNode'),I=Java.type('org.objectweb.asm.tree.InsnNode');
 function method(node,official,srg,desc){for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if((String(m.name)===official||String(m.name)===srg)&&String(m.desc)===desc)return m;}return null;}
 var postDesc='(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V';
 var chestDesc='(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;IIILnet/minecraft/resources/ResourceLocation;)Z';
 function structure(kind){return function(node){
  var post=method(node,'postProcess','m_213694_',postDesc);if(post===null)throw new Error('Book054 missing structure owner');var srg=String(post.name)!=='postProcess';
  if(method(node,'createChest','m_213787_',chestDesc)!==null)throw new Error('Book054 existing chest override');
  var sites=0;for(var at=post.instructions.getFirst();at!==null;at=at.getNext())if(at.getOpcode()===O.INVOKEVIRTUAL&&String(at.desc)===chestDesc){if(String(at.name)!==(srg?'m_213787_':'createChest'))throw new Error('Book054 unexpected native chest mapping');sites++;}
  if(sites!==(kind===0?1:2))throw new Error('Book054 native chest call sites '+sites);
  var begin=new L();begin.add(new V(O.ALOAD,0));begin.add(new I(O.ICONST_0+kind));begin.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceReferencedChests','begin','(Ljava/lang/Object;I)V',false));post.instructions.insert(begin);post.maxStack=Math.max(post.maxStack,2);
  var m=new M(O.ACC_PROTECTED,srg?'m_213787_':'createChest',chestDesc,null,null);
  for(var i=0;i<4;i++)m.instructions.add(new V(O.ALOAD,i));
  m.instructions.add(new V(O.ALOAD,0));for(var i=4;i<7;i++)m.instructions.add(new V(O.ILOAD,i));
  m.instructions.add(new C(O.INVOKEVIRTUAL,String(node.name),srg?'m_163582_':'getWorldPos','(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;',false));
  m.instructions.add(new I(O.ICONST_0+kind));for(var i=4;i<7;i++)m.instructions.add(new V(O.ILOAD,i));
  m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceReferencedChests','create','(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;IIII)Z',false));m.instructions.add(new I(O.IRETURN));m.maxStack=9;m.maxLocals=8;node.methods.add(m);return node;
 };}
 return {
  source_desert_books_054:{target:{type:'CLASS',name:'net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece'},transformer:structure(0)},
  source_jungle_books_054:{target:{type:'CLASS',name:'net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece'},transformer:structure(1)},
  source_library_books_054:{target:{type:'CLASS',name:'net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces$Library'},transformer:structure(2)},
  source_book_signature_release_054:{target:{type:'CLASS',name:'net.minecraft.world.entity.item.ItemEntity'},transformer:function(node){
   var tick=method(node,'tick','m_8119_','()V');if(tick===null)throw new Error('Book054 unknown item mapping');var srg=String(tick.name)!=='tick',desc='(Lnet/minecraft/world/entity/Entity$RemovalReason;)V';if(method(node,'remove','m_142687_',desc)!==null)throw new Error('Book054 native ItemEntity removal override already exists');
   var m=new M(O.ACC_PUBLIC,srg?'m_142687_':'remove',desc,null,null);m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new C(O.INVOKESTATIC,'dev/koikaster/mite/SourceReferencedChests','removed','(Ljava/lang/Object;Ljava/lang/Object;)V',false));m.instructions.add(new V(O.ALOAD,0));m.instructions.add(new V(O.ALOAD,1));m.instructions.add(new C(O.INVOKESPECIAL,String(node.superName),String(m.name),desc,false));m.instructions.add(new I(O.RETURN));m.maxStack=2;m.maxLocals=2;node.methods.add(m);return node;
  }}
 };
}

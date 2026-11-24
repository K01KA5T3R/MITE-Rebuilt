function sourceFloat038(value){var A=Java.type('net.minecraftforge.coremod.api.ASMAPI');return A.castNumber(value,A.NumberType.FLOAT);}
function sourceFloatEquals038(value,expected){return sourceFloat038(expected).equals(value);}
/** Source default body dimensions must also reach native pre-creation spawn
 * collision checks. Only two sized calls, three changed Float constants. */
function initializeCoreMod(){
 var O=Java.type('org.objectweb.asm.Opcodes');
 return {mite_source_skeleton_registered_dimensions_038:{target:{type:'CLASS',name:'net.minecraft.world.entity.EntityType'},transformer:function(node){var found=0;
  for(var i=0;i<node.methods.size();i++){var m=node.methods.get(i);if(String(m.name)!=='<clinit>')continue;var role=null;for(var it=m.instructions.iterator();it.hasNext();){var x=it.next();if(x.getOpcode()===O.LDC&&typeof x.cst==='string')role=String(x.cst)==='skeleton'?'skeleton':String(x.cst)==='wither_skeleton'?'wither':null;
   if(role===null||String(x.owner)!=='net/minecraft/world/entity/EntityType$Builder'||String(x.desc)!=='(FF)Lnet/minecraft/world/entity/EntityType$Builder;')continue;
   var height=x.getPrevious(),width=height.getPrevious();if(height.getOpcode()!==O.LDC||width.getOpcode()!==O.LDC||!sourceFloatEquals038(width.cst,role==='skeleton'?0.6:0.7)||!sourceFloatEquals038(height.cst,role==='skeleton'?1.99:2.4))throw new Error('Unexpected native registered skeleton dimensions '+role);
   width.cst=sourceFloat038(role==='skeleton'?0.6:0.72);height.cst=sourceFloat038(role==='skeleton'?1.8:2.34);role=null;found++;
  }}if(found!==2)throw new Error('Registered skeleton dimension sites '+found);return node;
 }}};
}

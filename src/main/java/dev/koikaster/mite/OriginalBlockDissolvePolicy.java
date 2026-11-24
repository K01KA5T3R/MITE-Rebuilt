package dev.koikaster.mite;

/** New039 aqz.getDissolvePeriod branching. Facts are source block/
 * material identities, not guesses from modern destroy speed or block tags.
 * The native counterpart classification is wired but still requires its scope audit. */
public final class OriginalBlockDissolvePolicy {
 public interface Facts {
  boolean tripwire();boolean materialHarmedByPepsin();boolean materialHarmedByAcid();
  boolean cloth();boolean carpet();boolean cake();
  /** R195 anz/ank/amx/aqy/aph, tested before all later families. */
  boolean earlyMaterialGatedFamily();
  boolean anvil();boolean anvilMetalHarmedByAcid();
  /** amy/arr/apb/aqj/aoy/anb/ast/asu/ang/apg/apy/aqf/ari/aoc,
   * skull(cl), anp, daylight sensor(cs), aot, hay(cB). */
  boolean alwaysGradualAcidFamily();
  /** aqr/aqb: stone buttons and redstone dust, even if material is susceptible. */
  boolean acidProtectedCircuitFamily();
  /** aoh/aog/anj or cocoa(bU). */
  boolean gradualFenceGateCauldronCocoaFamily();
  /** aro/aoz. */
  boolean lilyOrLeaves();
  boolean sourceSolidAtMetadataZero();boolean tripwireHook();
 }
 public static int ticks(Facts block,boolean acidic){
  if(!acidic){
   if(block.tripwire())return 400;
   if(block.materialHarmedByPepsin())return block.cloth()||block.carpet()||block.cake()?400:0;
  }else{
   if(block.earlyMaterialGatedFamily())return block.materialHarmedByAcid()?400:-1;
   if(block.anvil())return block.anvilMetalHarmedByAcid()?400:-1;
   if(block.alwaysGradualAcidFamily())return 400;
   if(block.materialHarmedByAcid()){
    if(block.acidProtectedCircuitFamily())return -1;
    if(block.gradualFenceGateCauldronCocoaFamily())return 400;
    if(block.lilyOrLeaves()||block.cloth()||block.cake())return 0;
    if(!block.sourceSolidAtMetadataZero())return block.tripwireHook()?400:0;
   }
  }
  return -1;
 }
 public static int sourceSeconds(Facts block,boolean acidic){int ticks=ticks(block,acidic);return ticks>=0?ticks/20:-1;}
 private OriginalBlockDissolvePolicy(){}
}

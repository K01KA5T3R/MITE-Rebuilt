package dev.koikaster.mite;

import java.time.LocalDate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkStatus;

/** tw/AnimalWatcher native owner. Equipment and conversion cores are the
 * sealed066 implementations. There is one native target/path and cure clock. */
public final class SourceZombieBody implements SourcePigmanGear.Host,SourceZombieEquipment.Host,SourceZombieLoot.Host,SourceZombieCure.Clock,SourceZombieCure.Start,SourceZombieCure.Blocks,OriginalMoveToFoodGoal.Owner,OriginalPickupState.Policy,OriginalParentHarmState.Flags {
 final Zombie actor;final SourceZombieOwner owner;
 public final OriginalDigging digging;public final SourcePigmanGear gear;public final SourceLegacyOffhand legacy;public final OriginalParentHarmState harm;
 private final OriginalPickupState food;
 private boolean smart,massiveFall,pickedHeld,decided,fleeing;private int conversionTime=-1,wetTicks;
 public SourceZombieBody(Zombie actor,SourceZombieOwner owner){
  this.actor=actor;this.owner=owner;
  actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);actor.getAttribute(Attributes.ARMOR).setBaseValue(0);actor.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);actor.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).23f);actor.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);actor.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(actor.getRandom().nextDouble()*(double).1f);actor.setHealth(20);smart=actor.getRandom().nextInt(8)==0;
  actor.setMaxUpStep(.5f);((GroundPathNavigation)actor.getNavigation()).setCanOpenDoors(true);
  digging=new OriginalDigging(actor);gear=new SourcePigmanGear(this);legacy=new SourceLegacyOffhand(actor);food=new OriginalPickupState(actor,this);harm=new OriginalParentHarmState(actor,this);
 }
 public void goals(){
  var goals=((SourceMobAi)(Object)actor).mite$goals();var targets=((SourceMobAi)(Object)actor).mite$targets();
  goals.addGoal(1,new OriginalWatchAnimalGoal(digging));goals.addGoal(0,new SourceZombieIdleGoals.Swim(actor));goals.addGoal(1,new OriginalBreakPortalGoal(actor));
  goals.addGoal(2,new OriginalMeleeGoal(actor,Player.class,1,false,digging));goals.addGoal(3,new OriginalMeleeGoal(actor,Villager.class,1,true,digging));
  goals.addGoal(4,new MoveTowardsRestrictionGoal(actor,1));goals.addGoal(5,new SourceZombieVillageGoal(actor));goals.addGoal(6,new OriginalWanderGoal(actor,1));
  goals.addGoal(7,new SourceZombieIdleGoals.Watch(actor));goals.addGoal(7,new SourceZombieIdleGoals.Look(actor));
  targets.addGoal(1,new OriginalHurtByGoal(actor,true));targets.addGoal(2,new OriginalNearestTargetGoal(actor,Player.class,0,true));targets.addGoal(2,new OriginalNearestTargetGoal(actor,Villager.class,0,false));
  goals.addGoal(2,new OriginalMoveToFoodGoal(actor,this,1,true));goals.addGoal(4,new OriginalMeleeGoal(actor,net.minecraft.world.entity.animal.Animal.class,1,true,digging));targets.addGoal(3,new OriginalNearestTargetGoal(actor,net.minecraft.world.entity.animal.Animal.class,10,true));goals.addGoal(3,new OriginalBurningTreeGoal(actor,1));
 }
 SourcePigmanNativeParent parent(){return (SourcePigmanNativeParent)(Object)actor;}
 public void beforeTick(){SourceZombieCure.beforeParent(this);harm.beforeTick();}
 public void beforeAi(){
  if(actor.level().isClientSide)return;
  wetTicks=OriginalMobEnvironment.nextWetTicks(actor,wetTicks);
  OriginalMobEnvironment.sunlight(actor,wetTicks);
  digging.beforeAi();
 }
 public void afterAi(){if(!IteEliteSpawns.isElite(actor)){gear.pickup();food.tick();}}
 public void web(net.minecraft.world.level.block.state.BlockState state){if(!actor.level().isClientSide&&state.is(Blocks.COBWEB)){actor.getPersistentData().putInt("MiteSourceWebTick",actor.tickCount);EnchantmentEffects.refreshMovement(actor);}}
 public boolean canDig(){return !digging.preventsDigging()&&(smart||SourceMobFrenzy.active(actor)||SourcePigmanKills.tool(actor.getMainHandItem()));}
 public boolean smart(){return smart;}
 public boolean hurt(DamageSource source,float amount){
  if(source.is(net.minecraft.world.damagesource.DamageTypes.FALL)&&amount>=actor.getMaxHealth()*.5f)massiveFall=true;
  if(actor.level().isClientSide)return parent().mite$pigmanMonsterHurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(actor,outcome->harm.afterDamage(source,outcome.negativelyAffected()))){boolean accepted=parent().mite$pigmanMonsterHurt(source,amount);var result=frame.finish();if(!result.destroyed()&&result.negativelyAffected()&&source.getEntity() instanceof Player)smart=true;return accepted;}
 }
 public void deathLoot(boolean recent,int looting){digging.cancel();if(!massiveFall||actor.getRandom().nextInt(10)==0)SourceZombieLoot.drop(this,recent,looting,massiveFall);gear.death(recent,looting);legacy.drop();}
 private ItemStack randomEquipment(Item item,boolean mayEnchant){
  return randomEquipment(actor.getRandom(),item,mayEnchant);
 }
 static ItemStack randomEquipment(net.minecraft.util.RandomSource random,Item item,boolean mayEnchant){
  var stack=new ItemStack(item);
  if(mayEnchant&&random.nextFloat()<.1f){
   for(var entry:ClassicEnchanting.selectNative(random,stack,5+random.nextInt(18),false)){
    stack.enchant(entry.enchantment,entry.level);
   }
  }
  if(stack.isDamageableItem()){
   float fraction=(stack.isEnchanted()?.5f:.7f)+.3f*random.nextFloat();
   stack.setDamageValue(Math.min(stack.getMaxDamage()-1,(int)(stack.getMaxDamage()*fraction)));
  }
  return stack;
 }
 public record Group(boolean baby,boolean villager) implements SpawnGroupData,SourcePigmanParentSpawn.ZombieGroup {}
 // abw.b / abw.I at the spawning mob's feet, without demanding a new chunk.
 static float localSpawnFactor(ServerLevelAccessor level,BlockPos at){
  var world=level.getLevel();
  if(WorldContentMode.rebuilt(world))return 1f;
  if(at.getY()<level.getMinBuildHeight()||at.getY()>=level.getMaxBuildHeight())return 0;
  int chunkX=at.getX()>>4,chunkZ=at.getZ()>>4;
  var chunk=level instanceof ServerLevel running?running.getChunkSource().getChunkNow(chunkX,chunkZ)
   :level.getChunk(chunkX,chunkZ,ChunkStatus.EMPTY,false);
  if(chunk==null)return 0;
  var difficulty=world.getDifficulty();
  boolean hard=difficulty==Difficulty.HARD;
  float local=Mth.clamp((float)chunk.getInhabitedTime()/3_600_000f,0f,1f)*(hard?1f:.75f);
  int phase=SourceMoonCycle.phase(world.getGameTime());
  float moon=1f-Math.min(phase,8-phase)*.25f;
  local+=moon*.25f;
  if(difficulty.getId()<2)local*=difficulty.getId()/2f;
  return Mth.clamp(local,0f,hard?1.5f:1f);
 }
 public SpawnGroupData initialize(ServerLevelAccessor world,MobSpawnType reason,SpawnGroupData input){
  Object raw=input instanceof Zombie.ZombieGroupData flags?new Group(flags.isBaby,owner.sourceVillager()):input;
  if(reason==MobSpawnType.CONVERSION){owner.sourceVillager(false);raw=null;}
  Object result=SourceZombieSpawn.initialize(new SourcePigmanParentSpawn.Host(){
   public double nextGaussian(){return actor.getRandom().nextGaussian();}public float nextFloat(){return SourceZombieBody.this.nextFloat();}public double nextDouble(){return actor.getRandom().nextDouble();}public float worldNextFloat(){return world.getRandom().nextFloat();}
   private void modifier(Attribute attribute,String name,double value,AttributeModifier.Operation operation){actor.getAttribute(attribute).addPermanentModifier(new AttributeModifier(name,value,operation));}
   public void followBonus(double value){SourceSpawnInitialization.apply(actor,reason,value);}public void lootPickup(boolean value){actor.setCanPickUpLoot(value);}public void equipment(){SourceZombieEquipment.equipment(SourceZombieBody.this);}public boolean headEmpty(){return actor.getItemBySlot(EquipmentSlot.HEAD).isEmpty();}
   public boolean halloween(){var d=LocalDate.now();return d.getMonthValue()==10&&d.getDayOfMonth()==31;}public void pumpkin(boolean lit){actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(lit?Blocks.JACK_O_LANTERN:Blocks.CARVED_PUMPKIN));}public void headDropChance(float value){dropChance(4,value);}
   public void knockbackBonus(double value){modifier(Attributes.KNOCKBACK_RESISTANCE,"Random spawn bonus",value,AttributeModifier.Operation.ADDITION);}public void reinforcementBonus(double value){modifier(Attributes.SPAWN_REINFORCEMENTS_CHANCE,"Leader zombie bonus",value,AttributeModifier.Operation.ADDITION);}public void maximumHealthBonus(double value){modifier(Attributes.MAX_HEALTH,"Leader zombie bonus",value,AttributeModifier.Operation.MULTIPLY_TOTAL);}
   public void villager(boolean value){owner.sourceVillager(value);if(value)owner.sourceProfession(0);}public void baby(boolean ignored){actor.setBaby(false);}
  },raw,localSpawnFactor(world,actor.blockPosition()));
  if(result instanceof SpawnGroupData data)return data;if(result instanceof SourcePigmanParentSpawn.ZombieGroup flags)return new Group(flags.baby(),flags.villager());return null;
 }
 public InteractionResult interact(Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);if(!stack.is(Items.GOLDEN_APPLE)&&!stack.is(Items.ENCHANTED_GOLDEN_APPLE))return InteractionResult.PASS;
  boolean accepted=SourceZombieCure.interact(new SourceZombieCure.Interaction(){public boolean zombie(){return true;}public boolean villager(){return owner.sourceVillager();}public boolean converting(){return owner.sourceConverting();}public boolean client(){return actor.level().isClientSide;}public boolean server(){return !actor.level().isClientSide;}public boolean weakness(){return actor.hasEffect(MobEffects.WEAKNESS);}public int subtype(){return stack.is(Items.ENCHANTED_GOLDEN_APPLE)?1:0;}public int nextInt(int n){return SourceZombieBody.this.nextInt(n);}public boolean creative(){return player.getAbilities().instabuild;}public void begin(int ticks){SourceZombieCure.begin(SourceZombieBody.this,ticks);}public void consume(){stack.shrink(1);}});
  return accepted?InteractionResult.sidedSuccess(actor.level().isClientSide):InteractionResult.PASS;
 }
 public boolean handleEvent(byte event){if(event!=16)return false;actor.level().playLocalSound(actor.getX()+.5,actor.getY()+.5,actor.getZ()+.5,SoundEvents.ZOMBIE_VILLAGER_CURE,actor.getSoundSource(),1+nextFloat(),nextFloat()*.7f+.3f,false);return true;}
 public void save(CompoundTag tag){tag.putBoolean("R195ZombieOwner",true);tag.putBoolean("is_smart",smart);tag.putBoolean("picked_up_a_held_item",pickedHeld);tag.putBoolean("has_taken_massive_fall_damage",massiveFall);tag.putInt("ConversionTime",converting()?conversionTime:-1);if(owner.sourceVillager()){tag.putBoolean("IsVillager",true);tag.putInt("Profession",owner.sourceProfession());}else{tag.remove("IsVillager");tag.remove("Profession");}tag.remove("IsBaby");harm.save(tag);digging.state.save(tag);legacy.save(tag);}
 public void load(CompoundTag tag){
  smart=tag.getBoolean("is_smart");pickedHeld=tag.getBoolean("picked_up_a_held_item");massiveFall=tag.getBoolean("has_taken_massive_fall_damage");owner.sourceVillager(tag.getBoolean("IsVillager")||actor instanceof ZombieVillager&&!tag.getBoolean("R195ZombieOwner"));if(tag.contains("Profession",Tag.TAG_ANY_NUMERIC))owner.sourceProfession(tag.getInt("Profession"));harm.load(tag);digging.state.load(tag);legacy.load(tag);
  conversionTime=-1;owner.sourceConverting(false);if(tag.contains("ConversionTime",Tag.TAG_ANY_NUMERIC)&&tag.getInt("ConversionTime")>-1)SourceZombieCure.begin(this,tag.getInt("ConversionTime"));actor.setBaby(false);
  if(!tag.getBoolean("R195ZombieOwner")){actor.getAttribute(Attributes.ARMOR).setBaseValue(0);actor.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);actor.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);for(var modifier:java.util.List.copyOf(actor.getAttribute(Attributes.FOLLOW_RANGE).getModifiers()))if(modifier.getName().equals("Random zombie-spawn bonus"))actor.getAttribute(Attributes.FOLLOW_RANGE).removeModifier(modifier);}
 }
 public static VillagerProfession profession(int value){return switch(value){case 1->VillagerProfession.LIBRARIAN;case 2->VillagerProfession.CLERIC;case 3->VillagerProfession.ARMORER;case 4->VillagerProfession.BUTCHER;default->VillagerProfession.FARMER;};}
 public static int profession(VillagerProfession value){return value==VillagerProfession.LIBRARIAN?1:value==VillagerProfession.CLERIC?2:value==VillagerProfession.ARMORER||value==VillagerProfession.TOOLSMITH||value==VillagerProfession.WEAPONSMITH?3:value==VillagerProfession.BUTCHER?4:0;}
 @Override public void finish(){
  if(!(actor.level() instanceof ServerLevel level))return;var villager=EntityType.VILLAGER.create(level);if(villager==null)return;villager.moveTo(actor.getX(),actor.getY(),actor.getZ(),actor.getYRot(),actor.getXRot());
  net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(villager,level,level.getCurrentDifficultyAt(villager.blockPosition()),MobSpawnType.CONVERSION,null,null);villager.setVillagerData(SourceVillagerPolicy.data(villager.getVillagerData()));if(villager instanceof SourceVillager source)source.sourceCured();legacy.drop();digging.cancel();actor.discard();level.addFreshEntity(villager);villager.addEffect(new MobEffectInstance(MobEffects.CONFUSION,200,0));level.levelEvent(null,1027,actor.blockPosition(),0);
 }
 @Override public Mob actor(){return actor;}@Override public float dropChance(int slot){return owner.sourceDropChance(slot);}@Override public void dropChance(int slot,float value){actor.setDropChance(SourcePigmanGear.slot(slot),value);}@Override public boolean massiveFall(){return massiveFall;}@Override public boolean pickedUpHeld(){return pickedHeld;}@Override public void pickedUpHeld(boolean value){pickedHeld=value;}
 @Override public boolean villager(){return owner.sourceVillager();}@Override public boolean revenant(){return false;}@Override public boolean hard(){return actor.level().getDifficulty()==Difficulty.HARD;}@Override public float nextFloat(){return actor.getRandom().nextFloat();}@Override public int nextInt(int n){return actor.getRandom().nextInt(n);}
 @Override public void weapon(SourceZombieEquipment.Weapon weapon,boolean enchant){var item=switch(weapon){case WOOD_SHOVEL->Items.WOODEN_SHOVEL;case RUSTED_SHOVEL->Content.get("rusted_iron_shovel");case WOOD_CUDGEL->Content.get("wood_cudgel");case WOOD_CLUB->Content.get("wood_club");case RUSTED_SWORD->Content.get("rusted_iron_sword");case RUSTED_DAGGER->Content.get("rusted_iron_dagger");case RUSTED_SHEARS->Content.get("rusted_iron_shears");case RUSTED_SCYTHE->Content.get("rusted_iron_scythe");};actor.setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(item,enchant));}
 @Override public boolean armorEmpty(int slot){return actor.getItemBySlot(SourcePigmanGear.slot(slot)).isEmpty();}@Override public void armor(int slot,int material,boolean enchant){String part=new String[]{"","boots","leggings","chestplate","helmet"}[slot];var item=material==0?switch(slot){case 1->Items.LEATHER_BOOTS;case 2->Items.LEATHER_LEGGINGS;case 3->Items.LEATHER_CHESTPLATE;default->Items.LEATHER_HELMET;}:Content.get(new String[]{"","rusted_iron_chainmail_","copper_chainmail_","rusted_iron_","copper_"}[material]+part);actor.setItemSlot(SourcePigmanGear.slot(slot),randomEquipment(item,enchant));}
 @Override public void drop(SourceZombieLoot.Item item){dropZombieLoot(actor,item);}
 static void dropZombieLoot(Mob actor,SourceZombieLoot.Item item){
  actor.spawnAtLocation(switch(item){
   case ROTTEN_FLESH -> Items.ROTTEN_FLESH;
   case COPPER_NUGGET -> Content.get("copper_nugget");
   case SILVER_NUGGET -> Content.get("silver_nugget");
   case GOLD_NUGGET -> Items.GOLD_NUGGET;
   case IRON_NUGGET -> Items.IRON_NUGGET;
   case WHEAT_SEEDS -> Items.WHEAT_SEEDS;
   case PUMPKIN_SEEDS -> Items.PUMPKIN_SEEDS;
   case MELON_SEEDS -> Items.MELON_SEEDS;
   case CARROT -> Items.CARROT;
   case POTATO -> Items.POTATO;
   case ONION -> Content.get("onion");
  });
 }
 @Override public boolean client(){return actor.level().isClientSide;}@Override public boolean converting(){return owner.sourceConverting();}@Override public int time(){return conversionTime;}@Override public void time(int ticks){conversionTime=ticks;}@Override public int progress(){return SourceZombieCure.progress(this);}@Override public int difficulty(){return actor.level().getDifficulty().getId();}@Override public void markConverting(){owner.sourceConverting(true);}@Override public void removeWeakness(){actor.removeEffect(MobEffects.WEAKNESS);}@Override public void strength(int ticks,int amplifier){actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,ticks,amplifier));}@Override public void conversionEvent(){actor.level().broadcastEntityEvent(actor,(byte)16);}@Override public int x(){return (int)actor.getX();}@Override public int y(){return (int)actor.getY();}@Override public int z(){return (int)actor.getZ();}@Override public boolean specialBlock(int x,int y,int z){var s=actor.level().getBlockState(new BlockPos(x,y,z));return s.is(Blocks.IRON_BARS)||s.getBlock() instanceof BedBlock;}
 @Override public int pickupCooldown(){return food.cooldown;}@Override public boolean canEat(){return !IteEliteSpawns.isElite(actor)&&!actor.isRemoved()&&actor.getHealth()>0;}@Override public boolean isFood(ItemStack stack){return OriginalMeatItems.includes(stack);}@Override public boolean willEat(ItemStack stack){return isFood(stack)&&canEat();}@Override public boolean allowed(ItemEntity item){if(item.hasPickUpDelay()||item.isOnFire())return false;for(float f:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,item.getBoundingBox().getCenter(),actor.position().add(0,actor.getBbHeight()*f,0)))return true;return false;}@Override public void ate(ItemStack ignored){}@Override public boolean willRepair(ItemStack ignored){return false;}@Override public void repaired(ItemStack ignored){}@Override public boolean willTakeAsValuable(ItemStack ignored){return false;}@Override public void contain(Item ignored){throw new IllegalStateException("tw has no valuables policy");}@Override public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
}

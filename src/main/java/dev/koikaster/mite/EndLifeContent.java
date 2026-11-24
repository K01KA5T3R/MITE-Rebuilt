package dev.koikaster.mite;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.registries.RegistryObject;

/** N008/V018: new outer-End ecology. These are selected expansion rules,
 * not R195 species or replacements for the source animal spawn algorithm. */
public final class EndLifeContent {
 public static final RegistryObject<EntityType<Raider>> PROWLER=Mobs.ENTITIES.register("ender_prowler",()->EntityType.Builder.of(Raider::new,MobCategory.MONSTER).sized(.6f,2.9f).build(MiteReforged.ID+":ender_prowler"));
 public static final RegistryObject<EntityType<Raider>> SENTINEL=Mobs.ENTITIES.register("ender_sentinel",()->EntityType.Builder.of(Raider::new,MobCategory.MONSTER).sized(.7f,3.2f).build(MiteReforged.ID+":ender_sentinel"));
 public static final RegistryObject<EntityType<EndPig>> PIG=Mobs.ENTITIES.register("end_pig",()->EntityType.Builder.of(EndPig::new,MobCategory.CREATURE).sized(.9f,.9f).build(MiteReforged.ID+":end_pig"));
 public static final RegistryObject<EntityType<EndCow>> COW=Mobs.ENTITIES.register("end_cow",()->EntityType.Builder.of(EndCow::new,MobCategory.CREATURE).sized(.9f,1.4f).build(MiteReforged.ID+":end_cow"));
 public static final RegistryObject<Item> RAW_PORK=Content.item("raw_end_porkchop",()->new Meat(4,MobEffects.MOVEMENT_SPEED,200));
 public static final RegistryObject<Item> COOKED_PORK=Content.item("cooked_end_porkchop",()->new Meat(8,MobEffects.MOVEMENT_SPEED,600));
 public static final RegistryObject<Item> RAW_BEEF=Content.item("raw_end_beef",()->new Meat(5,MobEffects.DAMAGE_RESISTANCE,200));
 public static final RegistryObject<Item> COOKED_BEEF=Content.item("cooked_end_beef",()->new Meat(10,MobEffects.DAMAGE_RESISTANCE,600));
 public static final class Meat extends Item {
  public final MobEffect effect;public final int ticks;
  Meat(int energy,MobEffect effect,int ticks){super(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(energy).saturationMod(.5f).meat().effect(()->new MobEffectInstance(effect,ticks),1).build()));this.effect=effect;this.ticks=ticks;}
 }
 public static void register(){egg("ender_prowler",PROWLER,0x241a31,0xc178e8);egg("ender_sentinel",SENTINEL,0x151b2a,0x6f8de8);egg("end_pig",PIG,0xa461bd,0xe6bcf0);egg("end_cow",COW,0x35263d,0xb393e5);}
 private static <T extends Mob> void egg(String name,RegistryObject<EntityType<T>> type,int primary,int secondary){Content.item(name+"_spawn_egg",()->new ForgeSpawnEggItem(type,primary,secondary,new Item.Properties()));}
 public static void attributes(EntityAttributeCreationEvent e){
  e.put(PROWLER.get(),EnderMan.createAttributes().add(Attributes.MAX_HEALTH,48).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.ARMOR,1).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,32).build());
  e.put(SENTINEL.get(),EnderMan.createAttributes().add(Attributes.MAX_HEALTH,72).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.ARMOR,4).add(Attributes.KNOCKBACK_RESISTANCE,.5).add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.FOLLOW_RANGE,32).build());
  e.put(PIG.get(),Pig.createAttributes().add(Attributes.MAX_HEALTH,16).build());e.put(COW.get(),Cow.createAttributes().add(Attributes.MAX_HEALTH,20).build());
 }
 public static boolean outer(Level level,BlockPos pos){return level.dimension()==Level.END&&(long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()>1024L*1024L;}
 public static boolean habitat(ServerLevelAccessor level,BlockPos pos){return WorldContentMode.rebuilt(level.getLevel())&&outer(level.getLevel(),pos)&&level.getBlockState(pos.below()).is(Blocks.END_STONE)&&level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.above()).isEmpty();}
 static boolean animal(EntityType<? extends Animal> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){return habitat(level,pos);}
 static boolean hostile(EntityType<Raider> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){return level.getDifficulty()!=Difficulty.PEACEFUL&&habitat(level,pos);}
 public static void placements(SpawnPlacementRegisterEvent e){
  e.register(PROWLER.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,EndLifeContent::hostile,SpawnPlacementRegisterEvent.Operation.REPLACE);
  e.register(SENTINEL.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,EndLifeContent::hostile,SpawnPlacementRegisterEvent.Operation.REPLACE);
  e.register(PIG.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,EndLifeContent::animal,SpawnPlacementRegisterEvent.Operation.REPLACE);
  e.register(COW.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,EndLifeContent::animal,SpawnPlacementRegisterEvent.Operation.REPLACE);
 }
 public static final class Raider extends EnderMan {
  public Raider(EntityType<? extends EnderMan> type,Level level){super(type,level);xpReward=sentinel()?25:15;}
  public boolean sentinel(){return getType()==SENTINEL.get();}
  @Override protected void registerGoals(){
   // No neutral stare/freeze or block-pickup goals: these family members hunt players.
   goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,1,0));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
   targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
  }
  @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level instanceof ServerLevelAccessor server&&hostile((EntityType<Raider>)getType(),server,reason,blockPosition(),random);}
  @Override public int getMaxSpawnClusterSize(){return sentinel()?1:2;}
  @Override public boolean doHurtTarget(Entity target){boolean hit=super.doHurtTarget(target);if(hit&&target instanceof LivingEntity living){if(sentinel())living.knockback(.8,getX()-living.getX(),getZ()-living.getZ());else living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100),this);}return hit;}
 }
 public static final class EndPig extends Pig {
  public EndPig(EntityType<? extends Pig> type,Level level){super(type,level);}
  @Override public EndPig getBreedOffspring(ServerLevel level,AgeableMob other){return PIG.get().create(level);}
  @Override public boolean canMate(Animal other){return other instanceof EndPig&&super.canMate(other);}
  @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return level.getBlockState(pos.below()).is(Blocks.END_STONE)?10:super.getWalkTargetValue(pos,level);}
  @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level instanceof ServerLevelAccessor server&&habitat(server,blockPosition());}
  @Override public void aiStep(){super.aiStep();portal(this);}
 }
 public static final class EndCow extends Cow {
  public EndCow(EntityType<? extends Cow> type,Level level){super(type,level);}
  @Override public EndCow getBreedOffspring(ServerLevel level,AgeableMob other){return COW.get().create(level);}
  @Override public boolean canMate(Animal other){return other instanceof EndCow&&super.canMate(other);}
  @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return level.getBlockState(pos.below()).is(Blocks.END_STONE)?10:super.getWalkTargetValue(pos,level);}
  @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level instanceof ServerLevelAccessor server&&habitat(server,blockPosition());}
  @Override public void aiStep(){super.aiStep();portal(this);}
 }
 private static void portal(Animal animal){if(animal.level().isClientSide&&animal.tickCount%12==0)animal.level().addParticle(ParticleTypes.PORTAL,animal.getRandomX(.5),animal.getRandomY(),animal.getRandomZ(.5),0,.02,0);}
 /** Keep R195 butchering amounts/bonuses, with the selected new meats. */
 static boolean butcher(LivingEntity actor,ObjectArrayList<ItemStack> loot,RandomSource random,boolean burning,int butchering){
  if(!(actor instanceof EndPig)&&!(actor instanceof EndCow))return false;
  var remove=java.util.Set.of(Items.PORKCHOP,Items.COOKED_PORKCHOP,Items.BEEF,Items.COOKED_BEEF,Items.LEATHER,RAW_PORK.get(),COOKED_PORK.get(),RAW_BEEF.get(),COOKED_BEEF.get());loot.removeIf(stack->remove.contains(stack.getItem()));
  if(((Animal)actor).isBaby())return true;
  boolean cow=actor instanceof EndCow;if(cow)loot.add(new ItemStack(Items.LEATHER,1+random.nextInt(3)));
  int n=1+random.nextInt(3)+random.nextInt(1+butchering);if(cow&&n==1&&random.nextInt(2)==0)n++;
  loot.add(new ItemStack(cow?(burning?COOKED_BEEF.get():RAW_BEEF.get()):(burning?COOKED_PORK.get():RAW_PORK.get()),n));return true;
 }
 private EndLifeContent(){}
}

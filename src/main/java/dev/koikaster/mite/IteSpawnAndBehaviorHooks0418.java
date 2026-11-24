package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MITE-ITE 2.0.30 Spawn conditions, day-capped mob scaling join/finalize hook,
 * mob weapon-damage boost & 10% defense/disarm, End Crystal Vibranium+ gate,
 * skeleton arrow knockback & velocity, and Wandering Witch untargetability.
 * Strictly silent outside WorldContentMode.ite(level).
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class IteSpawnAndBehaviorHooks0418 {
    public static final int DEFAULT_ANIMAL_ILL_TO_DEATH_TIME = 12000;
    public static final int DEFAULT_GHAST_SPAWN_LIMIT_DAY = 64;
    public static final double DEFAULT_MOB_MAX_SPAWN_INCREASE_PER_DAY = 0.05D;
    public static final int DEFAULT_BLOOD_MOON_MAX_HOSTILE_FRACTION = 128;

    private IteSpawnAndBehaviorHooks0418() {}

    /** Exposes upstream MITEITESpawnConditions check cleanly for static and runtime verification. */
    public static boolean allowSpecialSpawn(Entity entity, ServerLevel level) {
        if (entity == null || level == null || !WorldContentMode.ite(level)) return true;
        int day = IteMobScaling0418.getCappedIteDay(level);
        boolean bloodMoon = SourceMoonCycle.blood(level.getDayTime(), true);
        if (entity instanceof Ghast && level.dimension() == Level.OVERWORLD) {
            return bloodMoon && day >= DEFAULT_GHAST_SPAWN_LIMIT_DAY;
        }
        if (entity instanceof Giant && level.dimension() == Level.OVERWORLD) {
            var nearby = level.getEntitiesOfClass(Giant.class, entity.getBoundingBox().inflate(96.0D), LivingEntity::isAlive);
            return bloodMoon && nearby.isEmpty();
        }
        if (entity instanceof SourceBoneLord lord && lord.ancient() && level.dimension() == Level.OVERWORLD) {
            return bloodMoon;
        }
        if (entity instanceof IteEntities0418.AnnihilationSkeletonEntity) {
            return level.getFluidState(entity.blockPosition()).isEmpty();
        }
        return true;
    }

    public static int dynamicSpawnCap(int baseCap, Level level) {
        if (level == null || !WorldContentMode.ite(level)) return baseCap;
        int day = IteMobScaling0418.getCappedIteDay(level);
        return baseCap + (int) Math.floor(DEFAULT_MOB_MAX_SPAWN_INCREASE_PER_DAY * day);
    }

    public static double weaponDamageMultiplier(Mob attacker) {
        if (attacker == null || !WorldContentMode.ite(attacker.level())) return 1.0D;
        ItemStack held = attacker.getMainHandItem();
        if (!held.isEmpty() && (held.getItem() instanceof TieredItem || held.getItem() instanceof SwordItem || held.getItem() instanceof DiggerItem)) {
            if (attacker instanceof IteEntities0418.ZombieLordEntity) return 1.35D;
            if (attacker instanceof SourceBoneLord lord && lord.ancient()) return 1.30D;
            return 1.25D;
        }
        return 1.0D;
    }

    public static boolean canShatterEndCrystal(ItemStack held, Entity attacker) {
        if (held == null || held.isEmpty()) return false;
        if (IteEnchantments0417.isVibraniumEquipment(held)) return true;
        if (held.getItem() instanceof TieredItem tiered) {
            return tiered.getTier().getLevel() >= 6;
        }
        return false;
    }

    public static int randomBreedExp(RandomSource rand) {
        return rand.nextInt(7) + 1;
    }

    /** Livestock illness death counter helper (death_counter NBT) matching upstream EntityLivestockTrans. */
    public static boolean tickAnimalIllness(Animal animal, boolean isWell, int threshold) {
        if (animal == null || !WorldContentMode.ite(animal.level())) return false;
        CompoundTag pdata = animal.getPersistentData();
        int counter = pdata.getInt("death_counter");
        if (!isWell) {
            counter++;
            pdata.putInt("death_counter", counter);
            if (counter >= threshold) {
                animal.kill();
                return true;
            }
        } else if (counter > 0) {
            pdata.putInt("death_counter", Math.max(0, counter - 2));
        }
        return false;
    }

    /** Experience orb value cap check matching EntityExperienceOrbTrans (256,000 max orb). */
    public static int cappedXpValue(int rawValue) {
        return Math.max(0, Math.min(256000, rawValue));
    }

    /** Prevents non-player / hopper auto-collection of player-dropped important loot in ITE mode when tagged. */
    public static boolean allowHopperPickup(ItemEntity item, Level level) {
        if (item == null || level == null || !WorldContentMode.ite(level)) return true;
        return !item.getPersistentData().getBoolean("MiteIteAntiHopperPickup");
    }

    /** Attempts ITE mob defense parry (10%) & optional disarm on attacker's held weapon. */
    public static boolean tryMobDefenseParry(Mob defender, Entity attacker, RandomSource rand, boolean allowDisarm) {
        if (defender == null || attacker == null || !WorldContentMode.ite(defender.level())) return false;
        if (defender.getMainHandItem().isEmpty()) return false;
        if (rand.nextInt(10) <= 8) return false;
        defender.level().playSound(null, defender.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 0.8F, 1.2F);
        if (allowDisarm && attacker instanceof ServerPlayer sp && sp.getMainHandItem().getItem() instanceof TieredItem) {
            if (rand.nextInt(3) == 0) {
                ItemStack held = sp.getMainHandItem().copy();
                sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ItemEntity dropped = sp.drop(held, true);
                if (dropped != null) {
                    dropped.getPersistentData().putBoolean("MiteIteAntiHopperPickup", true);
                }
                sp.displayClientMessage(Component.translatable("miteite.msg.disarmed"), true);
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !WorldContentMode.ite(sl)) return;
        IteMobScaling0418.applyScaling(event.getEntity(), sl);
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !WorldContentMode.ite(sl)) return;
        if (event.getSpawnType() == MobSpawnType.NATURAL && !allowSpecialSpawn(event.getEntity(), sl)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !WorldContentMode.ite(sl)) return;
        Entity entity = event.getEntity();
        if (entity instanceof Mob mob) {
            IteMobScaling0418.applyScaling(mob, sl);
        } else if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof Skeleton skel) {
            int day = IteMobScaling0418.getCappedIteDay(sl);
            int punch = IteMobScaling0418.skeletonBowPunchBonus(day);
            if (punch > 0) {
                arrow.setKnockback(arrow.getKnockback() + punch);
            }
            if (IteEnchantments0417.isVibraniumEquipment(skel.getMainHandItem())) {
                arrow.setBaseDamage(arrow.getBaseDamage() * 1.35D);
            }
        } else if (entity instanceof ExperienceOrb orb) {
            int capped = cappedXpValue(orb.getValue());
            if (capped != orb.getValue()) {
                orb.value = capped;
            }
        }
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!WorldContentMode.ite(event.getEntity().level())) return;
        LivingEntity target = event.getNewTarget();
        if (target != null && !IteEntities0418.canBeTargeted(target, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide || !WorldContentMode.ite(event.getEntity().level())) return;
        if (event.getTarget() instanceof EndCrystal) {
            Player player = event.getEntity();
            if (!player.isCreative() && !canShatterEndCrystal(player.getMainHandItem(), player)) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("miteite.msg.crystal_locked"), true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        Level lvl = event.getEntity().level();
        if (lvl.isClientSide || !WorldContentMode.ite(lvl) || event.getSource().is(DamageTypes.THORNS)) return;
        Entity sourceEntity = event.getSource().getEntity();
        int day = IteMobScaling0418.getCappedIteDay(lvl);

        if (sourceEntity instanceof Mob attacker) {
            double mul = weaponDamageMultiplier(attacker);
            if (mul > 1.0D) {
                event.setAmount((float) (event.getAmount() * mul));
            }
            if (attacker instanceof Zombie && !(attacker instanceof ZombifiedPiglin)) {
                if (attacker.getRandom().nextFloat() < IteMobScaling0418.zombieFireChance(day)) {
                    event.getEntity().setSecondsOnFire(5);
                }
            } else if (attacker instanceof ZombifiedPiglin || attacker instanceof MagmaCube) {
                if (attacker.getRandom().nextFloat() < 0.35F) {
                    event.getEntity().setSecondsOnFire(5);
                }
            }
        }

        if (event.getEntity() instanceof Mob defender && sourceEntity != null) {
            if (tryMobDefenseParry(defender, sourceEntity, defender.getRandom(), false)) {
                event.setAmount(Math.max(0.0F, Math.min(Float.MAX_VALUE * 0.25F, event.getAmount() * 0.5F)));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!WorldContentMode.ite(event.getEntity().level())) return;
        if (event.getEntity() instanceof Mob mob && mob instanceof Giant && !(mob instanceof SourceGiantZombieBoss boss && boss.ritual())) {
            int day = IteMobScaling0418.getCappedIteDay(mob.level());
            event.getDrops().clear();
            Item reward = day >= 128 ? Content.get("mithril_ingot") : Content.get("ancient_metal_nugget");
            int count = day >= 128 ? 1 : (4 + mob.getRandom().nextInt(2));
            event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(reward, count)));
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getParentA() == null || !WorldContentMode.ite(event.getParentA().level())) return;
        if (event.getParentA().level() instanceof ServerLevel sl) {
            int xp = randomBreedExp(sl.getRandom());
            ExperienceOrb.award(sl, event.getParentA().position(), xp);
        }
    }
}

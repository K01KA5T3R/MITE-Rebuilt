package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * MITE-ITE 2.0.30 unified day-capped mob attribute, armor/enchantment rolling,
 * and creeper/dragon/golem scaling engine. Strictly gated behind WorldContentMode.ite(level).
 */
public final class IteMobScaling0418 {
    public static final String SCALED_TAG = "MiteIte0418Scaled";
    public static final String FUSE_TAG = "MiteIte0418Fuse";
    public static final String BOOSTED_TAG = "MiteIte0418Boosted";

    private IteMobScaling0418() {}

    public static int getCappedIteDay(Level level) {
        if (level == null || !WorldContentMode.ite(level)) return 0;
        Level overworld = level;
        if (level instanceof ServerLevel sl && sl.getServer().overworld() != null) {
            overworld = sl.getServer().overworld();
        }
        int rawDay = (int) Math.max(overworld.getDayTime() / 24000L, 0L);
        return Math.min(IteConfig.effective(level).enhanceLimit(), rawDay);
    }

    public static double zombieMaxHealth(int day) { return 15.0 + day / 16.0; }
    public static double zombieAttack(int day) { return 8.0 + day / 24.0; }
    public static float zombieFireChance(int day) { return Math.min(0.05F + day / 800.0F, 0.25F); }

    public static double skeletonMaxHealth(int day, boolean type2) { return (type2 ? 45.0 : 15.0) + day / (type2 ? 16.0 : 14.0); }
    public static double skeletonAttack(int day, boolean type2) { return (type2 ? 13.0 : 8.0) + day / 24.0; }
    public static int skeletonBowPunchBonus(int day) { return day >= 64 ? Math.min(1 + (day - 64) / 48, 5) : 0; }

    public static double creeperMaxHealth(int day) { return 20.0 + day / 8.0; }
    public static int creeperFuseTicks(int day) { return Math.max(30 * 3 - (int) (day * 0.6D), 40); }
    public static float creeperRadius(boolean infernal) { return infernal ? 2.5F : 1.25F; }

    public static double boneLordMaxHealth(int day) { return 35.0 + day / 16.0; }
    public static double boneLordAttack(int day) { return 10.0 + day / 20.0; }

    public static double ancientBoneLordMaxHealth(int day) { return 40.0 + day / 12.0; }
    public static double ancientBoneLordAttack(int day) { return 13.0 + day / 20.0; }

    public static double giantMaxHealth(int day) { return 100.0 + day / 16.0; }
    public static double giantAttack(int day) { return 25.0 + day / 16.0; }

    public static double zombiePigmanMaxHealth(int day) { return 40.0 + day / 16.0; }
    public static double zombiePigmanAttack(int day) { return 12.0 + day / 20.0; }

    public static double spiderMaxHealth(double base, int day) { return base * 1.5D + day / 24.0D; }
    public static double spiderAttack(double base, int day) { return base + day / 24.0D; }

    public static double zombieLordMaxHealth(int day) { return 50.0 + day / 14.0D; }
    public static double zombieLordAttack(int day) { return 12.0 + day / 48.0D; }

    public static double ironGolemMaxHealth(int day) { return 200.0 + day / 26.0D; }
    public static double ironGolemAttack(int day) { return 25.0 + day / 16.0D; }

    public static double enderDragonMaxHealth() { return 1250.0D; }

    public static double blazeMaxHealth() { return 40.0D; }
    public static double ghastMaxHealth() { return 15.0D; }

    public static int getRandomItemTier(RandomSource rand, int day) {
        int maxTier = Math.max(1, Math.min(10, day / 16));
        int minTier = Math.min(maxTier, rand.nextInt(2 + Math.min(day / 64, 6)) + 1);
        int now = minTier;
        while (now < maxTier && rand.nextInt(Math.max(2 * now + 1 - day / 12, 1)) == 0) {
            now++;
        }
        if (day > 365 && now < 10 && rand.nextInt(4) == 0) {
            now++;
        }
        return Math.max(1, Math.min(10, now));
    }

    public static Item armorPieceForTier(int tier, EquipmentSlot slot) {
        String mat = switch (Math.max(1, Math.min(10, tier))) {
            case 1 -> "leather";
            case 2 -> "rusted_iron";
            case 3 -> "copper";
            case 4 -> "silver";
            case 5 -> "gold";
            case 6 -> "iron";
            case 7 -> "mithril";
            case 8 -> "adamantium";
            case 9 -> "ancient_metal";
            default -> "vibranium";
        };
        if ("leather".equals(mat)) {
            return switch (slot) {
                case HEAD -> Items.LEATHER_HELMET;
                case CHEST -> Items.LEATHER_CHESTPLATE;
                case LEGS -> Items.LEATHER_LEGGINGS;
                default -> Items.LEATHER_BOOTS;
            };
        }
        if ("gold".equals(mat)) {
            return switch (slot) {
                case HEAD -> Items.GOLDEN_HELMET;
                case CHEST -> Items.GOLDEN_CHESTPLATE;
                case LEGS -> Items.GOLDEN_LEGGINGS;
                default -> Items.GOLDEN_BOOTS;
            };
        }
        if ("iron".equals(mat)) {
            return switch (slot) {
                case HEAD -> Items.IRON_HELMET;
                case CHEST -> Items.IRON_CHESTPLATE;
                case LEGS -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            };
        }
        String part = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            default -> "boots";
        };
        return Content.get(mat + "_" + part);
    }

    public static void addRandomEnchantments(int day, ItemStack stack, RandomSource rand, boolean elite) {
        if (stack.isEmpty()) return;
        double prob = elite ? (0.2D + day / 640.0D) : (0.1D + day / 640.0D);
        if (rand.nextDouble() >= prob) return;
        int lvl = Math.max(1, (int) (5.0F + day * 0.15F + (5 - rand.nextInt(10)) * rand.nextFloat()));
        int cap = Math.min(2 + day / 24, 15);
        int countCap = Math.min(1 + day / 72, 4);
        int added = 0;
        for (var entry : ClassicEnchanting.selectNative(rand, stack, Math.min(lvl, cap * 3), false)) {
            if (added >= countCap) break;
            stack.enchant(entry.enchantment, Math.min(cap, Math.max(1, entry.level)));
            added++;
        }
    }

    public static void addDefaultArmor(int day, Mob mob, boolean haveAll) {
        RandomSource rand = mob.getRandom();
        boolean triggered = haveAll || day > 365 || rand.nextInt(Math.max(1, 3 - Math.min(day / 128, 2))) == 0;
        if (!triggered) return;
        int tier = getRandomItemTier(rand, day);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!haveAll && day <= 192 && rand.nextInt(4) == 0) continue;
            if (!mob.getItemBySlot(slot).isEmpty()) continue;
            ItemStack piece = new ItemStack(armorPieceForTier(tier, slot));
            if (day > 64) {
                addRandomEnchantments(day, piece, rand, mob instanceof SourceBoneLord || mob instanceof IteEntities0418.ZombieLordEntity);
            }
            mob.setItemSlot(slot, piece);
            mob.setDropChance(slot, 0.0F);
        }
    }

    public static void applyScaling(Mob mob, ServerLevel level) {
        if (mob == null || level == null || !WorldContentMode.ite(level)) return;
        CompoundTag pdata = mob.getPersistentData();
        if (pdata.getBoolean(SCALED_TAG)) return;
        pdata.putBoolean(SCALED_TAG, true);

        int day = getCappedIteDay(level);
        if (mob instanceof IteEntities0418.ZombieLordEntity) {
            setBase(mob.getAttribute(Attributes.MAX_HEALTH), zombieLordMaxHealth(day));
            setBase(mob.getAttribute(Attributes.ATTACK_DAMAGE), zombieLordAttack(day));
            setBase(mob.getAttribute(Attributes.MOVEMENT_SPEED), 0.30D);
            mob.setHealth(mob.getMaxHealth());
            return;
        }
        if (mob instanceof IteEntities0418.AnnihilationSkeletonEntity || mob instanceof IteEntities0418.WanderingWitchEntity || mob instanceof IteEntities0418.AncientDragonEntity) {
            return;
        }
        if (mob instanceof IronGolem golem) {
            setBase(golem.getAttribute(Attributes.MAX_HEALTH), ironGolemMaxHealth(day));
            setBase(golem.getAttribute(Attributes.ATTACK_DAMAGE), ironGolemAttack(day));
            setBase(golem.getAttribute(Attributes.MOVEMENT_SPEED), 0.25D);
            pdata.putBoolean(BOOSTED_TAG, true);
            golem.setHealth(golem.getMaxHealth());
            return;
        }
        if (mob instanceof EnderDragon dragon) {
            setBase(dragon.getAttribute(Attributes.MAX_HEALTH), enderDragonMaxHealth());
            dragon.setHealth(dragon.getMaxHealth());
            return;
        }
        if (mob instanceof SourceCreeper creeper) {
            setBase(creeper.getAttribute(Attributes.MAX_HEALTH), creeperMaxHealth(day));
            setBase(creeper.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            int fuse = creeperFuseTicks(day);
            creeper.iteFuse(fuse);
            creeper.iteRadius(creeperRadius(creeper.infernal()));
            pdata.putInt(FUSE_TAG, fuse);
            creeper.setHealth(creeper.getMaxHealth());
            return;
        }
        if (mob instanceof Creeper creeper) {
            setBase(creeper.getAttribute(Attributes.MAX_HEALTH), creeperMaxHealth(day));
            setBase(creeper.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            int fuse = creeperFuseTicks(day);
            CompoundTag nbt = new CompoundTag();
            creeper.addAdditionalSaveData(nbt);
            nbt.putShort("Fuse", (short) fuse);
            creeper.readAdditionalSaveData(nbt);
            pdata.putInt(FUSE_TAG, fuse);
            creeper.setHealth(creeper.getMaxHealth());
            return;
        }
        if (mob instanceof SourceBoneLord lord) {
            if (lord.ancient()) {
                setBase(lord.getAttribute(Attributes.MAX_HEALTH), ancientBoneLordMaxHealth(day));
                setBase(lord.getAttribute(Attributes.ATTACK_DAMAGE), ancientBoneLordAttack(day));
                setBase(lord.getAttribute(Attributes.FOLLOW_RANGE), 48.0D);
                setBase(lord.getAttribute(Attributes.MOVEMENT_SPEED), 0.30D);
                addDefaultArmor(Math.max(day - 16, 0), lord, true);
            } else {
                setBase(lord.getAttribute(Attributes.MAX_HEALTH), boneLordMaxHealth(day));
                setBase(lord.getAttribute(Attributes.ATTACK_DAMAGE), boneLordAttack(day));
                setBase(lord.getAttribute(Attributes.FOLLOW_RANGE), 44.0D);
                setBase(lord.getAttribute(Attributes.MOVEMENT_SPEED), 0.27D);
                if (day >= 96) addDefaultArmor(day, lord, true);
            }
            lord.setHealth(lord.getMaxHealth());
            return;
        }
        if (mob instanceof SourceRevenant revenant) {
            setBase(revenant.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            if (day >= 128) {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    revenant.setItemSlot(slot, ItemStack.EMPTY);
                }
                addDefaultArmor(day, revenant, true);
            }
            revenant.setHealth(revenant.getMaxHealth());
            return;
        }
        if (mob instanceof Skeleton skel) {
            boolean type2 = skel instanceof SourceSkeleton ss && ss.skeletonType() == 2;
            setBase(skel.getAttribute(Attributes.MAX_HEALTH), skeletonMaxHealth(day, type2));
            setBase(skel.getAttribute(Attributes.ATTACK_DAMAGE), skeletonAttack(day, type2));
            setBase(skel.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            setBase(skel.getAttribute(Attributes.MOVEMENT_SPEED), type2 ? 0.30D : 0.25D);
            addDefaultArmor(day, skel, false);
            skel.setHealth(skel.getMaxHealth());
            return;
        }
        if (mob instanceof ZombifiedPiglin pigman) {
            setBase(pigman.getAttribute(Attributes.MAX_HEALTH), zombiePigmanMaxHealth(day));
            setBase(pigman.getAttribute(Attributes.ATTACK_DAMAGE), zombiePigmanAttack(day));
            setBase(pigman.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            setBase(pigman.getAttribute(Attributes.MOVEMENT_SPEED), 0.25D);
            pdata.putBoolean(BOOSTED_TAG, true);
            pigman.setHealth(pigman.getMaxHealth());
            return;
        }
        if (mob instanceof Zombie zombie) {
            if (zombie instanceof IteEliteSpawns.Host host && host.iteElite()) return;
            setBase(zombie.getAttribute(Attributes.MAX_HEALTH), zombieMaxHealth(day));
            setBase(zombie.getAttribute(Attributes.ATTACK_DAMAGE), zombieAttack(day));
            setBase(zombie.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            setBase(zombie.getAttribute(Attributes.MOVEMENT_SPEED), 0.23D);
            if (level.dimension() == Level.NETHER && day < 64) {
                addDefaultArmor(64, zombie, true);
            } else {
                addDefaultArmor(day, zombie, false);
            }
            applyDayNightPotions(day, level, zombie);
            zombie.setHealth(zombie.getMaxHealth());
            return;
        }
        if (mob instanceof Giant giant) {
            setBase(giant.getAttribute(Attributes.MAX_HEALTH), giantMaxHealth(day));
            setBase(giant.getAttribute(Attributes.ATTACK_DAMAGE), giantAttack(day));
            setBase(giant.getAttribute(Attributes.MOVEMENT_SPEED), 0.30D);
            AttributeInstance follow = giant.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null) follow.setBaseValue(follow.getBaseValue() * 2.0D);
            giant.setHealth(giant.getMaxHealth());
            return;
        }
        if (mob instanceof Spider spider) {
            double baseHp = spider.getAttributeBaseValue(Attributes.MAX_HEALTH);
            double baseAtk = spider.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            setBase(spider.getAttribute(Attributes.MAX_HEALTH), spiderMaxHealth(baseHp, day));
            setBase(spider.getAttribute(Attributes.ATTACK_DAMAGE), spiderAttack(baseAtk, day));
            setBase(spider.getAttribute(Attributes.FOLLOW_RANGE), 64.0D);
            spider.setHealth(spider.getMaxHealth());
            return;
        }
        if (mob instanceof Blaze blaze) {
            setBase(blaze.getAttribute(Attributes.MAX_HEALTH), blazeMaxHealth());
            setBase(blaze.getAttribute(Attributes.ATTACK_DAMAGE), 6.0D);
            blaze.setHealth(blaze.getMaxHealth());
            return;
        }
        if (mob instanceof Ghast ghast) {
            setBase(ghast.getAttribute(Attributes.MAX_HEALTH), ghastMaxHealth());
            ghast.setHealth(ghast.getMaxHealth());
            return;
        }
        if (mob instanceof MagmaCube magma) {
            AttributeInstance hp = magma.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(hp.getBaseValue() + 30.0D);
            magma.setHealth(magma.getMaxHealth());
        }
    }

    private static void applyDayNightPotions(int day, ServerLevel level, Mob mob) {
        if (day <= 32) return;
        long timeOfDay = level.getDayTime() % 24000L;
        boolean night = timeOfDay >= 12000L || day > 128;
        if (!night) return;
        RandomSource rand = mob.getRandom();
        int spdAmp = Math.min(rand.nextInt(Math.max((day - 32) / 96, 1)), 4);
        if (spdAmp > 0) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, spdAmp - 1, false, false));
        }
        if (day > 96 && rand.nextInt(5) == 0) {
            int strAmp = Math.min((day - 96) / 32, 3);
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, strAmp, false, false));
        }
    }

    private static void setBase(AttributeInstance inst, double value) {
        if (inst != null) inst.setBaseValue(value);
    }
}

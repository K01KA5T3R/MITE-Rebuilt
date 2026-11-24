package dev.koikaster.mite;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class KingSlimeRewards {
    private static final int HOOK_REWARD = 1;
    private static final int HOOD_REWARD = 2;
    private static final int CUSHION_REWARD = 4;
    private static final RegistryObject<Item> HOOK = Content.item("slime_hook", Hook::new);
    private static final RegistryObject<Item> HOOD = Content.item("ninja_hood", Hood::new);
    private static final RegistryObject<Item> CUSHION = Content.item("slime_cushion", Cushion::new);

    private KingSlimeRewards() {}

    static void register() {
        // Load these items before the common item register is attached to the mod bus.
    }

    static void drop(ServerLevel level, SourceKingSlime boss) {
        var rewards = level.getServer().overworld().getDataStorage().computeIfAbsent(FirstVictory::load,
            FirstVictory::new, "mite_king_slime_rewards");
        boss.spawnAtLocation(new ItemStack(Items.SLIME_BALL, 6 + boss.getRandom().nextInt(4)));
        rewards.dropOnce(boss, HOOK.get(), HOOK_REWARD);
        rewards.dropOnce(boss, HOOD.get(), HOOD_REWARD);
        rewards.dropOnce(boss, CUSHION.get(), CUSHION_REWARD);
    }

    static double detectionMultiplier(Entity observer, LivingEntity target) {
        return observer instanceof Enemy && target instanceof Player && target.getItemBySlot(EquipmentSlot.HEAD).is(HOOD.get())
            ? .70 : 1;
    }

    static boolean canDetect(Mob observer, LivingEntity target, double horizontal, double vertical) {
        double multiplier = detectionMultiplier(observer, target);
        return multiplier == 1 || observer.getBoundingBox().inflate(horizontal * multiplier, vertical * multiplier,
            horizontal * multiplier).intersects(target.getBoundingBox());
    }

    @SubscribeEvent
    public static void nativeVisibility(LivingEvent.LivingVisibilityEvent event) {
        double multiplier = detectionMultiplier(event.getLookingEntity(), event.getEntity());
        if (multiplier != 1) event.modifyVisibility(multiplier);
    }

    private static final class FirstVictory extends SavedData {
        private int drops;

        private static FirstVictory load(CompoundTag tag) {
            var rewards = new FirstVictory();
            rewards.drops = tag.getInt("Drops");
            return rewards;
        }

        private void dropOnce(SourceKingSlime boss, Item item, int flag) {
            if ((drops & flag) != 0) return;
            var reward = new ItemStack(item);
            // A world-first reward is finite even when it has a metal armor profile.
            MineralSupply.stock(reward);
            if (boss.spawnAtLocation(reward) == null) return;
            drops |= flag;
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("Drops", drops);
            return tag;
        }
    }

    private static final class Hood extends ArmorItem {
        private Hood() {
            super(ArmorMaterials.IRON, Type.HELMET, new Properties());
        }

        @Override
        public String getArmorTexture(ItemStack stack, Entity wearer, EquipmentSlot slot, String layer) {
            return "mite_reforged:textures/models/armor/ninja_hood_layer_1.png";
        }
    }

    private static final class Hook extends Item {
        private Hook() {
            super(new Properties().stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!hasWall(level, player)) return InteractionResultHolder.pass(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            if (player == null || context.getClickedFace().getAxis().isVertical() || !hasWall(context.getLevel(), player)) {
                return InteractionResult.PASS;
            }
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return 72000;
        }

        @Override
        public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
            if (!(entity instanceof Player player) || !hasWall(level, player)) {
                entity.stopUsingItem();
                return;
            }
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, .19, motion.z);
            player.fallDistance = 0;
        }

        private static boolean hasWall(Level level, Player player) {
            if (player.isSpectator() || !player.isAlive() || player.isPassenger() || player.isInWater() || player.isInLava()) {
                return false;
            }
            Direction facing = player.getDirection();
            var body = player.getBoundingBox();
            var handHeight = new AABB(body.minX + .03, body.minY + .27, body.minZ + .03,
                body.maxX - .03, body.minY + .77, body.maxZ - .03);
            var probe = handHeight.expandTowards(facing.getStepX() * .22, 0, facing.getStepZ() * .22);
            return level.getBlockCollisions(player, probe).iterator().hasNext();
        }
    }

    private static final class Cushion extends Item {
        private static final int SLOW_FALLING_TICKS = 5 * 20;
        private static final int COOLDOWN_TICKS = 30 * 20;

        private Cushion() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS));
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}

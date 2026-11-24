package dev.koikaster.mite;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class StingerBoomerang extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(StingerBoomerang.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_FLIGHT_TICKS = 82;
    private static final int OWNER_GRACE_TICKS = 200;
    private ItemStack payload = ItemStack.EMPTY;
    private UUID thrower;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private int throwSlot = -1;
    private int missingOwnerTicks;
    private int flightTicks;

    StingerBoomerang(EntityType<? extends StingerBoomerang> type, Level level) { super(type, level); }

    void launch(Player player, InteractionHand hand, ItemStack payload) {
        this.payload = payload;
        this.thrower = player.getUUID();
        this.hand = hand;
        this.throwSlot = player.getInventory().selected;
        setOwner(player);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(.43)).add(0, -.14, 0);
        setPos(start);
        shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.36f, 0);
    }

    @Override
    protected void defineSynchedData() { entityData.define(RETURNING, false); }

    @Override
    public ItemStack getItem() { return new ItemStack(QueenBeeContent.BOOMERANG.get()); }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            if (tickCount % 3 == 0) level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        ++flightTicks;
        if (entityData.get(RETURNING)) {
            returnTick(level);
        } else {
            outboundTick(level);
        }
        updateRotation();
    }

    private void outboundTick(ServerLevel level) {
        Vec3 before = position();
        Vec3 velocity = getDeltaMovement();
        Vec3 next = before.add(velocity);
        if (flightTicks >= 14 || !level.hasChunkAt(BlockPos.containing(next))) {
            entityData.set(RETURNING, true);
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        hit = hitGiantPart(level, before, next, hit);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            if (hit instanceof EntityHitResult direct) {
                Entity target = direct.getEntity();
                if (target.hurt(level.damageSources().thrown(this, getOwner()), QueenBeeContent.BOOMERANG_DAMAGE)
                        && target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, QueenBeeContent.BOOMERANG_POISON, 0), getOwner());
                }
            }
            setPos(hit.getLocation().subtract(velocity.normalize().scale(.10)));
            entityData.set(RETURNING, true);
            level.playSound(null, blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, .8f, 1.25f);
        } else {
            setPos(next);
            setDeltaMovement(velocity.scale(.976).add(0, -.012, 0));
        }
    }

    private HitResult hitGiantPart(ServerLevel level, Vec3 start, Vec3 end, HitResult vanilla) {
        Vec3 limit = vanilla.getType() == HitResult.Type.MISS ? end : vanilla.getLocation();
        double closest = start.distanceToSqr(limit);
        HitResult result = vanilla;
        // The parent is intentionally unpickable. Forge adds its separate hitboxes to the level query.
        for (var entity : level.getEntities(this, new AABB(start, limit).inflate(.35),
                candidate -> candidate instanceof GiantZombieLeg part && part.canBeHitByProjectile())) {
            var part = (GiantZombieLeg) entity;
            var box = part.getBoundingBox().inflate(.15);
            var clip = part.hasLiftedHitbox() ? part.clipLifted(start, limit, .15)
                : box.contains(start) ? java.util.Optional.of(start) : box.clip(start, limit);
            if (clip.isEmpty()) continue;
            Vec3 point = clip.get();
            double distance = start.distanceToSqr(point);
            if (distance < closest && level.clip(new ClipContext(start, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                closest = distance;
                result = new EntityHitResult(part, point);
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settleDepartingFlight(player, player.serverLevel().getServer().getLevel(event.getFrom()));
        }
    }

    @SubscribeEvent
    public static void loggingOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settleDepartingFlight(player, player.serverLevel());
        }
    }

    private static void settleDepartingFlight(ServerPlayer player, ServerLevel origin) {
        if (origin == null || !player.isAlive() || player.isSpectator()) return;
        // Dimension changes run before the old chunks unload. Logout runs before the player's save.
        for (var flight : loadedFlights(player, origin)) flight.deliver(player);
    }

    static void captureDeathFlights(ServerPlayer player, Collection<ItemEntity> drops) {
        var level = player.serverLevel();
        boolean keepInventory = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        for (var flight : loadedFlights(player, level)) {
            if (!keepInventory || !flight.returnToInventory(player)) {
                drops.add(flight.dropItem(level, player.position().add(0, .35, 0)));
                flight.payload = ItemStack.EMPTY;
            }
            flight.discard();
        }
    }

    private static List<? extends StingerBoomerang> loadedFlights(ServerPlayer player, ServerLevel level) {
        return level.getEntities(QueenBeeContent.THROWN_BOOMERANG.get(), flight ->
            !flight.isRemoved() && !flight.payload.isEmpty() && player.getUUID().equals(flight.thrower));
    }

    private void returnTick(ServerLevel level) {
        ServerPlayer player = thrower == null ? null : level.getServer().getPlayerList().getPlayer(thrower);
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
            setDeltaMovement(Vec3.ZERO);
            if (++missingOwnerTicks >= OWNER_GRACE_TICKS) drop(level, position());
            return;
        }
        missingOwnerTicks = 0;
        if (player.level() != level || flightTicks > MAX_FLIGHT_TICKS) {
            deliver(player);
            return;
        }
        Vec3 catchPoint = player.getEyePosition().add(0, -.28, 0);
        Vec3 diff = catchPoint.subtract(position());
        if (diff.lengthSqr() < 1.25) {
            deliver(player);
            return;
        }
        Vec3 step = diff.normalize().scale(Math.min(1.42, diff.length()));
        Vec3 next = position().add(step);
        if (!level.hasChunkAt(BlockPos.containing(next))) {
            deliver(player);
            return;
        }
        setDeltaMovement(step);
        setPos(next);
    }

    private void deliver(ServerPlayer player) {
        if (!payload.isEmpty() && !returnToInventory(player)) {
            drop(player.serverLevel(), player.position().add(0, .35, 0));
            return;
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, .7f, 1.2f);
        discard();
    }

    private boolean returnToInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        InteractionHand returnHand = hand == InteractionHand.OFF_HAND && !ModernOffhand.allowed(player, payload)
                ? InteractionHand.MAIN_HAND : hand;
        if (player.getItemInHand(returnHand).isEmpty()) {
            player.setItemInHand(returnHand, payload);
        } else {
            int slot = hand == InteractionHand.MAIN_HAND && throwSlot >= 0 && throwSlot < 9
                    && inventory.getItem(throwSlot).isEmpty() ? throwSlot : inventory.getFreeSlot();
            if (slot < 0) return false;
            inventory.setItem(slot, payload);
        }
        payload = ItemStack.EMPTY;
        inventory.setChanged();
        return true;
    }

    private void drop(ServerLevel level, Vec3 position) {
        if (!payload.isEmpty()) {
            if (!level.addFreshEntity(dropItem(level, position))) return;
            payload = ItemStack.EMPTY;
        }
        discard();
    }

    private ItemEntity dropItem(ServerLevel level, Vec3 position) {
        var item = new ItemEntity(level, position.x, position.y, position.z, payload.copy());
        item.setTarget(thrower);
        item.setDefaultPickUpDelay();
        return item;
    }

    @Override
    protected boolean canHitEntity(Entity candidate) {
        if (!(candidate instanceof LivingEntity) || candidate == getOwner()) return false;
        if (candidate instanceof Player target && getOwner() instanceof Player thrower && !thrower.canHarmPlayer(target)) return false;
        return super.canHitEntity(candidate);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Returning", entityData.get(RETURNING));
        tag.putInt("FlightTicks", flightTicks);
        tag.putBoolean("OffHand", hand == InteractionHand.OFF_HAND);
        tag.putInt("ThrowSlot", throwSlot);
        if (!payload.isEmpty()) tag.put("Payload", payload.save(new CompoundTag()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RETURNING, tag.getBoolean("Returning"));
        flightTicks = Math.max(0, tag.getInt("FlightTicks"));
        thrower = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        hand = tag.getBoolean("OffHand") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        throwSlot = tag.contains("ThrowSlot") ? tag.getInt("ThrowSlot") : -1;
        payload = ItemStack.of(tag.getCompound("Payload"));
        if (!payload.is(QueenBeeContent.BOOMERANG.get())) payload = ItemStack.EMPTY;
        else payload.setCount(1);
    }
}

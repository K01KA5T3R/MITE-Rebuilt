package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class ElderRitualContent {
    static final RegistryObject<Item> CALL = Content.item("monument_call", Call::new);
    static final RegistryObject<Item> TROPHY = BossTrophies.register("elder_guardian_trophy", Blocks.PRISMARINE_BRICKS);
    static final ResourceKey<Structure> CLASSIC_MONUMENT =
        ResourceKey.create(Registries.STRUCTURE, Content.id("classic_monument"));

    private ElderRitualContent() {}

    static void register() {}

    static AABB monument(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD || !level.hasChunkAt(pos)) return null;
        var structures = level.structureManager();
        var start = structures.getStructureWithPieceAt(pos, BuiltinStructures.OCEAN_MONUMENT);
        if (!start.isValid()) start = structures.getStructureWithPieceAt(pos, CLASSIC_MONUMENT);
        if (!start.isValid()) return null;
        var box = start.getBoundingBox();
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }

    public static void populateMonument(WorldGenLevel level, BoundingBox bounds, BlockPos at) {
        if (!bounds.isInside(at)) return;
        var type = WorldContentMode.rebuiltBosses(level.getLevel()) ? EntityType.GUARDIAN : EntityType.ELDER_GUARDIAN;
        var guardian = type.create(level.getLevel());
        if (guardian == null) return;
        guardian.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
        guardian.setPersistenceRequired();
        level.addFreshEntityWithPassengers(guardian);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void replaceOldElder(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().getType() != EntityType.ELDER_GUARDIAN
                || !WorldContentMode.rebuiltBosses(level)
                || event.getEntity() instanceof SourceElderGuardianBoss boss && boss.ritual()) return;
        event.setCanceled(true);
        var elder = event.getEntity();
        var server = level.getServer();
        // Loaded entities are offered to the manager while the chunk is being integrated.
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
            if (level.getEntity(elder.getUUID()) != null) return;
            var replacement = EntityType.GUARDIAN.create(level);
            if (replacement == null) return;
            replacement.setUUID(elder.getUUID());
            replacement.moveTo(elder.getX(), elder.getY(), elder.getZ(), elder.getYRot(), elder.getXRot());
            replacement.setCustomName(elder.getCustomName());
            replacement.setCustomNameVisible(elder.isCustomNameVisible());
            if (elder instanceof LivingEntity living && living.isAlive()) {
                replacement.setHealth(replacement.getMaxHealth() * (living.getHealth() / living.getMaxHealth()));
            }
            replacement.setPersistenceRequired();
            level.addFreshEntity(replacement);
        }));
    }

    @SubscribeEvent
    public static void restoreFatigue(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SourceElderGuardianBoss.restoreFatigueOwner(player);
    }

    @SubscribeEvent
    public static void captureFatigueOverlap(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SourceElderGuardianBoss.rememberExternalFatigue(player, event.getOldEffectInstance(), event.getEffectInstance());
        }
    }

    @SubscribeEvent
    public static void tickFatigueOverlap(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            SourceElderGuardianBoss.tickFatigue(player, true);
        }
    }

    @SubscribeEvent
    public static void saveFatigueOwner(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SourceElderGuardianBoss.tickFatigue(player, false);
    }

    @SubscribeEvent
    public static void leaveFatigueDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SourceElderGuardianBoss.restoreFatigueOwner(player);
    }

    private static final class Call extends Item {
        Call() { super(new Properties().stacksTo(16)); }

        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!WorldContentMode.rebuiltBosses(level) || level.dimension() != Level.OVERWORLD || !player.isEyeInFluid(FluidTags.WATER)) return InteractionResultHolder.pass(stack);
            if (level instanceof ServerLevel server) {
                BlockPos at = player.blockPosition();
                AABB arena = monument(server, at);
                if (arena == null || !(player instanceof ServerPlayer) || !level.mayInteract(player, at)
                        || !SourceElderGuardianBoss.summon(server, player.position(), player.getYRot() + 180, arena)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}

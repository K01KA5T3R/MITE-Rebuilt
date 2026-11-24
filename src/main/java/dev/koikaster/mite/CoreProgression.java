package dev.koikaster.mite;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class CoreProgression {
    public static final ResourceKey<Level> WORLD = ResourceKey.create(Registries.DIMENSION, Content.id("core"));
    public static final ResourceKey<DamageType> HEAT = ResourceKey.create(Registries.DAMAGE_TYPE, Content.id("core_heat"));
    public static final int SKY_FRAME_Y = 319;
    public static final int HEAT_INTERVAL = 20;

    public static final net.minecraftforge.registries.RegistryObject<Block> HIGH_HEAT_ORE = Content.block("high_heat_ore",
        () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).strength(5.0F, 7.5F).lightLevel(state -> 5)),
        () -> new Item.Properties().fireResistant());
    public static final net.minecraftforge.registries.RegistryObject<Item> HIGH_ENERGY_EYE = Content.item("high_energy_ender_eye",
        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final net.minecraftforge.registries.RegistryObject<Block> ENDER_OBSIDIAN = Content.block("ender_obsidian",
        () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
    public static final net.minecraftforge.registries.RegistryObject<SkyEndPortal> SKY_PORTAL = Content.BLOCKS.register("sky_end_portal",
        () -> new SkyEndPortal(BlockBehaviour.Properties.copy(Blocks.NETHER_PORTAL).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    static void register() {}

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || !WorldContentMode.rebuilt(player.level()) || player.level().dimension() != WORLD
            || !player.isAlive() || player.isCreative() || player.isSpectator()
            || player.tickCount % HEAT_INTERVAL != 0 || player.hasEffect(MobEffects.FIRE_RESISTANCE)) return;

        var heat = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEAT);
        player.hurt(new DamageSource(heat), 1.0F);
    }

    private CoreProgression() {}
}

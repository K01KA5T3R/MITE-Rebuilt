package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;

/** Admission for queued main-channel state packets; existing state application stays unchanged. */
public final class ClientStateConnectionGate {
    private ClientStateConnectionGate() {}

    /** Check at execution time, after any queued disconnect or new-login work. */
    public static void receive(CompoundTag data, Connection sender) {
        if (current(sender)) ClientState.receive(data);
    }

    public static void crafting(String key, int progress, int total, Connection sender) {
        if (current(sender)) ClientState.crafting(key, progress, total);
    }

    public static void shadowVision(float value, Connection sender) {
        if (current(sender)) OriginalShadowVision.receive(value);
    }

    private static boolean current(Connection sender) {
        var client = Minecraft.getInstance();
        var listener = client.getConnection();
        return sender != null && listener != null && listener.getConnection() == sender;
    }
}

package dev.koikaster.mite;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;

/** Offline LAN clients may not possess the authority key for a signed host. */
public final class OfflineChat1158 {
    public static boolean send(OutgoingChatMessage.Player outgoing, ServerPlayer recipient,
                               boolean filter, ChatType.Bound type) {
        if (recipient.server.usesAuthentication() || !outgoing.message().hasSignature()) return false;
        var message = outgoing.message().filter(filter);
        if (!message.isFullyFiltered()) {
            var content = message.filterMask().isEmpty() ? message.decoratedContent()
                    : message.filterMask().applyWithFormatting(message.signedContent());
            if (content != null) recipient.connection.sendDisguisedChatMessage(content, type);
        }
        return true;
    }

    private OfflineChat1158() {}
}

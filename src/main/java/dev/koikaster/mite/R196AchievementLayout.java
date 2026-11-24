package dev.koikaster.mite;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import dev.koikaster.mite.SourceAchievementCatalog561.Node;

/** Pose and parent changes extracted from the R196 achievement definitions. */
final class R196AchievementLayout {
    static final int BETTER_TOOLS = 135;
    private static final String RESOURCE = "/data/mite_reforged/source_achievements196_changes.json";

    static List<Node> apply(List<Node> original) {
        try (var stream = R196AchievementLayout.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing R196 achievement changes");
            var layout = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var patches = new HashMap<Integer, JsonObject>();
            for (var entry : layout.getAsJsonArray("nodes")) {
                var patch = entry.getAsJsonObject();
                if (patches.put(patch.get("id").getAsInt(), patch) != null) {
                    throw new IOException("Duplicate R196 achievement change");
                }
            }
            var added = addedNode(layout.getAsJsonObject("added"));
            var result = new ArrayList<Node>(original.size() + 1);
            for (var node : original) {
                var patch = patches.remove(node.id());
                result.add(patch == null ? node : replace(node, patch));
                if (node.id() == added.parent()) result.add(added);
            }
            if (!patches.isEmpty() || result.size() != original.size() + 1 || added.id() != BETTER_TOOLS) {
                throw new IOException("Unmatched R196 achievement changes");
            }
            return List.copyOf(result);
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Node addedNode(JsonObject json) {
        return new Node(json.get("id").getAsInt(), json.get("name").getAsString(),
            json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("parent").getAsInt(),
            json.get("second_parent").getAsInt(), json.get("challenge").getAsBoolean(),
            json.get("flipped").getAsBoolean(), json.get("tooltip_width").getAsInt(),
            json.get("icon").getAsString(), json.get("title").getAsString(), json.get("description").getAsString());
    }

    private static Node replace(Node old, JsonObject patch) {
        return new Node(old.id(), old.name(), value(patch, "x", old.x()), value(patch, "y", old.y()),
            value(patch, "parent", old.parent()), old.secondParent(), old.challenge(), old.flipped(),
            old.tooltipWidth(), patch.has("icon") ? patch.get("icon").getAsString() : old.icon(),
            old.title(), old.description());
    }

    private static int value(JsonObject patch, String key, int original) {
        return patch.has(key) ? patch.get(key).getAsInt() : original;
    }

    private R196AchievementLayout() {}
}

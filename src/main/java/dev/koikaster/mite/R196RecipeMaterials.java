package dev.koikaster.mite;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RepairItemRecipe;

/** R196 RecipeHelper material tags captured from the original registered recipes. */
final class R196RecipeMaterials {
    private static final String RESOURCE = "/data/mite_reforged/source_r196_recipe_materials.json";
    private static final int ANY_SUBTYPE = Short.MAX_VALUE;
    private record Component(SourceSkillFacts1710.Identity identity, int count) {
        boolean matches(Component actual) {
            return identity.id() == actual.identity.id() && (identity.subtype() == ANY_SUBTYPE
                || identity.subtype() == actual.identity.subtype()) && count <= actual.count;
        }
    }
    private record Route(int amount, int grade, List<Component> ingredients) {}
    private record Facts(Map<SourceSkillFacts1710.Identity, Integer> fixed,
                         Map<SourceSkillFacts1710.Identity, List<Route>> choices, Map<Integer, Integer> repair) {}

    private static final class Original {
        private static final Facts FACTS = load();
    }

    static OptionalInt grade(Recipe<?> recipe, CraftingContainer grid, ItemStack output) {
        if (recipe == null) return OptionalInt.empty();
        var namespace = recipe.getId().getNamespace();
        if (!namespace.equals("minecraft") && !namespace.equals(MiteReforged.ID)) return OptionalInt.empty();
        var item = SourceSkillFacts1710.original(output);
        if (item == null) return OptionalInt.empty();
        var facts = Original.FACTS;
        if (recipe instanceof ClassicCraftRepair || recipe instanceof RepairItemRecipe) {
            return OptionalInt.of(facts.repair().getOrDefault(item.id(), 0));
        }
        Integer fixed = facts.fixed().get(item);
        if (fixed != null) return OptionalInt.of(fixed);
        var choices = facts.choices().get(item);
        if (choices == null) return OptionalInt.empty();
        var actual = new ArrayList<Component>();
        for (var stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            var identity = SourceSkillFacts1710.original(stack);
            if (identity == null) return OptionalInt.empty();
            actual.add(new Component(identity, stack.getCount()));
        }
        Integer grade = null;
        for (var route : choices) {
            if (route.amount() != output.getCount() || route.ingredients().size() != actual.size()
                || !matches(route.ingredients(), actual, 0, 0)) continue;
            if (grade != null && grade != route.grade()) return OptionalInt.empty();
            grade = route.grade();
        }
        return value(grade);
    }

    private static boolean matches(List<Component> required, List<Component> actual, int index, int used) {
        if (index == required.size()) return true;
        for (int slot = 0; slot < actual.size(); slot++) {
            int bit = 1 << slot;
            if ((used & bit) == 0 && required.get(index).matches(actual.get(slot))
                && matches(required, actual, index + 1, used | bit)) return true;
        }
        return false;
    }

    private static OptionalInt value(Integer grade) {
        return grade == null ? OptionalInt.empty() : OptionalInt.of(grade);
    }

    private static Facts load() {
        try (var stream = R196RecipeMaterials.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing R196 recipe material facts");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var fixed = new HashMap<SourceSkillFacts1710.Identity, Integer>();
            root.getAsJsonObject("fixed").entrySet().forEach(entry -> fixed.put(identity(entry.getKey()), entry.getValue().getAsInt()));
            var choices = new HashMap<SourceSkillFacts1710.Identity, List<Route>>();
            root.getAsJsonObject("choices").entrySet().forEach(entry -> {
                var routes = new ArrayList<Route>();
                for (var raw : entry.getValue().getAsJsonArray()) {
                    var entryValue = raw.getAsJsonObject();
                    var ingredients = new ArrayList<Component>();
                    for (var rawPart : entryValue.getAsJsonArray("ingredients")) {
                        var part = rawPart.getAsJsonArray();
                        ingredients.add(new Component(new SourceSkillFacts1710.Identity(part.get(0).getAsInt(), part.get(1).getAsInt()), part.get(2).getAsInt()));
                    }
                    routes.add(new Route(entryValue.get("count").getAsInt(), entryValue.get("grade").getAsInt(), List.copyOf(ingredients)));
                }
                choices.put(identity(entry.getKey()), List.copyOf(routes));
            });
            var repair = new HashMap<Integer, Integer>();
            root.getAsJsonObject("repair").entrySet().forEach(entry -> repair.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt()));
            return new Facts(Map.copyOf(fixed), Map.copyOf(choices), Map.copyOf(repair));
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static SourceSkillFacts1710.Identity identity(String key) {
        int separator = key.indexOf(':');
        return new SourceSkillFacts1710.Identity(Integer.parseInt(key.substring(0, separator)), Integer.parseInt(key.substring(separator + 1)));
    }

    private R196RecipeMaterials() {}
}

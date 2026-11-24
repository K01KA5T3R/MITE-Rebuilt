package dev.koikaster.mite;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** R195 material arrays use any susceptible component, including zh's item
 * overrides. Placed block material and its dropped item are separate entries. */
final class SourceChemicalCatalogue {
 private static final JsonObject DATA=load();
 private static JsonObject load(){try(var stream=SourceChemicalCatalogue.class.getResourceAsStream("/data/mite_reforged/source_chemical_materials_039.json")){return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 static boolean material(String name,boolean acid){var value=DATA.getAsJsonObject("materials").getAsJsonObject(name);return value==null?acid:value.get(acid?"acid":"pepsin").getAsBoolean();}
 static Boolean entry(String category,String id,boolean acid){var value=DATA.getAsJsonObject(category).getAsJsonObject(id);if(value==null)return null;for(var token:value.getAsJsonArray("materials"))if(material(token.getAsString(),acid))return true;return false;}
 private SourceChemicalCatalogue(){}
}

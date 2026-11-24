package dev.koikaster.mite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Explicitly enabled 0.42-only server reporter. Every success is persisted immediately. */
public final class SourcePortTestReporter implements TestReporter {
 private static final String VERSION="0.1.42";
 private static final String CONTEXT="dedicated-server-new-bowls-witch-batch";
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 private static SourcePortTestReporter instance;
 private final LogTestReporter console=new LogTestReporter();
 private final Path output;
 private final JsonObject input;
 private final Set<String> expected=new LinkedHashSet<>();
 private final Set<String> frozen=new LinkedHashSet<>();
 private final Set<String> passed=new LinkedHashSet<>();
 private final Map<String,String> failed=new LinkedHashMap<>();
 private final List<Map<String,Object>> events=new ArrayList<>();
 private final Map<String,Object> report=new LinkedHashMap<>();
 private final Set<String> contexts=new LinkedHashSet<>();
 private final Map<String,Object> caseDetails=new LinkedHashMap<>();
 private final Map<String,Object> nativeSubscopes=new LinkedHashMap<>();
 private boolean begun;
 private boolean finished;
 private int newContexts;

 /** Call during mod construction; does nothing without -Dmite.sourcePortValidation=true. */
 public static synchronized void install(){
  if(!Boolean.getBoolean("mite.sourcePortValidation")||instance!=null)return;

  instance=new SourcePortTestReporter();GlobalTestReporter.replaceWith(instance);
 }

 /** Call from every new 0.38 @BeforeBatch. The first call checks the complete native registry. */
 public static synchronized void begin(ServerLevel level){
  if(!Boolean.getBoolean("mite.sourcePortValidation"))return;
  if(instance==null)throw new IllegalStateException("Install the source port reporter before beginning its batches");
  instance.beginBatch(level);
 }

 public static synchronized boolean confirmed(String key){return instance!=null&&instance.nativeSubscopes.containsKey(key);}
 public static synchronized void confirm(String key,Object evidence){
  if(instance==null)throw new IllegalStateException("Reporter missing");
  if(instance.nativeSubscopes.putIfAbsent(key,evidence)!=null)throw new IllegalStateException("Confirmed subscope replay: "+key);
  instance.persist();
 }

 /** Retain observations from a new case before its assertions can fail. */
 public static synchronized void recordDetails(String key,Object details){
  if(!Boolean.getBoolean("mite.sourcePortValidation"))return;
  if(instance==null)throw new IllegalStateException("New-case reporter is not installed");
  instance.caseDetails.put(key,details);instance.persist();
 }

 private SourcePortTestReporter(){
  try{
   output=Path.of(requiredProperty("mite.sourcePortReport")).toAbsolutePath().normalize();
   Path inputPath=Path.of(requiredProperty("mite.sourcePortInput")).toAbsolutePath().normalize();
   if(Files.exists(output))throw new IllegalStateException("A retained report already exists; select a new report and input for a retry: "+output);
   byte[] raw=Files.readAllBytes(inputPath);input=JsonParser.parseString(new String(raw,StandardCharsets.UTF_8)).getAsJsonObject();
   if(!input.get("version").getAsString().equals(VERSION)||!output.equals(Path.of(input.get("expected_report_path").getAsString()).toAbsolutePath().normalize()))throw new IllegalStateException("The prepared input belongs to another version or report; prepare a new retry input explicitly");
   for(var name:input.getAsJsonArray("expected_current_inventory"))expected.add(normalize(name.getAsString()));
   for(var name:input.getAsJsonArray("frozen_methods"))frozen.add(normalize(name.getAsString()));
   if(expected.isEmpty()&&(!input.has("contexts_only")||!input.get("contexts_only").getAsBoolean())||expected.stream().anyMatch(frozen::contains))throw new IllegalStateException("Current inventory is empty without explicit context-only mode, or repeats a previously successful case");
   for(var context:input.getAsJsonArray("frozen_contexts"))contexts.add(context.getAsString());
   if(input.has("frozen_native_subscopes"))for(var row:input.getAsJsonObject("frozen_native_subscopes").entrySet())nativeSubscopes.put(row.getKey(),row.getValue().deepCopy());
   report.put("version",VERSION);report.put("status","INSTALLED_AWAITING_NATIVE_REGISTRY");
   report.put("createdAt",Instant.now().toString());report.put("input_path",inputPath.toString());
   report.put("input_sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)));
   report.put("runtime_input",input);report.put("main_source_sha256",input.get("main_source_sha256").getAsString());
   report.put("production_source_sha256",input.get("production_source_sha256").getAsString());
   report.put("case_inventory",new ArrayList<>(expected));report.put("all_new_case_inventory",input.getAsJsonArray("all_new_case_inventory"));
   report.put("method_body_sha256",input.getAsJsonObject("method_body_sha256"));
   report.put("case_metadata",input.getAsJsonObject("case_metadata"));
   report.put("frozen_methods",new ArrayList<>(frozen));report.put("frozen_contexts",new ArrayList<>(contexts));
   report.put("selectedNewCasesThisLaunch",expected.size());
   report.put("historicalTestsExecuted",0);report.put("confirmedCasesReplayed",0);
   report.put("oldModelChecks",0);report.put("oldPngChecks",0);report.put("oldScalarChecks",0);report.put("oldSaveChecks",0);
   report.put("narratorEnabled",false);report.put("environment",FMLEnvironment.dist==Dist.DEDICATED_SERVER?"DEDICATED_SERVER":"CLIENT_WITH_INTEGRATED_SERVER");
   Path root=Path.of(input.get("project_root").getAsString());
   String launch=Files.readString(root.resolve("work/game-validation-last-launch.txt")).trim();
   String previous=input.get("prior_actual_launch").getAsString();
   double seconds=Duration.between(Instant.parse(previous),Instant.parse(launch)).toNanos()/1_000_000_000.0;
   if(seconds<900)throw new IllegalStateException("Shared game launch interval was not satisfied: "+seconds);
   report.put("game_launch",launch);report.put("previous_game_launch",previous);report.put("launch_interval_seconds",seconds);
   persist();
  }catch(RuntimeException error){throw error;}catch(Exception error){throw new IllegalStateException("Cannot initialize source port evidence",error);}
 }

 private synchronized void beginBatch(ServerLevel level){
  GlobalTestReporter.replaceWith(this);
  if(begun)return;
  var actual=new LinkedHashSet<String>();var full=new ArrayList<String>();
  for(var function:GameTestRegistry.getAllTestFunctions()){
   String name=normalize(function.getTestName());full.add(function.getTestName());
   if(!function.getBatchName().endsWith("_042")||!actual.add(name)||!expected.contains(name)||frozen.contains(name)){
    report.put("status","REJECTED_HISTORICAL_OR_UNEXPECTED_REGISTRY");report.put("unexpected_registry_entry",function.getTestName());persist();
    throw new IllegalStateException("Native registry contains a historical, duplicated, frozen or unexpected test: "+function.getTestName());
   }
  }
  if(!actual.equals(expected)){
   report.put("status","REJECTED_INCOMPLETE_REGISTRY");report.put("actual_registry",full);persist();
   throw new IllegalStateException("Native test registry does not exactly match the prepared new inventory");
  }

  begun=true;report.put("actual_registry",full);report.put("registryMatchesPreparedInventory",true);
  report.put("firstBatchDimension",level.dimension().location().toString());
  if(contexts.add(FMLEnvironment.dist==Dist.DEDICATED_SERVER?CONTEXT:"client-new-bowls-witch-batch"))newContexts++;
  report.put("status","RUNNING");persist();
 }

 @Override public synchronized void onTestSuccess(GameTestInfo test){
  String name=admit(test);passed.add(name);failed.remove(name);
  event(test,name,"PASS",null);persist();console.onTestSuccess(test);
 }

 @Override public synchronized void onTestFailed(GameTestInfo test){
  String name=admit(test);StringWriter stack=new StringWriter();
  if(test.getError()!=null)test.getError().printStackTrace(new PrintWriter(stack));
  failed.put(name,stack.toString());event(test,name,"FAIL",stack.toString());persist();console.onTestFailed(test);
 }

 private String admit(GameTestInfo test){
  String name=normalize(test.getTestName());
  if(!begun||!expected.contains(name)||frozen.contains(name)||passed.contains(name)||failed.containsKey(name)){
   report.put("status","REJECTED_UNEXPECTED_OR_REPLAYED_CALLBACK");report.put("rejected_callback",test.getTestName());persist();
   throw new IllegalStateException("Unexpected or replayed source port callback: "+test.getTestName());
  }
  return name;
 }

 private void event(GameTestInfo test,String name,String status,String failure){
  var row=new LinkedHashMap<String,Object>();row.put("method",name);row.put("native_test_name",test.getTestName());
  row.put("status",status);row.put("recordedAt",Instant.now().toString());row.put("runtime_ms",test.getRunTime());
  if(failure!=null)row.put("error",failure);events.add(row);
 }

 @Override public synchronized void finish(){finished=true;persist();console.finish();}

 private void persist(){
  report.put("updatedAt",Instant.now().toString());report.put("methods",new ArrayList<>(passed));
  report.put("case_details",caseDetails);report.put("native_subscopes",nativeSubscopes);
  report.put("passed_method_body_sha256",passedFingerprints());report.put("failed_methods",new ArrayList<>(failed.keySet()));
  report.put("failures",failed);report.put("events",events);report.put("passedCases",passed.size());report.put("failedCases",failed.size());
  report.put("contexts",new ArrayList<>(contexts));report.put("newContextsThisLaunch",newContexts);report.put("finished",finished);
  if(begun&&!String.valueOf(report.get("status")).startsWith("REJECTED")){if(!failed.isEmpty())report.put("status","FAILURES_RECORDED");else if(passed.size()==expected.size())report.put("status","PASS");else if(finished)report.put("status","INCOMPLETE");}
  try{
   Files.createDirectories(output.getParent());Path temporary=output.resolveSibling(output.getFileName()+".tmp");
   Files.writeString(temporary,GSON.toJson(report)+"\n",StandardCharsets.UTF_8);
   try{Files.move(temporary,output,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException error){Files.move(temporary,output,StandardCopyOption.REPLACE_EXISTING);}
  }catch(IOException error){throw new IllegalStateException("Cannot retain source port results immediately",error);}
 }

 private Map<String,String> passedFingerprints(){
  var values=new LinkedHashMap<String,String>();var fingerprints=input.getAsJsonObject("method_body_sha256");
  for(String name:passed)values.put(name,fingerprints.get(name).getAsString());return values;
 }
 private static String normalize(String name){String value=name.toLowerCase(Locale.ROOT);return value.substring(Math.max(value.lastIndexOf('.'),value.lastIndexOf(':'))+1);}
 private static String requiredProperty(String name){String value=System.getProperty(name);if(value==null||value.isBlank())throw new IllegalStateException("Missing explicit validation property "+name);return value;}
}

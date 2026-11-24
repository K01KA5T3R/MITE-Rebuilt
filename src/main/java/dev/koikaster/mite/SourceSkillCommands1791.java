package dev.koikaster.mite;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** R195 aa learning/abandon transactions; no production switch for enabling skills. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceSkillCommands1791 {
 private record Skill(int bit,String key,List<String> names){}
 private static final List<Skill> SKILLS;
 static {
  var skills=new ArrayList<Skill>();
  try(var in=SourceSkillCommands1791.class.getResourceAsStream("/data/mite_reforged/source_skill_names1791.json")){
   if(in==null)throw new java.io.IOException("Missing source skill names");
   for(var raw:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
    var row=raw.getAsJsonObject();
    skills.add(new Skill(row.get("bit").getAsInt(),row.get("key").getAsString(),List.of(row.get("name").getAsString(),row.get("profession").getAsString(),row.get("zh").getAsString(),row.get("zh_profession").getAsString())));
   }
  }catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
  SKILLS=List.copyOf(skills);
 }
 private static Skill find(String name){for(var skill:SKILLS)for(var alias:skill.names)if(alias.equalsIgnoreCase(name))return skill;return null;}
 private static boolean available(CommandSourceStack source){return source.getEntity() instanceof ServerPlayer player&&SourceSkills1685.enabled(player.level());}
 private static int fail(CommandSourceStack source,String key,Object... args){source.sendFailure(Component.translatable("message.mite_reforged.skill_"+key,args));return 0;}
 private static int cost(ServerPlayer player){return Rules.xpForLevel(5*(SourceSkillRules1685.count(SourceSkills1685.bits(player))+1));}
 private static int perform(CommandContext<CommandSourceStack> context,String action)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
  var source=context.getSource();var player=source.getPlayerOrException();
  if(!SourceSkills1685.enabled(player.level()))return fail(source,"disabled");
  var skill=find(StringArgumentType.getString(context,"skill"));if(skill==null)return fail(source,"invalid");
  int bits=SourceSkills1685.bits(player);boolean owned=(bits&skill.bit)!=0;
  if(action.equals("skill")){source.sendSuccess(()->owned?Component.translatable(skill.key):Component.translatable("message.mite_reforged.skill_cost",Component.translatable(skill.key),cost(player)),false);return 1;}
  var data=(SourceSkillAccess1685.PlayerData)player;
  if(action.equals("learn")){
   if(owned)return fail(source,"already");
   int level=5*(SourceSkillRules1685.count(bits)+1),xp=Rules.xpForLevel(level);
   if(PlayerState.data(player).getInt("Xp")<xp)return fail(source,"level",level);
   int today=SourceMoonCycle.dayOfWorld(player.level().getGameTime());
   if(SourceSkills1685.day(player)>=today)return fail(source,"daily");
   data.mite$skillBits1685(bits|skill.bit);data.mite$skillDay1685(today);
   PlayerState.addXp(player,-xp);
   source.sendSuccess(()->Component.translatable("message.mite_reforged.skill_learned",Component.translatable(skill.key)),false);
  }else{
   if(!owned)return fail(source,"missing");
   data.mite$skillBits1685(bits&~skill.bit);Network.sync(player);
   var random=player.level().random;
   player.level().playSound(null,player,SourceCreatureSounds.SKILL_DRAIN.get(),SoundSource.PLAYERS,.9f+(random.nextFloat()+random.nextFloat())/10f,.9f+(random.nextFloat()+random.nextFloat())/10f);
   source.sendSuccess(()->Component.translatable("message.mite_reforged.skill_forgot",Component.translatable(skill.key)),false);
  }
  return 1;
 }
 @SubscribeEvent public static void register(RegisterCommandsEvent event){
  var dispatcher=event.getDispatcher();
  for(String action:List.of("learn","abandon","skill"))dispatcher.register(net.minecraft.commands.Commands.literal(action).requires(SourceSkillCommands1791::available).then(net.minecraft.commands.Commands.argument("skill",StringArgumentType.greedyString()).suggests((context,builder)->SharedSuggestionProvider.suggest(SKILLS.stream().flatMap(s->s.names.stream()),builder)).executes(context->perform(context,action))));
  dispatcher.register(net.minecraft.commands.Commands.literal("skills").requires(SourceSkillCommands1791::available).executes(context->{var text=Component.empty();for(var skill:SKILLS){if(!text.getSiblings().isEmpty())text.append(", ");text.append(Component.translatable(skill.key));}context.getSource().sendSuccess(()->text,false);return 1;}));
 }
 private SourceSkillCommands1791(){}
}

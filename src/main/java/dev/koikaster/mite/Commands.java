package dev.koikaster.mite;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class Commands {
 @SubscribeEvent public static void register(RegisterCommandsEvent e){
  e.getDispatcher().register(net.minecraft.commands.Commands.literal("mite")
   .then(net.minecraft.commands.Commands.literal("status").requires(s->!net.minecraftforge.fml.loading.FMLEnvironment.production).executes(ctx->{var p=ctx.getSource().getPlayerOrException();var t=PlayerState.data(p);ctx.getSource().sendSuccess(()->Component.translatable("message.mite_reforged.status",PlayerState.level(p),t.getInt("Xp"),p.getHealth(),p.getMaxHealth(),p.getFoodData().getFoodLevel(),PlayerState.foodCap(p),(int)p.getFoodData().getSaturationLevel(),PlayerState.foodCap(p)),false);return 1;}))
   .then(net.minecraft.commands.Commands.literal("nutrition").requires(s->!net.minecraftforge.fml.loading.FMLEnvironment.production).executes(ctx->{var diet=Diet.read(ctx.getSource().getPlayerOrException());ctx.getSource().sendSuccess(()->Component.translatable("message.mite_reforged.nutrition",diet.protein,diet.fats,diet.phytonutrients,diet.insulin,Component.translatable("message.mite_reforged.insulin_"+diet.resistance)),false);return 1;}))
   .then(net.minecraft.commands.Commands.literal("grave").executes(ctx->{var t=PlayerState.data(ctx.getSource().getPlayerOrException());if(t.contains("GravePos")){var pos=net.minecraft.core.BlockPos.of(t.getLong("GravePos"));ctx.getSource().sendSuccess(()->Component.translatable("message.mite_reforged.grave",pos.getX(),pos.getY(),pos.getZ()),false);}return 1;}))
   .then(net.minecraft.commands.Commands.literal("xp").requires(s->s.hasPermission(2)).then(net.minecraft.commands.Commands.argument("player",EntityArgument.player()).then(net.minecraft.commands.Commands.argument("amount",IntegerArgumentType.integer(-1000000,1000000)).executes(ctx->{PlayerState.addXp(EntityArgument.getPlayer(ctx,"player"),IntegerArgumentType.getInteger(ctx,"amount"));return 1;})))));
 }
 private Commands(){}
}

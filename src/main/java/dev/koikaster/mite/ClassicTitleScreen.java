package dev.koikaster.mite;

import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** 1.6.4 blt layout and original assets, with current world/account screens behind it. */
public final class ClassicTitleScreen extends TitleScreen {
 private static final ResourceLocation LOGO=asset("minecraft.png"),SPLASHES=new ResourceLocation(MiteReforged.ID,"texts/classic_splashes.txt");
 private final boolean minceraft=RandomSource.create().nextFloat()<1.0E-4f;
 private final ClassicTitlePanorama panorama=new ClassicTitlePanorama();
 private String splash="";
 private int ticks;
 static ResourceLocation asset(String path){return new ResourceLocation(MiteReforged.ID,"textures/gui/classic_title/"+path);}

 @Override protected void init(){
  int x=width/2-100,y=height/4+48;
  addRenderableWidget(new ClassicTitleButton(x,y,200,Component.translatable("menu.singleplayer"),b->minecraft.setScreen(new SelectWorldScreen(this)),false));
  var multiplayer=addRenderableWidget(new ClassicTitleButton(x,y+24,200,Component.translatable("menu.multiplayer"),b->minecraft.setScreen(new JoinMultiplayerScreen(this)),false));
  multiplayer.active=minecraft.allowsMultiplayer();
  addRenderableWidget(new ClassicTitleButton(x,y+84,98,Component.translatable("menu.options"),b->minecraft.setScreen(new OptionsScreen(this,minecraft.options)),false));
  addRenderableWidget(new ClassicTitleButton(width/2+2,y+84,98,Component.translatable("menu.quit"),b->minecraft.stop(),false));
  addRenderableWidget(new ClassicTitleButton(width/2-124,y+84,20,Component.translatable("narrator.button.language"),b->minecraft.setScreen(new LanguageSelectScreen(this,minecraft.options,minecraft.getLanguageManager())),true));
  if(splash.isEmpty())splash=chooseSplash();
 }
 private String chooseSplash(){
  List<String> choices=new ArrayList<>();
  try(BufferedReader reader=minecraft.getResourceManager().openAsReader(SPLASHES)){
   for(String line;(line=reader.readLine())!=null;){line=line.trim();if(!line.isEmpty()&&line.hashCode()!=125780783)choices.add(line);}
  }catch(java.io.IOException ignored){}
  String selected=choices.isEmpty()?"":choices.get(RandomSource.create().nextInt(choices.size()));
  var date=Calendar.getInstance();int month=date.get(Calendar.MONTH)+1,day=date.get(Calendar.DAY_OF_MONTH);
  if(month==11&&day==9)return "Happy birthday, ez!";
  if(month==6&&day==1)return "Happy birthday, Notch!";
  if(month==12&&day==24)return "Merry X-mas!";
  if(month==1&&day==1)return "Happy new year!";
  if(month==10&&day==31)return "OOoooOOOoooo! Spooky!";
  return selected;
 }
 @Override public void tick(){ticks++;}
 @Override public void removed(){panorama.close();}
 @Override public void added(){}
 @Override public boolean shouldCloseOnEsc(){return false;}
 @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
  panorama.render(graphics,width,height,ticks+partialTick);
  graphics.fillGradient(0,0,width,height,0x80ffffff,0x00ffffff);
  graphics.fillGradient(0,0,width,height,0,0x80000000);
  int x=width/2-137,y=30;
  if(minceraft){
   graphics.blit(LOGO,x,y,0,0,99,44);graphics.blit(LOGO,x+99,y,129,0,27,44);
   graphics.blit(LOGO,x+125,y,126,0,3,44);graphics.blit(LOGO,x+128,y,99,0,26,44);
  }else graphics.blit(LOGO,x,y,0,0,155,44);
  graphics.blit(LOGO,x+155,y,0,45,155,44);
  if(!splash.isEmpty()){
   var pose=graphics.pose();pose.pushPose();pose.translate(width/2+90,70,0);pose.mulPose(Axis.ZP.rotationDegrees(-20));
   float scale=(1.8f-Mth.abs(Mth.sin((Util.getMillis()%1000L)/1000f*(float)Math.PI*2)*.1f))*100/(font.width(splash)+32);
   pose.scale(scale,scale,scale);graphics.drawCenteredString(font,splash,0,-8,0xffff00);pose.popPose();
  }
  graphics.drawString(font,"Minecraft "+SharedConstants.getCurrentVersion().getName(),2,height-10,0xffffff);
  String copyright="Copyright Mojang AB. Do not distribute!";
  graphics.drawString(font,copyright,width-font.width(copyright)-2,height-10,0xffffff);
  // Screen.render would dispatch TitleScreen's modern logo and Forge branding again.
  for(var widget:renderables)widget.render(graphics,mouseX,mouseY,partialTick);
 }
}

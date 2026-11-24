package dev.koikaster.mite;

import com.mojang.blaze3d.platform.Window;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.lwjgl.glfw.GLFW;

/** No loading presentation. Initial window is published only after a ready frame. */
public final class ClientResourceLoading {
 private static final Set<LoadingOverlay> FINISHED=Collections.newSetFromMap(new WeakHashMap<>());
 private static boolean readyToShow,shown,hiddenAtStartup;
 private static long startupWindow;
 public static void hideUntilReady(Window window){startupWindow=window.getWindow();GLFW.glfwHideWindow(startupWindow);hiddenAtStartup=GLFW.glfwGetWindowAttrib(startupWindow,GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE;}
 public static void render(LoadingOverlay owner,Minecraft minecraft,ReloadInstance reload,Consumer<Optional<Throwable>> finish,GuiGraphics graphics,int mouseX,int mouseY,float partial){
  if(reload.isDone()&&FINISHED.add(owner)){
   try{reload.checkExceptions();finish.accept(Optional.empty());}catch(Throwable failure){finish.accept(Optional.of(failure));}
   if(minecraft.getOverlay()==owner)minecraft.setOverlay(null);
   if(minecraft.screen!=null)minecraft.screen.init(minecraft,graphics.guiWidth(),graphics.guiHeight());
   readyToShow=minecraft.getOverlay()==null;
  }
  // Existing screen remains visible during reload; no fill, logo, bar, or fade.
  if(minecraft.screen!=null)minecraft.screen.render(graphics,mouseX,mouseY,partial);
 }
 public static void showWhenReady(Window window){
  if(!readyToShow||shown||window.getWindow()!=startupWindow)return;
  shown=true;GLFW.glfwShowWindow(window.getWindow());
 }
 private ClientResourceLoading(){}
}

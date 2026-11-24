package dev.koikaster.mite;

import net.minecraft.client.gui.components.toasts.*;

/** Admission of modern automatic reminders; operational failures retain native handling. */
public final class ClientToastPolicy1551 {
 public static boolean blocked(Toast toast){
  if(toast instanceof TutorialToast||toast instanceof RecipeToast)return true;
  if(toast instanceof SystemToast system)return system.getToken()==SystemToast.SystemToastIds.TUTORIAL_HINT||system.getToken()==SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING;
  return false;
 }
 private ClientToastPolicy1551(){}
}

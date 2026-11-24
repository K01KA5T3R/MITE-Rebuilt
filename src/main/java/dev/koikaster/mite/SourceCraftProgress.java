package dev.koikaster.mite;
import net.minecraft.client.gui.GuiGraphics;
/** Original arrow glyphs, clipped horizontally by actual completed ticks. */
final class SourceCraftProgress {
 static void draw(GuiGraphics graphics,int x,int y,int progress,int total,boolean table){
  int width=table?23:17,height=table?16:14;
  int filled=(int)((long)Math.max(0,Math.min(progress,total))*width/Math.max(1,total));
  if(filled>0)graphics.blit(Content.id("textures/gui/progress_r195/"+(table?"crafting_table":"inventory")+".png"),x,y,176,0,filled,height);
 }
 private SourceCraftProgress(){}
}

package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;
/** Keep the native paged book UI; custom identity needs an explicit BookAccess. */
final class ClientReferencedBooks {
 static void open(ItemStack stack){Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));}
 private ClientReferencedBooks(){}
}

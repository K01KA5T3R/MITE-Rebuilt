package dev.koikaster.mite;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.*;

/** Write a complete native SavedData envelope before replacing the old file.
 * Refuses a non-atomic fallback. An interrupted temporary image is never read.
 * File force and atomic rename cover process interruption; storage hardware
 * and directory-metadata durability under power loss remain separate claims. */
final class AtomicGraveFile207 {
 static void write(Path target,CompoundTag envelope)throws IOException {
  target=target.toAbsolutePath().normalize();var parent=target.getParent();
  if(parent==null)throw new IOException("Missing grave data directory");
  Files.createDirectories(parent);
  var temporary=parent.resolve("."+target.getFileName()+"-"+UUID.randomUUID()+".tmp");
  try {
   try(var channel=FileChannel.open(temporary,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
       var gzip=new GZIPOutputStream(Channels.newOutputStream(channel));
       var output=new DataOutputStream(gzip)){
    NbtIo.write(envelope,output);output.flush();gzip.finish();channel.force(true);
   }
   Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }finally{try{Files.deleteIfExists(temporary);}catch(IOException cleanup){com.mojang.logging.LogUtils.getLogger().warn("Could not remove an interrupted grave temporary file",cleanup);}}
 }
 private AtomicGraveFile207(){}
}

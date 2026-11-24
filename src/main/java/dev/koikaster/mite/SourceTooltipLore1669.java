package dev.koikaster.mite;
import java.util.*;import net.minecraft.network.chat.*;
/** Native lore identity, scoped to this tooltip including nested calls and exceptions. */
public final class SourceTooltipLore1669 {
 private static final class Frame {final Object stack;final IdentityHashMap<Component,Boolean> notes=new IdentityHashMap<>();Frame(Object stack){this.stack=stack;}}
 private static final ThreadLocal<ArrayDeque<Frame>> CONTEXT=ThreadLocal.withInitial(ArrayDeque::new);
 public static void begin(Object stack){CONTEXT.get().push(new Frame(stack));}
 public static MutableComponent capture(MutableComponent value,Object stack){for(var f:CONTEXT.get())if(f.stack==stack){f.notes.put(value,Boolean.TRUE);break;}return value;}
 public static void end(Object stack){var frames=CONTEXT.get();if(!frames.isEmpty()&&frames.peek().stack==stack)frames.pop();else for(var it=frames.iterator();it.hasNext();)if(it.next().stack==stack){it.remove();break;}}
 public static boolean authored(Object stack,Component value){for(var f:CONTEXT.get())if(f.stack==stack)return f.notes.containsKey(value);return false;}
 public static boolean authored(Component value){var f=CONTEXT.get().peek();return f!=null&&f.notes.containsKey(value);}
 private SourceTooltipLore1669(){}
}

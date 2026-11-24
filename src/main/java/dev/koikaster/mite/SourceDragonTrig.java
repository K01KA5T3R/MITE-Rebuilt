package dev.koikaster.mite;
/** The original ls sine table and operation order, independent of game code. */
public final class SourceDragonTrig {
 private static final float[] SIN=new float[65536];
 static{for(int i=0;i<65536;i++)SIN[i]=(float)Math.sin((double)i*Math.PI*2.0/65536.0);}
 public static float sin(float value){return SIN[(int)(value*10430.378f)&65535];}
 public static float cos(float value){return SIN[(int)(value*10430.378f+16384f)&65535];}
 private SourceDragonTrig(){}
}

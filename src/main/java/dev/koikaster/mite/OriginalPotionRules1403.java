package dev.koikaster.mite;

/** R195 inherited zp: parser, bitwise ingredient conversion and effect arithmetic. */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class OriginalPotionRules1403 {
    public static final String a;
    public static final String b;
    public static final String c;
    public static final String d;
    public static final String e;
    public static final String f;
    public static final String g;
    public static final String h;
    public static final String i;
    public static final String j;
    public static final String k;
    public static final String l;
    private static final HashMap m;
    private static final HashMap n;
    private static final HashMap o;
    private static final String[] p;

    public static boolean a(int n, int n2) {
        return (n & 1 << n2) != 0;
    }

    private static int c(int n, int n2) {
        return OriginalPotionRules1403.a(n, n2) ? 1 : 0;
    }

    private static int d(int n, int n2) {
        return OriginalPotionRules1403.a(n, n2) ? 0 : 1;
    }

    public static int a(int n) {
        return OriginalPotionRules1403.a(n, 5, 4, 3, 2, 1);
    }

    public static int a(Collection<Effect> collection) {
        int n = 3694022;
        if (collection == null || collection.isEmpty()) {
            return n;
        }
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        for (Effect nj2 : collection) {
            int n2 = Type.a[nj2.a()].j();
            for (int i = 0; i <= nj2.c(); ++i) {
                f += (float)(n2 >> 16 & 0xFF) / 255.0f;
                f2 += (float)(n2 >> 8 & 0xFF) / 255.0f;
                f3 += (float)(n2 >> 0 & 0xFF) / 255.0f;
                f4 += 1.0f;
            }
        }
        f = f / f4 * 255.0f;
        f2 = f2 / f4 * 255.0f;
        f3 = f3 / f4 * 255.0f;
        return (int)f << 16 | (int)f2 << 8 | (int)f3;
    }

    public static boolean b(Collection<Effect> collection) {
        for (Effect nj2 : collection) {
            if (nj2.e()) continue;
            return false;
        }
        return true;
    }

    public static int a(int n, boolean bl) {
        if (!bl) {
            if (o.containsKey(n)) {
                return (Integer)o.get(n);
            }
            int n2 = OriginalPotionRules1403.a(OriginalPotionRules1403.b(n, false));
            o.put(n, n2);
            return n2;
        }
        return OriginalPotionRules1403.a(OriginalPotionRules1403.b(n, bl));
    }

    public static String c(int n) {
        int n2 = OriginalPotionRules1403.a(n);
        return p[n2];
    }

    private static int a(boolean bl, boolean bl2, boolean bl3, int n, int n2, int n3, int n4) {
        int n5 = 0;
        if (bl) {
            n5 = OriginalPotionRules1403.d(n4, n2);
        } else if (n != -1) {
            if (n == 0 && OriginalPotionRules1403.h(n4) == n2) {
                n5 = 1;
            } else if (n == 1 && OriginalPotionRules1403.h(n4) > n2) {
                n5 = 1;
            } else if (n == 2 && OriginalPotionRules1403.h(n4) < n2) {
                n5 = 1;
            }
        } else {
            n5 = OriginalPotionRules1403.c(n4, n2);
        }
        if (bl2) {
            n5 *= n3;
        }
        if (bl3) {
            n5 *= -1;
        }
        return n5;
    }

    private static int h(int n) {
        int n2 = 0;
        while (n > 0) {
            n &= n - 1;
            ++n2;
        }
        return n2;
    }

    private static int a(String string, int n, int n2, int n3) {
        if (n >= string.length() || n2 < 0 || n >= n2) {
            return 0;
        }
        int n4 = string.indexOf(124, n);
        if (n4 >= 0 && n4 < n2) {
            int n5 = OriginalPotionRules1403.a(string, n, n4 - 1, n3);
            if (n5 > 0) {
                return n5;
            }
            int n6 = OriginalPotionRules1403.a(string, n4 + 1, n2, n3);
            if (n6 > 0) {
                return n6;
            }
            return 0;
        }
        int n7 = string.indexOf(38, n);
        if (n7 >= 0 && n7 < n2) {
            int n8 = OriginalPotionRules1403.a(string, n, n7 - 1, n3);
            if (n8 <= 0) {
                return 0;
            }
            int n9 = OriginalPotionRules1403.a(string, n7 + 1, n2, n3);
            if (n9 <= 0) {
                return 0;
            }
            if (n8 > n9) {
                return n8;
            }
            return n9;
        }
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;
        boolean bl5 = false;
        int n10 = -1;
        int n11 = 0;
        int n12 = 0;
        int n13 = 0;
        for (int i = n; i < n2; ++i) {
            char c = string.charAt(i);
            if (c >= '0' && c <= '9') {
                if (bl) {
                    n12 = c - 48;
                    bl2 = true;
                    continue;
                }
                n11 *= 10;
                n11 += c - 48;
                bl3 = true;
                continue;
            }
            if (c == '*') {
                bl = true;
                continue;
            }
            if (c == '!') {
                if (bl3) {
                    n13 += OriginalPotionRules1403.a(bl4, bl2, bl5, n10, n11, n12, n3);
                    bl4 = false;
                    bl5 = false;
                    bl = false;
                    bl2 = false;
                    bl3 = false;
                    n12 = 0;
                    n11 = 0;
                    n10 = -1;
                }
                bl4 = true;
                continue;
            }
            if (c == '-') {
                if (bl3) {
                    n13 += OriginalPotionRules1403.a(bl4, bl2, bl5, n10, n11, n12, n3);
                    bl4 = false;
                    bl5 = false;
                    bl = false;
                    bl2 = false;
                    bl3 = false;
                    n12 = 0;
                    n11 = 0;
                    n10 = -1;
                }
                bl5 = true;
                continue;
            }
            if (c == '=' || c == '<' || c == '>') {
                if (bl3) {
                    n13 += OriginalPotionRules1403.a(bl4, bl2, bl5, n10, n11, n12, n3);
                    bl4 = false;
                    bl5 = false;
                    bl = false;
                    bl2 = false;
                    bl3 = false;
                    n12 = 0;
                    n11 = 0;
                    n10 = -1;
                }
                if (c == '=') {
                    n10 = 0;
                    continue;
                }
                if (c == '<') {
                    n10 = 2;
                    continue;
                }
                if (c != '>') continue;
                n10 = 1;
                continue;
            }
            if (c != '+' || !bl3) continue;
            n13 += OriginalPotionRules1403.a(bl4, bl2, bl5, n10, n11, n12, n3);
            bl4 = false;
            bl5 = false;
            bl = false;
            bl2 = false;
            bl3 = false;
            n12 = 0;
            n11 = 0;
            n10 = -1;
        }
        if (bl3) {
            n13 += OriginalPotionRules1403.a(bl4, bl2, bl5, n10, n11, n12, n3);
        }
        return n13;
    }

    public static List<Effect> b(int n, boolean bl) {
        ArrayList<Effect> arrayList = null;
        for (Type ni2 : Type.a) {
            int n2;
            String string;
            if (ni2 == null || ni2.i() && !bl || (string = (String)m.get(ni2.c())) == null || (n2 = OriginalPotionRules1403.a(string, 0, string.length(), n)) <= 0) continue;
            int n3 = 0;
            String string2 = (String)OriginalPotionRules1403.n.get(ni2.c());
            if (string2 != null && (n3 = OriginalPotionRules1403.a(string2, 0, string2.length(), n)) < 0) {
                n3 = 0;
            }
            if (ni2.b()) {
                n2 = 1;
            } else {
                n2 = 1200 * (n2 * 3 + (n2 - 1) * 2);
                n2 >>= n3;
                n2 = (int)Math.round((double)n2 * ni2.g());
                if ((n & 0x4000) != 0) {
                    n2 = (int)Math.round((double)n2 * 0.75 + 0.5);
                }
            }
            if (arrayList == null) {
                arrayList = new ArrayList<Effect>();
            }
            Effect nj2 = new Effect(ni2.c(), n2, n3);
            if ((n & 0x4000) != 0) {
                nj2.a(true);
            }
            arrayList.add(nj2);
        }
        return arrayList;
    }

    private static int a(int n, int n2, boolean bl, boolean bl2, boolean bl3) {
        if (bl3) {
            if (!OriginalPotionRules1403.a(n, n2)) {
                return 0;
            }
        } else {
            n = bl ? (n &= ~(1 << n2)) : (bl2 ? ((n & 1 << n2) == 0 ? (n |= 1 << n2) : (n &= ~(1 << n2))) : (n |= 1 << n2));
        }
        return n;
    }

    public static int a(int n, String string) {
        int n2 = 0;
        int n3 = string.length();
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;
        int n4 = 0;
        for (int i = n2; i < n3; ++i) {
            char c = string.charAt(i);
            if (c >= '0' && c <= '9') {
                n4 *= 10;
                n4 += c - 48;
                bl = true;
                continue;
            }
            if (c == '!') {
                if (bl) {
                    n = OriginalPotionRules1403.a(n, n4, bl3, bl2, bl4);
                    bl4 = false;
                    bl2 = false;
                    bl3 = false;
                    bl = false;
                    n4 = 0;
                }
                bl2 = true;
                continue;
            }
            if (c == '-') {
                if (bl) {
                    n = OriginalPotionRules1403.a(n, n4, bl3, bl2, bl4);
                    bl4 = false;
                    bl2 = false;
                    bl3 = false;
                    bl = false;
                    n4 = 0;
                }
                bl3 = true;
                continue;
            }
            if (c == '+') {
                if (!bl) continue;
                n = OriginalPotionRules1403.a(n, n4, bl3, bl2, bl4);
                bl4 = false;
                bl2 = false;
                bl3 = false;
                bl = false;
                n4 = 0;
                continue;
            }
            if (c != '&') continue;
            if (bl) {
                n = OriginalPotionRules1403.a(n, n4, bl3, bl2, bl4);
                bl4 = false;
                bl2 = false;
                bl3 = false;
                bl = false;
                n4 = 0;
            }
            bl4 = true;
        }
        if (bl) {
            n = OriginalPotionRules1403.a(n, n4, bl3, bl2, bl4);
        }
        return n & Short.MAX_VALUE;
    }

    public static int a(int n, int n2, int n3, int n4, int n5, int n6) {
        return (OriginalPotionRules1403.a(n, n2) ? 16 : 0) | (OriginalPotionRules1403.a(n, n3) ? 8 : 0) | (OriginalPotionRules1403.a(n, n4) ? 4 : 0) | (OriginalPotionRules1403.a(n, n5) ? 2 : 0) | (OriginalPotionRules1403.a(n, n6) ? 1 : 0);
    }

    static {
        m = new HashMap();
        n = new HashMap();
        a = null;
        c = "+0-1-2-3&4-4+13";
        m.put(Type.l.c(), "0 & !1 & !2 & !3 & 0+6");
        b = "-0+1-2-3&4-4+13";
        m.put(Type.c.c(), "!0 & 1 & !2 & !3 & 1+6");
        h = "+0+1-2-3&4-4+13";
        m.put(Type.n.c(), "0 & 1 & !2 & !3 & 0+6");
        f = "+0-1+2-3&4-4+13";
        m.put(Type.h.c(), "0 & !1 & 2 & !3");
        d = "-0-1+2-3&4-4+13";
        m.put(Type.u.c(), "!0 & !1 & 2 & !3 & 2+6");
        e = "-0+3-4+13";
        m.put(Type.t.c(), "!0 & !1 & !2 & 3 & 3+6");
        m.put(Type.i.c(), "!0 & !1 & 2 & 3");
        m.put(Type.d.c(), "!0 & 1 & !2 & 3 & 3+6");
        g = "+0-1-2+3&4-4+13";
        m.put(Type.g.c(), "0 & !1 & !2 & 3 & 3+6");
        l = "-0+1+2-3+13&4-4";
        m.put(Type.r.c(), "!0 & 1 & 2 & !3 & 2+6");
        m.put(Type.p.c(), "!0 & 1 & 2 & 3 & 2+6");
        j = "+5-6-7";
        n.put(Type.c.c(), "5");
        n.put(Type.e.c(), "5");
        n.put(Type.g.c(), "5");
        n.put(Type.l.c(), "5");
        n.put(Type.i.c(), "5");
        n.put(Type.h.c(), "5");
        n.put(Type.m.c(), "5");
        n.put(Type.u.c(), "5");
        i = "-5+6-7";
        k = "+14&13-13";
        o = new HashMap();
        p = new String[]{"potion.prefix.mundane", "potion.prefix.uninteresting", "potion.prefix.bland", "potion.prefix.clear", "potion.prefix.milky", "potion.prefix.diffuse", "potion.prefix.artless", "potion.prefix.thin", "potion.prefix.awkward", "potion.prefix.flat", "potion.prefix.bulky", "potion.prefix.bungling", "potion.prefix.buttered", "potion.prefix.smooth", "potion.prefix.suave", "potion.prefix.debonair", "potion.prefix.thick", "potion.prefix.elegant", "potion.prefix.fancy", "potion.prefix.charming", "potion.prefix.dashing", "potion.prefix.refined", "potion.prefix.cordial", "potion.prefix.sparkling", "potion.prefix.potent", "potion.prefix.foul", "potion.prefix.odorless", "potion.prefix.rank", "potion.prefix.harsh", "potion.prefix.acrid", "potion.prefix.gross", "potion.prefix.stinky"};
    }

    /** Ingredient expressions are the yc singleton declarations, not modern brewing IDs. */
    public static java.util.Map<String,String> ingredients(){return java.util.Map.ofEntries(
      java.util.Map.entry("minecraft:gunpowder",k),java.util.Map.entry("minecraft:redstone",i),
      java.util.Map.entry("minecraft:glowstone_dust",j),java.util.Map.entry("minecraft:sugar",b),
      java.util.Map.entry("minecraft:ghast_tear","+0-1-2-3&4-4+13"),java.util.Map.entry("minecraft:nether_wart","+4"),
      java.util.Map.entry("minecraft:spider_eye",d),java.util.Map.entry("minecraft:fermented_spider_eye",e),
      java.util.Map.entry("minecraft:blaze_powder",g),java.util.Map.entry("minecraft:magma_cream",h),
      java.util.Map.entry("minecraft:glistering_melon_slice",f),java.util.Map.entry("minecraft:golden_carrot",l));}
    public static int mixedSubtype(int previous,String ingredient){String expression=ingredients().get(ingredient);return expression==null?previous:a(previous,expression);}
    /** arx.l()/u(): effectless non-water bottles cannot be changed into another empty brew. */
    public static boolean canBrew(int previous,int next){
      if((previous&16384)==0&&(next&16384)!=0)return true;
      List<Effect> before=b(previous,false),after=b(next,false);
      return !(previous>0&&before==after||before!=null&&(before.equals(after)||after==null)||previous==next);
    }
    public static List<Effect> effects(int subtype){var effects=b(subtype,false);return effects==null?List.of():List.copyOf(effects);}
    public static final class Effect{
      private final int id,duration,amplifier;private boolean splash;
      Effect(int id,int duration,int amplifier){this.id=id;this.duration=duration;this.amplifier=amplifier;}
      void a(boolean value){splash=value;}int a(){return id;}int c(){return amplifier;}boolean e(){return false;}
      public int id(){return id;}public int duration(){return duration;}public int amplifier(){return amplifier;}public boolean splash(){return splash;}
      public boolean equals(Object other){return other instanceof Effect value&&id==value.id&&duration==value.duration&&amplifier==value.amplifier&&splash==value.splash;}
      public int hashCode(){return id;}
    }
    private static final class Type{
      static final Type[] a=new Type[32];
  static final Type c=new Type(1,8171462,1.0,false);
  static final Type d=new Type(2,5926017,0.5,false);
  static final Type e=new Type(3,14270531,1.5,false);
  static final Type f=new Type(4,4866583,0.5,false);
  static final Type g=new Type(5,9643043,1.0,false);
  static final Type h=new Type(6,16262179,1.0,true);
  static final Type i=new Type(7,4393481,0.5,true);
  static final Type j=new Type(8,7889559,1.0,false);
  static final Type k=new Type(9,5578058,0.25,false);
  static final Type l=new Type(10,13458603,0.25,false);
  static final Type m=new Type(11,10044730,1.0,false);
  static final Type n=new Type(12,14981690,1.0,false);
  static final Type o=new Type(13,3035801,1.0,false);
  static final Type p=new Type(14,8356754,1.0,false);
  static final Type q=new Type(15,2039587,0.25,false);
  static final Type r=new Type(16,2039713,1.0,false);
  static final Type s=new Type(17,5797459,0.5,false);
  static final Type t=new Type(18,4738376,0.5,false);
  static final Type u=new Type(19,5149489,0.25,false);
  static final Type v=new Type(20,3484199,0.25,false);
  static final Type w=new Type(21,16284963,1.0,false);
  static final Type x=new Type(22,2445989,1.0,false);
  static final Type y=new Type(23,16262179,1.0,true);
      final int id,color;final double duration;final boolean instant;
      Type(int id,int color,double duration,boolean instant){this.id=id;this.color=color;this.duration=duration;this.instant=instant;a[id]=this;}
      int c(){return id;}boolean i(){return false;}boolean b(){return instant;}double g(){return duration;}int j(){return color;}
    }
    private OriginalPotionRules1403(){}
}

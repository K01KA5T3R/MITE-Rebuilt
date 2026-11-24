package dev.koikaster.mite;

/** R195 ajt math copied from the original class. Class names and the
 * empty ajy base are adapted; algorithms and Random consumption remain source.
 * The source dimension adapter owns generation and seed binding. */
import java.util.Random;

public class OriginalUnderworldOctaves {
    private OriginalUnderworldImprovedNoise[] a;
    private int b;

    public OriginalUnderworldOctaves(Random par1Random, int par2) {
        this.b = par2;
        this.a = new OriginalUnderworldImprovedNoise[par2];
        for (int var3 = 0; var3 < par2; ++var3) {
            this.a[var3] = new OriginalUnderworldImprovedNoise(par1Random);
        }
    }

    private static long sourceFloorLong(double value){long whole=(long)value;return value<(double)whole?whole-1:whole;}

    public double[] a(double[] par1ArrayOfDouble, int par2, int par3, int par4, int par5, int par6, int par7, double par8, double par10, double par12) {
        if (par1ArrayOfDouble == null) {
            par1ArrayOfDouble = new double[par5 * par6 * par7];
        } else {
            for (int var14 = 0; var14 < par1ArrayOfDouble.length; ++var14) {
                par1ArrayOfDouble[var14] = 0.0;
            }
        }
        double var27 = 1.0;
        for (int var16 = 0; var16 < this.b; ++var16) {
            double var17 = (double)par2 * var27 * par8;
            double var19 = (double)par3 * var27 * par10;
            double var21 = (double)par4 * var27 * par12;
            long var23 = sourceFloorLong(var17);
            long var25 = sourceFloorLong(var21);
            var17 -= (double)var23;
            var21 -= (double)var25;
            this.a[var16].a(par1ArrayOfDouble, var17 += (double)(var23 %= 0x1000000L), var19, var21 += (double)(var25 %= 0x1000000L), par5, par6, par7, par8 * var27, par10 * var27, par12 * var27, var27);
            var27 /= 2.0;
        }
        return par1ArrayOfDouble;
    }

    public double[] a(double[] par1ArrayOfDouble, int par2, int par3, int par4, int par5, double par6, double par8, double par10) {
        return this.a(par1ArrayOfDouble, par2, 10, par3, par4, 1, par5, par6, 1.0, par8);
    }
}

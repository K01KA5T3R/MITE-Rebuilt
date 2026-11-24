package dev.koikaster.mite;

/** World-owned reuse of the frozen R195 table initializer. The initializer
 * arithmetic and Random draw order are unchanged. This owner prevents one
 * integrated world from borrowing the prior world's table seed. */
import java.util.Random;

final class SourceUnderworldRandomTables {
    public static final int MAX_INDEX = Short.MAX_VALUE;
    private static final int MAX_INDEX_PLUS_1 = 32768;
    final int[] int_max = new int[32768];
    final int[] int_2 = new int[32768];
    final int[] int_4 = new int[32768];
    final int[] int_8 = new int[32768];
    final int[] int_16 = new int[32768];
    final int[] int_32 = new int[32768];
    final int[] int_64 = new int[32768];
    final int[] int_128 = new int[32768];
    final int[] int_256 = new int[32768];
    final int[] int_3 = new int[32768];
    final int[] int_5 = new int[32768];
    final int[] int_6 = new int[32768];
    final int[] int_7 = new int[32768];
    final int[] int_9 = new int[32768];
    final int[] int_10 = new int[32768];
    final int[] int_13 = new int[32768];
    final int[] int_14 = new int[32768];
    final int[] int_17 = new int[32768];
    final int[] int_25 = new int[32768];
    final int[] int_33 = new int[32768];
    final int[] int_49 = new int[32768];
    final int[] int_65 = new int[32768];
    final int[] int_100 = new int[32768];
    final int[] int_126 = new int[32768];
    final int[] int_4_minus_int_4 = new int[32768];
    final int[] int_8_minus_int_8 = new int[32768];
    final int[] int_16_plus_8 = new int[32768];
    final int[] int_7_minus_3 = new int[32768];
    final int[] int_9_minus_4 = new int[32768];
    final int[] int_13_minus_6 = new int[32768];
    final int[] int_14_plus_1 = new int[32768];
    final int[] int_17_minus_8 = new int[32768];
    final int[] int_25_minus_12 = new int[32768];
    final int[] int_33_minus_16 = new int[32768];
    final int[] int_49_minus_24 = new int[32768];
    final int[] int_65_minus_32 = new int[32768];
    final int[] int_126_plus_1 = new int[32768];
    final boolean[] chance_in_2 = new boolean[32768];
    final boolean[] chance_in_3 = new boolean[32768];
    final boolean[] chance_in_4 = new boolean[32768];
    final boolean[] chance_in_6 = new boolean[32768];
    final boolean[] chance_in_8 = new boolean[32768];
    final boolean[] chance_in_10 = new boolean[32768];
    final boolean[] chance_in_16 = new boolean[32768];
    final boolean[] chance_in_32 = new boolean[32768];
    final boolean[] chance_in_100 = new boolean[32768];
    final float[] float_1 = new float[32768];
    final float[] float_1_minus_float_1 = new float[32768];
    final float[] float_1_times_float_1 = new float[32768];
    final float[] float_2Pi = new float[32768];
    final float[] float_1_minus_a_half_times_a_quarter = new float[32768];
    final float[] float_2_plus_float_1 = new float[32768];
    final double[] double_1 = new double[32768];
    private boolean random_numbers_have_been_initialized = false;
    private int random_number_index;

    final long worldSeed;
    SourceUnderworldRandomTables(long seed){worldSeed=seed;initialize(seed);}

    private void initialize(long seed) {
        if (random_numbers_have_been_initialized) {
            return;
        }
        random_numbers_have_been_initialized = true;
        Random rand = new Random();
        rand.setSeed(seed);
        int i = -1;
        while (++i < 32768) {
            double double_a;
            int int_a = rand.nextInt() & Integer.MAX_VALUE;
            int int_b = rand.nextInt() & Integer.MAX_VALUE;
            this.int_max[i] = int_a;
            this.int_2[i] = int_a & 1;
            this.int_4[i] = int_a & 3;
            this.int_8[i] = int_a & 7;
            this.int_16[i] = int_a & 0xF;
            this.int_32[i] = int_a & 0x1F;
            this.int_64[i] = int_a & 0x3F;
            this.int_128[i] = int_a & 0x7F;
            this.int_256[i] = int_a & 0xFF;
            this.int_3[i] = int_a % 3;
            this.int_5[i] = int_a % 5;
            this.int_6[i] = int_a % 6;
            this.int_7[i] = int_a % 7;
            this.int_9[i] = int_a % 9;
            this.int_10[i] = int_a % 10;
            this.int_13[i] = int_a % 13;
            this.int_14[i] = int_a % 14;
            this.int_17[i] = int_a % 17;
            this.int_25[i] = int_a % 25;
            this.int_33[i] = int_a % 33;
            this.int_49[i] = int_a % 49;
            this.int_65[i] = int_a % 65;
            this.int_100[i] = int_a % 100;
            this.int_126[i] = int_a % 126;
            this.int_4_minus_int_4[i] = int_4[i] - (int_b & 3);
            this.int_8_minus_int_8[i] = int_8[i] - (int_b & 7);
            this.int_16_plus_8[i] = int_16[i] + 8;
            this.int_7_minus_3[i] = int_7[i] - 3;
            this.int_9_minus_4[i] = int_9[i] - 4;
            this.int_13_minus_6[i] = int_13[i] - 6;
            this.int_14_plus_1[i] = int_14[i] + 1;
            this.int_17_minus_8[i] = int_17[i] - 8;
            this.int_25_minus_12[i] = int_25[i] - 12;
            this.int_33_minus_16[i] = int_33[i] - 16;
            this.int_49_minus_24[i] = int_49[i] - 24;
            this.int_65_minus_32[i] = int_65[i] - 32;
            this.int_126_plus_1[i] = int_126[i] + 1;
            this.chance_in_2[i] = int_2[i] == 0;
            this.chance_in_3[i] = int_3[i] == 0;
            this.chance_in_4[i] = int_4[i] == 0;
            this.chance_in_6[i] = int_6[i] == 0;
            this.chance_in_8[i] = int_8[i] == 0;
            this.chance_in_10[i] = int_10[i] == 0;
            this.chance_in_16[i] = int_16[i] == 0;
            this.chance_in_32[i] = int_32[i] == 0;
            this.chance_in_100[i] = int_100[i] == 0;
            float float_a = rand.nextFloat();
            float float_b = rand.nextFloat();
            this.float_1[i] = float_a;
            this.float_1_minus_float_1[i] = float_a - float_b;
            this.float_1_times_float_1[i] = float_a * float_b;
            this.float_2Pi[i] = (float)((double)(float_a * 2.0f) * Math.PI);
            this.float_1_minus_a_half_times_a_quarter[i] = (float_a - 0.5f) * 0.25f;
            this.float_2_plus_float_1[i] = float_a * 2.0f + float_b;
            this.double_1[i] = double_a = rand.nextDouble();
        }
        random_number_index = rand.nextInt();
    }
}

package dev.koikaster.mite;
import java.util.Random;
/** R195 ChunkProviderUnderworld cave density + adr deep strata, native seed binding.
 * Algorithms/RNG order copied. Per-owner fields and synchronized generation
 * adapt source static workspace for modern generation threads. Mantle127 is a
 * semantic token, not an invented world block ID. Ore population is a separate owner under33D; this kernel does not claim
 * complete biome decoration or dimensional route implementation. */
public final class OriginalUnderworldTerrain {
    private Random hellRNG;
    private OriginalUnderworldOctaves netherNoiseGen1;
    private OriginalUnderworldOctaves netherNoiseGen2;
    private OriginalUnderworldOctaves netherNoiseGen3;
    private OriginalUnderworldOctaves slowsandGravelNoiseGen;
    private OriginalUnderworldOctaves netherrackExculsivityNoiseGen;
    public OriginalUnderworldOctaves netherNoiseGen6;
    public OriginalUnderworldOctaves netherNoiseGen7;
    private double[] noiseField;
    double[] noiseData1;
    double[] noiseData2;
    double[] noiseData3;
    double[] noiseData4;
    double[] noiseData5;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_1a;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_1b;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_2;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_3;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_4;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_1a_bump;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_1b_bump;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_1c_bump;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_2_bump;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_3_bump;
    private OriginalUnderworldOctaves noise_gen_bedrock_strata_4_bump;
    private double[] bedrock_strata_1a_noise;
    private double[] bedrock_strata_1b_noise;
    private double[] bedrock_strata_2_noise;
    private double[] bedrock_strata_3_noise;
    private double[] bedrock_strata_4_noise;
    private double[] bedrock_strata_1a_bump_noise;
    private double[] bedrock_strata_1b_bump_noise;
    private double[] bedrock_strata_1c_bump_noise;
    private double[] bedrock_strata_2_bump_noise;
    private double[] bedrock_strata_3_bump_noise;
    private double[] bedrock_strata_4_bump_noise;

    private final SourceUnderworldRandomTables tables;
    private final long hashedSeed;
    public static final byte AIR=0,STONE=1,WATER=9,BEDROCK=7,COBWEB=30,MANTLE=127,LAVA_STILL=11,LAVA_FLOWING=10;
    private static long sourceHashedSeed(long seed){var random=new Random(seed);random.nextInt();return random.nextLong();}
    private static int chunkHash(int x,int z){int hash=17;hash=hash*31+x;return hash*31+z;}
    private double getMax(double a,double b){return a>b?a:b;}
    /** Column layout matches legacy x,z,y; tokens map to native block states in
     * the pending ChunkGenerator adapter. Ore placement is separate under33D. */
    public synchronized byte[] generate(int chunkX,int chunkZ){
        hellRNG.setSeed((long)chunkX*341873128712L+(long)chunkZ*132897987541L);
        byte[] caves=new byte[32768];generateNetherTerrain(chunkX,chunkZ,caves);
        // Source replaceBlocksForBiome returns immediately for y_offset120.
        placeRandomCobwebs(chunkX,chunkZ,caves,hellRNG);
        return strata(chunkX,chunkZ,caves);
    }
    public OriginalUnderworldTerrain(long par2) {
        this.hashedSeed=sourceHashedSeed(par2);
        tables=new SourceUnderworldRandomTables(par2);
        this.hellRNG = new Random(par2);
        this.netherNoiseGen1 = new OriginalUnderworldOctaves(this.hellRNG, 16);
        this.netherNoiseGen2 = new OriginalUnderworldOctaves(this.hellRNG, 16);
        this.netherNoiseGen3 = new OriginalUnderworldOctaves(this.hellRNG, 8);
        this.slowsandGravelNoiseGen = new OriginalUnderworldOctaves(this.hellRNG, 4);
        this.netherrackExculsivityNoiseGen = new OriginalUnderworldOctaves(this.hellRNG, 4);
        this.netherNoiseGen6 = new OriginalUnderworldOctaves(this.hellRNG, 10);
        this.netherNoiseGen7 = new OriginalUnderworldOctaves(this.hellRNG, 16);
        noise_gen_bedrock_strata_1a = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_1b = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_2 = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_3 = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_4 = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_1a_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_1b_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_1c_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_2_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_3_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
        noise_gen_bedrock_strata_4_bump = new OriginalUnderworldOctaves(this.hellRNG, 4);
    }
    public void generateNetherTerrain(int par1, int par2, byte[] par3ArrayOfByte) {
        int var4 = 4;
        int var5 = 32;
        int var6 = var4 + 1;
        int var7 = 17;
        int var8 = var4 + 1;
        this.noiseField = this.initializeNoiseField(this.noiseField, par1 * var4, 0, par2 * var4, var6, var7, var8);
        for (int var9 = 0; var9 < var4; ++var9) {
            for (int var10 = 0; var10 < var4; ++var10) {
                for (int var11 = 0; var11 < 16; ++var11) {
                    double var12 = 0.125;
                    double var14 = this.noiseField[((var9 + 0) * var8 + var10 + 0) * var7 + var11 + 0];
                    double var16 = this.noiseField[((var9 + 0) * var8 + var10 + 1) * var7 + var11 + 0];
                    double var18 = this.noiseField[((var9 + 1) * var8 + var10 + 0) * var7 + var11 + 0];
                    double var20 = this.noiseField[((var9 + 1) * var8 + var10 + 1) * var7 + var11 + 0];
                    double var22 = (this.noiseField[((var9 + 0) * var8 + var10 + 0) * var7 + var11 + 1] - var14) * var12;
                    double var24 = (this.noiseField[((var9 + 0) * var8 + var10 + 1) * var7 + var11 + 1] - var16) * var12;
                    double var26 = (this.noiseField[((var9 + 1) * var8 + var10 + 0) * var7 + var11 + 1] - var18) * var12;
                    double var28 = (this.noiseField[((var9 + 1) * var8 + var10 + 1) * var7 + var11 + 1] - var20) * var12;
                    for (int var30 = 0; var30 < 8; ++var30) {
                        double var31 = 0.25;
                        double var33 = var14;
                        double var35 = var16;
                        double var37 = (var18 - var14) * var31;
                        double var39 = (var20 - var16) * var31;
                        for (int var41 = 0; var41 < 4; ++var41) {
                            int var42 = var41 + var9 * 4 << 11 | 0 + var10 * 4 << 7 | var11 * 8 + var30;
                            int var43 = 128;
                            double var44 = 0.25;
                            double var46 = var33;
                            double var48 = (var35 - var33) * var44;
                            for (int var50 = 0; var50 < 4; ++var50) {
                                int var51 = 0;
                                if (var11 * 8 + var30 < var5 - 8) {
                                    var51 = WATER;
                                }
                                if (var46 > 0.0) {
                                    var51 = STONE;
                                }
                                par3ArrayOfByte[var42] = (byte)var51;
                                var42 += var43;
                                var46 += var48;
                            }
                            var33 += var37;
                            var35 += var39;
                        }
                        var14 += var22;
                        var16 += var24;
                        var18 += var26;
                        var20 += var28;
                    }
                }
            }
        }
    }
    private double[] initializeNoiseField(double[] par1ArrayOfDouble, int par2, int par3, int par4, int par5, int par6, int par7) {
        int var15;
        if (par1ArrayOfDouble == null) {
            par1ArrayOfDouble = new double[par5 * par6 * par7];
        }
        double var8 = 684.412;
        double var10 = 2053.236;
        this.noiseData4 = this.netherNoiseGen6.a(this.noiseData4, par2, par3, par4, par5, 1, par7, 1.0, 0.0, 1.0);
        this.noiseData5 = this.netherNoiseGen7.a(this.noiseData5, par2, par3, par4, par5, 1, par7, 100.0, 0.0, 100.0);
        this.noiseData1 = this.netherNoiseGen3.a(this.noiseData1, par2, par3, par4, par5, par6, par7, var8 / 80.0, var10 / 60.0, var8 / 80.0);
        this.noiseData2 = this.netherNoiseGen1.a(this.noiseData2, par2, par3, par4, par5, par6, par7, var8, var10, var8);
        this.noiseData3 = this.netherNoiseGen2.a(this.noiseData3, par2, par3, par4, par5, par6, par7, var8, var10, var8);
        int var12 = 0;
        int var13 = 0;
        double[] var14 = new double[par6];
        for (var15 = 0; var15 < par6; ++var15) {
            var14[var15] = Math.cos((double)var15 * Math.PI * 6.0 / (double)par6) * 2.0;
            double var16 = var15;
            if (var15 > par6 / 2) {
                var16 = par6 - 1 - var15;
            }
            if (!(var16 < 4.0)) continue;
            var16 = 4.0 - var16;
            int n = var15;
            var14[n] = var14[n] - var16 * var16 * var16 * 10.0;
        }
        for (var15 = 0; var15 < par5; ++var15) {
            for (int var36 = 0; var36 < par7; ++var36) {
                double var17 = (this.noiseData4[var13] + 256.0) / 512.0;
                if (var17 > 1.0) {
                    var17 = 1.0;
                }
                double var19 = 0.0;
                double var21 = this.noiseData5[var13] / 8000.0;
                if (var21 < 0.0) {
                    var21 = -var21;
                }
                if ((var21 = var21 * 3.0 - 3.0) < 0.0) {
                    if ((var21 /= 2.0) < -1.0) {
                        var21 = -1.0;
                    }
                    var21 /= 1.4;
                    var21 /= 2.0;
                    var17 = 0.0;
                } else {
                    if (var21 > 1.0) {
                        var21 = 1.0;
                    }
                    var21 /= 6.0;
                }
                var17 += 0.5;
                var21 = var21 * (double)par6 / 16.0;
                ++var13;
                for (int var23 = 0; var23 < par6; ++var23) {
                    double var34;
                    double var24 = 0.0;
                    double var26 = var14[var23];
                    double var28 = this.noiseData2[var12] / 512.0;
                    double var30 = this.noiseData3[var12] / 512.0;
                    double var32 = (this.noiseData1[var12] / 10.0 + 1.0) / 2.0;
                    var24 = var32 < 0.0 ? var28 : (var32 > 1.0 ? var30 : var28 + (var30 - var28) * var32);
                    var24 -= var26;
                    if (var23 > par6 - 4) {
                        var34 = (float)(var23 - (par6 - 4)) / 3.0f;
                        var24 = var24 * (1.0 - var34) + -10.0 * var34;
                    }
                    if ((double)var23 < var19) {
                        var34 = (var19 - (double)var23) / 4.0;
                        if (var34 < 0.0) {
                            var34 = 0.0;
                        }
                        if (var34 > 1.0) {
                            var34 = 1.0;
                        }
                        var24 = var24 * (1.0 - var34) + -10.0 * var34;
                    }
                    par1ArrayOfDouble[var12] = var24;
                    ++var12;
                }
            }
        }
        return par1ArrayOfDouble;
    }
    private void placeRandomCobwebs(int chunk_x, int chunk_z, byte[] block_ids, Random rand) {
        int random_number_index = rand.nextInt();
        byte web_block_id = (byte)COBWEB;
        byte lava_still_block_id = (byte)LAVA_STILL;
        byte lava_moving_block_id = (byte)LAVA_FLOWING;
        byte stone_block_id = (byte)STONE;
        int frequency = 128;
        for (int attempts = 0; attempts < frequency; ++attempts) {
            int y;
            int z;
            int index;
            int x = tables.int_14_plus_1[++random_number_index & Short.MAX_VALUE];
            ++random_number_index;
            if (block_ids[index = ((z = tables.int_14_plus_1[++random_number_index & Short.MAX_VALUE]) * 16 + x) * 128 + (y = tables.int_126_plus_1[random_number_index & Short.MAX_VALUE])] != 0) continue;
            byte block_id_above = block_ids[index + 1];
            byte block_id_below = block_ids[index - 1];
            byte block_id_front = block_ids[index + 128];
            byte block_id_back = block_ids[index - 128];
            byte block_id_right = block_ids[index + 2048];
            byte block_id_left = block_ids[index - 2048];
            int solid_face_adjacent_blocks = 0;
            if (block_id_above != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (block_id_below != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (block_id_front != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (block_id_back != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (block_id_right != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (block_id_left != 0) {
                ++solid_face_adjacent_blocks;
            }
            if (solid_face_adjacent_blocks < 4 || block_id_below == lava_still_block_id || block_id_below == lava_moving_block_id || block_id_above != stone_block_id && block_id_below != stone_block_id && block_id_front != stone_block_id && block_id_back != stone_block_id && block_id_right != stone_block_id && block_id_left != stone_block_id) continue;
            block_ids[index] = web_block_id;
            attempts -= frequency * 4;
        }
    }
    private byte[] strata(int chunkX,int chunkZ,byte[] caves){
        byte[] result=new byte[16*16*256];
            int base_x = chunkX * 16;
            int base_z = chunkZ * 16;
            Random random = new Random(hashedSeed * (long)chunkHash(chunkX, chunkZ));
            int y_offset = 120;
            double scale_xz = 0.015625;
            double scale_y = 0.03125;
            this.bedrock_strata_1a_noise = this.noise_gen_bedrock_strata_1a.a(this.bedrock_strata_1a_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 2.0, scale_y * 2.0, scale_xz * 2.0);
            this.bedrock_strata_1b_noise = this.noise_gen_bedrock_strata_1b.a(this.bedrock_strata_1b_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 2.0, scale_y * 2.0, scale_xz * 2.0);
            this.bedrock_strata_2_noise = this.noise_gen_bedrock_strata_2.a(this.bedrock_strata_2_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            this.bedrock_strata_3_noise = this.noise_gen_bedrock_strata_3.a(this.bedrock_strata_3_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            this.bedrock_strata_4_noise = this.noise_gen_bedrock_strata_4.a(this.bedrock_strata_4_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            scale_xz = 0.25;
            this.bedrock_strata_1a_bump_noise = this.noise_gen_bedrock_strata_1a_bump.a(this.bedrock_strata_1a_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 0.5, scale_y * 2.0, scale_xz * 0.5);
            this.bedrock_strata_1b_bump_noise = this.noise_gen_bedrock_strata_1b_bump.a(this.bedrock_strata_1b_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 1.0, scale_y * 2.0, scale_xz * 1.0);
            this.bedrock_strata_1c_bump_noise = this.noise_gen_bedrock_strata_1c_bump.a(this.bedrock_strata_1c_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 2.0, scale_y * 2.0, scale_xz * 2.0);
            this.bedrock_strata_2_bump_noise = this.noise_gen_bedrock_strata_2_bump.a(this.bedrock_strata_2_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            this.bedrock_strata_3_bump_noise = this.noise_gen_bedrock_strata_3_bump.a(this.bedrock_strata_3_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            this.bedrock_strata_4_bump_noise = this.noise_gen_bedrock_strata_4_bump.a(this.bedrock_strata_4_bump_noise, base_x, 0, base_z, 16, 1, 16, scale_xz * 4.0, scale_y * 2.0, scale_xz * 4.0);
            int rng_index = chunkX * 2653 + chunkZ * 6714631;
            for (int var6 = 0; var6 < 16; ++var6) {
                for (int var7 = 0; var7 < 16; ++var7) {
                    int num_bedrock_blocks = y_offset == 0 ? 0 : random.nextInt(3) + 1;
                    for (int var8 = 0 - y_offset; var8 < (y_offset == 0 ? 128 : 256 - y_offset); ++var8) {
                        byte var9;
                        int index;
                        if (var8 < 0 || var8 > 127) {
                            index = -1;
                            if ((var8 += y_offset) < num_bedrock_blocks) {
                                var9 = (byte)MANTLE;
                            } else if (var8 > 255 - num_bedrock_blocks) {
                                var9 = (byte)BEDROCK;
                            } else {
                                var9 = (byte)STONE;
                                byte block_bedrock_id = (byte)BEDROCK;
                                int local_xz_index = var7 + var6 * 16;
                                double bedrock_noise = this.getMax(this.bedrock_strata_1a_noise[local_xz_index], this.bedrock_strata_1b_noise[local_xz_index]);
                                int dy = var8 - 3;
                                double bump_noise = this.bedrock_strata_1a_bump_noise[local_xz_index];
                                if (bump_noise > 0.0) {
                                    bedrock_noise += bump_noise * 0.25;
                                }
                                if ((bump_noise = this.bedrock_strata_1b_bump_noise[local_xz_index]) > 0.0) {
                                    bedrock_noise += bump_noise * 0.125;
                                }
                                if ((bump_noise = this.bedrock_strata_1c_bump_noise[local_xz_index]) > 0.0) {
                                    bedrock_noise += bump_noise * 0.125;
                                }
                                if ((bump_noise = this.bedrock_strata_4_bump_noise[local_xz_index]) > 0.0) {
                                    bedrock_noise += bump_noise * 0.09375 + 0.125;
                                }
                                if (bedrock_noise > 0.0 && (double)dy <= bedrock_noise * 7.0) {
                                    var9 = (byte)BEDROCK;
                                }
                                if (var9 != block_bedrock_id) {
                                    dy = var8 - 32;
                                    if ((bedrock_noise = this.bedrock_strata_2_noise[local_xz_index] - bedrock_noise * 1.5) > 0.0) {
                                        if (dy > 0 && (bump_noise = this.bedrock_strata_2_bump_noise[local_xz_index]) > 0.0) {
                                            bedrock_noise += bump_noise * 0.25 + 0.25;
                                        }
                                        if (dy < 0) {
                                            if (tables.chance_in_2[++rng_index & Short.MAX_VALUE]) {
                                                ++dy;
                                            }
                                            dy = -dy;
                                        }
                                        if ((double)dy <= bedrock_noise * 2.0) {
                                            var9 = (byte)BEDROCK;
                                        }
                                    }
                                }
                                if (var9 != block_bedrock_id) {
                                    dy = var8 - 72;
                                    bedrock_noise = this.bedrock_strata_3_noise[local_xz_index] - this.bedrock_strata_4_noise[local_xz_index] * 0.375;
                                    if ((bedrock_noise += 0.5) > 0.0) {
                                        if (dy > 0 && (bump_noise = this.bedrock_strata_3_bump_noise[local_xz_index]) > 0.0) {
                                            bedrock_noise += bump_noise * 0.25 + 0.25;
                                        }
                                        if (dy < 0) {
                                            if (tables.chance_in_2[++rng_index & Short.MAX_VALUE]) {
                                                ++dy;
                                            }
                                            dy = -dy;
                                        }
                                        if ((double)dy <= bedrock_noise * 2.0) {
                                            var9 = (byte)BEDROCK;
                                        }
                                    }
                                }
                                if (var9 != block_bedrock_id) {
                                    dy = var8 - 96;
                                    bedrock_noise = this.bedrock_strata_4_noise[local_xz_index] - this.bedrock_strata_3_noise[local_xz_index] * 0.375;
                                    if ((bedrock_noise += 0.5) > 0.0) {
                                        if (dy > 0 && (bump_noise = this.bedrock_strata_4_bump_noise[local_xz_index]) > 0.0) {
                                            bedrock_noise += bump_noise * 0.25 + 0.25;
                                        }
                                        if (dy < 0) {
                                            if (tables.chance_in_2[++rng_index & Short.MAX_VALUE]) {
                                                ++dy;
                                            }
                                            dy = -dy;
                                        }
                                        if ((double)dy <= bedrock_noise * 2.0) {
                                            var9 = (byte)BEDROCK;
                                        }
                                    }
                                }
                            }
                        } else {
                            index = var6 << 11 | var7 << 7 | var8;
                            var9 = index < 0 || index >= caves.length ? (byte)STONE : caves[index];
                            var8 += y_offset;
                        }
                        result[(var6*16+var7)*256+var8]=var9;
                        var8 -= y_offset;
                    }
                }
            }
        return result;
    }

}

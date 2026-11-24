package dev.koikaster.mite;
/** Exact new post-field dependencies from adr/ls/abw; no new RNG is substituted. */
final class OriginalPostMath {
 static int hash(int x,int z){int hash=17;hash=hash*31+x;hash=hash*31+z;return hash;}
 static int chunk(int block){return block>>4;}
 static int floor(double value){int integral=(int)value;return value<integral?integral-1:integral;}
 static double distanceSq(double x,double z){return x*x+z*z;}
 static double distance(double x,double z){return (float)Math.sqrt(distanceSq(x,z));}
}

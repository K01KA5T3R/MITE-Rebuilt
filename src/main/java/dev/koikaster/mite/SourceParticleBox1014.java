package dev.koikaster.mite;
/** R195 asx axis offsets and limits; only the temporary AABB pool becomes a fresh Java value. */
public final class SourceParticleBox1014 {
 public double a,b,c,d,e,f;
 public SourceParticleBox1014(double a,double b,double c,double d,double e,double f){this.a=a;this.b=b;this.c=c;this.d=d;this.e=e;this.f=f;}
 public SourceParticleBox1014 copy(){return new SourceParticleBox1014(a,b,c,d,e,f);}
 public SourceParticleBox1014 offset(double x,double y,double z){a+=x;b+=y;c+=z;d+=x;e+=y;f+=z;return this;}
 public SourceParticleBox1014 swept(double x,double y,double z){double minX=a,minY=b,minZ=c,maxX=d,maxY=e,maxZ=f;if(x<0)minX+=x;if(x>0)maxX+=x;if(y<0)minY+=y;if(y>0)maxY+=y;if(z<0)minZ+=z;if(z>0)maxZ+=z;return new SourceParticleBox1014(minX,minY,minZ,maxX,maxY,maxZ);}
 public boolean intersects(SourceParticleBox1014 other){return other.d>a&&other.a<d&&other.e>b&&other.b<e&&other.f>c&&other.c<f;}
    public double calculateXOffset(SourceParticleBox1014 par1AxisAlignedBB, double par2, double[] limits) {
        if (par1AxisAlignedBB.e > this.b && par1AxisAlignedBB.b < this.e) {
            if (par1AxisAlignedBB.f > this.c && par1AxisAlignedBB.c < this.f) {
                double var4;
                if (par2 > 0.0 && par1AxisAlignedBB.d <= this.a) {
                    if (Double.isNaN(limits[0]) || limits[0] > this.a) {
                        limits[0] = this.a;
                    }
                    if ((var4 = this.a - par1AxisAlignedBB.d) < par2) {
                        par2 = var4;
                    }
                }
                if (par2 < 0.0 && par1AxisAlignedBB.a >= this.d) {
                    if (Double.isNaN(limits[1]) || limits[1] < this.d) {
                        limits[1] = this.d;
                    }
                    if ((var4 = this.d - par1AxisAlignedBB.a) > par2) {
                        par2 = var4;
                    }
                }
                return par2;
            }
            return par2;
        }
        return par2;
    }
    public double calculateYOffset(SourceParticleBox1014 par1AxisAlignedBB, double par2, double[] limits) {
        if (par1AxisAlignedBB.d > this.a && par1AxisAlignedBB.a < this.d) {
            if (par1AxisAlignedBB.f > this.c && par1AxisAlignedBB.c < this.f) {
                double var4;
                if (par2 > 0.0 && par1AxisAlignedBB.e <= this.b) {
                    if (Double.isNaN(limits[0]) || limits[0] > this.b) {
                        limits[0] = this.b;
                    }
                    if ((var4 = this.b - par1AxisAlignedBB.e) < par2) {
                        par2 = var4;
                    }
                }
                if (par2 < 0.0 && par1AxisAlignedBB.b >= this.e) {
                    if (Double.isNaN(limits[1]) || limits[1] < this.e) {
                        limits[1] = this.e;
                    }
                    if ((var4 = this.e - par1AxisAlignedBB.b) > par2) {
                        par2 = var4;
                    }
                }
                return par2;
            }
            return par2;
        }
        return par2;
    }
    public double calculateZOffset(SourceParticleBox1014 par1AxisAlignedBB, double par2, double[] limits) {
        if (par1AxisAlignedBB.d > this.a && par1AxisAlignedBB.a < this.d) {
            if (par1AxisAlignedBB.e > this.b && par1AxisAlignedBB.b < this.e) {
                double var4;
                if (par2 > 0.0 && par1AxisAlignedBB.f <= this.c) {
                    if (Double.isNaN(limits[0]) || limits[0] > this.c) {
                        limits[0] = this.c;
                    }
                    if ((var4 = this.c - par1AxisAlignedBB.f) < par2) {
                        par2 = var4;
                    }
                }
                if (par2 < 0.0 && par1AxisAlignedBB.c >= this.f) {
                    if (Double.isNaN(limits[1]) || limits[1] < this.f) {
                        limits[1] = this.f;
                    }
                    if ((var4 = this.f - par1AxisAlignedBB.c) > par2) {
                        par2 = var4;
                    }
                }
                return par2;
            }
            return par2;
        }
        return par2;
    }
}

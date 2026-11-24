package dev.koikaster.mite;
/** R195 nn.getBlockRestingOn3. Collision bounds are the world API transport. */
public final class SourceRestingRules1182 {
 public interface View<B,S> {
  boolean grounded();boolean riding();double minX();double maxX();double minY();double maxY();double minZ();double maxZ();double x();double z();
  B block(int x,int y,int z);Double collisionTop(B block,int x,int y,int z);S support(B block,int x,int y,int z);
 }
 public static <B,S> S resting(View<B,S> h){
  if(!h.grounded()||h.riding())return null;
  S info=null;double highest=0d,shortest=0d;
  int minX=(int)Math.floor(h.minX()),maxX=(int)Math.ceil(h.maxX())-1,minY=(int)Math.floor(h.minY()),maxY=(int)Math.ceil(h.maxY())-1,minZ=(int)Math.floor(h.minZ()),maxZ=(int)Math.ceil(h.maxZ())-1;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)for(int y=minY-1;y<=maxY;y++){
   B block=h.block(x,y,z);Double top;if(block==null||(top=h.collisionTop(block,x,y,z))==null||top!=h.minY())continue;
   if(info==null||top>highest){info=h.support(block,x,y,z);highest=top;double dx=(double)x+.5-h.x(),dz=(double)z+.5-h.z();shortest=dx*dx+dz*dz;continue;}
   double dx,dz,distance;
   if(top!=highest||!((distance=(dx=(double)x+.5-h.x())*dx+(dz=(double)z+.5-h.z())*dz)<shortest))continue;
   info=h.support(block,x,y,z);shortest=distance;
  }
  return info;
 }
 private SourceRestingRules1182(){}
}

package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.EnderDragonPart;

/** Seven source parts, registered through the native Forge multipart carrier. */
public final class SourceDragonPart extends EnderDragonPart {
 private EntityDimensions sourceSize;
 public SourceDragonPart(SourceEnderDragon parent,String name,float width,float height){super(parent,name,width,height);sourceSize=EntityDimensions.scalable(width,height);refreshDimensions();}
 public void size(float width,float height){if(sourceSize.width!=width||sourceSize.height!=height)sourceSize=EntityDimensions.scalable(width,height);}
 @Override public EntityDimensions getDimensions(Pose pose){return sourceSize==null?super.getDimensions(pose):sourceSize;}
 public void position(double x,double y,double z){xo=getX();yo=getY();zo=getZ();xOld=xo;yOld=yo;zOld=zo;if(getBbWidth()!=sourceSize.width||getBbHeight()!=sourceSize.height)refreshDimensions();setPos(x,y,z);setRot(0,0);}
 // si forwards every part hit before the parent decides acceptance.
 @Override public boolean hurt(DamageSource source,float amount){return parentMob.hurt(this,source,amount);}
}

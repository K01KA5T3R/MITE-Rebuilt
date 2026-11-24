package dev.koikaster.mite;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Native position/motion/Forge-part transport for the original sk.c math. */
public final class SourceDragonKinematicsAdapter implements SourceDragonKinematics.MotionHost,SourceDragonKinematics.LayoutHost {
 private final SourceEnderDragon actor;
 private boolean originalContact;
 public SourceDragonKinematicsAdapter(SourceEnderDragon actor){this.actor=actor;}
 public SourceDragonRules.State state(){return actor.sourceBrain().state;}public double x(){return actor.getX();}public double y(){return actor.getY();}public double z(){return actor.getZ();}public float yaw(){return actor.getYRot();}public void yaw(float value){actor.setYRot(value);}public float turning(){return actor.yRotA;}public void turning(float value){actor.yRotA=value;}
 public SourceDragonKinematics.Vector motion(){var m=actor.getDeltaMovement();return new SourceDragonKinematics.Vector(m.x,m.y,m.z);}public void motion(SourceDragonKinematics.Vector value){actor.setDeltaMovement(value.x(),value.y(),value.z());}
 public SourceDragonKinematics.Target tracked(){if(state().target instanceof SourceDragonBrain.Node node){var target=node.value();return new SourceDragonKinematics.Target(target.getX(),target.getZ(),target.getBoundingBox().minY);}return null;}
 public double nextGaussian(){return actor.getRandom().nextGaussian();}public boolean horizontalCollision(){return actor.horizontalCollision;}public boolean verticalCollision(){return actor.verticalCollision;}public boolean blocked(){return actor.inWall;}public void chooseTarget(){SourceDragonRules.chooseTarget(actor.sourceBrain());}public void accelerate(float value){actor.moveRelativeSource(value);}public void move(SourceDragonKinematics.Vector value){actor.move(MoverType.SELF,new Vec3(value.x(),value.y(),value.z()));}
 public double[] history(int offset,float partial){return actor.getLatencyPos(offset,partial);}public void size(int part,float width,float height){actor.sourceParts()[part].size(width,height);}public void part(int part,double x,double y,double z){actor.sourceParts()[part].position(x,y,z);}
 void layout(boolean allowContact){originalContact=allowContact;SourceDragonKinematics.layout(this);}
 public boolean contactAllowed(){return originalContact&&!actor.level().isClientSide&&actor.hurtTime==0&&!actor.isNoAi();}
 public void contact(){var p=actor.sourceParts();var source=actor.sourceBrain();source.knock(p[5].getBoundingBox().inflate(4,2,4).move(0,-2,0));source.knock(p[6].getBoundingBox().inflate(4,2,4).move(0,-2,0));source.bite(p[0].getBoundingBox().inflate(1));}
}

package dev.koikaster.mite.visual;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** bbj.a(FFFFFF,Entity), original 1.6.4/R195 player branch only.
 * Runs after native humanoid setup and before PlayerModel copies clothing overlays.
 * Entity posture, hitbox, equipment, use timer and swing state are read-only.
 */
@OnlyIn(Dist.CLIENT)
public final class ClassicThirdPersonAnimations {
 public static void apply(Object rawModel,Object rawEntity,float limb,float amount,float age,float yaw,float pitch){
  if(!ClassicVisuals.entities()||!(rawModel instanceof HumanoidModel<?> m)||!(rawEntity instanceof AbstractClientPlayer p))return;
  // Keep functional modern poses and other mods' specialized hand poses intact.
  if(p.getMainArm()!=HumanoidArm.RIGHT||!p.getOffhandItem().isEmpty()||p.isFallFlying()||p.isAutoSpinAttack()||m.swimAmount>0||p.getPose()!=Pose.STANDING&&p.getPose()!=Pose.CROUCHING)return;
  if(!oldPose(m.rightArmPose)||!oldPose(m.leftArmPose))return;
  m.head.yRot=yaw/57.295776f;m.head.xRot=pitch/57.295776f;m.head.zRot=0;
  m.body.setPos(0,0,0);m.body.zRot=0;m.rightArm.setPos(-5,2,0);m.leftArm.setPos(5,2,0);
  m.rightLeg.x=-2;m.leftLeg.x=2;m.rightLeg.zRot=m.leftLeg.zRot=0;
  m.rightArm.xRot=Mth.cos(limb*.6662f+(float)Math.PI)*amount;m.leftArm.xRot=Mth.cos(limb*.6662f)*amount;
  m.rightArm.zRot=m.leftArm.zRot=0;m.rightLeg.xRot=Mth.cos(limb*.6662f)*1.4f*amount;m.leftLeg.xRot=Mth.cos(limb*.6662f+(float)Math.PI)*1.4f*amount;
  m.rightLeg.yRot=m.leftLeg.yRot=0;
  if(m.riding){m.rightArm.xRot-=.62831855f;m.leftArm.xRot-=.62831855f;m.rightLeg.xRot=m.leftLeg.xRot=-1.2566371f;m.rightLeg.yRot=.31415927f;m.leftLeg.yRot=-.31415927f;}
  if(m.leftArmPose!=HumanoidModel.ArmPose.EMPTY)m.leftArm.xRot=m.leftArm.xRot*.5f-.31415927f*(m.leftArmPose==HumanoidModel.ArmPose.BLOCK?3:1);
  if(m.rightArmPose!=HumanoidModel.ArmPose.EMPTY)m.rightArm.xRot=m.rightArm.xRot*.5f-.31415927f*(m.rightArmPose==HumanoidModel.ArmPose.BLOCK?3:1);
  m.rightArm.yRot=m.leftArm.yRot=0;
  float attack=m.attackTime;m.body.yRot=Mth.sin(Mth.sqrt(attack)*(float)Math.PI*2)*.2f;
  m.rightArm.z=Mth.sin(m.body.yRot)*5;m.rightArm.x=-Mth.cos(m.body.yRot)*5;m.leftArm.z=-Mth.sin(m.body.yRot)*5;m.leftArm.x=Mth.cos(m.body.yRot)*5;
  m.rightArm.yRot+=m.body.yRot;m.leftArm.yRot+=m.body.yRot;m.leftArm.xRot+=m.body.yRot;
  float eased=1-attack;eased*=eased;eased*=eased;eased=1-eased;
  float sweep=Mth.sin(eased*(float)Math.PI),head=Mth.sin(attack*(float)Math.PI)*-(m.head.xRot-.7f)*.75f;
  m.rightArm.xRot=(float)(m.rightArm.xRot-((double)sweep*1.2+head));m.rightArm.yRot+=m.body.yRot*2;m.rightArm.zRot=Mth.sin(attack*(float)Math.PI)*-.4f;
  if(m.crouching){m.body.xRot=.5f;m.rightArm.xRot+=.4f;m.leftArm.xRot+=.4f;m.rightLeg.z=m.leftLeg.z=4;m.rightLeg.y=m.leftLeg.y=9;m.head.y=1;}
  else{m.body.xRot=0;m.rightLeg.z=m.leftLeg.z=.1f;m.rightLeg.y=m.leftLeg.y=12;m.head.y=0;}
  bob(m,age);
  if(m.rightArmPose==HumanoidModel.ArmPose.BOW_AND_ARROW){m.rightArm.zRot=m.leftArm.zRot=0;m.rightArm.yRot=-.1f+m.head.yRot;m.leftArm.yRot=.1f+m.head.yRot+.4f;m.rightArm.xRot=m.leftArm.xRot=-1.5707964f+m.head.xRot;bob(m,age);}
  m.hat.copyFrom(m.head);
 }
 private static boolean oldPose(HumanoidModel.ArmPose pose){return pose==HumanoidModel.ArmPose.EMPTY||pose==HumanoidModel.ArmPose.ITEM||pose==HumanoidModel.ArmPose.BLOCK||pose==HumanoidModel.ArmPose.BOW_AND_ARROW;}
 private static void bob(HumanoidModel<?> m,float age){m.rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;m.leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;m.rightArm.xRot+=Mth.sin(age*.067f)*.05f;m.leftArm.xRot-=Mth.sin(age*.067f)*.05f;}
 private ClassicThirdPersonAnimations(){}
}

package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Source pl control and nine ry temptation identities. Other livestock parents
 * and the human-retained modern animal feeding remain in their existing owners. */
public final class SourcePig extends Pig {
 private SourcePigControl control;
 public SourcePig(EntityType<? extends Pig> type,Level level){super(type,level);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.25));control=new SourcePigControl(this);goalSelector.addGoal(2,control);goalSelector.addGoal(3,new BreedGoal(this,1));
  for(String material:SourceCarrotSticks.MATERIALS)goalSelector.addGoal(4,new TemptGoal(this,1.2,Ingredient.of(SourceCarrotSticks.item(material)),false));
  goalSelector.addGoal(4,new TemptGoal(this,1.2,Ingredient.of(Items.CARROT,Items.POTATO,Items.BEETROOT),false));goalSelector.addGoal(5,new FollowParentGoal(this,1.1));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
 }
 public SourcePigControl control(){return control;}
 @Override public LivingEntity getControllingPassenger(){return null;} // pl, rather than the modern local-rider clock, owns motion.
 @Override public boolean boost(){if(control==null||!control.canBoost())return false;control.boost();return true;}
 void sourceRiderTravel(float forward){setSpeed((float)getAttributeValue(Attributes.MOVEMENT_SPEED));super.travel(new Vec3(0,0,forward));}
 @Override public void travel(Vec3 input){if(!level().isClientSide&&control!=null&&control.running())return;super.travel(input);}
}

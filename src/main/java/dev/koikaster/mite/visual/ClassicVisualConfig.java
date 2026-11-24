package dev.koikaster.mite.visual;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client presentation only. No server, world, or gameplay parameters. */
final class ClassicVisualConfig {
 static final ForgeConfigSpec SPEC;
 static final ForgeConfigSpec.BooleanValue ENABLED,TEXTURES,LIGHTING,SKY,FOG,WATER,ENTITIES,FIRST_PERSON,PARTICLES,GUI,FONT;
 static{
  var b=new ForgeConfigSpec.Builder();
  b.push("visuals");ENABLED=b.define("enabled",true);TEXTURES=b.define("textures",true);LIGHTING=b.define("lighting",true);SKY=b.define("skyAndClouds",true);FOG=b.define("fog",true);WATER=b.define("water",true);ENTITIES=b.define("entityAppearance",true);FIRST_PERSON=b.define("firstPersonAnimations",true);PARTICLES=b.define("particles",true);GUI=b.define("gui",true);FONT=b.define("font",true);b.pop();SPEC=b.build();
 }
 private ClassicVisualConfig(){}
}

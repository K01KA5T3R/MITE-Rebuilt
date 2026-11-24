package dev.koikaster.mite;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){
  e.enqueueWork(()->{var bow=TorchBowContent.BOW.get();net.minecraft.client.renderer.item.ItemProperties.register(bow,new ResourceLocation("pull"),(stack,level,entity,seed)->entity!=null&&entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/(float)ClassicArchery.maxPull(stack):0);net.minecraft.client.renderer.item.ItemProperties.register(bow,new ResourceLocation("pulling"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);});
  e.enqueueWork(()->MenuScreens.register(AdvancedEnchantContent.MENU.get(),AdvancedEnchantScreen::new));
  e.enqueueWork(()->MenuScreens.register(MithrilAutoWorkbench.MENU.get(),MithrilAutoWorkbenchScreen::new));
  e.enqueueWork(()->{for(var rune:new net.minecraft.world.level.block.Block[]{SourceRunestoneContent.MITHRIL.get(),SourceRunestoneContent.ADAMANTIUM.get()})net.minecraft.client.renderer.item.ItemProperties.register(rune.asItem(),Content.id("rune"),(stack,level,entity,seed)->SourceRunestoneItem.rune(stack));MenuScreens.register(Content.ENCHANT_MENU.get(),SourceEnchantScreen::new);MenuScreens.register(Content.REPAIR_MENU.get(),SourceRepairScreen::new);MenuScreens.register(Content.CRAFT_MENU.get(),SourceCraftScreen::new);MenuScreens.register(Content.FURNACE_MENU.get(),SourceFurnaceScreen::new);MenuScreens.register(Content.BREW_MENU.get(),SourceBrewingScreen1412::new);
   for(String material:new String[]{"copper","silver","gold","bronze","iron","steel","mithril","adamantium","ancient_metal"})net.minecraft.client.renderer.item.ItemProperties.register(Content.get(material+"_anvil"),Content.id("wear"),(stack,level,entity,seed)->SourceAnvil.stage(material,stack.getTagElement("BlockEntityTag")==null?0:stack.getTagElement("BlockEntityTag").getInt("damage")));
   for(String metal:Content.METALS){
    var bucket=Content.get(metal+"_bucket");net.minecraft.client.renderer.item.ItemProperties.register(bucket,Content.id("fluid"),(stack,level,entity,seed)->stack.getItem() instanceof SpecialItems.MetalBucket item?switch(item.fluid(stack)){case "water"->1;case "lava"->2;case "milk"->3;case "stone"->4;default->0;}:0);
    if(metal.equals("mithril")){var bow=Content.get(metal+"_bow");net.minecraft.client.renderer.item.ItemProperties.register(bow,new ResourceLocation("pull"),(stack,level,entity,seed)->entity!=null&&entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/(float)ClassicArchery.maxPull(stack):0);
    net.minecraft.client.renderer.item.ItemProperties.register(bow,new ResourceLocation("pulling"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);}
   }
   net.minecraft.client.renderer.item.ItemProperties.register(Content.ANCIENT_METAL_BUCKET.get(),Content.id("fluid"),(stack,level,entity,seed)->switch(ClassicVessels.fluid(stack)){case "water"->1;case "lava"->2;case "milk"->3;case "stone"->4;default->0;});
   net.minecraft.client.renderer.item.ItemProperties.register(Items.BOW,new ResourceLocation("pull"),(stack,level,entity,seed)->entity!=null&&entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/(float)ClassicArchery.maxPull(stack):0);
   for(String name:OriginalToolItems.ENTRIES.keySet()){
    var item=Content.get(name);
    if(item instanceof BowItem){net.minecraft.client.renderer.item.ItemProperties.register(item,new ResourceLocation("pull"),(stack,level,entity,seed)->entity!=null&&entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/(float)ClassicArchery.maxPull(stack):0);net.minecraft.client.renderer.item.ItemProperties.register(item,new ResourceLocation("pulling"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);}
    if(item instanceof FishingRodItem)net.minecraft.client.renderer.item.ItemProperties.register(item,new ResourceLocation("cast"),(stack,level,entity,seed)->entity instanceof net.minecraft.world.entity.player.Player player&&player.fishing!=null&&(player.getMainHandItem()==stack||player.getOffhandItem()==stack)?1:0);
   }
net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(Content.ONIONS.get(),net.minecraft.client.renderer.RenderType.cutout());
   for(var crop:new net.minecraft.world.level.block.Block[]{Content.DEAD_WHEAT.get(),Content.DEAD_CARROTS.get(),Content.DEAD_POTATOES.get(),Content.DEAD_ONIONS.get()})net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(crop,net.minecraft.client.renderer.RenderType.cutout());
  });
 }
 @SubscribeEvent public static void scorchedFoodColor(RegisterColorHandlersEvent.Item event){
  event.register((stack,layer)->layer==0?0x644B41:-1,ScorchedFood2072.ITEM.get());
 }
 @SubscribeEvent public static void packs(net.minecraftforge.event.AddPackFindersEvent event){ClassicResourcePacks.addImports(event);}
 @SubscribeEvent public static void records(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event){for(var entries=event.getEntries().iterator();entries.hasNext();)if(removedCreative(entries.next().getKey()))entries.remove();}
 @SubscribeEvent public static void dimensions(RegisterDimensionSpecialEffectsEvent event){event.register(new ResourceLocation("minecraft:overworld"),new ClassicDimensionEffects());}
 @SubscribeEvent public static void armorLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(SourceHorseArmorModel.LAYER,SourceHorseArmorModel::layer);}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerEntityRenderer(GiantSkeletonContent.BOSS.get(),GiantSkeletonClient::new);
  e.registerEntityRenderer(RedstoneGolemContent.GOLEM.get(),ctx->new net.minecraft.client.renderer.entity.IronGolemRenderer(ctx){
   { model = new RedstoneGolemModel(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.IRON_GOLEM)); }
   @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.IronGolem golem){
    return new ResourceLocation(MiteReforged.ID,"textures/entity/redstone_iron_golem.png");
   }
  });
  e.registerEntityRenderer(MythrilKnightContent.MOUNT.get(),ctx->new RavagerRenderer(ctx){
   { model = new MythrilRavagerModel(ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.RAVAGER)); }
   @Override public ResourceLocation getTextureLocation(Ravager entity){return Content.id("textures/entity/mythril_ravager.png");}
  });
  e.registerEntityRenderer(MythrilKnightContent.RIDER.get(),ctx->new PillagerRenderer(ctx){
   @Override public ResourceLocation getTextureLocation(Pillager entity){return Content.id("textures/entity/mythril_knight.png");}
  });
  e.registerEntityRenderer(TorchBowContent.SHOT.get(),ThrownItemRenderer::new);
  e.registerEntityRenderer(SourceBrick.TYPE.get(),ThrownItemRenderer::new);
  e.registerBlockEntityRenderer(AdvancedEnchantContent.ENTITY.get(),net.minecraft.client.renderer.blockentity.EnchantTableRenderer::new);
  e.registerEntityRenderer(SourceCreeperContent.INFERNAL.get(),ctx->new CreeperRenderer(ctx){public ResourceLocation getTextureLocation(Creeper entity){return Content.id("textures/entity/infernal_creeper.png");}});
  e.registerEntityRenderer(net.minecraft.world.entity.EntityType.WITHER_SKELETON,SourceWitherSkeletonRenderer::new);e.registerEntityRenderer(SourceStalkerContent.STALKER.get(),SourceInvisibleStalkerRenderer::new);
  e.registerEntityRenderer(SourceNetherspawnContent.NETHERSPAWN.get(),ctx->new net.minecraft.client.renderer.entity.SilverfishRenderer(ctx){public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Silverfish entity){return Content.id("textures/entity/netherspawn.png");}});
  e.registerEntityRenderer(SourceSilverfishContent.COPPERSPINE.get(),ctx->new net.minecraft.client.renderer.entity.SilverfishRenderer(ctx){public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Silverfish entity){return Content.id("textures/entity/copperspine.png");}});
  e.registerEntityRenderer(SourceSilverfishContent.HOARY.get(),ctx->new net.minecraft.client.renderer.entity.SilverfishRenderer(ctx){public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Silverfish entity){return Content.id("textures/entity/hoary_silverfish.png");}});
  e.registerEntityRenderer(net.minecraft.world.entity.EntityType.SKELETON,SourceSkeletonRenderer::new);e.registerEntityRenderer(SourceBoneLordContent.LORD.get(),SourceSkeletonRenderer::new);e.registerEntityRenderer(SourceBoneLordContent.ANCIENT.get(),SourceSkeletonRenderer::new);
  e.registerEntityRenderer(SourceLongdeadContent.LONGDEAD.get(),SourceLongdeadRenderer::new);e.registerEntityRenderer(SourceLongdeadContent.GUARDIAN.get(),SourceLongdeadRenderer::new);
  e.registerBlockEntityRenderer(Content.EMERALD_ENCHANTING_ENTITY.get(),net.minecraft.client.renderer.blockentity.EnchantTableRenderer::new);e.registerEntityRenderer(Content.ANVIL_FALLING.get(),FallingBlockRenderer::new);
  Mobs.UNDEAD.forEach((name,type)->{if(java.util.Set.of("earth_elemental","fire_elemental","shadow","clay_golem").contains(name))e.registerEntityRenderer(type.get(),SourceStalkerRenderer::new);else e.registerEntityRenderer(type.get(),ClassicMobModels::renderer);});
  e.registerEntityRenderer(SourceArachnidContent.WEB.get(),SourceWebRenderer::new);
  Mobs.SPIDERS.forEach((name,t)->e.registerEntityRenderer(t.get(),ctx->new SourceArachnidRenderer(ctx,name)));
  e.registerEntityRenderer(net.minecraft.world.entity.EntityType.SPIDER,ctx->new SourceArachnidRenderer(ctx,"spider"));
  e.registerEntityRenderer(net.minecraft.world.entity.EntityType.CAVE_SPIDER,ctx->new SourceArachnidRenderer(ctx,"cave_spider"));
  Mobs.SLIMES.values().forEach(t->e.registerEntityRenderer(t.get(),SourceGelatinousRenderer::new));e.registerEntityRenderer(SourceGelatinousContent.SPHERE.get(),ThrownItemRenderer::new);
  Mobs.WOLVES.forEach((name,t)->e.registerEntityRenderer(t.get(),ctx->new WolfRenderer(ctx){{addLayer(ClassicMobModels.glow(this,name));}public ResourceLocation getTextureLocation(Wolf entity){return Content.id("textures/entity/"+Mobs.name(entity)+".png");}}));
  e.registerEntityRenderer(net.minecraft.world.entity.EntityType.BAT,ctx->new SourceBatRenderer(ctx,"bat"));
  e.registerEntityRenderer(Mobs.BAT.get(),ctx->new SourceBatRenderer(ctx,"vampire_bat"));
  e.registerEntityRenderer(SourceWitchContent.PROJECTILE.get(),ThrownItemRenderer::new);
  e.registerEntityRenderer(SourceBatContent.GIANT.get(),ctx->new SourceBatRenderer(ctx,"vampire_bat"));
  e.registerEntityRenderer(SourceBatContent.NIGHTWING.get(),ctx->new SourceBatRenderer(ctx,"nightwing"));
 }
 public static final class SourceFurnaceScreen extends AbstractContainerScreen<SourceFurnaceMenu>{
  private final ResourceLocation TEXTURE;private net.minecraft.client.gui.components.Button experience;
  public SourceFurnaceScreen(SourceFurnaceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);TEXTURE=new ResourceLocation("minecraft","textures/gui/container/"+(menu.cookingMode==ModernCooking.SMOKER?"smoker":menu.cookingMode==ModernCooking.BLAST?"blast_furnace":"furnace")+".png");}
  protected void init(){super.init();titleLabelX=(imageWidth-font.width(title))/2;experience=null;if(minecraft==null||minecraft.level==null||!WorldContentMode.rebuilt(minecraft.level))return;experience=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.mite_reforged.stored_experience",menu.storedExperience()),button->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+103,topPos+61,67,16).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.mite_reforged.collect_experience"))).build());experience.active=menu.storedExperience()>0;}
  protected void containerTick(){super.containerTick();if(experience==null)return;experience.active=menu.storedExperience()>0;experience.setMessage(Component.translatable("gui.mite_reforged.stored_experience",menu.storedExperience()));}
  protected void renderBg(GuiGraphics graphics,float partial,int x,int y){graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);if(menu.isLit()){int height=menu.getLitProgress();graphics.blit(TEXTURE,leftPos+56,topPos+36+12-height,176,12-height,14,height+1);}int width=menu.getBurnProgress();graphics.blit(TEXTURE,leftPos+79,topPos+34,176,14,width+1,16);}
  public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
 }
 private static boolean removedCreative(ItemStack stack){return LegacyMetalOres.hidden(stack)||ClassicMusic.removed(stack)||stack.is(Items.SHIELD)||ModernContentPolicy.removed(stack)||ModernContentPolicy.onlyRemovedEnchantments(stack);}
 private ClientSetup(){}
}

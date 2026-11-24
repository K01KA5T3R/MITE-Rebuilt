package dev.koikaster.mite.visual;

import java.io.InputStream;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.resource.PathPackResources;

/** Filtering only client assets; data packs and registrations are never involved. */
public final class ClassicPackResources extends PathPackResources {
 private final java.util.function.Predicate<ResourceLocation> filter;
 public ClassicPackResources(String id,Path root){super(id,true,root);filter=ClassicVisuals.resourceFilter();}
 @Override public IoSupplier<InputStream> getResource(PackType type,ResourceLocation id){
  return type==PackType.CLIENT_RESOURCES&&filter.test(id)?super.getResource(type,id):null;
 }
 @Override public void listResources(PackType type,String namespace,String path,ResourceOutput output){
  if(type==PackType.CLIENT_RESOURCES)super.listResources(type,namespace,path,(id,source)->{if(filter.test(id))output.accept(id,source);});
 }
}

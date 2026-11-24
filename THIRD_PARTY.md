# Third-party notices

The0.1.15 modern cooking adapters reuse the already attributed original furnace, fuel and result-slot rules. Smoker, blast furnace and campfire adaptations implement the user's explicit choices for post-R195 devices and reference native Minecraft GUI textures. The staged coin, vessel and repair arithmetic in docs/staged-production derives from the supplied R195 ItemCoin, wr, amv, va and aaf; these adapted portions retain their original authors' rights and are excluded from the MIT licensing claim. They are not yet activated gameplay.

The Gradle wrapper scripts and wrapper JAR were obtained from the official Forge 47.4.28 MDK. Gradle is licensed under Apache License 2.0; the original wrapper notices are retained. See https://www.apache.org/licenses/LICENSE-2.0 and https://gradle.org/.

Minecraft Forge is a build/runtime dependency under LGPL 2.1 with its stated exceptions. Its MDK license is reproduced in third_party/FORGE-MDK-LICENSE.txt. See https://github.com/MinecraftForge/MinecraftForge.

The user supplied **MITE 1.6.4 R195.zip** and explicitly requested reuse of its textures. The optional built-in pack at `src/main/resources/retro_r195` currently reuses **232 original PNG files**, mapped without changing their bytes to 236 output paths. These include material equipment, furnace faces, anvils, creature skins and emissive masks, and selected GUI textures. Adapted resource models and path mappings are recorded in `docs/r195-assets.json`. Original artwork remains the property of its original authors, including Avernite and the original MITE contributors; it is **not relicensed under MIT**. No assertion of permission to redistribute third-party artwork beyond the user's requested use is made.

The scalar growth, XP and level-bonus calculations in `Rules.java` and the lighting, moon and fog calculations in `LegacyLighting.java` were adapted from classes in that supplied R195 build. The isolated test-only `R195LightmapReference.java` preserves arithmetic extracted from its EntityRenderer lightmap method for numerical comparison. Those adapted portions are attributed to the original MITE/Minecraft authors and excluded from this project's MIT licensing claim. Complete original patched classes, decompiled game sources and the original installation archive are not included in this source distribution.

The earlier 426 generated modern-block redraws and other procedural mod PNGs are retired and are not included in the runtime resources. Native Minecraft item models and Programmer Art are referenced from the installed game. Two unmodified vanilla iron armor PNGs are copied for the custom armor material path; these retain Mojang rights.

Standalone texture ZIPs carry these same notices. They do not include the classic lightmap implementation or gameplay classes. This independent project is not an official MITE port or an endorsement by the original author.


## Base Metals 1.12 original PNG assets

Original artwork by Eejit and Base Metals contributors. Source: https://github.com/MinecraftModDevelopmentMods/BaseMetals, commit 197a4b36eaeb16617c9b0d2c2d373e7771354a4b. The selected PNGs are copied byte for byte; paths and hashes are recorded in third_party/basemetals-source.json and docs/legacy-asset-sources.json. Artwork remains under CC BY-NC-SA 4.0; see third_party/BaseMetals-LICENSE-ART.txt. No MIT relicensing is asserted. Native Minecraft fallback references and copied iron armor PNGs retain Mojang rights. The earlier generated modern-block replacements have been retired at the user request.

The two-store food tick and test-only R195FoodReference.java adapt FoodStats (ux) arithmetic from the supplied R195 build. Original MITE/Minecraft author rights are retained; these adapted portions are excluded from the MIT licensing claim. Modern nutrient categories, action inputs and non-counterpart foods remain project adaptations. MITE_icons.png is a separate custom atlas and is no longer substituted for the native Minecraft HUD atlas.

The native offhand-compatible inventory background is referenced from the installed game instead of substituting the old 1.6.4 background. Cooking recipe declarations imported from the installed Minecraft 1.20.1 client retain their original source attribution; the migration report is docs/cooking-reference.json.

The single nutrition HUD row, hunger jitter condition and sprint food-energy gate adapt the supplied R195 avj/uf/bex behavior. See docs/r195-hud-reference.json. Those adapted portions retain their original MITE/Minecraft author rights and are excluded from the MIT licensing claim.

Nutrient/insulin arithmetic, wetness, edible-item counterparts, cake/milk behavior, regeneration and inventory status logic adapt supplied R195 jv/yc/ux/axp and related classes. R195DietReference.java is a test-only extracted arithmetic fixture. Two additional GUI PNGs are copied verbatim for inventory status rendering under this mod namespace; they do not replace native HUD or main inventory textures. These adapted portions retain original author rights and are excluded from the MIT licensing claim. Modern profile choices and maximum enchanting power are documented adaptations.

0.1.6农业：枯死作物17张、施肥耕地2张、粪肥1张新增PNG，以及两个洋葱阶段修正均直接复制用户提供的R195原包字节；没有绘图、裁切或重新着色。对应路径与SHA256见docs/agriculture-asset-validation.json。

The0.1.16 source currency adapter and LegacyProduction arithmetic derive from the supplied R195 ItemCoin/RecipesMITE/wd/aaf. Original author rights are retained and those adapted portions are excluded from the MIT licensing claim. The one added ancient metal coin PNG is copied verbatim; its source path and hash are in docs/currency-new-asset.json. Vessel/repair portions of the unchanged numeric helper remain prepared for subsequent runtime integration.

The0.1.17 vessel adapter derives from supplied R195 wr/ItemVessel/gt/gs/jv/ss/RecipesMITE/abw; original rights retained, these adapted portions excluded from the MIT licensing claim. Eleven added PNGs are copied verbatim as recorded in docs/vessel-new-assets.json. Selected bronze/steel stone states reuse the original iron image without alteration.

The0.1.18 repair/anvil adapter derives from supplied R195 va/vc/amv/TileEntityAnvil/awz/aph and related original classes. Original rights retained; adapted portions excluded from the MIT licensing claim.21 additional PNGs are copied verbatim; source/hash evidence in docs/repair-new-assets.json. Selected bronze/steel new states reuse iron assets without editing.

The0.1.19 workbench/leather repair and source falling-anvil damage derive from supplied R195 aaf/sr; original rights retained and adapted portions excluded from the MIT licensing claim. Original new-world professions default evidence is in docs/source-professions-default.json. No new PNGs or models were created.

0.1.20 armor, defense, blocking and silver poison-duration branches derive from supplied R195 yc/wh/zx/ud/of/Damage/nb/abg/aas/RecipeHelper/aaf/nj.72 new PNGs are exact supplied original bytes. Source horse armor UV rectangles derive from Mojang1.6.4 bbp, fitted to modern HorseModel geometry/poses without editing source texture pixels, verified official client SHA1 in docs/horse-model-provenance.json. Original rights retained; adapted code/artwork excluded from the MIT licensing claim. No texture pixels edited.

0.1.21 enchanting/crop/loot/combat/curse-audit code derives from supplied R195 aau/aaw/vm/aoc/uf/tm/abh/anr/ani/apt/BlockOnion/rr/ry/rz/rs/of/jw/yb. Three new PNGs are byte-exact: two supplied R195 emerald table images and the officially SHA1-verified Mojang1.6.4 native GUI (no MITE override). Original rights retained; derived code/artwork is excluded from the MIT licensing claim. No PNG pixels edited.

0.1.22 world-curse code derives from supplied R195 Curse/CurseInstance/WorldInfoShared/tv/re/js/uf/of/ye/wh/vw/ank/bdi/tg/bcw/avj/axp/nn and food/material declarations. English curse strings copied from supplied MITE.lang; Chinese strings translated. Existing frozen original PNGs reused by UV; no new or modified PNGs. Original rights retained; derived code and strings are excluded from the MIT licensing claim.

0.1.23 reach and raycast code derives from supplied R195 uf/of/og/nn/yc/xj/sd/rp/oc/uj/rs/Raycast/RaycastPolicies/RaycastCollision/ka/atv/aot/anj/asx, adapted to Forge1.20.1 voxel-shape, pose, input and packet APIs. Existing numeric tool bonuses and original assets are reused; no new/modified PNGs or models. Derived code remains excluded from the MIT licensing claim; original rights retained.

0.1.24 vision/despawn/lifecycle adapters derive from supplied R195 nn/of/og/rp/re/tm/rv/rx/se/sf/sk/sm/tv/tw/ub/abw/aci/aps/aqs/bfl/ka/Packet87SetDespawnCounters/Raycast/RaycastPolicies/aqz, adapted to Forge1.20.1 entity, NBT, voxel, render, target and spawn APIs. Existing original artwork remains unchanged. Derived code remains excluded from the MIT licensing claim; original rights retained.

0.1.25 source NPC adapters derive from supplied R195 qa/re/qy/qx/qm/pm/pj/EntityAnimalWatcher/EntityAIWatchAnimal and selected species, with native1.20.1 navigation, damage and renderer API adaptation. SourceCraftScreen adapts the supplied native Minecraft1.20.1 CraftingScreen/RecipeBookMenu behavior; original rights retained. Existing PNG/UV resources are unchanged.

0.1.26 item movement and food pickup adapt supplied R195 EntityAIMovementTask/EntityAIMoveToItem/EntityAIMoveToFoodItem/og/ss/yc to native1.20.1 paths, item data, source item ages and concrete Ghoul/Revenant actors. Other owner traits remain incomplete; original rights retained. Existing PNG/UV assets unchanged.

0.1.27 adapts supplied R195 RNG, nn/og/abw/aoi/aoa/bcw, EntityAIGetOutOfWater/EntityAISeekShelterFromRain/EntityAIMoveToTree and listed source actors to native1.20.1. Original rights retained. No new or edited PNG/model assets. Full roster/environment remains incomplete.

0.1.28 adapts user-supplied R195 special creature classes, js/tm/rp/sf/oq, po/qp, uf/jv/bdi/bcw/ka and source renderers.13PNG+7OGG copied unchanged from supplied archive; original rights retained. Full port incomplete.

0.1.29 adapts R195 aci/rs/tr/sb/og/ss/EntityHellhound/pp/pw/px/ql/EntityAIGetOutOfLava/EntityRepairFX/bcw/bfl. Seven original Hellhound OGGs and one complete original particle atlas copied unchanged. No PNG editing. Original rights retained.

0.1.30 adapts R195 sf/rp/on/tm/EntityArachnid/EntityWoodSpider/EntityDemonSpider/EntityPhaseSpider/EntityWeb/uq and native owners. The original web icon is copied unchanged; the existing complete particle atlas is reused without image editing. Original rights retained.

0.1.31 adapts R195 of/on/tm harmful-entity memory, flee decisions and arachnid path/decision callers to native modern hurt/tick/navigation. Existing image/audio bytes remain unchanged. Original rights retained.

0.1.32 copies/adapts R195 ajs/ajt/ChunkProviderUnderworld, adr Underworld strata, aet cobweb placement, WorldInfoShared seed hashing, ls floor and BlockMantleOrCore/ape material checks. The existing frozen RNG initializer is reused with per-world ownership. Original mantle/core PNGs and core animation metadata are copied unchanged. Original rights retained.

0.1.33 copies/adapts supplied R195 aps/acj ordinary portal roles/search/placement and spawn transition, and EntityWight/EntityShadow/EntityEarthElemental isImmuneTo with nb/nc/xj/yn material, magic and tool aspects. Modern native frame/POI/dimension/network APIs are adapters. Three portal PNGs and animation metadata are copied unchanged from the supplied R195/vanilla1.6.4 archives. Original rights retained.

0.1.34 copies/adapts supplied R195 ChunkProviderUnderworld.populate, afp, mk, EntityLongdead/EntityLongdeadGuardian, tr/qn/qp, EntityAISeekFiringPosition, og/ye equipment and ka/nn/uh skeleton target prediction. Four custom records are excluded by the human music rule; quality stays disabled and new dungeon equipment uses fixed regional tension under58D. Three longdead PNGs are copied unchanged from the supplied original resource archive. Original rights retained.

0.1.35 adapts supplied R195 BlockRunestone/ItemRunestone, wd result-slot selection, aps portal seed/landing/ambient owners, and bej/bfl portal particle owners. Thirty-two runestone PNGs and one runegate ambient OGG are copied unchanged. The original runegate particle atlas/provider is reused unchanged. Original rights retained; no music/disc is added.

0.1.36 adapts supplied R195 ChunkPostField/ChunkPost, BiomeGenUnderworld.placeMycelium, afl default brown giant, aou texture/drop owner, acu.K/afw springs, adr/aee natural-mycelium flag and chunk persistence. New source-shaped models reference existing vanilla textures; no PNG is generated, edited or copied. Original rights retained.

0.1.37 adapts supplied R195 tr/og, EntityBoneLord/EntityAncientBoneLord, js.tryCreateNewLivingEntityCloseTo and aci spawn-surface rules. Lord body/eye textures are copied unchanged from the supplied pack. Existing archery, repair and source combat helpers retain prior evidence. Original rights retained.

0.1.38 adapts supplied R195 tr type1, tm melee frenzy, of skeleton eye geometry and EntityInvisibleStalker/RenderInvisibleStalker. Six Stalker sound files copied unchanged; existing Wight texture reused. Original rights retained.

0.1.39 adapts supplied R195 Cubic/Gelatinous classes, original chemical material arrays and block/item overrides, and sphere projectile behaviour. Six new original PNG files copied unchanged. Original rights retained. Unrequested crop disease was removed; historical source evidence remains in docs/history.

Official Crafter appearance for N017: Minecraft Java 26.3; original block models and PNG bytes copied unchanged from Mojang client e877b6a07acd633fb3bb475002175cec036e7b87. See docs/mithril-auto-049-official-assets.json for official manifest, source URL and per-file hashes. Mojang/Microsoft retain the original asset rights.

## Terraria Hallowed Bar visual direction (0.1.50)

Hallowed Bar sprite: © Re-Logic. Original PNG bytes were copied unchanged at the user's request. Reference: https://terraria.wiki.gg/wiki/Hallowed_Bar . The publicly readable sprite mirror was https://terraria.arcenserv.info/w/images/Hallowed_Bar.png ; provenance and SHA256 are in third_party/terraria-hallowed-source.json. New Holy Steel model colors are sampled from this sprite; original Minecraft/R195 tool, bucket and armor PNGs remain unchanged. No black Base Metals Starsteel candidates were imported.

## Bundled Just Enough Items (0.1.91)

JEI 15.74.0.230 for Minecraft 1.20.1 Forge, by mezz and contributors, is included unchanged using Forge Jar-in-Jar. Upstream: https://github.com/mezz/JustEnoughItems . License: MIT; its original notice remains inside the nested JAR at META-INF/LICENSE.txt. JEI's own nested MezzConfig dependency and its original notices remain intact. This distribution does not bundle the optional MezzConfig GUI module. The existing MITE recipe adapter remains separate from upstream JEI.

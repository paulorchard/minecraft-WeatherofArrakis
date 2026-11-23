# Notice

IslandCraft - Weather of Arrakis contains code and resources **copied from Immersive Storms** by **TheDeathlyCow**.

- Source: https://github.com/TheDeathlyCow/immersive-storms
- Branch / commit copied from: `26.1.x` / `66c883b5c1facba2219c9a31e1f93acfe2e333c9` ("bump version and add changelog"), mod version 2.0.0+26.1.x-beta.3, Minecraft 26.1.2
- Licence: GNU Lesser General Public License v3.0 or later (`LICENSE-LGPL-3.0.txt`, the plain GNU LGPL v3 text from the repository's LICENSE file). The upstream README says the mod may be copied, modified and redistributed under that licence.
- The rest of this mod keeps its own licence. Only the copied files below (and the copied resources) are LGPL-3.0-or-later.
- Upstream says the sandstorm code was extracted from the author's own mod Scorchful (LGPL-3.0). The upstream git history (180 commits, all by TheDeathlyCow) contains no commit that says code was taken from anywhere else.

Every copied Java file keeps its upstream content and gains one first line: `// Adapted for IslandCraft - Weather of Arrakis; see NOTICE.md` (upstream files have no licence header of their own).

## Licence inconsistencies found

1. Upstream `META-INF/neoforge.mods.toml` says `license = "MIT"`, but its LICENSE file is LGPL-3.0 (and its `fabric.mod.json` says `LGPL-3.0`). We follow the LICENSE file: LGPL-3.0-or-later.
2. Upstream `CREDITS` also names a "Trinkets Updated" MIT build script (irrelevant to us, not copied) and five Pixabay sound effects (next section).

## RELEASE BLOCKER: the sound files (Pixabay)

The wind and sandstorm `.ogg` files are copied so the owner can hear the real thing. Upstream's CREDITS credits them to Pixabay authors (storegraphic, Harisankar Sahoo, u_m9b3aw9s3j, Ghostie Graves, Jurij). Pixabay's licence allows use in a project but not redistributing the files on their own, and the repository gives no per-file terms. **Before this mod is published: keep them with the credits, replace them, or drop them.** Files: everything under `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/`.

The three `dust_grain` textures and the particle JSON have no credit line upstream, so they are covered by the repository's LGPL. The upstream history shows them added by the author himself (commit 1db4504, 2025-04-23, "dust grain particle in sandstorms"; the sounds in 5d08fb4, same day, "add sandstorm sounds"); neither commit message names another source.

## Edits made to the copied files (every one, by rule)

**Rule 1 - package rename.** `com.thedeathlycow.immersive.storms` to `com.paulorchard.islandcraft.weatherofarrakis.storms`, same sub-packages, in every file (mechanical find and replace). Class names are unchanged.

**Rule 2 - namespace rename.** `immersive-storms` and `immersive_storms` to `islandcraft_weatherofarrakis`, in every copied file and path: `ImmersiveStorms.MOD_ID` and `MOD_NAMESPACE`, all resource ids (particle, sound events, environment attributes, attribute types, payload, biome tags), the asset and data folders, `sounds.json`, `particles/*.json` texture ids, subtitle keys, the mixin config names (`islandcraft_weatherofarrakis.mixins.json`, `islandcraft_weatherofarrakis.client.mixins.json`), the service file names, the `@Share` namespace in WeatherEffectRendererMixin, and the method prefix `immersive_storms$` of the interface-injected methods (now `islandcraft_weatherofarrakis$`). Remaining upstream strings: only the author's Discord URL in a comment of AdvancedSettings.

**Rule 3 - dependencies we do not have (Yumi, YACL).** Shim classes (our own code, in `storms/shim/`, no upstream content) stand in for them; the copied files only had their import lines changed:
- `dev.yumi.commons.event.Event` / `EventManager` imports changed in: ImmersiveStorms, ISCommandRegistrationCallback, ISServerLifecycleEvents, ISClientTickEvents, ConfigSyncPacketPayload.
- `dev.yumi.mc.core.api.YumiMods` import changed in: ImmersiveStorms, ImmersiveStormsClient, Updater, ImmersiveStormsNeo, ServicesHelper.
- `dev.isxander.yacl3...` imports (ConfigClassHandler, SerialEntry, AutoGen, FloatSlider, IntSlider, TickBox, GsonConfigSerializerBuilder) changed in: AdvancedSettings, GeneralSettings, SandstormSettings, SettingsSchema, Translate. The settings keep their fields, getters and default values; `ConfigClassHandler` is an in-memory holder (load and save do nothing, no file is read or written), created on first use because the settings classes assign static defaults after their HANDLER field.
- Entrypoints: `ImmersiveStorms` no longer implements Yumi's `ModInitializer` (the `@Override` and the `ModContainer` parameter of `onInitialize` are removed); `ImmersiveStormsClient` the same for `ClientModInitializer`. Our `@Mod` class (WeatherOfArrakis) calls them. Because the built-in registries are already frozen at mod construction in this NeoForge, the common init and the construction of `ImmersiveStormsNeo` run inside the first `RegisterEvent` (upstream order: constructor first, then init; ours: init first, then constructor, because the constructor reaches the registered attributes).
- `ImmersiveStormsNeo` and `ImmersiveStormsNeoClientMod`: the `@Mod` annotations are removed (our one `@Mod` class constructs them); the client one also loses the config-screen extension point (its ISConfigScreen is not copied).
- `META-INF/services` files: only the `.neoforge.` lines are kept (Fabric classes are not copied). `ServicesHelper` is copied unchanged apart from the YumiMods import; its Fabric detection is false here.

**Rule 4 - Fabric-only API calls.** None were needed in the copied files.

**Rule 5/6 - other.**
- The two mixin configs gain one line `"plugin": ".../storms/StormsMixinPlugin"` (our own class) so that no copied mixin is applied when the real Immersive Storms is on the classpath (otherwise our mixins touch attributes we did not register and the game crashes at load).
- Upstream's `inject-interface` lines (Fabric Loom classtweaker) become `META-INF/interfaceinjection.json` plus `interfaceInjectionData` in `build.gradle`, which only makes the interface methods visible at compile time; the mixins still implement the interface at run time.
- The three `accessible class` lines of upstream's classtweaker are appended to our `META-INF/accesstransformer.cfg`.
- `build.gradle`: `compileOnly maven.modrinth:DistantHorizonsApi:7.0.0` (build-only, as upstream; StormFogModifier compiles against it and only uses it when Distant Horizons is loaded).
- **Sandstorm in every biome (owner request after step-6, deliberate deviation):** `ISBiomeTags` gains `ALL_BIOMES`; `UnboundRegistryObjectStream.Tag.listHolders` resolves that pseudo tag to every biome; `AdvancedSettings.sandstormBiomes()` adds it. The upstream tag list stays, so the red and pink colour entries still apply. `/sandstorm start` has no dimension check.
- Two subtitle keys are added to our `lang/en_us.json`.

## Left out of the copy

Fabric support (`src/fabric`), ModMenu, datagen (the generated biome tag JSON is copied instead), the YACL config screen and controllers (`client/config/*`), the compat mixins and their plugin (Particle Rain, Subtle Effects), the icon. Kept even though not on the sandstorm path, because shared files are copied whole: black rain (BlackRainEffect, BlackWaterDropParticle, the WeatherEffectRenderer and WeatherRenderState mixins), blizzard and dense-fog archetypes, ambient wind (BiomeWindEffects), NSBiomes, the config updater and schema files, the `/islandcraft_weatherofarrakis reload` command.

## New code (not copied)

`storms/shim/*` (Yumi and YACL stand-ins), `storms/StormsClientBoot`, `storms/StormsMixinPlugin`, the `/sandstorm start [minutes] [thunder]` and `/sandstorm stop` commands (vanilla rain trigger), and the debug tools in `client/`.

## Copied files

- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/ImmersiveStormsClient.java`  (upstream: `client/ImmersiveStormsClient.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/mixin/client/AtmosphericFogEnvironmentMixin.java`  (upstream: `client/mixin/client/AtmosphericFogEnvironmentMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/mixin/client/ClientLevelMixin.java`  (upstream: `client/mixin/client/ClientLevelMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/mixin/client/ClientPacketListenerMixin.java`  (upstream: `client/mixin/client/ClientPacketListenerMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/mixin/client/WeatherEffectRendererMixin.java`  (upstream: `client/mixin/client/WeatherEffectRendererMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/mixin/client/WeatherRenderStateMixin.java`  (upstream: `client/mixin/client/WeatherRenderStateMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/particle/BlackWaterDropParticle.java`  (upstream: `client/particle/BlackWaterDropParticle.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/particle/ClientParticleHelper.java`  (upstream: `client/particle/ClientParticleHelper.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/particle/DustGrainParticle.java`  (upstream: `client/particle/DustGrainParticle.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/util/ClientServicesHelper.java`  (upstream: `client/util/ClientServicesHelper.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/util/ISClientTickEvents.java`  (upstream: `client/util/ISClientTickEvents.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/BiomeWindEffects.java`  (upstream: `client/world/BiomeWindEffects.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/BlackRainEffect.java`  (upstream: `client/world/BlackRainEffect.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/SandstormParticles.java`  (upstream: `client/world/SandstormParticles.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/SandstormSounds.java`  (upstream: `client/world/SandstormSounds.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/StormFogModifier.java`  (upstream: `client/world/StormFogModifier.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/WeatherArchetype.java`  (upstream: `client/world/WeatherArchetype.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/WeatherBasedAttributeInterpolator.java`  (upstream: `client/world/WeatherBasedAttributeInterpolator.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/client/world/WeatherRenderStateExtension.java`  (upstream: `client/world/WeatherRenderStateExtension.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/compat/NSBiomes.java`  (upstream: `compat/NSBiomes.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/data/CodecGsonAdapter.java`  (upstream: `config/data/CodecGsonAdapter.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/data/IdentifierGsonAdapter.java`  (upstream: `config/data/IdentifierGsonAdapter.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/data/UnboundRegistryObjectStream.java`  (upstream: `config/data/UnboundRegistryObjectStream.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/data/WeatherBasedAttributes.java`  (upstream: `config/data/WeatherBasedAttributes.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/data/WeatherCondition.java`  (upstream: `config/data/WeatherCondition.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/schema/ConfigUpdater.java`  (upstream: `config/schema/ConfigUpdater.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/schema/JsonCopyHelper.java`  (upstream: `config/schema/JsonCopyHelper.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/schema/SchemaV2.java`  (upstream: `config/schema/SchemaV2.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/schema/SchemaV3.java`  (upstream: `config/schema/SchemaV3.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/section/AdvancedSettings.java`  (upstream: `config/section/AdvancedSettings.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/section/GeneralSettings.java`  (upstream: `config/section/GeneralSettings.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/section/SandstormSettings.java`  (upstream: `config/section/SandstormSettings.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/section/SettingsSchema.java`  (upstream: `config/section/SettingsSchema.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/Translate.java`  (upstream: `config/Translate.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/config/Updater.java`  (upstream: `config/Updater.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/ImmersiveStorms.java`  (upstream: `ImmersiveStorms.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/mixin/EnvironmentAttributeProbeMixin.java`  (upstream: `mixin/EnvironmentAttributeProbeMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/mixin/EnvironmentAttributeSystemMixin.java`  (upstream: `mixin/EnvironmentAttributeSystemMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/mixin/LevelMixin.java`  (upstream: `mixin/LevelMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/mixin/MinecraftServerMixin.java`  (upstream: `mixin/MinecraftServerMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/mixin/ServerLevelMixin.java`  (upstream: `mixin/ServerLevelMixin.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/network/ConfigSyncPacketPayload.java`  (upstream: `network/ConfigSyncPacketPayload.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/particle/DustGrainParticleEffect.java`  (upstream: `particle/DustGrainParticleEffect.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/particle/ParticleHelper.java`  (upstream: `particle/ParticleHelper.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/ISAttributeTypes.java`  (upstream: `registry/ISAttributeTypes.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/ISEnvironmentAttributes.java`  (upstream: `registry/ISEnvironmentAttributes.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/ISParticleTypes.java`  (upstream: `registry/ISParticleTypes.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/ISSoundEvents.java`  (upstream: `registry/ISSoundEvents.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/tag/CommonBiomeTags.java`  (upstream: `registry/tag/CommonBiomeTags.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/registry/tag/ISBiomeTags.java`  (upstream: `registry/tag/ISBiomeTags.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/sound/StormSounds.java`  (upstream: `sound/StormSounds.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/util/ISCommandRegistrationCallback.java`  (upstream: `util/ISCommandRegistrationCallback.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/util/ISMath.java`  (upstream: `util/ISMath.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/util/ISServerLifecycleEvents.java`  (upstream: `util/ISServerLifecycleEvents.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/util/ServicesHelper.java`  (upstream: `util/ServicesHelper.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/world/AttributeMapHolder.java`  (upstream: `world/AttributeMapHolder.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/world/Biome2WeatherAttributeMapImpl.java`  (upstream: `world/Biome2WeatherAttributeMapImpl.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/world/Biome2WeatherAttributeMapView.java`  (upstream: `world/Biome2WeatherAttributeMapView.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/neoforge/client/ClientParticleHelperImpl.java`  (upstream: `neoforge/client/ClientParticleHelperImpl.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/neoforge/client/ImmersiveStormsNeoClientMod.java`  (upstream: `neoforge/client/ImmersiveStormsNeoClientMod.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/neoforge/ImmersiveStormsNeo.java`  (upstream: `neoforge/ImmersiveStormsNeo.java`)
- `src/main/java/com/paulorchard/islandcraft/weatherofarrakis/storms/neoforge/ParticleHelperImpl.java`  (upstream: `neoforge/ParticleHelperImpl.java`)

Resources:

- `src/main/resources/assets/islandcraft_weatherofarrakis/particles/black_rain.json`
- `src/main/resources/assets/islandcraft_weatherofarrakis/particles/dust_grain.json`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/sandstorm/soft1.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/sandstorm/soft2.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft1.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft2.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft3.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft4.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft5.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/soft6.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/strong1.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/strong2.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather/wind/strong3.ogg`
- `src/main/resources/assets/islandcraft_weatherofarrakis/sounds.json`
- `src/main/resources/assets/islandcraft_weatherofarrakis/textures/particle/dust_grain/var0.png`
- `src/main/resources/assets/islandcraft_weatherofarrakis/textures/particle/dust_grain/var1.png`
- `src/main/resources/assets/islandcraft_weatherofarrakis/textures/particle/dust_grain/var2.png`
- `src/main/resources/data/islandcraft_weatherofarrakis/tags/worldgen/biome/has_blizzards.json`
- `src/main/resources/data/islandcraft_weatherofarrakis/tags/worldgen/biome/has_dense_fog.json`
- `src/main/resources/data/islandcraft_weatherofarrakis/tags/worldgen/biome/has_sandstorms.json`
- `src/main/resources/data/islandcraft_weatherofarrakis/tags/worldgen/biome/is_black_rain_affected.json`
- `src/main/resources/data/islandcraft_weatherofarrakis/tags/worldgen/biome/is_windy.json`
- `src/main/resources/islandcraft_weatherofarrakis.mixins.json`
- `src/main/resources/islandcraft_weatherofarrakis.client.mixins.json`
- `src/main/resources/META-INF/services/com.paulorchard.islandcraft.weatherofarrakis.storms.client.particle.ClientParticleHelper`
- `src/main/resources/META-INF/services/com.paulorchard.islandcraft.weatherofarrakis.storms.particle.ParticleHelper`

Also copied and edited (rules above): `META-INF/accesstransformer.cfg` lines, `lang/en_us.json` subtitle keys, both mixin config files (listed above).

## Replacing the adapted code with a newer upstream copy

The copied code is the package `com.paulorchard.islandcraft.weatherofarrakis.storms` (minus `shim`, `StormsClientBoot`, `StormsMixinPlugin`), the resources listed above, and the two mixin configs. To update or modify it: copy the new upstream `src/main` and `src/neoforge` Java and resources over those paths, repeat the package and namespace renames (rules 1 and 2), re-apply the small edits listed under rule 3 (imports, entrypoints, `@Mod` removal), keep the plugin line in the mixin configs, and rebuild with `./gradlew build`. The rest of the mod calls only `ImmersiveStorms.onInitialize()`, `ImmersiveStormsNeo`, `ImmersiveStormsNeoClientMod`, `ImmersiveStormsClient.onInitializeClient()` and reads a few registries in the debug log, so it is unaffected.

## Modifications after prompt 03 (prompt 04 on; each is a small block marked `IslandCraft change` in the file)

Prompt 03's "verbatim" rule ended with prompt 04. The upstream code at commit 66c883b is otherwise as copied.

| File | Change | Reason |
|---|---|---|
| `storms/config/section/SandstormSettings.java` | `getSandstormParticleDensityMultiplier()` and `dustParticleSize()` read `StormsClientConfig` | the settings are now live and editable (weatherofarrakis-storms-client.toml, /storm_tune) |
| `storms/client/world/SandstormParticles.java` | the velocity passed to `addParticle` is `PARTICLE_VELOCITY * StormSpeed.factor(level)` instead of the constant | speed is a setting scaled by rain and thunder strength |
| `storms/client/particle/DustGrainParticle.java` | friction and lifetime come from the config; a `tick()` override reports measured grain speed to `StormSpeed` | settings, and the measurement for the debug log |

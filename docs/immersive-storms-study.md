# Immersive Storms: what its source does (read 2026-11-22)

Source: `src-ref/immersive-storms` (outside this repo, not committed), branch `26.1.x`, commit `66c883b`. LGPL-3.0-or-later (LICENSE read: plain GNU LGPL v3; the README repeats it).
Note: its `neoforge.mods.toml` says `license = "MIT"` while the LICENSE file is LGPL; we follow the LICENSE file.

## Is a storm active, how strong, how it blends
- It reads vanilla rain: `level.isRaining()` (particles), `level.getRainLevel(1f) < 0.7` (strong wind sound), and the rain gradient through the 26.1 environment-attribute system: its own attributes `WEATHER_ARCHETYPE`, `STORM_FOG_END_DISTANCE`, `HAS_SANDSTORM_DUST`, `STORM_SOUNDS` (`registry/ISEnvironmentAttributes.java`), interpolated by `WeatherBasedAttributeInterpolator` and fed from biome-tag data (`#immersive-storms:has_sandstorms` etc.). Blending is whatever vanilla rain does (it is vanilla's own gradient).
- Five common mixins in `immersive-storms.mixins.json` (`EnvironmentAttributeProbeMixin`, `EnvironmentAttributeSystemMixin`, `LevelMixin`, `MinecraftServerMixin`, `ServerLevelMixin`) wire the attributes in on both sides (the mod also has server parts: rain length etc.).

## Fog
- `client/mixin/client/AtmosphericFogEnvironmentMixin` (`@WrapMethod setupFog`): after vanilla's atmospheric fog, if `rainFogMultiplier > 0` it calls `StormFogModifier.applyStormFogDistance`. That multiplies `fog.environmentalEnd`, `skyEnd` and `cloudEnd` by `STORM_FOG_END_DISTANCE x config multiplier` when it is below 1. It does not touch the render-distance fog fields, and only the atmospheric environment is changed (water, lava, powder snow, darkness use their own environments, so they are untouched).
- No colour change in this file; fog colour comes from the sky/fog colour attributes. No density value (26.1 fog is linear start/end).
- Indoors/underground: the attribute is evaluated by position (`camera.attributeProbe()`), biome based; it does not read sky light. Underground the vanilla rain fog multiplier is already 0, which is what gates it.
- Shader packs: author says the fog likely does not work with most shader packs. We confirmed that (Iris + Photon, see report 02).

## Particles
- `client/world/SandstormParticles`: each client tick while raining, for every column in a `renderDistance` square around the camera (shifted `renderDistance/2` along x so the area upwind is not empty): chance `1/60 x densityMultiplier`; y = camera y + random in [-r/2, r/2] but not below the motion-blocking height; position random inside the block; velocity (-1, 0, 0), i.e. along x only (no wind direction); requires `getPrecipitationAt == NONE` and `HAS_SANDSTORM_DUST`. Particle `DustGrainParticleEffect` (colour + size 10 x config), `DustGrainParticle extends DustParticleBase` with gravity 0.1, textures `dust_grain/var0..2`.
- `client/world/BiomeWindEffects`: ambient wind particles (3 attempts per tick, size 4) and soft sounds, separate from the storm.
- Shader note: particles and sounds work with shaders; only fog does not.
- Counts: with the default render distance and 1/60 chance, an order of 100 to 1000 dust per second; ours measured 700 to 800/s at radius 24.

## Sound
- `client/world/SandstormSounds`: only when `getRainLevel >= 0.7`; every few ticks plays a one-shot `weather.strong_wind` (or soft) at a random spot within +-10 blocks of the camera, `SoundSource.WEATHER`, volume 0.5 pitch 1 (or 0.1 pitch 0.5 when the sound is above and the camera is under cover). Sounds are chosen by the `STORM_SOUNDS` attribute.
- Custom files: `assets/immersive-storms/sounds/weather/{wind,sandstorm}/*.ogg` (11 files). `CREDITS` names five Pixabay authors. Pixabay's content licence is not a free licence, and the repository gives no per-file terms, so redistribution is NOT confirmed. We do not copy them.

## Sky, light, overlay
- No screen overlay, no sky or light technique of its own (sky tint comes through vanilla attributes). So the overlay is our invention (prompt 01) and the original does not support it.

## Mixins and compat
- Client: `AtmosphericFogEnvironmentMixin`, `ClientLevelMixin`, `ClientPacketListenerMixin`, `WeatherEffectRendererMixin`, `WeatherRenderStateMixin`. Compat (plugin-gated): Subtle Effects `WeatherEffectRendererMixin`, Particle Rain `TintTypeMixin`. Config in JSON through YACL, with an advanced settings file. Distant Horizons fog hook.

## Why its look works where prompt 01's did not
Part A (see report 02): prompt 01's code did fire in the dev client without shaders (payload, fog and particle counters all non-zero). What it lacked is exactly what the original does: the sand was a vanilla block particle of about 0.1 block (tiny), and every effect except particles was fog or tint, which shaders ignore. The original's particle is a large flat dust grain, dense around the whole camera, which carries the look with or without fog.

## Decision list
- **Port as is:** the grid spawn algorithm and constants (1/60 chance, upwind shift, clamp to terrain height); the fog rule (end, sky end and cloud end shortened together, only atmospheric).
- **Adapt:** input is our server intensity and wind vector, not rain; vanilla `DustParticleOptions` (scale capped at 4) instead of a custom particle, so no asset is needed; fog through `ViewportEvent.RenderFog`, no mixin; config values on the client; outdoor factor from sky light.
- **Drop:** environment attributes and biome tags (our server decides), all mixins, the YACL config UI, the black-rain and ambient-wind parts, and all of its sounds and textures (licence not confirmed). Sound stays our vanilla loop.

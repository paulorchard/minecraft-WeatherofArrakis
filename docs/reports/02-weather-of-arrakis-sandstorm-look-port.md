# Report — Prompt 02 Weather of Arrakis: fix the sandstorm look by adapting Immersive Storms
Status: partly (diagnosed, ported, proven by logs and screenshots in the dev client with and without Sodium/Iris + Photon; the cause of the owner's "nothing showed" without shaders could not be reproduced)   Commit / tag: step-5 4e27a0d

## Diagnosis (Part A)
Run: dev client (`runClient`) against the dev server, desert superflat, `/sandstorm start 2`. Counters were added (`ClientDebug`), logged once a second as `[storm-debug]`, plus `/storm_debug` and `/storm_debug shot`.

What the logs of the OLD code (prompt 01) showed with no shaders, Sodium loaded:
- `payload received: kind=1 stage=0 intensity=1.0 wind=(1.0,0.0) blend=30.0s` arrived (payload registered, sent and received; dimension is in `dimensions`).
- `renderFog=119 (applied 119, end=55.0) fogColor=119 (applied 119) particles=125/s overlay drawn=119 (alpha 0.222) wind playing=true vol=0.55 outdoor=1.0`. So listeners were on the right bus and side, the outdoor factor was 1, nothing was cancelled, the overlay layer was drawn at alpha 0.22, and the wind instance played. The fog and tint were visible in a screenshot (`run/screenshots/plain.png`, orange-brown wall at 55 blocks) — the effects did work in this setup.
- Particles were the weak part: 125/s of tiny vanilla BLOCK particles (about 0.1 block), which read as "a few grey cubes" in the screenshot.
- With Iris + Photon (`iris.png`): the same counters (`fogEvents 119 applied 119`) but the picture shows a clear blue sky and no fog at all. The shader pack ignores the fog values. The overlay is a GUI layer and was hidden in my first screenshots (GUI hidden), so it is unjudged.
- Not reproduced: a total failure without shaders. Possible reasons for the owner's observation (not provable here): the owner's runs were with Iris enabled in the instance (iris.properties there says `enableShaders=true`, `Solas-Shader-V3.9.zip`, i.e. shaders on by default); the effect is weak at low intensity or at night; the instance jar was older. Treat "fog ignored under shaders" and "particles too small to carry the look" as the two confirmed causes.

## What was built
- `/storm_debug` (client command), `/storm_debug shot` (screenshot after 2 s, GUI visible), the once-a-second log, `-Pautoshot=<name>`, `-Pquickplay=localhost`, `-Psodium`, `-Piris` dev run options.
- `docs/immersive-storms-study.md` (Part B).
- Adapted package `client/immersive/`: `SandstormParticles`, `SandstormFog` (LGPL headers) and our own `StormLook` (the interface `StormLook.set(intensity, windX, windZ, blendSeconds)`) and `ShaderDetect` (Iris by reflection).
- `ClientWeather.apply` now sends sandstorm payloads to `StormLook` and keeps the old look CLEAR for them. The Coriolis stages still use the old fog/overlay/particle/wind code, untouched.
- Client config: `sandstorm_fog`, `_fog_start` (4), `_fog_end` (55), `_fog_colour` (9E5720), `_particle_scale` (1.0), `_particle_radius` (24), `sandstorm_particles_boost_with_shaders` (on, 2x), `_wind_volume` (1.0), `sandstorm_overlay` (0), `yield_to_immersive_storms` (on), `sandstorm_biome_filter` (off).
- `NOTICE.md`, `LICENSE-LGPL-3.0.txt`, README credit, `credits` in `neoforge.mods.toml`.

## Confirmed
- Read from source: how Immersive Storms decides, fogs, spawns particles and plays sound (study).
- Tested in the dev client (logs + screenshots in `docs/screenshots/`): payload received; fog event applied every frame; particles 700 to 800 per second at full intensity; wind loop playing at volume 0.55; plain-Sodium screenshot shows a dense orange sandstorm with large dust grains and fog at 55 blocks; with Iris + Photon the fog is absent but the grains are visible.
- Ramp in the log, 0 to 1 over 30 s: fog end 983, 865, 697, 506, 319, 165, 71, 55 (every 4 s), particles 32, 118, 211, 396, 532, 691, 714, 810 per second, intensity 0.04 to 1.0. Ramp out: intensity 0.999, 0.94, 0.80, 0.62, 0.43, 0.24, 0.09, 0.007.
- Underground: the outdoor factor goes 1.0 to 9e-4 within 4 s of a teleport into rock, particles 0/s, fog not applied; back above ground it returns to 0.999 within 4 s. Wind stays at the 12 percent indoor floor.
- Dedicated server starts with the jar every run (server logs clean), `./gradlew test` passes, build clean.
- Every adapted file has the LGPL header; `NOTICE.md` lists them.

## Inferred
- Whether the tint overlay works (default 0, never judged: the GUI was hidden in the first captures and the Coriolis stage 1 alpha is below the draw threshold).
- Sound levels and the look of the wind loop (not heard).
- Other shader packs (Solas, Complementary, miniature) behave like Photon: not tested.
- The look at night and at dawn/dusk, the look of the dust at scale 4 up close.

## Answers to the prompt's questions
- **How Immersive Storms works:** see the study. Fog: one mixin on `AtmosphericFogEnvironment.setupFog` scales fog end, sky end and cloud end. Particles: a grid scan of 1/60 chance per column of a custom large dust grain, x-only velocity, y clamped above terrain. Sound: a one-shot wind sound at a random spot near the camera each few ticks, from 11 Pixabay-credited .ogg files.
- **Ported / adapted / dropped:** ported the grid scan, the fog rule; adapted the inputs (our intensity and wind), the particle type (vanilla `DustParticleOptions`, scale cap 4, no asset), fog via `ViewportEvent.RenderFog`; dropped mixins, attributes, biome tags, YACL UI, all sounds and textures.
- **Mixins added:** none. The Forge-event fog hook works (proven), and the vanilla dust particle needs no registration. Nothing to fail soft.
- **Asset licences:** code is LGPL-3.0-or-later. Sounds are credited to five Pixabay authors; Pixabay's licence is not a free redistribution licence and the repository gives no per-file terms, so NOT used. Textures (`dust_grain/var0..2`) and particle JSON have no stated licence, NOT used. The repository's mods.toml says MIT while LICENSE is LGPL: noted, we follow LICENSE.
- **Sound:** our own vanilla loop (`ambient.basalt_deltas.loop`), volume 0.55 x intensity x `sandstorm_wind_volume`, 12 percent floor indoors. Missing relative to the original: the random one-shot gusts and the "above" muffled variant. No sound approval needed because nothing non-vanilla ships.
- **Outdoor factor:** kept prompt 01's technique: sky light at the camera, `clamp((sky-3)/10)`, smoothed at 2.5 per second. Result confirmed above (full outdoors, ~0 in rock, 4 s transition). The original has no better technique (it is biome-based).
- **Shaders:** `ShaderDetect` finds Iris by reflection; with a pack active the particle density doubles and the dust colour is lightened 1.9x (shader lighting made it near black). The one-time notice ("fog may not show with shaders") was NOT added: it is not done. The log line `shaders=true` shows detection.
- **Old overlay and fog for the Coriolis stages:** the code runs and fires (Coriolis approach run: `overlay drawn` counted, fog applied 118/119 frames), so it is not dead without shaders, but its tint is below the draw threshold in stage 1 and it is shader-blind like all fog. It is untuned. Prompt 03 should drive the Coriolis stages through `StormLook` plus the particles, and treat fog and overlay as no-shader extras.
- **Coexistence:** `yield_to_immersive_storms` (default on) reads `ModList.isLoaded("immersive_storms")`. NOT tested: no NeoForge build of Immersive Storms was available to put beside ours. Particle Rain: not handled.
- **Not allowed / worked around:** fog under shaders (cannot fix from a NeoForge event; particles carry it); vanilla dust scale capped at 4 (a custom particle with its own texture would allow 10 but needs an asset we may not copy; we could draw our own texture later); `sandstorm_biome_filter` uses vanilla biome tags, not the original's tag files.

## Numbers
- Particles at full intensity: about 750 per second (radius 24, density 1.0), 1196 per second at intensity 0.5 with shaders (density x2). Scan cost: (2 x 24)^2 = 2304 random draws per tick plus heightmap lookups for the hits only.
- Fog: end 55 blocks, start 4 at full intensity; sky and cloud end follow.
- Ramp 30 s, outdoor smoothing about 4 s.

## Surprises, problems, deviations from the prompt
- The old look did work without shaders in my dev client; the owner's failure is not reproduced (see diagnosis).
- A test world quirk: `time set noon` jumped the schedule clock and started a Coriolis approach, which blocks `sandstorm start`; the first ramp run showed the Coriolis stages instead. Re-run in a fresh world.
- Python was not on this machine; patches used node.
- The Immersive Storms jar was not installed beside ours; the yield is untested.
- Overlay default is 0, not deleted (a config knob for the owner).
- The one-time shader action-bar notice was not built.

## How the owner can see it
Seed: any. Use a vanilla desert or superflat world, overworld. Jar is deployed in `ArrakisTest01/mods`. Look list:
1. `/sandstorm start 2`, no shaders: sandy-orange fog closing from about 400 to 55 blocks over 30 s, large grains of sand blowing from the west, wind sound; fades out again over 30 s. Step into a cave or a closed room: grains and fog gone within about 4 s.
2. Same with a shader pack: grains and sound present; fog will probably not show.
3. `/storm_debug` while it runs: payload, outdoor, fog end, particles per second, wind playing.
4. `/coriolis start 60 30`: runs as in prompt 01 (not retuned); say whether it shows anything.
5. Config (client): `sandstorm_overlay` 0.2 turns on a brown tint; `sandstorm_particle_scale` for density.

Screenshots: `docs/screenshots/sandstorm-plain.png` and `sandstorm-iris-photon.png`.

## Suggested next steps (your view)
- Judge the grain size and density first (they look heavy in the screenshots; `sandstorm_particle_scale` 0.5 to 0.7 may be nicer), then the overlay and the wind sound.
- If you want bigger grains than vanilla's 4: draw our own small texture and register a particle (no licence issue).
- Prompt 03: drive the Coriolis stages through `StormLook`, with grains as the main signal.
- Install a NeoForge build of Immersive Storms if one exists and test the yield.

## Owner observations (human)

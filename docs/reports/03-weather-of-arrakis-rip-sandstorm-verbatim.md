# Report — Prompt 03 Weather of Arrakis: rip Immersive Storms' sandstorm verbatim
Status: done (copy runs and matches upstream's own jar in the dev client, no shaders; shader and real-player feel need the owner)   Commit / tag: step-6 HASH

## What was built
- Copy of Immersive Storms (TheDeathlyCow, LGPL-3.0-or-later, branch 26.1.x, commit 66c883b5c1facba2219c9a31e1f93acfe2e333c9) into `com.paulorchard.islandcraft.weatherofarrakis.storms`: 74 Java files (62 copied, 12 of ours: shim, boot, plugin), 5 shared + 5 client mixins, resources (sounds, particles, textures, biome tags, mixin configs, services). Full list and every edit by rule: `NOTICE.md`.
- Our new code only: `storms/shim/*` (Yumi/YACL stand-ins, in-memory settings), `StormsClientBoot`, `StormsMixinPlugin`, the trigger, debug log.
- Trigger: `/sandstorm start [minutes] [thunder]` (default 1, op 2, refuses outside configured dimensions), `/sandstorm stop`. Sets vanilla weather via `MinecraftServer.setWeatherParameters(0, minutes*1200, true, thunder)`; stop sets rain off and a vanilla clear delay.
- Removed prompt 02's `client/immersive` and the old sandstorm fog, overlay, particles, wind and payload kind (tag `immersive-port-v1` before removal). `sandstorm_enabled` default false.
- Kept `/storm_debug`, `/storm_debug shot`, once-a-second log (now with rain gradient, archetype, fog end after the mixin, particle count, weather sounds played), dev run options (plus `-Prefrun`, which leaves our mixin configs out so upstream's jar can run alone).
- If the real mod is loaded: our copy registers nothing, logs one line, and a mixin plugin applies none of our mixins.

## Confirmed
- Clean build, `./gradlew test` pass; dedicated server starts with the jar (about 0.2 s, no client classes), including with thunder.
- Dev client, no shaders, superflat desert, `/sandstorm start`: log archetype=SANDSTORM, rain 0 to 1 in about 6 s, fog end 967 to 112 (thunder 56), about 1600-1700 particles, weather sounds played (about 7 per second); `/sandstorm stop` brings fog back and rain down.
- Reference comparison: upstream's own NeoForge jar (built from `src-ref`, with YACL and its own Yumi) in the same dev client and world with `/sandstorm start` (vanilla rain): the screenshot is visually the same as ours (amber wall at the horizon, brown grains, haze). Files: `docs/screenshots/reference-upstream-rain.png` vs `rip-plain.png`. Grain positions differ (random), nothing else I can see.
- Thunder (`rip-thunder.png`): much darker brown, fog about 56 blocks.
- Iris + Photon (`rip-iris-photon.png`): bright sand-coloured sky, many grains, no fog wall (the shader ignores fog), HUD visible.
- Yield test: upstream's jar beside ours (server): our copy logs one line, no crash, mixins not applied.
- No `immersive_storms`/`immersive-storms` strings remain except the author's Discord URL, credits/licence text, and our own ModList/classpath checks for the real mod.

## Inferred
- Sound quality and volume (counted, not heard). Look in the owner's actual instance and shader pack (Solas etc.). Fading in a house or cave (code path is upstream's biome-based logic; not sampled indoors this time).
- Whether the real mod and ours behave under a physical client together (only the server side of that was run; client run with the real jar alone was the reference).

## Answers to the prompt's questions
- **Shared mixins needed:** the environment-attribute ones (`EnvironmentAttributeProbeMixin`, `EnvironmentAttributeSystemMixin`, `LevelMixin`, `MinecraftServerMixin`, `ServerLevelMixin`, `ClientLevelMixin`, `ClientPacketListenerMixin`) give each level the per-biome weather attribute map, synced on login; `AtmosphericFogEnvironmentMixin` rewrites the fog distance; the two weather-renderer mixins handle black rain. Prompt 02's mixin-free version only used a Forge fog event with our own intensity, so it had no biome attributes, the 128/1024 light and 64/1024 thick fog rule, the fog-colour multiply, the particle volume or the sounds; hence the different look.
- **Upstream origin:** README and history say the sandstorm code came from the author's own Scorchful (LGPL-3.0); all 180 commits are the author's. Textures: commit 1db4504 (author, 2025-04-23); sounds: 5d08fb4 (same day), credited to Pixabay authors in CREDITS. Other licences: mods.toml says MIT while LICENSE is LGPL; a MIT build script is credited.
- **What the toggle sets:** clearTime 0, rainTime and thunderTime minutes*1200, raining true, thundering per flag. Weather packets reach the client (gradient reached 1.0 on the client). The timers count down 1 per tick so they do not end it early.
- **What can end it early:** sleeping (vanilla resets the weather cycle), the `advance_weather` gamerule false (freezes the timers instead, so it would not end), `/weather clear`. Also: a Coriolis approach blocks the trigger in my first test world; I disabled the Coriolis scheduler in `run-server` config for the dev tests.
- **Still running during a Coriolis storm:** the state machine, gameplay (damage, shelter), chat warnings, and the old staged fog/overlay/particle/wind code in `WeatherClientEvents`, untuned. The sandstorm kind of the staged look is now CLEAR.
- **Real mod beside ours:** NeoForge build exists only because I built it from source (not downloaded); tested as above.

## Numbers
- Build: upstream reference jar about 1.5 min; ours about 5 s incremental.
- Fog end (client log): rain 112 blocks, thunder 56 blocks; start 967 or similar at 0 intensity.
- Particles about 1650 in the engine; weather sounds about 7 per second.

## Surprises, problems, deviations from the prompt
- Registries are frozen at mod construction in this NeoForge, so the init runs in the first `RegisterEvent` (the constructor of `ImmersiveStormsNeo` after init, not before).
- The shim's config holder had to be lazy (a null list crashed the config sync on login).
- New compileOnly dependency: Distant Horizons API (Modrinth maven), needed to compile `StormFogModifier`; not in the jar.
- Interface injection (`interfaceinjection.json` + `interfaceInjectionData`) was needed for compile only.
- Our first client run showed a Coriolis approach in the thunder screenshot (scheduler fired); I disabled it in the dev server config and re-ran.
- The ref run first crashed our debug log (it touched registries the real mod owns); the debug log now checks.
- Several dev client JVMs from earlier runs stayed alive and locked `run/mods`; I stopped only those.
- `NOTICE.md` rule 5 list of left-out features is in the file.
- **Release blocker:** the sound files (Pixabay terms unconfirmed).

## How the owner can see it
Seed: any, superflat desert is easiest (`run-server/server.properties` has it). Jar is in `ArrakisTest01/mods`.
You look:
1. No shaders. `/sandstorm start 1`: does it look like the CurseForge page (dense orange fog, large dust grains, wind sound)? Go into a house or cave: does it fade?
2. `/sandstorm start 1 thunder`: darker and thicker?
3. Same two with your shader pack. Fog will probably not show; grains and sound should.
4. `/sandstorm stop`: clears.
5. `/storm_debug` and `/storm_debug shot` for the numbers and a screenshot.

## Suggested next steps (your view)
- Decide on the sounds (replace or confirm Pixabay terms) before any release.
- Drive the Coriolis storm from the same copied attributes (fog, particles, sound) instead of the old staged code.
- Hook the sandstorm to the weather schedule once the look is approved.

## Things I would change (not changed)
- The settings are in-memory only: the upstream config files are not read, so players cannot tune it.
- The copied mixins also apply black rain, blizzards, dense fog and ambient wind in other biomes.
- Hard dependency on a Distant Horizons API jar to compile.
- The `immersive_storms$` method prefix is renamed, so a newer upstream copy needs the same rename.
- Upstream oddities: log text "Thermoo Neoforged @Mod class initialized"; Cloth-era updater code; the fog-colour multiply naming.
- The Coriolis staged look is still the old, untuned code.

## Owner observations (human)

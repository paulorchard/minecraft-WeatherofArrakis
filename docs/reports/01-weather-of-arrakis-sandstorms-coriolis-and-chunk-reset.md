# Report — Prompt 01 Weather of Arrakis: sandstorms, the Coriolis storm and the chunk reset
Status: partly (everything is built; the server side is tested headless, every client-side part and every player-dependent step is untested)   Commit / tag: step-1 cd59816, step-2 4384388, step-3 b68b810, step-4 99dad43

## What was built
- **Project:** NeoForge 26.1.2.129 / Java 25, mod id `islandcraft_weatherofarrakis`, jar `IslandCraft-WeatherOfArrakis-0.1.0.jar`. One jar; client code is only reached from `Dist.CLIENT` subscribers and payload lambdas. Server config with every key and comment; client config for shake.
- **State and sync:** per-dimension weather state and the chunk records in one `SavedData` (overworld storage). One server-to-client payload for the effective weather, one for camera shakes. `StormQuery` (phase, seconds remaining, sandstorm intensity, isExposed, phase listeners).
- **Cycle:** CALM, APPROACH (6 staged looks, chat warnings at 5:00/4:00/3:00/2:00/1:00/0:30 with the real time left), STORM, CLEARING; next storm a uniform 3 to 6 game days later as a clock instant (sleeping brings it closer). Commands `coriolis status/start/stop/strike`, `sandstorm start/stop`.
- **Sandstorms:** random schedule, ramp, one weather at a time; look = fog colour and distance, tint/dim overlay, blowing sand particles, looping wind. No gameplay effect.
- **Exposure (Part 5):** pure `ShelterRules` (unit-tested), per-second gear loss and health loss, data-driven `arrakis_coriolis_storm` damage type with tags and death message, bone marker.
- **Major storm (Part 6):** `coriolis major <minutes> [stormSeconds [resetWindowSeconds]]` / `cancel`, kick and login refusal, altered-chunk records with ageing, unload-and-delete reset, `coriolis chunks / reset all / reset here / mark`, login rescue.
- **Claims (Part 7):** `ChunkProtection` interface, registry, FTB Chunks and Open Parties and Claims adapters by reflection, fail-closed.
- **Part 4:** visual-only vanilla lightning with our damage, camera shake (storm start, strikes, tremble), storm front approach B (directional fog tint) behind `storm_front`.

## Confirmed
- Tested (headless dedicated server and unit tests):
  - Clean build; jar loads on a dedicated server; config file written with all keys; server log clean.
  - `coriolis start` cycles APPROACH, STORM, CLEARING, CALM and schedules the next storm; no sandstorm starts during a Coriolis phase.
  - Ageing with three Major storms (`major_storm_disconnects=false`): the altered chunk survives storms 1 and 2 and is reset at storm 3; the reset chunk has no saved data, its chest and a dropped item are gone, and it stays reset across restarts.
  - A chunk protected by a fake provider is skipped by `reset all` while its neighbour is reset.
  - Unit tests: 11 shelter cases, state machine (cycle, warnings, stages, schedule rules, Major phase not resumed), chunk records (ageing, revisit, oldest first, rescue rule 1).
- Read from the jars: ViewportEvent fog/colour/camera hooks, the LightningBolt visual-only code path, FTB Chunks `FTBChunksAPI$API#getOwningTeam(Level, ChunkPos)` (26.1.2.11), IOWorker/EntityStorage/SectionStorage structure.

## Inferred
- Everything client-side (fog, overlay, particles, wind, shake, front) compiles and follows the API but was never seen or heard.
- Exposure, gear loss, damage, bones, rescue, kick and refusal were never run with a player.
- The Open Parties and Claims adapter is written from its Javadoc only.
- Sodium and Iris (in the test instance) may change how the fog events and overlay look.

## Answers to the prompt's questions
- **Part 0:** Immersive Storms (LGPL-3.0): idea adopted (fog, particles, wind as the whole sandstorm), its use of vanilla rain as the trigger rejected. Desert Storms: not read. RegenChunks (MIT): in-place regeneration idea kept in mind but not built; its region deletion helper is never called and it never unloads or handles entities/poi, so rejected. Claim mods: FTB Chunks (All Rights Reserved, no API artifact, `getOwningTeam`), Open Parties and Claims (LGPL-3.0, `OpenPACServerAPI`, claims manager `get(dim, chunkPos)`). Flan and others not researched. Details in `docs/study.md`.
- **Client hooks:** fog distance and colour `ViewportEvent.RenderFog` / `ComputeFogColor`; shake `ViewportEvent.ComputeCameraAngles`; overlay (tint and dim) a GUI layer above `camera_overlays`; particles `ClientLevel.addParticle` with the sand block-dust option; sound a looping `AbstractTickableSoundInstance`. Not possible cleanly: dimming real sunlight and the sky colour. 26.1 has an environment-attribute system (`SKY_COLOR`, `SKY_LIGHT_FACTOR`) but its layers are built in the `ClientLevel` constructor with no event, so a dynamic change needs a mixin; I left it out. Indoors and underground: the client reads the sky light at the camera, smoothed, so caves and rooms keep the vanilla look; the shelter rules themselves are server-side.
- **Sound:** vanilla only. A candidate wind bed is `ambient.basalt_deltas.loop` at pitch 0.8; nothing was heard. Nothing to approve unless you want a generated sound: nothing is shipped.
- **Reset mechanism:** unload-and-delete worked. For an unloaded chunk the region, entities and poi data are deleted through the game's own IO workers (access transformer for three private fields) and the poi manager's in-memory sections are dropped; the result is verified by reading the chunk back. Pending saves go through the same ordered queue. A chunk with a holder, a ticket or a force-load is skipped. The in-place fallback was NOT built, so loaded chunks cannot be reset.
- **Altered record:** counted are player-placed blocks (EntityPlaceEvent with a player), player-broken blocks (BreakBlockEvent) and storm bone markers. Not counted: pistons, fluids, explosions, fire, growth, entity actions, commands, container contents. Visit radius defaults to the server's simulation distance; one pass costs players x (2r+1)^2 hash lookups (441 per player at r=10), independent of how many chunks are recorded.
- **Rescue route:** (a), in the login event: load the chunk to FULL on the server thread, read the surface, teleport before the first tick. Route (b) not built. Untested.
- **Claim adapters:** built FTB Chunks and Open Parties and Claims, both by reflection, declared optional in `neoforge.mods.toml`, built only when `ModList` says the mod is loaded; fail closed. Not built: Flan and others (not researched).
- **Lightning and front:** the visual-only vanilla `LightningBolt` is usable: in the 26.1.2 source `visualOnly` skips fire, lightning rods, copper and `thunderHit` on entities, while the client still gets the sky flash and thunder. Whether it is bright enough through storm fog needs a client. Storm front: approach A (wall of particles or quads) NOT built, I cannot judge it without seeing it. Approach B (fog colour redder when looking upwind) is built behind `storm_front` (default off); the camera passed to the fog event does allow direction-dependent colour.
- **What the game would not allow, and what I did:** dynamic sunlight and sky colour (overlay instead); in-place regeneration (not built); refusing logins before the player joins (refused right after login instead); the client cannot read server config (storm fog distance is fixed at 20 blocks and `storm_fog_distance` was dropped; tremble and front switches travel in the payload).
- **Other storm mods:** Immersive Storms and similar client mods change fog and particles in deserts during rain, and a second client-side fog or sky changer will fight ours (last event listener wins). Sodium/Iris shaders can ignore fog events. Mods that also hook ComputeCameraAngles add to our shake. Mods that reset or regenerate chunks, or claim mods we have no adapter for, are not known to the reset (it never asks them).

## Numbers
- Shelter check: 3 lookups to start; worst case about 120 block lookups per player per second; 1 in the open.
- Reset: one-chunk passes took about 0.1 s each; the 100-chunk timing was not measured. Up to 8 chunks in flight.
- Major lead: warnings only in the last `coriolis_approach_seconds` (300); a longer lead announces at the command and every 10 minutes.

## Surprises, problems, deviations from the prompt
- Gradle picked an early-access JDK that cannot compile the Minecraft sources; `gradle.properties` pins the Temurin 25 path (machine-specific, move it to user-level Gradle settings if you prefer).
- A dedicated server pauses ticking when empty (`pause-when-empty-seconds`), which stalls every phase in headless tests.
- The `neoforge.mods.toml` template required the newest NeoForge build; relaxed to `[26.1.2,)`.
- `storm_fog_distance` was removed from the config (the client cannot read it).
- A test-only system property `islandcraft.weather.fakeProtected` registers a fake claim provider.
- No in-place regeneration fallback, no storm front wall, no pre-login refusal.
- Config defaults were not changed.

## How the owner can see it
Seed: any; use a vanilla desert, or superflat, in an ordinary world. `dimensions` already includes the overworld. Deploy the jar from `build/libs` to the instance (already done for step 3 builds). Look, in order:
1. `/sandstorm start 2`: orange-brown fog closing in, sand blowing from the west, a dimmer tint, wind sound; fades over 30 s. Check inside a cave or house: no change.
2. `/coriolis start 60 30`: chat warning with "in 1 minute" style text, the six stages shortened; the storm look at the end (fog about 20 blocks, heavy sand, dark tint); a storm-start shake. Stand in the open: after two warning seconds your armour wears and breaks, then health drops. Stand in a closed room or under a roof with a wall on the west side: sheltered message, no loss.
3. Die in the storm: death message "was flayed by the Coriolis storm" and a bone block at the death spot.
4. `/coriolis strike`: flash, thunder, small shake nearby. `storm_front=true` in the server config for the fog tint by direction.
5. Major: set `major_storm_disconnects=false`, put a block somewhere, `/coriolis mark`, run `/coriolis major 1 20 60` three times, `/coriolis chunks <x> <z>` shows the chunk gone from disk at storm 3. With disconnects on: you are kicked with the message, login is refused, then logs the pass line.
6. Rescue: dig a tunnel, log out, `/coriolis mark` it, run the Major storms until it is reset (or `/coriolis reset all` while you are offline), log in: on the surface with the message.

Needs a client: the look of each stage and of the storm at noon and midnight, cave and indoor look, wind levels, shake, lightning brightness, the front, chat colours, join message, death message, bones on real terrain.

## Suggested next steps (your view)
- Fly steps 1 to 4 in the test instance first, tune the overlay (alpha caps) and fog values, and decide on the wind sound.
- Build the in-place fallback only if you want to reset loaded chunks; otherwise document that a player must be away.
- A mixin (or a NeoForge event, if one appears) would give real sunlight and sky colour; the overlay is a stand-in.
- Test Open Parties and Claims with the real mod, and add Flan.

## Owner observations (human)

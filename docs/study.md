# Part 0 study (no code kept)

Time-boxed; read from public pages and repositories, nothing copied.

## Immersive Storms (Fabric, client-only)
- Small client mod: fog and particle effects per biome, driven by the weather. Sandstorms appear during rain/thunder in biomes that have no rain (so a desert "rain" event becomes a sandstorm; the client turns the vanilla weather state into the look). Standalone extraction of Scorchful's sandstorm feature. Newer builds soften the fog during "rain" and add a quieter wind sound.
- Licence: LGPL-3.0-or-later (Modrinth page).
- Adopted: the idea of fog + particles + wind as the whole sandstorm, client-side. Rejected: reusing vanilla rain as the trigger (we must not touch rain/thunder; our server sends its own state).

## Desert Storms (CurseForge)
- Not read in depth (listing only; licence not established). Not used.

## RegenChunks (Kazuto, MIT)
- Read from source. `/regenchunks <diameter>`: for each chunk, only if loaded: discard non-player entities in it, set all blocks to air, then `ChunkGenerator.fillFromNoise`, `createBiomes`, `buildSurface`, `applyBiomeDecoration`, remove trees at borders, relight and resend the chunk to players.
- The README says it deletes region data; the code has a region-file helper that zeroes sector entries but nothing calls it. It never unloads, handles pending saves, forced chunks, entities/poi files or block entities explicitly.
- Adopted: regeneration in place for loaded chunks (our fallback). Rejected: the rest; our reset unloads and deletes saved data (region, entities, poi) for unloaded chunks.

## Claim mods
| Mod | Public API | "Is this chunk claimed" | Licence | 26.1.2 NeoForge |
|---|---|---|---|---|
| FTB Chunks | `dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api()`; no published artifact found (library deps are ftb-library, ftb-teams) | `Optional<Team> API#getOwningTeam(Level, ChunkPos)` (added in 26.1.2.4 changelog) | All Rights Reserved (visible source) | yes, builds 26.1.2.x |
| Open Parties and Claims | `xaero.pac.common.server.api.OpenPACServerAPI.get(server)` then `getServerClaimsManager()`; artifacts `xaero.pac:open-parties-and-claims-neoforge-<mc>:<ver>` on chocolateminecraft.com/maven | `IPlayerChunkClaimAPI IServerClaimsManagerAPI#get(ResourceLocation dim, ChunkPos)` (null = unclaimed). Identifier vs ResourceLocation rename in 26.1 to be checked against the jar | LGPL-3.0 | yes, 0.27.5 to 0.29.x |
| Flan, others | not researched in this pass | | | |

Decision: both adapters will be reached by reflection (method handles by name) rather than compileOnly, because neither has a verified 26.1.2 API artifact and FTB's is not published. Built only when the mod is loaded.

## NeoForge 26.1.2 jar (read from the sources jar)
- Fog: `ViewportEvent.RenderFog` (`FogData`: environmentalStart/End, renderDistanceStart/End, skyEnd, cloudEnd) and `ViewportEvent.ComputeFogColor`.
- Camera shake: `ViewportEvent.ComputeCameraAngles` (yaw, pitch, roll).
- Sky and light: 26.1 has an environment-attribute system (`EnvironmentAttributes.SKY_COLOR`, `SKY_LIGHT_FACTOR`, `FOG_COLOR`...). Layers are built inside the `ClientLevel` constructor with no event, so a dynamic layer would need a mixin. We do not hack that. NeoForge also has custom skybox/cloud/weather renderers but they are selected by data attributes, not dynamically.
- Used instead: a GUI layer overlay (`RegisterGuiLayersEvent`) that tints and dims, scaled by the player's outdoor factor.
- Sound: `AbstractTickableSoundInstance` with `looping = true`, volume updated in `tick()`.
- Particles: `ClientLevel.addParticle` with `BlockParticleOption(ParticleTypes.BLOCK, sand state)`.

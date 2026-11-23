# Report — Prompt 05 Weather of Arrakis: skeleton skull death marker and a forced chunk reset command
Status: done (both parts built and tested headless and with a real player in the dev client; nothing was seen by a human)   Commit / tag: step-11 4f8c9e2, step-12 66c348f

## What was built
- **Part 1, skull marker (tag step-11).** `server/DeathMarker.java` replaces the inline bone code of `Exposure`. Config `bone_block` is gone; `bone_marker_style = "skull" | "bone_block" | "none"` (default skull, validated). Spot rules unchanged from prompt 01: the death position, else down up to 8, else the eight neighbours at the same height; the spot must be air without fluid and the block below needs a sturdy top face. `skull` sets `minecraft:skeleton_skull` with `rotation` = `level.getRandom().nextInt(16)`; `bone_block` places the old block; `none` places nothing and logs. A placement (never the block type) calls `Records.onMarker` (was `onBoneMarker`), which marks the chunk altered. Comments, the death-marker section name and the config text say marker/skull now; there was no README mention.
- **Part 2, forced reset (tag step-12).**
  - `coriolis reset force here [radius 0-4] [ignoreclaims]`, `coriolis reset force all [ignoreclaims] [confirm]`, `coriolis reset test here`, `coriolis help` (lists all four reset forms plus the test command). All at permission level 2. `reset all` and `reset here` are unchanged.
  - `server/ForceReset.java` (the async run), `server/ForceCommands.java` (the commands). `core/ChunkRecords` got a force sequence (`forceSeq`, `forceLedger`, `playerForce`, saved with optional codec fields, so old saves load).
  - Dev tools: `-Pfakeclaim="x,z;..."` (the existing fake claim provider), the client autocmd word `respawn`, an access transformer line for `ServerChunkCache.ticketStorage` (used only for the failure text). The headless `SelfTest` used for the tests below was removed afterwards (commit after step-12); it can be recovered from tag step-12.

## Confirmed
(tested = run in a world, with log lines; read = from the jar)
- **Jar, read:** `SkullBlock` and `AbstractSkullBlock` have no `canSurvive`, no `updateShape`; `neighborChanged` only handles `powered`. So a skull never pops off or drops; the "solid below" test is a placement rule, not a survival need. `SkullBlockEntity` is a normal block entity.
- **Skull placement, tested headless** (`-Pselftest=marker`, 11 of 11 passed):
  - on grass: placed at the death position, state `skeleton_skull[powered=false,rotation=0]`, block entity present, chunk in the altered record (so the record keys on the placement);
  - death on top of a bottom slab: spot rejected, nothing placed (the slab top is not a sturdy face); on a top slab: placed on it;
  - death in a 7x7 water pool: rejected, nothing placed;
  - every candidate blocked (3x3 stone, 9 deep): nothing placed, log line, no exception;
  - `none`: nothing placed; `bone_block`: the old block;
  - 200 placements: rotations 0 to 15 all seen (16 distinct);
  - the saved chunk data contains `minecraft:skull` (so it saves; it loads, because the chunk reload tests later read blocks back).
- **Skull in play (dev client, real death):** `coriolis start 3 60` as a survival player on a dedicated server: "Dev was flayed by the Coriolis storm", log `Death marker: skull placed at (8,-56,1) (death at (8,-56,1))`. Screenshot `docs/screenshots/skull.png`: a small skeleton skull on flat sand, dark storm sky and dust behind it, the crosshair selection box on it, seen from 3 blocks, tinted brown by the fog. I could not judge the angle from one frame.
- **The chunk reset removes the skull:** the neighbour chunk of the test (skull placed through the marker code, recorded) was reset by `force all confirm`, and afterwards its block-state hash equalled the fresh one on the flat world. Region data for that chunk is deleted, so the block entity goes with it (read-back of region, entity and POI stores is empty; checked in code on every reset).
- **Force reset mechanism, tested:** unload, delete, load fresh (the prompt 01 mechanism, extended). No in-place regeneration was needed or built.
  - With no player near (console): the chunks unload on their own, the three stores are deleted and read back empty, and the next load regenerates them.
  - With a real player in range (dev client on a dedicated server): the caller is moved 29 chunks (464 blocks) away, the chunks unload, the data is deleted, and the caller is put back at the same x and z on the new surface. `coriolis reset force here 1` reset 9 chunks in 0.6 s; radius 0 reset 1 in 0.2 s; reply: "Forced reset: asked 9, reset 9, skipped (claimed) 0, failed 0, took 0.6 s. Dev was moved out of range and back to the surface at the same x and z (the old height may be rock or air now). The chunks regenerate when next loaded; the Major counter is unchanged." Screenshots `reset-pattern.png` (gold 3x3 platform, a chest at its side, a gold ingot hovering above) and `reset-after.png` (the same spot: flat sand, nothing).
- **Headless self-test of the reset** (`-Pselftest=reset1`, then a restart with `reset2`; flat world, 24 + 4 checks, 0 failures):
  - one chunk with a gold block, a chest with items and a dropped item (all three confirmed stored before the reset) is reset: the block-state hash over the full height (-64 to 319, 98,304 positions) equals the hash taken before anything was altered, no item entity is left, the record entry is gone, the ledger entry is written, the Major counter is unchanged;
  - radius 1: exactly the 9 chunks reset and equal fresh; the neighbour outside the square keeps its skull and its record entry;
  - a claimed chunk (fake provider) is skipped without `ignoreclaims` ("skipped (claimed) 1 [fake test provider]") and reset with it;
  - `force all` without `confirm` prints the counts and changes nothing; with `confirm` it reset exactly the 3 recorded chunks; a chunk with a placed block that was not in the record kept its block;
  - a chunk kept loaded by a `/forceload`-style ticket is not reset: after 15 s "failed 1 [still loaded ... (tickets: ...)]", still recorded;
  - after a server restart: the reset chunks still hash equal to the fresh ones, the survivors are still in the record, the ledger entry persisted.
- **Login rescue, tested with a real player:** the dev client dug a pocket in chunk 2,2, was placed in it, `coriolis mark`, then its process was killed (the server logged "Dev left the game"). The console ran `coriolis reset force all` (counts only), then `... confirm` ("asked 1, reset 1"); `coriolis chunks 2 2` showed "altered false; last reset at counter 0; saved on disk: false". A second client joined: "Rescued Dev to the surface at 42 -56 42 (chunk was reset)". That log text is the rule-1 branch (the flat terrain makes the pocket solid, so rule 3 would also have fired, but the reason printed is the ledger rule). The unit tests cover the rule itself.
- **Player in range, tested once by accident of the run:** a console `force all` while the dev client was still logged in the chunk: "failed 1 [1x player in range (Dev)] ... 1 other player(s) in range (not moved): Dev." Nobody was moved and nothing was reset.
- Clean build and `./gradlew test` pass (ChunkRecords 7 tests incl. 2 new for the force sequence, LevelWeather 7, ShelterRules 11). The dedicated server starts in 0.33 s from the built classes, the console commands answer, no client class errors. The jar is copied to `ArrakisTest01/mods`.
- Prompt 04 refusals in `WeatherCommands.java` are untouched; `git diff` for that file shows only the new `.then(ForceCommands...)` nodes.

## Inferred
- How the skull's angle and the dust look to a player; whether the teleport out and back feels fine; how a chunk looks as it reloads (the client sees a loading screen for the far spot and a regenerated terrain around the return point).
- Other mods that load chunks (chunk loaders) or other players: they are reported as failures, not worked around. Not tested with two real players (only the console-with-player case above).
- That `none` and `bone_block` behave the same in play as in the headless test (the code path is shared).
- Claim mods: the skip text uses whatever the provider reports; only the fake provider was exercised.

## Answers to the prompt's questions
- **Is "solid below" enough for a skull?** Yes. The skull blocks have no survival check at all (read from the jar), so nothing pops off; no extra candidate logic was needed. The sturdy-top rule also keeps skulls off bottom slabs and stairs, which is a look rule, not a safety rule.
- **Slab:** on a bottom slab the spot is rejected (no sturdy top), on a top slab it is accepted. In water: rejected.
- **Does the record key on the placement?** Yes (`Records.onMarker` is called by the placement code for any style but `none`). It never looked at the block type.
- **Saves and loads / chunk reset removes it:** yes, see Confirmed.
- **Which unload mechanism:** the first one ("release, unload, delete, load fresh"), plus moving the caller. Moving the caller "3 chunks outside the area" is not enough: a player keeps the chunks at full status to view distance + 2 and holders at lower status for about 12 chunks more. The first versions of the command moved the player 15 chunks away with view distance 10 and every chunk was still held after 15 s (holder level 36, no ticket). The command now moves the caller view distance + 17 chunks (29 with the default 10) and treats another player within view distance + 15 as "in range". That distance also explains why `isLoaded` had to stay "a holder exists", not "chunk is full".
- **Players in range:** the caller is moved out and back; other players are never moved. Chunks near another player fail with "player in range (name)" and are reported. This is the "refuse" option, per chunk.
- **What the reply shows:** asked, reset, skipped (claimed, with the provider's text), failed (with reasons, grouped), time, other players in range, the move note, and that the counter is unchanged. The same text goes to the log.
- **Hash comparison against a fresh chunk of the same seed:** exact on the flat test world (every compared chunk equal, also after a restart). **Not exact on natural terrain**: a regenerated chunk has the same terrain shape and biomes but its trees and ore blobs differ (a centre chunk of the 3x3: 301 of 98,304 blocks; corner chunks up to 2,367). I measured that this is the vanilla generator, not the reset: generating the same 9x9 area twice in the same order in two fresh worlds of the same seed gave different hashes for all nine middle chunks, and a reverse order differed as well. Features that cross chunk borders depend on the generation order and thread timing. So "matches a fresh chunk of the same seed" holds for the terrain, not blob for blob, on natural worlds.
- **What the game would not allow:** a plain "move the player 3 chunks away" (above); setting the player's position does not by itself update the chunk tickets until the next move packet, so the command calls `ServerChunkCache.move(player)` after each teleport; the console cannot do `here` or `test here` (no position), so those answer with a message; no in-place regeneration was built.

## Numbers
- Marker headless: 11 checks; 200 placements, 16 distinct rotations.
- Reset headless (flat): 24 checks in run 1, 4 after the restart; reset times 0.0 to 0.1 s per command (1 to 9 chunks).
- Reset with a real player: 9 chunks 0.6 s, 1 chunk 0.2 s; the player was moved 464 blocks and back.
- Unload wait that fails: 15 s (300 ticks), then the chunk is reported as still loaded.
- Natural terrain, regenerated vs first generation: 301 to 2,367 differing blocks per chunk (about 0.3 to 2.4 percent of the 98,304 positions); two identical-order fresh generations also differed.

## Surprises, problems, deviations from the prompt
- **The unload distance** (above) was the main problem; found by logging the holder levels per chunk (a 29x29 map showed the held region around the far player).
- **The "fresh chunk" comparison** cannot be exact on natural terrain (above). The prompt asked for a hash match; I gave it on flat and measured the reason for natural terrain.
- **Rescue after a forced reset:** the ledger rule is `ledger > logout counter`, and a forced reset does not change the counter, so the old rule would never have fired. I added a separate force sequence (`forceSeq`, `forceLedger`, `playerForce`); a forced reset writes the normal ledger entry too. Logout stores the current sequence. Old saves load (optional fields). Not in the prompt; needed to meet "the login rescue works".
- The world-spawn chunks were not a problem in this version (no spawn-chunk ticket type remains); the failure text prints the ticket list for any chunk that stays held.
- Every `force here` moves the caller far, which makes the game generate the far spot (a few chunks); it is quick on flat terrain and will take longer on a natural world.
- `reset force all` from a player covers the player's own dimension; from the console every configured dimension, as asked.
- I removed the config key `bone_block`; an old config file simply ignores it (the test server's file still shows it until regenerated).
- The headless `SelfTest` used for the tests was removed from the code after step-12; it is still in tag step-12.
- No new library or dependency.

## How the owner can see it
Any world, creative, op; superflat shows the reset most clearly (the terrain is identical afterwards, the pattern is gone). Jar: `ArrakisTest01/mods` (already copied).
1. `/coriolis reset test here`: a gold 3x3 platform, a chest with 3 diamonds and a gold ingot hovering over it appear in the middle of your chunk; the chunk is marked altered.
2. `/coriolis reset force here`: after a few seconds you are moved far away and put back at the same x and z; the pattern and everything you built in this chunk are gone, the terrain is natural again; the reply lists what was reset. (If the reply says "still loaded", something other than a player holds the chunk, e.g. `/forceload`.)
3. Build in two neighbouring chunks, `/coriolis reset force here 1`: all nine chunks around you reset, nothing outside them.
4. `/coriolis reset force all`: shows how many recorded chunks it would reset and how many are protected; add `confirm` to run it. Chunks you built in that are not in the record are not touched (use `/coriolis mark` to record one).
5. Claims: `... ignoreclaims` resets claimed chunks too.
6. With `major_storm_disconnects = false`: `/coriolis start 10 60`, stand in the open and die: a skeleton skull stands where you fell, at a random angle. `bone_marker_style` in the config: `bone_block` or `none`.
7. Login rescue: dig a tunnel, `/coriolis mark`, log out, `coriolis reset force all confirm` from the console, log in: you are on the surface.
Needs a client (not claimed): how the reset chunks look while they reload, how the teleport out and back feels, the skull's look and angle in a real storm.

## Suggested next steps (your view)
- Decide whether a forced reset on a natural world should reload the area around the player with a short loading screen (it already does through the teleport); if the 15 s timeout is too long, make it a setting.
- If exact regeneration matters, the reset could also re-run the generator for the neighbours' border feature spill; vanilla does not give a deterministic result, so I would accept the difference.
- Delete the leftover Pixabay sound files (still pending from report 04).
- A configuration key for the unload margin (`HOLD_MARGIN`, 15) if a server uses a very large view distance.

## Owner observations (human)

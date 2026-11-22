# Claim protection

The chunk reset never touches a protected chunk. Protection is an interface, so a claim mod (or Subfiefs of Arrakis later) plugs in without changes to the reset.

```java
package com.paulorchard.islandcraft.weatherofarrakis.claims;

public interface ChunkProtection {
    String id();                                                   // for the claim_providers config list
    boolean isProtected(ServerLevel level, ChunkPos pos) throws Exception;
    String describe(ServerLevel level, ChunkPos pos);              // shown by "coriolis chunks <x> <z>"
}
```

Register with `ClaimProviders.register(provider)` (built-in ids `ftbchunks` and `openpartiesandclaims` are switched on or off in `claim_providers`; ids of other providers are always on).

Rules:
- No provider, nothing protected.
- Fail closed: if a provider throws, the chunk counts as protected and the error is logged once. If a claim mod is installed but its adapter cannot be built, every chunk counts as protected.
- Protected chunks are skipped by the Major pass, by `coriolis reset all` and by `coriolis reset here`.

## Adapters

| Mod | Status | How |
|---|---|---|
| FTB Chunks | built, tested against 26.1.2.11 (class and method found in the jar) | reflection: `FTBChunksAPI.api().getOwningTeam(Level, ChunkPos)`, owned if the Optional holds a team |
| Open Parties and Claims | built from its published Javadoc, **not tested** (mod not in the test instance) | reflection: `OpenPACServerAPI.get(server).getServerClaimsManager().get(dimension, chunkPos)`, owned if not null |
| Flan and others | not researched | add through the same interface |

Both are soft dependencies: declared `optional` in `neoforge.mods.toml`, no compile dependency, built only when `ModList` says the mod is loaded, and touched only by reflection so the jar loads without them. Reflection was chosen because FTB Chunks publishes no API artifact and the Open Parties and Claims artifact for 26.1.2 was not verified.

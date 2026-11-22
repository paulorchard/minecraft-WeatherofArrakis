# Notice

IslandCraft - Weather of Arrakis contains code adapted from **Immersive Storms** by **TheDeathlyCow**
(https://github.com/TheDeathlyCow/immersive-storms, branch `26.1.x`, commit `66c883b5c1facba2219c9a31e1f93acfe2e333c9`), licensed under the
**GNU Lesser General Public License v3.0 or later**. The full text is in `LICENSE-LGPL-3.0.txt`.

## Derived files (the whole adapted package)

`src/main/java/com/paulorchard/islandcraft/weatherofarrakis/client/immersive/`

| File | Adapted from | What changed |
|---|---|---|
| `SandstormParticles.java` | `client/world/SandstormParticles.java` | grid scan kept; driven by our intensity and wind vector, vanilla dust particle, shader boost, config |
| `SandstormFog.java` | `client/world/StormFogModifier.java` | fog/sky/cloud end pulled in together, configured start/end/colour, via `ViewportEvent.RenderFog` (no mixin) |

`StormLook.java` and `ShaderDetect.java` in the same package are our own code, kept there so the package can be replaced as one unit. Every adapted file
carries the original copyright and the LGPL header. Nothing else from the original project (no jar, no sounds, no textures, no particle JSON) is
included: its sound files are credited to Pixabay authors and their redistribution terms are not confirmed, so we use vanilla sounds.

## Replacing the adapted package (LGPL)

The rest of the mod calls only `StormLook.set(...)`, `StormLook.intensity()/windX()/windZ()`, `SandstormParticles.tick(...)`,
`SandstormFog.apply(...)/colour(...)` and `ShaderDetect.active()`. To use a modified version of the adapted code, edit or replace the files in the
package above and rebuild with `./gradlew build`; the rest of the mod is unaffected. The source is in this repository.

The rest of this mod is under its own licence; only the files listed above are LGPL-3.0-or-later.

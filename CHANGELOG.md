# Changelog

All releases are licensed under the **GNU Affero General Public License v3.0**. The `LICENSE` file was added to the repository in v1.4; as sole copyright holder, nameefef releases every earlier version (v1.0–v1.3) under the same terms.

Note that the v1.0–v1.3 APK binaries do not physically contain the licence text, since it was not bundled until v1.4. The licence still applies to them; if you want a binary that carries its own licence text, use v1.4 or later.

---

## v1.4 — 2026-08-20

Licensing only, no gameplay changes.

- Licensed the project under AGPL-3.0 (`LICENSE`, 662 lines of licence text)
- Added the standard AGPL notice to the top of all 11 Kotlin sources
- Bundled the full licence text into the APK at `assets/LICENSE`
- Added a menu footer naming the licence and pointing at the repository
- Licence sections in both READMEs

## v1.3 — 2026-08-20

- **Bounded prey size properly.** A single screen-fraction cap was not enough — ten critters at a high multiplier still swamped the field. Size is now limited by two rules, whichever is tighter: no critter exceeds 30% of the screen's short edge, and the combined footprint of all critters stays under 28% of the play area, so they shrink as the count rises.
- **Kept custom bitmaps sharp when scaled.** The 32 built-in characters are Canvas vector paths and are resolution independent by construction. The custom-image character was the real exposure — it decoded at 512px and softened visibly at high multipliers on a dense screen. Raised to 1536px (backgrounds 2048 → 2560) and enabled bilinear filtering and dithering on every bitmap draw path, including the menu preview tiles.

## v1.2 — 2026-08-20

- **Size expanded to 10 steps, 0.5×–3.4×** (was 5 steps topping out at 1.8×), labelled with the actual multiplier rather than adjectives.
- **Fixed a boundary bug the larger sizes exposed.** Prey kept a full body length of margin from the screen edges, so a large critter on a short landscape screen produced a negative usable area and got stuck in a corner. Margins were reduced to 0.6–0.7 body lengths and size was capped against the screen.
- The size preference moved to a new key, so old 5-step values are not reinterpreted as points on the new scale.

## v1.1 — 2026-08-20

- **Prey size control**: a 5-step multiplier applied to every character. The hit radius scales with it, so larger prey is genuinely easier for a dog to land a paw on. Menu preview tiles scale too.
- **In-app language switcher**: English was already the default locale, but only surfaced when the system language was English. Added a Language control backed by `AppCompatDelegate.setApplicationLocales`, with the AppCompat locale service registered so the choice persists below Android 13.

## v1.0 — 2026-08-20

First release.

- **32 built-in characters** across three groups — animals, bugs & birds, toys & lights (including the red dot) — plus Mixed and Custom image
- **9 procedural backgrounds** plus custom image
- **5 speed settings, 1–10 critters** on screen
- **28 procedurally synthesised sound effects**; catches and misses sound different, with a reward chime every 10 in a row
- **Haptic feedback** at three strength levels
- **Exit protection**: back button and back gesture swallowed, edge gesture exclusion, hold the top-left circle for 2/3/5 seconds to quit
- **Custom assets**: character image, background image and sound file, copied into the app's private storage
- Localised in English and Chinese
- All artwork drawn as Canvas vector paths, all audio synthesised at runtime — no image or audio asset files apart from the launcher icon

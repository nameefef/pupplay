# Changelog

All versions of PupPlay are licensed under the **GNU Affero General Public License v3.0**. The `LICENSE` file landed in v1.4; as sole copyright holder, nameefef licenses all earlier work under the same terms.

Only **v1.4 and later are distributed as binaries**. The v1.0–v1.3 releases were withdrawn, because their APKs were built before the licence text was bundled into the package. The entries below stay as a record of what changed in each version — the source for every one of them is still in the repository history under tags `v1.0`–`v1.3`.

---

## v1.7 — 2026-08-20

- **Size is now set in real millimetres, not as a share of the screen.** A dog's paw is a fixed physical size — a medium dog's paw pad is 35–45mm wide — but a phone in landscape is only 65–71mm tall, while a 10.5" tablet is 141mm. Any screen-proportional scale is therefore wrong on at least one of them: v1.6's default worked out to 21mm on a phone, smaller than a small dog's paw pad, which is why the slider had to be pushed to maximum to feel right.
- Steps now run 10mm to 55mm of drawn height, measured with the display's physical density (`xdpi`/`ydpi`, falling back to `densityDpi` where a device reports nonsense). **The default is step 7 at 36mm**, about a medium dog's paw pad. Drawn size is still bounded at 68% of the screen's short edge, so the top steps clamp on small phones.
- Fixed unescaped apostrophes in the English strings, which broke the resource build.

---

## v1.6 — 2026-08-20

- **Fixed the size scale being undersized.** The internal `size` value is a character's *body length*, not its drawn height — a mouse stands about 0.67× its `size`, a fox about 0.8×. v1.5 treated the two as the same thing, so every step was roughly a fifth smaller than its label claimed, and the curve itself was set too low. Steps now define the **drawn height** as a share of the screen, and the curve was raised: what used to be the maximum (10/10) is now around step 7, with three steps of headroom above it. The default step 5 is about 60% larger than v1.5's default.
- **Loosened the crowding rule.** The combined-footprint budget went from 28% to 42% of the play area. At 28% the cap bound with only two or three critters on screen, so the top steps did nothing — the slider moved and the prey didn't. With many critters the top steps still compress, which is the point of the rule.
- **Made the character picker legible.** The grid went from 4 columns to 3 with taller cells, and characters are now sized to fill the cell rather than drawn at a fixed fraction of it — roughly 53dp instead of 27dp on a common phone.

---

## v1.5 — 2026-08-20

- **Size is now measured against the screen instead of a fixed multiplier.** Each of the 10 steps defines the critter's size as a percentage of the screen's short edge (7% to 40% of screen height in landscape), so a given step looks the same on a small phone and on a tablet. Previously size was `character base size × multiplier`, which meant the same setting produced wildly different results across devices, and the multiplier numbers were misleading — a fox could not exceed 1.86× on a typical phone no matter how far the slider went, so the top steps did nothing.
- **Step 5 is the auto-fitted sweet spot and the new default.** On a common 1080×2400 phone that makes the default roughly 50–60% larger than before, and considerably larger on tablets, where the old fixed sizes were far too small.
- Character size differences are preserved but compressed to a 0.75–1.25 band, so a fox still reads as bigger than a mouse without hitting the screen limit several steps before it does.
- The per-critter screen cap was removed as redundant — the step definition bounds size inherently. The rule that keeps all critters' combined footprint under 28% of the play area still applies.
- The size preference moved to a new key, since the step numbers changed meaning.

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

# 🐕 PupPlay

**English** · [简体中文](README.zh-CN.md)

A touchscreen hunting game for dogs. Lay your phone flat on the floor and let your dog paw or nose at the critters darting across the screen — every catch gets a sound and a haptic buzz.

The UI is available in English and Chinese — it follows your system language, and there is an in-app switcher if you want to override it.

## Features

- **32 built-in characters**, in three groups:
  - Animals: mouse, fox, rabbit, squirrel, cat, hedgehog, raccoon, duck, chick, sheep, pig, frog, snake, crab, fish
  - Bugs & birds: beetle, butterfly, bee, spider, dragonfly, bird, firefly
  - Toys & lights: **red dot**, green laser, blue dot, tennis ball, frisbee, bone, rope toy, bubble, star, feather
  - Plus **Mixed** (every critter re-rolls its character) and **Custom image**
- **9 built-in backgrounds**: pure dark, grass, starry sky, snow, wood floor, carpet, beach, blue sky, forest — plus your own photo
- **5 speed settings, 1–10 critters** on screen at once, and **10 size levels (0.5×–3.4×)** — scale prey right up for clumsy paws, or down for precise little dogs
- **28 procedurally synthesised sounds** — catches and misses sound different. Pin one sound, or import your own audio file.
- **Haptic feedback**: a double tap on a catch, a light tick on a miss, a triple buzz every 10 in a row. Three strength levels.
- **Exit protection**: the back button and back gesture are both swallowed. To quit, you must hold the small circle in the top-left corner for 2/3/5 seconds (configurable).
- **Custom assets**: swap in your own character image, background image, and sound file. Files are copied into the app's private storage, so deleting the originals is fine.

## Design notes — why it looks like this

- **The palette is built for canine vision.** Dogs are dichromats on a blue–yellow axis; red and green both read as dull yellow-brown to them. So prey is bright yellow, bright blue, and white against a dark background for maximum contrast. The red dot is kept because people ask for it — a dog sees a dim little dot, but it will still chase anything that moves that fast.
- **Movement is freeze → dash → freeze**, not smooth drifting. That is how real prey moves, and it is what actually triggers the prey drive.
- **A near miss startles the prey into bolting** — that moment is the strongest hook for keeping a dog engaged.
- **Hit detection is generous.** Paws are imprecise, so the hit radius is larger than the drawn sprite.

## Download

Grab an APK from [Releases](../../releases):

| File | Notes |
|---|---|
| `pupplay-universal.apk` | **Recommended** — runs on every Android phone |
| `pupplay-arm64-v8a.apk` | 64-bit ARM |
| `pupplay-armeabi-v7a.apk` | 32-bit ARM (older devices) |

> This app is pure Kotlin with **no native libraries**, so all three APKs are byte-for-byte equivalent in what they contain. Take whichever you like.

Requires Android 7.0 (API 24) or newer. Enable "install from unknown sources" before opening the APK.

## Building it yourself

### Option 1: Android Studio (easiest)

1. Install [Android Studio](https://developer.android.com/studio)
2. `Open` → select this repository
3. It downloads Gradle and the Android SDK on first open (a few hundred MB)
4. Enable USB debugging on your phone, plug it in, hit the green ▶

### Option 2: Command line

```bash
# 1) JDK 17, Android command line tools
brew install openjdk@17
brew install --cask android-commandlinetools

# 2) SDK packages + licences
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 3) Build
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle assembleRelease

# 4) Install
adb install -r app/build/outputs/apk/release/app-universal-release.apk
```

APKs land in `app/build/outputs/apk/`.

> **Building from mainland China**: Gradle will likely need a proxy to reach `dl.google.com`. Add this to `~/.gradle/gradle.properties`:
> ```properties
> systemProp.https.proxyHost=127.0.0.1
> systemProp.https.proxyPort=7890
> systemProp.http.proxyHost=127.0.0.1
> systemProp.http.proxyPort=7890
> ```
> Adjust the port to match your setup.

## Tips for actually playing it with a dog

- **Lay the phone flat on the floor.** Use a rugged case or a screen protector — claws scratch.
- Start with **2–3 critters at speed 2–3**, then ramp up.
- **5–10 minutes per session** is plenty. Longer and most dogs get over-aroused.
- For real safety, turn on **Screen pinning / App pinning** in system settings and pin the game — then your dog can't swipe out at all.
- **Give a real reward afterwards** (a treat, a toy). A dog that only ever chases things it can never physically catch can get frustrated.

## Project layout

```
app/src/main/java/com/easonyin/dogplay/
├── MainActivity.kt      Menu: character / background / speed / count / sound / haptics / custom assets
├── GameActivity.kt      Immersive fullscreen, back button swallowed, edge gesture exclusion
├── GameView.kt          Game loop, multi-touch, particles, HUD, hold-to-exit
├── Prey.kt              Prey movement state machine (pause → run → startled bolt)
├── PreyType.kt          Parameter table for all 32 characters
├── PreyRenderer.kt      Vector artwork for all 32 characters (pure Canvas, no image assets)
├── Background.kt        Procedural rendering for the 9 backgrounds
├── SoundEngine.kt       Waveform synthesis for 28 sounds + AudioTrack playback
├── Haptics.kt           Vibration feedback
├── Prefs.kt             Settings storage + custom asset management
└── PreviewViews.kt      Character / background preview tiles in the menu

app/src/main/res/
├── values/strings.xml       English (default)
└── values-zh/strings.xml    Chinese
```

All artwork and audio is generated in code — there are no image or audio asset files in the repo apart from the launcher icon.

## Signing

The APKs in Releases are signed with Android's default debug key so you can install and try them immediately. **They cannot be published to an app store.** Generate your own keystore and re-sign for a real release.

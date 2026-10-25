# Android per-game configuration regression tests

Run from the repository root with Python 3, a Java runtime, a Kotlin compiler
distribution, the ini4j dependency, and a host C++20 compiler. Configure the
Android CMake build first so its generated native setting-key header exists:

```sh
git submodule update --init externals/inih/inih externals/boost
python src/android/tests/per_game_settings/run.py \
  --kotlin-home /path/to/kotlinc --ini4j /path/to/ini4j.jar
```

Use `--java /path/to/java`, `--cxx /path/to/c++`, or `--zig /path/to/zig` as needed.
Set Zig's cache environment variables to writable directories when sandboxed.
The runner finds the generated header under `app/.cxx` automatically, or accepts
`--generated-headers /path/to/generated/src` (containing `common/setting_keys.h`).

The runner compiles the production settings descriptors, model, SettingsFile,
and SettingsActivityPresenter. Small Android/SAF adapters use a temporary local
filesystem, and only the native setting-key JNI bindings and Android resources
are replaced with generated constants. The native test compiles the production
AndroidConfigIni against Azahar's pinned INIReader dependency.
It also compiles the actual Android default configuration and generated native
keys, and runs the same declaration/omission gate used at native startup. This
catches missing keys that would otherwise abort the process on folder selection.

Covered behavior: sparse persistence, reopening a profile, global inheritance,
individual and full resets, quick control writes, switching titles on one model,
editor isolation, applying the active game's editor, retaining edits across
presenter recreation, inheritance flags, invalid IDs, malformed native values,
and exclusion of shared settings.

These are host regression checks. They do not validate Android lifecycle APIs,
real document-provider permissions or cache behavior, UI rendering, JNI wiring,
renderer startup/shutdown, or APK builds. Validate those on an Android device
with both Vanilla and Google Play builds before distributing an APK.

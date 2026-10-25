# Azahar-PerGameSettings-Android

An unofficial Android fork of [Azahar](https://github.com/azahar-emu/azahar)
with per-game configuration. It is based on development commit
[`12bde376ce8fc2012af61ea791f94172a82e67e0`](https://github.com/azahar-emu/azahar/commit/12bde376ce8fc2012af61ea791f94172a82e67e0)
(24 October 2026), with the changes described below.

## Per-game settings

Long-press a game in the library and choose **Per-game settings**. The in-game
Settings entry also edits the running game's profile. General, System INI
settings, Graphics, Layout, Audio and Debug are available. Settings show
**Global** or **Game override**. Long-press an override to return to its global
value, or choose **Use global settings** to reset the entire game profile.

Profiles are sparse INI files at `config/custom/<16-digit-title-ID>.ini`.
Missing values follow global settings. Profiles load before renderer startup
and native global settings are restored on exit. Shared account credentials,
storage paths, controller mappings, theme and NAND user-profile data stay global.
Titles without a nonzero program ID do not receive profiles.

The fork also fixes a development-base startup assertion caused by missing
battery default declarations. Its host regression suite checks that gate,
profile persistence, inheritance, resets, editor isolation and Kotlin/native
file-format interoperability.

## Installation and frontend compatibility

The release APKs use the original package names and activity classes:

| Variant | Package | Main activity |
|---|---|---|
| Vanilla | `org.azahar_emu.azahar` | `org.citra.citra_emu.ui.main.MainActivity` |
| Google Play-style file access | `io.github.lime3ds.android` | `org.citra.citra_emu.ui.main.MainActivity` |

This preserves package/activity-based frontend integration. The APKs are signed
with this fork's own release key, **not Azahar's official signature**. They cannot
update an official installation in place. Back up your user directory and any
app-private data you need, uninstall the matching official app, and install the
fork. Then select your existing user directory. Uninstalling removes that app's
private data; do not assume Android will retain it. Switching back to official
Azahar also requires uninstalling this fork first.

The universal 64-bit APKs target ARM64 and x86-64, with Android 10 or newer
required. Frontend launch compatibility should be tested with your particular
frontend. This is an unofficial release, not an endorsement by Azahar maintainers.

## Source and building

See [BUILDING.md](BUILDING.md), [release notes](RELEASE_NOTES.md), and
[the host regression suite](src/android/tests/per_game_settings/README.md).
Pinned upstream submodules are retained; clone with `--recurse-submodules`.
The original README is preserved in [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).
Upstream GitHub workflows are preserved under `.github/upstream-workflows` and
do not run automatically in this independent repository.

## License and provenance

Original copyright and license notices are retained. See [LICENSE.txt](LICENSE.txt)
and `misc/licenses/`. This fork contains substantial AI-assisted implementation;
it is not an upstream-approved contribution. The original [AI-POLICY.md](AI-POLICY.md)
is preserved for context. No PR or issue was submitted to upstream Azahar.

# Build the Android release

Tested toolchain: JDK 21, Gradle wrapper 8.14.5, Android SDK/build-tools 35,
NDK 27.3.13750724, and SDK CMake 3.31.6. Initialize all pinned submodules:

```sh
git submodule update --init --recursive
```

Provide your own signing key through environment variables. Keep the keystore
and its password outside the repository. Never commit either one:

```sh
export ANDROID_KEYSTORE_FILE=/absolute/path/to/your-release.p12
export ANDROID_KEYSTORE_PASS='your-keystore-password'
export ANDROID_KEY_ALIAS=azahar-per-game-settings
export AZAHAR_FORK_VERSION=1.0.0-pergame
cd src/android
./gradlew :app:assembleVanillaRelease :app:assembleGooglePlayRelease
```

On Windows use PowerShell environment variables and `gradlew.bat`. Configure
ANDROID_HOME and JAVA_HOME for your local installation. The default build
includes ARM64 and x86-64; `-PazaharAbis=arm64-v8a` selects an ARM64-only build.
APKs appear under `app/build/outputs/apk/<variant>/release/`.

Use the same private release key for every future update of this fork. Changing
the key prevents an in-place update. The official Azahar private key is not
included and cannot be derived from an official APK or public certificate.
The upstream Gradle script falls back to a debug key if release signing
variables are omitted; always configure a real release key for distribution.

Follow the host test README for regression checks. Device checks should include
folder setup, two-game profile isolation, persistence after restart, global
inheritance, individual/full reset, Debug options, shortcut/frontend launches,
and switching between renderer profiles. New devices and frontends require
their own smoke testing; a successful build is not a universal compatibility test.

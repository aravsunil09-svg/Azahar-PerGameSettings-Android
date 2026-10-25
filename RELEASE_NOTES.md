# 1.0.0-pergame

- Adds Android per-game profiles with global inheritance and individual/full resets.
- Adds the game-library entry point, game identification, and override labels.
- Includes the Debug menu and per-game CPU/JIT, shader and debug options.
- Loads native profiles before selecting the renderer and restores globals afterward.
- Preserves edits on backgrounding and refreshes external-display configuration.
- Invalidates the SAF config cache after creating or deleting a profile.
- Fixes the development base's missing battery default declarations and adds
  a native startup-validation regression check.
- Uses original package/activity identities for frontend integrations, with
  a separate unofficial release signing key.

Base: upstream development commit `12bde376ce8fc2012af61ea791f94172a82e67e0`.
User testing of the preceding ARM64 test build reported the main configuration
flows working. Release builds, additional devices, x86-64 environments and
individual frontends still need real-device testing.

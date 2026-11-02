# Fancy Fluid Storage

[![Build status](https://github.com/MrFuzzihead/FFS/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/FFS/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/FFS?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/FFS/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%203.0-lightgrey)](LICENSE.md)

**Fancy Fluid Storage (FFS)** is a Minecraft **1.7.10 (Forge)** mod that lets you build
multiblock fluid tanks out of almost any block in the game. Place a valve, click it,
and the mod validates the shell, turns the frame into tank blocks, and gives you a
single tank of fluid with one shared capacity. Build a glass aquarium, a stone silo, a
wool-lined nightmare, or a tank whose frame is made of actual lava blocks — if it
forms a valid shell, FFS will hold your fluid.

This repository is a community-maintained continuation of the original
[Fancy Fluid Storage](https://github.com/Lordmau5/FFS) by Lordmau5, kept alive for
1.7.10 with crash fixes, review-driven hardening, a modern Gradle toolchain, and an
automated test suite.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Downloads](#downloads)
- [Building from source](#building-from-source)
- [Testing](#testing)
- [Contributing](#contributing)
- [Changes in this fork](#changes-in-this-fork)
- [Credits](#credits)
- [License](#license)

## Features

- **Multiblock tanks from (almost) anything** — the shell can be stone, glass, metal,
  wood, wool, and blocks from other mods. The tank frame is rendered with the original
  block, so a glass tank looks like glass.
- **One tank, many valves** — add as many valves as you like to the shell; they share
  the same fluid, capacity, and auto-output state.
- **Automatic shape validation** — the valve walks the six cardinal directions,
  validates the shell, and refuses malformed or interrupted tanks.
- **Configurable tank size** — default `MAX_SIZE` is 16 blocks per axis including the
  frame (3–32 in the config; 32 is opt-in for large servers).
- **Fluid containers and pipes** — right-click with a bucket or any
  `IFluidContainerItem` to fill/drain; BuildCraft pipes, comparators, and hoppers work
  out of the box.
- **Auto output** — push fluid into adjacent tanks or `IFluidHandler`s without a pipe.
- **Leaks and fire** — flammable shells can burn, and leaky materials can spring a
  leak when the tank is hot or full (both configurable).
- **ComputerCraft and OpenComputers** — read the fluid name/amount/capacity and toggle
  auto-output from Lua.
- **WAILA, Chisel, FunkyLocomotion and RemainInMotion** support.
- **Highly configurable** — tank size, capacity per block, frame material rules,
  burning, leaking, and rendering.

## Requirements

| Dependency | Version | Notes |
| ---------- | ------- | ----- |
| Minecraft | 1.7.10 | — |
| Minecraft Forge | 10.13.4.1614 | [Download](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html) |
| Java | 8+ | The 1.7.10 runtime uses Java 8; the build toolchain provisions the correct JDK automatically. |

All of the following are **optional**; FFS detects them at runtime:

| Mod | Integration |
| --- | ----------- |
| WAILA | Tank/valve tooltips |
| Chisel | Facade support for tank frames |
| BuildCraft | Pipe connections and auto output |
| ComputerCraft | Lua peripheral |
| OpenComputers | Component API |
| FunkyLocomotion | Prevents frames/valves from being moved |
| RemainInMotion | Keeps tanks safe during frames |

## Installation

1. Install **Minecraft Forge 10.13.4.1614** for Minecraft 1.7.10 (run the installer
   and select *Client*).
2. Download the latest **FFS** jar from the [Releases](https://github.com/MrFuzzihead/FFS/releases/latest)
   page.
3. Drop the jar into your `.minecraft/mods` folder (and any optional integration mods
   you want).
4. Launch the game with the Forge profile.

On a server, install the same jar in the server's `mods` folder.

## Downloads

- **[GitHub Releases](https://github.com/MrFuzzihead/FFS/releases/latest)** — recommended;
  stable builds tagged from `main`.
- **[GitHub Actions](https://github.com/MrFuzzihead/FFS/actions)** — bleeding-edge
  snapshot builds from every push to `main`.

## Building from source

This project uses the [GTNewHorizons](https://github.com/GTNewHorizons) 1.7.10 Gradle
toolchain. The correct JDK is provisioned automatically by Gradle — no manual Java
setup required.

```bash
git clone https://github.com/MrFuzzihead/FFS.git
cd FFS
./gradlew build        # on Windows: gradlew.bat build
```

The built jars are placed in `build/libs/`. To launch a development client:

```bash
./gradlew runClient    # on Windows: gradlew.bat runClient
```

To launch a headless development server:

```bash
./gradlew runServer
```

## Testing

The mod ships with a JUnit 5 unit-test suite and a headless test task. No display or
Minecraft client is required:

```bash
./gradlew headlessTest
```

The suite covers the pure tank math (geometry, capacity, fill/auto-output rules),
NBT round-trips (including removed fluids and legacy tags), null/missing block
handling, and the headless harness smoke checks. `./gradlew build` runs the same
suite as part of `check`.

CI (the shared GTNH `build-and-test` workflow) runs:

1. `./gradlew build` — compiles, runs Spotless/Checkstyle, and runs the unit suite.
2. `./gradlew runServer` with a 90-second timeout and an automatic `stop` — a real
   headless dedicated-server boot that catches registration, classloading, and
   startup crashes.

The review that drives the current fix batches is in [`CODE_REVIEW.md`](CODE_REVIEW.md).

## Contributing

Bug reports and pull requests are welcome! Please open issues on the
[issue tracker](https://github.com/MrFuzzihead/FFS/issues).

- Branch naming follows `bugfix-<issue>-<short-name>` or `feature-<issue>-<short-name>`.
- CI runs the full build (including Checkstyle, Spotless, the unit suite, and the
  headless server smoke test) on every pull request, so please make sure
  `./gradlew build` passes locally before opening a PR.
- Run `./gradlew spotlessApply` before committing to apply the project formatter.
- New behaviour should come with a unit test; pure logic belongs in `util` classes so
  it can be tested without a running game.

## Changes in this fork

This fork continues the 1.7.10 lineage of FFS while modernising the build and hardening
the implementation. Work is organised in review-driven batches (see
[`CODE_REVIEW.md`](CODE_REVIEW.md)):

**Merged**

- Modernised build system for 2026 (GTNH convention plugin, Gradle wrapper,
  auto-provisioned JDK, GitHub Actions CI)
- Review document and test harness (JUnit 5 + `headlessTest`, 106 tests)
- Safety fixes: explosion handling, client-side container guard, NBT robustness,
  removed-mod block handling, leak-hardness crash, capacity overflow, fire config,
  `canFill` units, null-valve GUI guard
- Tank geometry rewrite: a fresh six-direction measurement (`TankGeometry`), no stale
  `length[]`, and `MAX_SIZE` now includes the shell as documented
- Tank ownership fixes: foreign masters are rejected, shared-wall frames are
  protected, absorbed slaves are demoted, stale slaves are invalidated, and placing a
  block inside a tank breaks it immediately
- Tank-wide auto-output (toggling a slave valve now affects the tank), single rain
  collection per tank per tick, and correct auto-output/fill rules for gases
- Correct chunk-rectangle loading, fixed RemainInMotion rotation, and CC peripheral
  equality
- Performance: frame tile entities no longer tick; tank reloads no longer mark every
  frame for update; fluid sync is throttled; the interior is a packed `LongSet`; the
  fluid renderer draws the bottom once; the tank-frame wrapper is reused;
  precipitation height is cached; and burn/leak use the world RNG
- Networking: packets are marshalled onto the server thread and validate reach and
  valve-name length
- Renderer/GUI: correct six-face inventory model and single-pass valve rendering with
  an inside-face accent; no global render-pass mutation; GL state fully restored;
  gas volumes use the same partial-column math as liquids; the GUI scissor is
  Y-flipped and bottom-aligned; the valve no longer leaks light; and slave render
  bounds are small
- Packaging: lowercase `ffs` mod ID and resource domain (fixes Linux/macOS assets),
  `after:` metadata for optional mods, optional dependencies no longer published as
  hard runtime deps, namespaced tile-entity IDs with legacy aliases, a named tank
  frame, and a default `MAX_SIZE` of 16 (32 remains opt-in), with a one-time
  `FFS.cfg` → `ffs.cfg` migration. Worlds saved with the old uppercase `FFS` mod id
  are migrated through Forge's missing-mapping event: old `FFS:blockValve` and
  `FFS:blockTankFrame` block/ItemBlock names are remapped to the current `ffs:*`
  blocks at their original numeric ids, so existing tanks are not removed.
- Architecture: `TankStructure` performs one direct shell/interior scan;
  `TankFluidHandler`, `TankHazards` and `ValveCommands` split the fluid rules,
  environment hazards and shared CC/OC semantics out of the block entity. The old
  `getBlocksBetweenPoints`/`getTankFrame` cuboid maps are gone.
- License: upstream CC BY-NC-SA 3.0 retained, with `NOTICE.md` attribution and both
  files included in the built jar.
- WAILA 1.19.34: compact NBT sync, config keys, advanced tooltip and localized
  head/body/tail sections.
- GUI: localized strings, change-only valve-name packets, correct Escape/key
  handling, and cached fluid/capacity reads.
- Thinning: `ValveState`/`ValveNbt` own save/load and compact client sync;
  `AutoOutputState`/`TankAutoOutput` own the flag, sync throttle and neighbour push;
  `TankOwnership`/`TankBuilder` own links and formation/teardown; and
  `ValveCommandAdapter` owns shared CC/OC argument handling. `TileEntityValve`
  dropped from ~1,314 to ~700 lines.

**Planned**

- Remaining: in-game validation of formation/teardown, rendering, WAILA/GUI and
  optional integration flows.

## Credits

- **Original Fancy Fluid Storage** by [Lordmau5](https://github.com/Lordmau5) and
  maxpowa
- **Continuation and maintenance** by [MrFuzzihead](https://github.com/MrFuzzihead)
- **GTNewHorizons** for the modern 1.7.10 build tooling and CI workflows
- Thanks to FridgeBoy for help with the original tank math

## License

This project is licensed under the
[Creative Commons Attribution-NonCommercial-ShareAlike 3.0](LICENSE.md) license.
The upstream license is intentionally retained at the original author's request.
Fancy Fluid Storage is the work of its original authors; this fork is a modified
continuation for 1.7.10, and the changes are identified under
[Changes in this fork](#changes-in-this-fork). Attribution and the upstream source
are recorded in [NOTICE.md](NOTICE.md). See [LICENSE.md](LICENSE.md) for the full
text.

# Fancy Fluid Storage — Code Review & Remediation Plan

Review of the 1.7.10 Forge implementation in `src/main/java` (28 files, ~3,900 LOC).
Tricky assumptions were verified against the decompiled 1.7.10 sources in
`build/rfg/minecraft-src` (`PlayerControllerMP`, `Chunk.func_150807_a`,
`Block.onBlockExploded`, `FluidStack`, `PlayerManager.markBlockForUpdate`).

Status key: **P0** = crash / dupe / data loss, **P1** = wrong behaviour,
**P2** = performance / robustness, **P3** = cleanup / style.

---

## 0. Batch plan

Each batch must ship with unit tests and keep the headless harness green.
The harness is:

- `./gradlew headlessTest` — JUnit 5 suite, no display required.
- `./gradlew build` — includes Spotless/Checkstyle + the unit suite.
- CI (`GTNH-Actions-Workflows/build-and-test.yml`) — runs `build` and then a
  90-second headless `runServer` smoke test (`run/eula.txt`, stdin `stop`).

| Batch | Scope | Status |
| ----- | ----- | ------ |
| 0 | Review doc, README, JUnit 5 + `headlessTest` task, pure-helper extraction | done |
| 1 | P0 safety fixes: explosions, client-side containers, NBT, null blocks, leak crash, capacity overflow, fire config, `canFill` units, GUI null | done |
| 2 | P1 correctness: tank ownership (shared walls/edge valves/stale slaves), `breakTank` fluid, auto-output per tank, interior invalidation, geometry (`length[]` reset, MAX_SIZE), fluid-handler contract, rain/negative density, RiM rotation, CC equality | done |
| 3 | P2 performance: frame ticking, packet storm, master/fluid caches, retained-map removal (LongSet), renderer bottom-face fix + wrapper reuse, precipitation cache, RNG reuse, server-thread packet queue/validation | done |
| 4 | Renderer/GUI: face/normal fixes, render-pass global hack, scissor math, GL state restore, bounding boxes, lighting | done |
| 5 | Build/packaging: `ffs` casing, resource domain, `mcmod.info`, optional dependency scopes, TE registry names, tests in CI | done |
| 6 | Architecture: split `TileEntityValve` into structure/fluid/hazards/peripheral classes; shared CC/OC command layer | done |
| 7 | License attribution/packaging, WAILA 1.19.34 data/config/advanced tooltip, GUI localization and name sync | done |
| 8 | NBT/auto-output thinning: `ValveState`/`ValveNbt`, `AutoOutputState`/`TankAutoOutput` | done |
| 9 | Ownership/setup/break and peripheral-argument extraction: `TankOwnership`, `TankBuilder`, `ValveCommandAdapter` | done |
| 10 | Legacy world migration: map old `FFS:blockValve`/`FFS:blockTankFrame` block and ItemBlock names to `ffs:*` | done |
| 11 | Renderer crash/invisible valve: frame pass-result tracking and solid unformed-valve body with accent overlay | done |
| 12 | Sync newly created tank-frame TEs to clients so formed tanks do not lose their shell | done |

### Test coverage shipped with batches 9-12

`./gradlew headlessTest` now runs 109 JUnit 5 tests. Batch 12 added frame initialization coverage. Batch 11 added `TankFrameRendererTest` (2) for the solid/transparent render-pass rules. Batch 10 added `LegacyMappingsTest` (5) for the old-uppercase block/item name migration. Batch 9 added:

- `TankOwnershipTest` (3) - master flag/link lifecycle, valve lists, name filtering.
- `ValveCommandAdapterTest` (4) - read-only commands, auto-output by name, invalid
  arguments/methods, null arguments.

### Test coverage shipped with batch 8

`./gradlew headlessTest` runs 94 JUnit 5 tests after batch 8. Batch 8 added:

- `AutoOutputStateTest` (2) - toggle and pending/throttled sync consumption.
- `TankAutoOutputTest` (1) - detached-valve safety.
- `ValveNbtTest` (4) - master/slave round-trips, missing tags and state codec.

### Test coverage shipped with batch 7

`./gradlew headlessTest` runs 87 JUnit 5 tests after batch 7. Batch 7 added:

- `LicenseNoticeTest` (3) - attribution/source/license URI, upstream license text and
  jar packaging.
- `WailaTankDataTest` (3) - detached-valve data, NBT round-trip and missing tags.

### Test coverage shipped with batch 6

`./gradlew headlessTest` runs 81 JUnit 5 tests after batch 6. Batch 6 added:

- `TankStructureTest` (6) — cell classification, 5×5×5/3×3×3 shell counts, non-air
  and Railcraft residual-heat interiors, inverted/null bounds.
- `ValveCommandsTest` (4) — empty fluid reports, output maps, unknown names, detached
  valve safety.
- `TankFluidHandlerTest` (1) — detached valves reject fill/drain queries.

### Test coverage shipped with batch 5

`./gradlew headlessTest` now runs 70 JUnit 5 tests. Batch 5 added:

- `ModNamesTest` (2) — lowercase modid/resource domain and namespaced TE names with
  legacy aliases.
- `ConfigDefaultsTest` (1) — the documented defaults, including `MAX_SIZE = 16`.
- `BlockTankFrameTest` (1) — the technical block has a name, tile entity and texture.
- `PackagingTest` (2) — processed `mcmod.info` (lowercase id, soft dependencies) and
  the lowercase asset domain for lang/sounds.
- `ConfigMigrationTest` (3) — legacy config copy, no overwrite, missing legacy file.

### Test coverage shipped with batch 4

`./gradlew headlessTest` runs 61 JUnit 5 tests after batch 4. Batch 4 added:

- `FluidRenderMathTest` (4) — fill clamping, interior column height, visible layer
  count, layer heights (including the 0.01 minimum bottom layer).
- `GuiScissorTest` (3) — Y-flip into framebuffer coordinates, scale clamping, negative
  dimensions.
- `ValveFaceTest` (1) — only the inside face receives the accent texture.

### Test coverage shipped with batch 3

`./gradlew headlessTest` runs 53 JUnit 5 tests after batch 3. Batch 3 added:

- `LongSetTest` (5) — add/contains/duplicates, resize, signed coordinate packing.
- `PacketValidationTest` (2) — reach boundary and valve-name length rules.
- `ServerTaskQueueTest` (3) — FIFO drain, null task, failed task isolation.
- `TankMathTest` — fluid sync throttle.
- `TileEntityTankFrameTest` — `canUpdate()` is false, valve-position change detection.

### Test coverage shipped with batch 2

`./gradlew headlessTest` now runs 40 JUnit 5 tests. Batch 2 added:

- `TankGeometryTest` (9) + `FakeBlockGrid` headless harness — valid shells, floor
  valves, minimum 3×3×3, MAX_SIZE including the frame, missing walls (no stale
  lengths), open areas, unknown `inside`, invalid arguments, call independence.
- `TankMathTest` (12) — added gas (`negativeDensity`) auto-output and fill-height
  rules.
- `TileEntityTankFrameTest` — added valve-position getter and ownership checks.

### Test coverage shipped with batch 1

`./gradlew headlessTest` started with 29 JUnit 5 tests:

- `TankMathTest` — capacity saturation, 0..1 fill fractions, 0..15 comparator,
  auto-output height, `canFill` height units, leak-hardness clamping/validity.
- `NbtHelperTest` — position round-trip, missing/short arrays, amount clamping,
  legacy fluid-name/ID selection, null-safety.
- `ExtendedBlockTest` — identity-based equality (including two blocks with the same
  unlocalized name), metadata handling, null-block safety, hashCode.
- `TileEntityTankFrameTest` — empty NBT, unknown/removed block IDs, valve-position
  persistence without a loaded master, world-free safety.
- `HeadlessHarnessTest` — `java.awt.headless`, ForgeDirection layout, NBT and
  unregistered Block usability, FML tile-entity mapping bootstrap.

The Forge `FluidStack` NBT path needs a launched game registry (FML's
`Loader.instance()` requires a LaunchClassLoader), so it is verified by the
headless `runServer` CI job rather than the JUnit JVM. A future batch can add a
small LaunchWrapper-based integration harness if that path needs direct coverage.


---

## 1. P0 — crashes, dupes and data loss

### 1.1 Explosions never remove a valve — item duplication
`BlockValve.java:51-59`, `BlockTankFrame.java:51-61`

`Block.onBlockExploded()` is the hook that removes the block (`setBlockToAir`).
Both overrides call `super.onBlockDestroyedByExplosion(...)` (a no-op) instead of
`super.onBlockExploded(...)`.

For the valve, `Explosion.doExplosionB` has already dropped the valve item before
calling `onBlockExploded`, and the override never removes the block: the valve
**survives the explosion and also drops an item**.

Fix: call `super.onBlockExploded(world, x, y, z, explosion)` (after `breakTank`).

### 1.2 Fluid-container handling runs on the client
`GenericUtil.java:147-230`, called from `BlockValve.java:78` and `BlockTankFrame.java:149`

`PlayerControllerMP.onPlayerRightClick` calls `Block.onBlockActivated` client-side
as well as server-side. The `IFluidContainerItem` branch guards on `!world.isRemote`
(line 207); the `FluidContainerRegistry` branch (lines 153-201) does not. The client
fills/drains its client-side TE and mutates the inventory, causing desync, ghost
items and fluid that disappears on the next sync.

Fix: early-return on `world.isRemote` in `fluidContainerHandler`.

### 1.3 NBT is not robust
`TileEntityValve.java:877-940`, `TileEntityTankFrame.java:133-164`

- `new FluidStack(null, amount)` throws `IllegalArgumentException` when a fluid mod
  is removed. Use `FluidStack.loadFluidStackFromNBT` / `writeToNBT` (also preserves
  the FluidStack NBT tag, which is currently lost).
- `getIntArray(...)[0]` throws on missing/legacy/corrupt tags (`masterValve`,
  `bottomDiagF`, and `topDiagF` is not even checked for existence).
- Fluid amounts are not clamped to `[0, capacity]` on load.
- `writeToNBT` writes `masterValve` / `valveX/Y/Z` only if the master TE is loaded;
  if not, the link is lost, and `getMaster()` during saving can force a chunk load.
  Persist the already-known `masterValvePos` / `valvePos`.
- `readFromNBT` skips `masterValve` when a master is currently cached, so a client
  `S35` packet cannot re-point a valve. Read it unconditionally for non-masters.
- Tank frames store the numeric block ID; after a mod is removed/reordered the
  frame can render/drop the wrong block. Store the registry name and handle missing
  mappings.

### 1.4 `breakTank` loses the fluid
`TileEntityValve.java:753-786` (line 762)

`setValid(false)` runs before `valve.fluidStack = this.getFluid()`, and
`getFluid()` returns `null` when `isValid` is false. Every other valve gets `null`.
Capture `this.fluidStack` before invalidating.

### 1.5 Null stored block → server NPEs after removing a mod
`FrameBlockAccessWrapper.java:46-57`, `BlockTankFrame.java:63-206`,
`GenericUtil.java:110-131`, `ExtendedBlock.java:24-51`

`TileEntityTankFrame.readFromNBT` can produce an `ExtendedBlock` with a null block.
These dereference it unchecked:

- `FrameBlockAccessWrapper.getBlockMetadata` checks `exBlock.getBlock() != null` but
  not `exBlock != null` (NPE every render frame).
- `BlockTankFrame.removedByPlayer`, `getPlayerRelativeBlockHardness`, `getPickBlock`,
  `getFlammability`, `getExplosionResistance`.
- `GenericUtil.isValidTankBlock` and `setupTank` (server crash on rebuild/chunk load).
- `ExtendedBlock.equals` / `equalsIgnoreMetadata` / `toString`.

### 1.6 Leak logic crashes on hardness 0 or −1
`TileEntityValve.java:316-326`

`hardness <= 1.0F` accepts `0.0F` and unbreakable `-1.0F` (and `NaN`).
`random.nextInt(0)` / `nextInt(-100)` throws `IllegalArgumentException` on the
server thread. Clamp to at least 1 and reject non-positive/NaN.

### 1.7 Capacity integer overflow
`TileEntityValve.java:604-607`, `FancyFluidStorage.java:60-65`

`fluidCapacity` is an `int`; `mbPerVirtualTank` accepts up to `Integer.MAX_VALUE`
(the `> Integer.MAX_VALUE` check is always false). A large config overflows to a
negative capacity, poisoning `fill`, the comparator and the renderer. Compute with
`long` and saturate, and validate the config.

### 1.8 `setWorldOnFire` does not stop fire
`TileEntityValve.java:270`, `276`, config at `FancyFluidStorage.java:93-95`

The option only gates `playSoundEffect`; `Blocks.fire` placement and frame burning
happen unconditionally. Gate the burn/fire block (or rename the option).

### 1.9 `canFill` unit bug
`TileEntityValve.java:1161-1165`, `1098-1101`

`getFillPercentage()` returns 0–100 but the expression multiplies it by
`getTankHeight()`, making the right side ~100× too large. `canFill` is effectively
always true. Use a 0–1 fraction. The `valveHeightPosition > getTankHeight()` clause
and `canAutoOutput`'s unused `tankHeight` parameter are also wrong/dead.

### 1.10 `GuiValve` NPE with a null valve
`GuiHandler.java:25-27`, `GuiValve.java:91-116`

A frame whose master valve is not loaded yields `new GuiValve(null, true)`;
`drawScreen` dereferences `this.valve.getFluid()` → client crash. Guard the handler
and the GUI.

### 1.11 Renderer crash / invisible unformed valve — fixed in batch 11

`TankFrameRenderer` returned `true` from a render pass even when it emitted no
vertices (for example an opaque frame checked during pass 1). `WorldRenderer`
then marked the pass as visible and called `Tessellator.getVertexState` with an
empty buffer, which crashes in `PriorityQueue`. It now returns the actual render
result and skips passes that cannot draw the stored block.

`BlockValve.getIcon` also returned the transparent `tankValve`/`tankMaster`
overlay for **unformed** valves, so the solid body disappeared. Unformed valves
now return the solid `blockValve` body, and `ValveRenderer` draws the accent as
an overlay, matching the old two-pass render.

Fixed in batch 12: newly created `TileEntityTankFrame`s were initialized only on
the server, so clients had no stored block and the shell rendered as nothing after
formation. `initialize()` now marks the frame for an S35 description-packet sync;
the renderer also draws a stone fallback in pass 0 if the TE has not arrived yet.

---

## 2. P1 — correctness

### Tank formation / ownership
- **Edge valves are rejected, not destroyed** (`TileEntityValve.java` validation): the
  original review suspected the `maps[0]` loop would replace a valve with a
  `blockTankFrame`, but `isValidTankBlock` / `areTankBlocksValid` reject a raw valve
  first, so the tank simply fails to form. The master valve also cannot land on an
  edge when `calculateInside` succeeds (the interior start would be solid). The real
  bugs were the ownership cases below.
- **`length[]` was never reset between `calculateInside` calls**
  (`TileEntityValve.java:460-480`); directions with no wall kept stale values, and
  the only validity check was `length[DOWN] != -1`. Fixed by `TankGeometry`, which
  allocates a fresh array and requires a wall in all six directions.
- **`MAX_SIZE` was off by 3**: the old check was `lengthW + lengthE > maxSize`, but the
  full width is `lengthW + lengthE + 3`. `TankGeometry` enforces the documented
  whole-tank size (`+ 3 <= maxSize`). Note: existing tanks larger than the configured
  limit are now rejected on rebuild; default 13 means a maximum 13-wide axis (the old
  code allowed 16).
- **Shared walls stole frames**: `setupTank` called `setValvePos` on any
  `TileEntityTankFrame`, including another tank's. `isOwnedByOther` now rejects a frame
  owned by a different, still-valid tank.
- **Second masters were not demoted**: a foreign master valve in `maps[1]` was added
  to `otherValves` and kept `isMaster = true`. `setupTank` now rejects a foreign
  master, and absorbed slaves are demoted.
- **Stale slaves stayed valid after a rebuild**: `buildTank` now invalidates the old
  `otherValves` before clearing the lists.
- **No interior invalidation**: placing a block inside a valid tank did not break it.
  `BlockTankFrame`/`BlockValve` now forward neighbour changes (both
  `onNeighborChange` and `onNeighborBlockChange`) and the master breaks the tank when
  an interior position becomes non-air.
- **`valveHeightPosition` was not persisted** while `isValid`/`tankHeight` were; for
  20 ticks after a chunk load valves acted as bottom valves. It is now saved/loaded.
- **Chunk-load loop was wrong** (`TileEntityValve.java:122-140`): it loaded
  `top + x` for `x ∈ [0, top − bottom]`. It now iterates the min/max chunk rectangle.


### Fluid logic
- **Fixed (batch 1):** `drain(ForgeDirection, FluidStack, boolean)` ignored the fluid
  type and drained `resource.amount` of whatever was in the tank. It now requires a
  matching fluid.
- **Fixed (batch 2):** `fill` spilled into adjacent tanks when full; the forwarding
  loop was removed, so a full tank returns 0.
- **Fixed (batch 2):** auto-output called `fill` twice; it now performs one real fill
  and drains the accepted amount.
- **Fixed (batch 1):** `getTankInfo` returned `null` for invalid tanks; it now returns
  an empty array.
- **Fixed (batch 2):** the dead full-tank loop in `canFillIncludingContainers` was
  removed; a full tank simply rejects further fill.
- **Partially fixed (batch 2):** rain no longer multiplies per top-level valve (one
  collection per tank per tick). The roof/sky-column check and the per-tick
  `getPrecipitationHeight` cost remain for batch 3.
- **Fixed (batch 2):** negative-density fluids now auto-output and obey the mirrored
  fill-height rule. The renderer still draws the full interior with a uniform alpha
  (batch 4).
- **Fixed (batch 1):** `getComparatorOutput` now returns 0–15.
- **Fixed (batch 2):** `fluidIntake` / `fluidOuttake` / `rainIntake` were removed.
  `getFluidLuminosity` remains unused (feature) and `ANONYMOUS_STATISTICS` remains a
  dead telemetry config option (batch 5).

### Auto-output / GUI / peripherals
- **Fixed (batch 2):** auto-output is now tank-wide; toggling a slave valve's GUI
  changes the master's flag and the toggle shows the master's state.
- **Fixed (batch 2):** `equals(IPeripheral)` returns `other == this`.
- OC `invoke` returns `null` for unknown methods; throw/return an error.
- **Fixed (batch 2):** `RiMEventHandler.tileRotation` now calls
  `getInside().getRotation(event.axis)` once instead of rotating around the axis
  twice. Verify the RiM mod id.
- **Fixed (batch 3):** packet handlers are scheduled onto the server thread via
  `ServerTaskQueue` (`ServerTickEvent`) and validate reach + valve-name length before
  touching the world. OC `invoke` still returns `null` for unknown methods.

### Rendering
- **Fixed (batch 4):** `ValveRenderer.renderInventoryBlock` used mismatched face
  methods (bottom twice, missing west, wrong normals). It now renders all six faces
  with the matching `renderFace*` method and normal.
- **Fixed (batch 4):** `renderWorldBlock` duplicated/mixed faces and z-fought. The
  block is rendered once; `BlockValve.getIcon` supplies the master/slave accent for
  the inside face and the base texture for the other faces.
- **Fixed (batch 4):** `ForgeHooksClient.setRenderPass(pass)` inside
  `canRenderInPass` was removed; the valve renders only in pass 0, and the frame
  renderer reads the pass that `EntityRenderer` already sets.
- **Fixed (batch 4):** degenerate `addVertexWithUV` calls were removed from both
  renderers.
- **Fixed (batch 4):** `preGL`/`postGL` now save/restore `GL_ALL_ATTRIB_BITS`, and the
  gas alpha is set before drawing.
- **Fixed (batch 4):** negative-density fluids use the same partial-column math as
  normal fluids (no more full-volume draw).
- **Fixed (batch 4):** GUI scissor uses `GuiScissor` (correct Y flip, output scale,
  bottom-aligned fill) instead of mixing `getScaleFactor()` with `gameSettings.guiScale`.
- **Fixed (batch 3):** the tank-frame wrapper is reused; **fixed (batch 4):** renderer
  exceptions are logged once per failing block instead of failing silently.
- **Fixed (batch 4):** the valve's `getLightOpacity()` is 255 (no more light leak),
  and `getRenderBoundingBox` returns a small box for slaves and an inflated box for the
  master.
- **Fixed (batch 7):** GUI strings are localized, the name packet is only sent when
  changed, Escape/unhandled keys behave correctly, and fluid/capacity reads are
  cached. `BlockTankFrame`'s fallback still renders stone but now logs once per block.

---

## 3. P2 — performance

1. **`TileEntityTankFrame` ticks every tick.** It does not override `canUpdate()`, so
   every frame TE in a large tank (`~1,500–5,800`) is ticked every tick to test a flag
   that is almost never set. **Fixed:** `canUpdate()` returns false; the dead
   `wantsUpdate`/`updateEntity` path was removed.
2. **Packet storm on rebuild.** `setupTank` → `updateBlockAndNeighbors()` →
   `markForUpdate(false)` marks **every frame** for update; `markBlockForUpdate` sends
   one `S35` TE packet per frame. **Fixed:** frames are only marked when their valve
   link changes; the per-tick init update was removed; fluid sync is throttled to one
   packet per 2 ticks; new frames use the packet already queued by `setBlock`.
3. **`getMaster()` is uncached** whenever `masterValvePos != null`; `isValid()`,
   `getFluid()`, `getCapacity()` call it repeatedly in hot paths. **Fixed:** the master
   is cached (invalidated when the TE is invalid), and `isValid`/`getFluid`/
   `getCapacity` perform one lookup.
4. **`getPrecipitationHeight` per tick per water valve** — **fixed:** cached per valve
   for 20 ticks.
5. **Full-cuboid maps.** `getBlocksBetweenPoints` allocated a `Position3D` +
   `ExtendedBlock` for every block in the cuboid, and `TileEntityValve.maps` retained
   all three maps for the tank's lifetime although only `maps[2]` is read later.
   **Fixed:** `TankStructure` performs one direct scan; shell cells go into two lists
   and the interior is a packed `LongSet` (relative coordinates). `getBlocksBetweenPoints`
   and `getTankFrame` were removed.
6. **Fluid renderer overdraw.** The bottom face was emitted for **every** height layer
   (`xSize × zSize × height` quads). **Fixed:** it is emitted only for `rY == 0`; the
   side faces were already boundary-only.
7. **Per-tick allocations / RNG**: **partially fixed** — burn/leak use `worldObj.rand`,
   the tank-frame wrapper is reused, fluid sync is throttled. Some short-lived lists
   remain in the build/hazard paths.
8. **Block update flags**: edge conversion now uses flag 2 instead of 3 (no neighbour
   updates for immutable frames).

### Tank size after the performance work

After batch 3 the per-tank costs are:

- **Tick:** frame TEs do not tick; only valves tick (auto-output/rain/hazards).
- **Memory:** ~8 bytes per interior position (`LongSet`), plus one TE per shell block.
- **Build/load:** one O(volume) direct scan (`TankStructure`), and one packet per
  newly formed frame (no repeat storm on reload).
- **Render:** O(surface area) quads (top + four walls), not O(volume).

Going from a 13-wide axis to 16 raises volume by ~1.9× and shell area by ~1.5×;
13→21 raises volume by ~4.2× and shell area by ~2.6×. Since batch 2 made `MAX_SIZE`
the true whole-tank limit, a default of **16** is defensible as a follow-up: it
restores the old effective 16-wide limit (the pre-batch-2 code allowed up to 16 with
the default 13) while keeping the documented semantics. Larger defaults (21/32)
should stay opt-in because memory, build time and formation packets grow with volume.
Recommendation: ship the performance work first, profile a 16-wide tank, then raise
the default in a separate change (and keep 32 as the opt-in maximum). **Shipped in
batch 5:** the default is now 16; 32 remains the opt-in maximum. Batch 6 removed the
transient cuboid scan, so a future 21 default is easier to justify, but it should
still be a deliberate server-owner choice because frame count/memory grow with volume.


---

## 4. P3 — refactors / maintainability

- **Fixed (batch 6):** `TankStructure` (direct scan), `TankFluidHandler` (fill/drain
  rules), `TankHazards` (rain/burn/leak) and `ValveCommands` (shared CC/OC semantics)
  were extracted from `TileEntityValve`.
- **Fixed (batch 8):** `ValveState`/`ValveNbt` own the save/load and compact client
  sync, and `AutoOutputState`/`TankAutoOutput` own the flag, sync throttle and
  neighbour push.
- **Fixed (batch 10):** `LegacyMappings` handles Forge's `FMLMissingMappingsEvent` for
  worlds saved with the old uppercase `FFS` mod id. Old block and ItemBlock names are
  remapped to the current blocks at their old numeric ids, so existing tanks survive
  the `FFS` -> `ffs` rename; the old `tileEntityValve`/`tileEntityTankFrame` ids were
  already load aliases.
- **Fixed (batch 9):** `TankOwnership` owns master/slave links and valve lists,
  `TankBuilder` owns formation/teardown (`TankStructure`-based), and
  `ValveCommandAdapter` owns the shared CC/OC argument handling. `TileEntityValve`
  is now ~700 lines and is effectively NBT/fluid/hazard/ownership coordination.
  Remaining optional polish: `TankReadout`/formatting helpers, `Position3D` finals,
  and broader world-level tests.
- **Fixed (batch 6):** `Map<Position3D, ExtendedBlock>[]` was replaced by typed shell
  lists plus a packed `LongSet` interior.
- Config: capacity is still saturated `int` (the 1.7.10 `IFluidTank` API is `int`);
  `MAX_SIZE` includes the shell and the default is 16; dead options (`ANONYMOUS_STATISTICS`)
  remain.
- NBT helpers; persist authoritative positions rather than resolving TEs. **Done.**
- `ExtendedBlock` uses `Block` identity + `hashCode` and tolerates null. **Done.**
- `Position3D` fields `final`; rename `getDistance` to `delta`.
- Use `@Override` consistently, `final` where possible.
- Optional mod references: `@Optional.Method` + separate compat classes; move
  client-only `IIcon`/renderer references out of `CommonProxy`.
- Log build failures as well as renderer exceptions.

---

## 5. Build / packaging / compatibility

- **Fixed (batch 5):** the mod ID, `@Instance`, resource domain, textures, GUI,
  lang and sounds now all use lowercase `ffs` (`ModNames` is the single source of
  truth). The lowercase resource domain fixes missing assets on Linux/macOS.
- **Fixed (batch 5):** `mcmod.info` lists optional mods as `after:` dependencies and
  includes RemainInMotion (`JAKJ_RedstoneInMotion`).
- **Fixed (batch 5):** optional integrations are `compileOnly` +
  `runtimeOnlyNonPublishable` + `testImplementation`; they are no longer published as
  hard runtime dependencies.
- **Fixed (batch 5):** tile entities are saved as `ffs:tileEntityValve` /
  `ffs:tileEntityTankFrame` with the legacy ids registered as load-time alternatives,
  so existing worlds keep working.
- **Fixed (batch 5):** `BlockTankFrame` has a block name and a lang entry.
- **Fixed (batch 5):** `MAX_SIZE` now defaults to **16** (whole tank including the
  frame), restoring the pre-review effective 16-wide default. 32 remains opt-in. A
  legacy `FFS.cfg` is copied to `ffs.cfg` once so existing settings survive the id
  change.
- **Retained upstream license (no change):** the work remains CC BY-NC-SA 3.0 as
  requested by the original author. `LICENSE.md` is byte-identical to upstream; do
  not relicense it. `NOTICE.md` records attribution, the upstream source, the license
  URI and the fact that this is a modified adaptation, and both files are included in
  the built jar.
- The JUnit 5 suite and `headlessTest` are now part of CI (`build` runs the tests).

---

## 6. What is good

- The tank volume/geometry classification is compact and correct in the common case.
- The deferred rebuild after chunk load is a sensible lag-avoidance idea.
- Optional integrations use Forge's `@Optional.Interface`/`@Method` correctly on the
  TE.
- Container handling is more complete than most 1.7.10 mods.
- Style is consistent and the feature set is fun.

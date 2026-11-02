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
| 3 | P2 performance: `canUpdate()=false` for frames, packet storm, master cache, map-free geometry, surface-only fluid renderer, wrapper reuse, precipitation cache, RNG reuse | planned |
| 4 | Renderer/GUI: face/normal fixes, render-pass global hack, scissor math, GL state restore, bounding boxes, lighting | planned |
| 5 | Build/packaging: `ffs` casing, resource domain, `mcmod.info`, optional dependency scopes, TE registry names, tests in CI | planned |
| 6 | Architecture: split `TileEntityValve` into structure/fluid/hazards/peripheral classes; shared CC/OC command layer | planned |

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
- Packet handlers do not validate distance/permissions and the valve name has no
  length limit. They also run on the Netty pipeline; schedule world access onto the
  server thread.

### Rendering
- `ValveRenderer.renderInventoryBlock` uses mismatched face methods (bottom twice,
  missing west, wrong normals); `renderWorldBlock` duplicates/`mixes` faces
  (`YNeg` twice, `YPos`+`ZNeg` for Z, `ZPos`+`XPos` for X).
- `ForgeHooksClient.setRenderPass(pass)` inside `canRenderInPass` mutates global state
  during chunk meshing; remove it and read `MinecraftForgeClient.getRenderPass()`.
- Degenerate `addVertexWithUV` calls pollute the chunk buffer; remove.
- `preGL`/`postGL` only save `GL_ENABLE_BIT`; blend/color/depth/texture state leaks.
- Negative-density alpha is set after the vertices are added.
- GUI scissor mixes `getScaleFactor()` with `gameSettings.guiScale` and does not flip
  Y; wrong for auto GUI scale.
- `TankFrameRenderer` allocates a wrapper per block per pass; reuse it. It also
  silently renders stone on exceptions; log them.
- `BlockValve.isOpaqueCube() == false` gives the valve `lightOpacity = 0` (light
  leaks through a solid metal block).
- `getRenderBoundingBox` returns the huge tank AABB for slaves too; return a small box
  for non-masters and inflate the master's by 1.

---

## 3. P2 — performance

1. **`TileEntityTankFrame` ticks every tick.** It does not override `canUpdate()`, so
   every frame TE in a large tank (`~1,500–5,800`) is ticked every tick to test a flag
   that is almost never set. Override `canUpdate()`.
2. **Packet storm on rebuild.** `setupTank` → `updateBlockAndNeighbors()` →
   `markForUpdate(false)` marks **every frame** for update; `markBlockForUpdate` sends
   one `S35` TE packet per frame. On chunk load a large tank sends thousands of packets.
   Initialise frames before they are synced and mark only new/changed frames.
3. **`getMaster()` is uncached** whenever `masterValvePos != null`; `isValid()`,
   `getFluid()`, `getCapacity()` call it repeatedly in hot paths. Cache and invalidate.
4. **`getPrecipitationHeight` per tick per water valve** — cache it.
5. **Full-cuboid maps.** `getBlocksBetweenPoints` allocates a `Position3D` +
   `ExtendedBlock` for every block in the cuboid, and `TileEntityValve.maps` retains
   all three maps for the tank's lifetime although only `maps[2]` is read later.
   Replace with a `TankStructure` of coordinate lists/bitsets.
6. **Fluid renderer overdraw.** Four side quads + a bottom quad per interior cell
   (~108k quads for a 30³ interior). Draw only the top surface, the four outer walls
   and one bottom quad.
7. **Per-tick allocations / RNG**: `new Random()`, new `ArrayList`s and `FluidStack`s
   in hot paths; use `worldObj.rand` and cached buffers.
8. **Block update flags**: `setupTank` uses flag 3 (neighbour updates) for every edge
   block; flag 2 is enough for the shell.

---

## 4. P3 — refactors / maintainability

- Split `TileEntityValve` (1,415 lines) into `TankStructure`, `TankFluidHandler`,
  `TankEnvironmentHandler`, `ValvePeripheral` and a thin block entity.
- Replace `Map<Position3D, ExtendedBlock>[]` with a typed structure.
- Config: `long` capacity, one validation path, MAX_SIZE includes the shell, remove
  dead options.
- NBT helpers; persist authoritative positions rather than resolving TEs.
- `ExtendedBlock` should use `Block` identity + `hashCode` and tolerate null.
- `Position3D` fields `final`; rename `getDistance` to `delta`.
- Use `@Override` consistently, `final` where possible.
- Optional mod references: `@Optional.Method` + separate compat classes;
  move client-only `IIcon`/renderer references out of `CommonProxy`.
- Log build failures and renderer exceptions instead of silently rendering stone.

---

## 5. Build / packaging / compatibility

- **Mod ID case mismatch**: `gradle.properties` uses `modId = ffs`; the code uses
  `"FFS"` / `@Instance("FFS")` and `mcmod.info` substitutes `"modid": "ffs"`.
  Use lowercase `ffs` everywhere.
- **Resource domain case**: textures/GUI/lang/sounds use `"FFS:"` but the folder is
  `assets/ffs/`. `ResourceLocation` does not lowercase the domain, so these assets
  are missing on case-sensitive filesystems (Linux/macOS clients). Use `"ffs:"`.
- `mcmod.info` lists optional mods as hard dependencies; use `after:` and add RiM.
- `dependencies.gradle` uses `implementation` for optional integrations; use
  `compileOnly` + `runtimeOnlyNonPublishable` / `devOnlyNonPublishable`.
- Namespace TE registry IDs (`ffs:tileEntityValve`).
- `BlockTankFrame` has no `setBlockName` and a registered ItemBlock with a valve
  texture; hide it or name it.
- License is CC BY-NC-SA 3.0 (per `LICENSE.md`), which is unusual for code (no
  patent grant, non-commercial restriction). Consider MIT/Apache-2.0.
- No tests existed before this work; add the JUnit 5 suite and keep it in CI.

---

## 6. What is good

- The tank volume/geometry classification is compact and correct in the common case.
- The deferred rebuild after chunk load is a sensible lag-avoidance idea.
- Optional integrations use Forge's `@Optional.Interface`/`@Method` correctly on the
  TE.
- Container handling is more complete than most 1.7.10 mods.
- Style is consistent and the feature set is fun.

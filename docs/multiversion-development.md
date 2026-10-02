# Multi-version development

Minecraft 1.18.2 is Create: Railward's canonical development target. New
features are designed, implemented, and fully verified there first. The
Minecraft 1.19.2 and 1.20.1 builds are additional ports and must not reduce or
delay the functionality of the 1.18.2 build.

## Build layout

Stonecutter owns the version projects under `versions/`. The checked-in source
tree remains under `src/`; generated version workspaces and build products must
not be edited directly.

The project deliberately uses Stonecutter 0.7.11 with Gradle 8.8. Newer
Stonecutter releases require Gradle 9, while the current ForgeGradle setup still
uses Gradle 8.8.

## Common tasks

- `gradlew compileAll` compiles every supported Minecraft version.
- `gradlew testAll` runs the unit-test suite against every supported version.
- `gradlew gameTestAll` starts Forge's dedicated GameTest server for every
  supported version and fails if a required in-game test fails.
- `gradlew buildAll` builds and tests every supported Minecraft version.
- `gradlew buildAndCollect` also copies the resulting jars to `build/libs`.
- `gradlew incrementPatchVersion` explicitly increments `mod_version.txt`.

Ordinary builds never modify `mod_version.txt`.

For disposable runtime tests, override the run directory so the primary
1.18.2 development world is never touched. On Windows PowerShell, quote the
property argument because the path contains version separators:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=.devdocs\tmp\smoke\1.18.2\server" -PrailwardConsoleLogLevel=info
```

On Linux/macOS:

```bash
./gradlew :1.18.2:runServer -PrailwardRunDirectory=.devdocs/tmp/smoke/1.18.2/server -PrailwardConsoleLogLevel=info
```

Use a different isolated directory for a separate client and for each Minecraft
version. The logging override is optional; ordinary development runs remain at
`debug` when it is omitted.

GameTests live in the separate `src/gameTest/` source set. They are available
to `runGameTestServer` but are not packaged in release JARs. Their default run
directories are already isolated at `.devdocs/tmp/gametest/<version>`.
Before the first local run, place `eula.txt` containing `eula=true` in each
version directory after reviewing and accepting the Minecraft EULA; an empty
`server.properties` also prevents a harmless missing-file error in the log.

Run the primary platform first:

```powershell
.\gradlew.bat :1.18.2:runGameTestServer -PrailwardConsoleLogLevel=info
```

After it passes, run all three versions with `gameTestAll`. The CI workflow
prepares disposable directories and runs this full set automatically.

CC-01 is an opt-in extension of that suite. The property below adds the pinned
full CC:Tweaked artifact only to the GameTest runtime and registers one
additional station-peripheral test; ordinary development and release runtime
dependencies remain unchanged. Run the canonical version first in an isolated
directory, then all ports:

```powershell
.\gradlew.bat :1.18.2:runGameTestServer "-PrailwardRunDirectory=.devdocs\tmp\computercraft\cc-01-<unique-id>\1.18.2" -PrailwardConsoleLogLevel=info -PrailwardCcTest=true
.\gradlew.bat gameTestAll -PrailwardConsoleLogLevel=info -PrailwardCcTest=true
```

The opt-in run must report 26/26 tests. Its additional test builds and assembles
a real Create station train, instantiates the CC:Tweaked station peripheral,
and validates `getStationId`, `getArrivals` (including limit validation and the
selected physical station UUID), and `getTrainSchedule`. Also retain a passing
ordinary 1.18.2 GameTest run without `-PrailwardCcTest=true`; it reports 25/25
and proves that Railward still loads when CC:Tweaked is absent.

The canonical SAVE-01 persistence probe is a separate two-process dedicated
server check. Use a new disposable directory, accept the Minecraft EULA there,
and run both commands against exactly the same directory. Each phase starts its
own conditional GameTest after the server reaches `Done`, logs a SAVE-01 PASS,
flushes the world and stops the server automatically:

```powershell
$save01Dir = ".devdocs\tmp\persistence\save-01-<unique-id>\1.18.2"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$save01Dir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=prepare
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$save01Dir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=verify
```

The prepare phase assembles a marked train and saves it with 96 iron ingots,
1500 mB of water, a station association, backwards direction, an active
schedule, a non-default throttle, a reversed modern controller and TYPE_3
buffer lights. The verify phase starts a fresh JVM and checks Create SavedData,
the restored schedule and authoritative direction, carriage entity, mounted
bulk-cargo controller and fill level, and mounted storage quantities.

NET-01 extends that same world with two real, sequential dedicated-client
connections. Start the server in one PowerShell terminal and leave it running:

```powershell
$save01Dir = ".devdocs\tmp\persistence\save-01-<same-unique-id>\1.18.2"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$save01Dir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=network
```

Then run these commands one after another in a second terminal. Each client
must log `Railward NET-01 client probe passed`, disconnect and exit successfully.
The server must then log its NET-01 PASS and stop itself:

```powershell
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\persistence\save-01-<same-unique-id>\client-1" -PrailwardClientProbe=true
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\persistence\save-01-<same-unique-id>\client-2" -PrailwardClientProbe=true
```

The client probe validates the restored train metadata and carriage entity,
server-authoritative backward direction, the client-visible mounted cargo
controller, routed capacity, fill level, item type and amount, cistern fluid
type and amount, reversed controller state and buffer light variant. Without
`railwardPersistenceTestPhase`, the conditional server
tests are not registered and the ordinary suite remains at 25 tests. Accepted
server phases are `prepare`, `verify`, `network`, `network_multi`, `controller`,
`scheduled`, `scheduled_route`, `signal`, `coupler_prepare`, `coupler`,
`coupler_auto_prepare`, `coupler_auto`, `coupler_storage_prepare` and
`coupler_storage`, plus `cargo_unloading`; any other value fails Gradle
configuration.
`railwardClientProbe` is likewise opt-in, so
ordinary client runs do not include `src/clientTest/`. Every probe-enabled
`runClient` also preserves the selected isolated directory's existing options
while forcing `soundCategory_music:0.0`, so concurrent probes do not play
overlapping music.

NET-02 reuses a world that has passed SAVE-01, but runs two dedicated clients
at the same time. Start the server in one terminal:

```powershell
$save01Dir = ".devdocs\tmp\persistence\save-01-<same-unique-id>\1.18.2"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$save01Dir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=network_multi
```

After the server reaches `Done`, start each of the following commands in its own
terminal so that both clients are connected concurrently:

```powershell
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\persistence\save-01-<same-unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=multi_client -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\persistence\save-01-<same-unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=multi_client -PrailwardClientProbeName=RailwardObserver
```

Both clients first capture the persisted backward state. The server then makes
one authoritative live update and moves the carriage a bounded distance. Both
clients must independently observe the movement, forward direction, forward
idle controller, TYPE_4 buffer lights, the mounted cargo controller remaining
valid while its contents change from 96 to 112 iron ingots and fill level from
1 to 2, 2000 mB of water and
the train door changing from open at the station to closed while moving,
log a NET-02 PASS and exit successfully. The server passes only after both
probes disconnect. Coupler topology is covered by the separate COUPLE-01 probe
below; transient sound/particle effects from the full NET-02 row remain manual.

COUPLE-01 uses its own minimal two-train world. In a fresh isolated directory,
accept the EULA and run the fixture phase first:

```powershell
$couplerDir = ".devdocs\tmp\coupler\couple-01-<unique-id>\1.18.2"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$couplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler_prepare
```

The fixture phase assembles and saves two separate one-carriage trains with
their couplers one block apart, then stops itself. Set `online-mode=false` only
in this disposable directory's `server.properties`. Start the saved world and
two clients concurrently from three terminals:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$couplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-01-<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-01-<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler -PrailwardClientProbeName=RailwardObserver
```

The server invokes the production coupler interaction path once on an outer
coupler and once on the resulting inner coupler. Both clients must independently
observe `2 x 1 carriage -> 1 x 2 carriages -> 2 x 1 carriage`, including the
retained train UUID on merge, a new UUID on split, carriage order, graph and
entity `trainId`/`carriageIndex` bindings. Require all three pass markers,
successful exits and no crash report. Actual coupler-model appearance still
requires visual inspection.

COUPLE-02 uses a separate four-pair world. In a fresh isolated directory,
accept the EULA and prepare the fixture:

```powershell
$autoCouplerDir = ".devdocs\tmp\coupler\couple-02-<unique-id>\1.18.2"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$autoCouplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler_auto_prepare
```

The preparation phase saves eight independent one-carriage trains. Set
`online-mode=false` only in that disposable server directory, then start the
server and both clients concurrently from three terminals:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$autoCouplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler_auto
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-02-<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler_auto -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-02-<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler_auto -PrailwardClientProbeName=RailwardObserver
```

The server exercises four production auto-coupling cases: disabled, above the
configured speed limit, permitted westbound and permitted eastbound. Both
clients must independently observe
`8 x 1 -> 6 x 1 + 1 x 2 -> 4 x 1 + 2 x 2`, with the two rejected pairs still
separate, retained identities on both merges and valid carriage entity
bindings. Require both client pass markers, the server pass marker, successful
exits and no crash report.

COUPLE-03 uses two two-carriage trains with a different item/fluid amount in
every carriage and one passenger seat on each train. In a fresh isolated
directory, accept the EULA and prepare the fixture:

```powershell
$storageCouplerDir = ".devdocs\tmp\coupler\couple-03-<unique-id>\server"
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$storageCouplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler_storage_prepare
```

Set `online-mode=false` only in that disposable server directory. Start the
server and the first two clients concurrently:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=$storageCouplerDir" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=coupler_storage
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-03-<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler_storage -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-03-<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler_storage -PrailwardClientProbeName=RailwardObserver
```

After `RailwardObserver` logs its pass and exits, keep the driver running and
start the reconnect client:

```powershell
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\coupler\couple-03-<unique-id>\rejoin" -PrailwardClientProbe=true -PrailwardClientProbeMode=coupler_storage -PrailwardClientProbeName=RailwardRejoin
```

The server and the first two clients must observe `2 x 2 -> 1 x 4 -> 2 x 2`
while retaining all four carriage entity UUIDs, two passenger UUIDs and seat
bindings, and the four item/fluid signatures. The server also requires the
paused schedule created on the combined train to survive on exactly one split
train. `RailwardRejoin` must receive the named final topology, all storage and
both passengers. Require all four process pass markers, successful exits and
no crash report.

CTRL-01/CTRL-02 uses a saved 1.18.2 world containing the assembled modern
controller (the verified NET-02 fixture is suitable). Copy it to a fresh
isolated directory, start a dedicated server with
`-PrailwardPersistenceTestPhase=controller`, then start two clients
concurrently from separate directories:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=.devdocs\tmp\controller\<unique-id>\1.18.2" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=controller
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\controller\<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=controller -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\controller\<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=controller -PrailwardClientProbeName=RailwardObserver
```

Run each command in its own terminal. The driver sends real reverser packets;
the server checks allowed stopped changes and denied moving, throttling and
braking changes. Both clients independently check synchronized control states
and movement, while the driver checks the allowed/denied prompts. A valid pass
requires both client pass markers, the server pass marker, successful exits and
no server crash report. This is not a visual inspection of lever animation.

CTRL-03 needs a fresh SAVE-01 fixture made with the current test sources: its
assembled train includes a Create conductor seat glued to the modern controller,
and the track includes a named destination station. Run the `prepare` and
`verify` phases above in a new isolated directory, then copy the verified server
directory to another disposable directory. Set `online-mode=false` only in that
disposable server copy and start it with two clients concurrently:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=.devdocs\tmp\scheduled-route\<unique-id>\1.18.2" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=scheduled_route
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\scheduled-route\<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=scheduled_route -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\scheduled-route\<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=scheduled_route -PrailwardClientProbeName=RailwardObserver
```

Run each command in its own terminal. The server seats a real NPC at the
captured conductor seat, verifies that Create can calculate the destination
path, and submits a real non-cyclic destination schedule. Require a CTRL-03
route PASS from both clients and the server, successful exits and no crash
report. The probe covers autonomous departure, movement, braking, arrival,
scheduled station delay, return to idle and synchronized controller direction.
Visual direction-dependent light colors still require manual inspection. The
older `scheduled` mode remains available as a narrower forced-motion diagnostic,
but it is not the CTRL-03 acceptance probe.

SIGNAL-01 also needs a fresh SAVE-01 fixture made with the current test
sources. Pass its `prepare` and `verify` phases, copy that verified directory
to a disposable server directory and set `online-mode=false` only in the copy.
Then start the server and two clients concurrently from separate terminals:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=.devdocs\tmp\signal\<unique-id>\1.18.2" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=signal
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\signal\<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=signal -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\signal\<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=signal -PrailwardClientProbeName=RailwardObserver
```

The server seats a real NPC and runs outbound and return schedules through
two-sided 20/60 km/h speed signs, whistle signs and a fixed-aspect Create
signal. Require the server and both clients to report all four cab-signal
aspects (`WHITE`, `GREEN`, `YELLOW`, `YELLOW_RED`), both synchronized throttle
limits, forward and backward movement, exactly two independent honk events and
the complete round trip. All three processes must exit successfully and no
crash report may exist. The clients mute music automatically. The probe does
not replace human inspection of the cab-signal/sign models or the spatial
quality of the whistle audio.

CARGO-03 uses a fresh isolated 1.18.2 server directory. Accept the EULA, set
`online-mode=false` only in that disposable directory, then start the server and
two clients concurrently from separate terminals:

```powershell
.\gradlew.bat :1.18.2:runServer "-PrailwardRunDirectory=.devdocs\tmp\cargo\<unique-id>\1.18.2" -PrailwardConsoleLogLevel=info -PrailwardPersistenceTestPhase=cargo_unloading
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\cargo\<unique-id>\driver" -PrailwardClientProbe=true -PrailwardClientProbeMode=cargo_unloading -PrailwardClientProbeName=RailwardDriver
.\gradlew.bat :1.18.2:runClient "-PrailwardRunDirectory=.devdocs\tmp\cargo\<unique-id>\observer" -PrailwardClientProbe=true -PrailwardClientProbeMode=cargo_unloading -PrailwardClientProbeName=RailwardObserver
```

The server waits for both clients to finish loading, then releases a direct
unloading hatch and an unloading-bunker fixture while leaving a third hatch
redstone-locked. It requires the direct hatch to transfer exactly 80 items, a
simulated bunker extraction to leave all 40 items untouched, an executed
13-item extraction to leave 27, and the blocked source/destination to remain
24/0. Both clients must independently observe synchronized falling-item state
and an active unloading loop for both transfers without a client-side
`ItemEntity`. Require all three pass markers, successful exits and no crash
report. The clients mute music automatically. Human inspection is still needed
for the appearance of the falling items and the spatial/start-stop quality of
the sound.

The maximum bulk-cargo test intentionally creates an `11x11x16` multiblock.
Development machines may log `Can't keep up!` while that test forms; judge the
run by the final required-test summary. A timeout, failed test or non-zero
Gradle exit remains a failure.

The mounted-storage tests use a captured `CarriageContraption` to exercise
Create's block capture/removal, mounted item/fluid storage, contraption NBT
round-trip and block restoration paths. An additional end-to-end test builds a
straight track, targeted station, bogey and controlled carriage at runtime and
uses Create's real station assembly/disassembly flow. It also advances the
assembled carriage on the track, verifies that the entity and travelling points
move without losing storage, and round-trips the production `Train` NBT. The
captured mounted handlers are placeholders until Create removes the assembled
blocks from the world and captures their capabilities. Bulk-cargo normalization
therefore runs after `Contraption.removeBlocksFromWorld()`, never merely when
`CarriageContraption.assemble()` returns. Manual `CARGO-02` remains required for
rendered fill-state inspection. The `FLUID-02` station test mounts Railward's
cistern hatch on that real carriage, transfers `1000 -> 1500 mB` through a
stationary fluid terminal, moves far enough to disconnect, returns and
reconnects, drains `250 mB` through the terminal, then verifies that station
disassembly restores exactly `1250 mB` to the world cistern. SAVE-01 and NET-01
cover the complementary fresh-JVM persistence and reconnect synchronization
paths.
The dedicated SAVE-01 probe covers the actual 1.18.2 world save and server
restart; its follow-up NET-01 clients cover initial late-join synchronization
and one reconnect for persisted schedule direction, controller, buffer lights,
cargo and fluid state. The concurrent NET-02 probe covers one server-authoritative
live state change and bounded movement as observed by a driver and observer in
separate client JVMs. The COUPLE-01 probe separately covers synchronized train
identity and carriage topology through a real merge/split cycle.

The COUPLE-02 probe additionally covers configuration and speed rejection plus
permitted approaches from both directions.

The COUPLE-03 probe covers a multi-carriage merge/split cycle with four distinct
item/fluid storage signatures, stable carriage and passenger identities, one
retained paused schedule after the split, and a fresh client reconnecting to the
final topology while the original driver remains connected.

The CARGO-03 probe covers exact server-authoritative unloading into static bulk
cargo, pull-only timed bunker extraction, redstone blocking, synchronized
falling-item state and active client audio in two independent client JVMs. It
also verifies that the visual path does not create client-side item entities.

The FLUID-01 GameTest coverage forms width-one cisterns at every supported
length from 1 through 11 on both horizontal axes. It additionally forms widths
1, 3 and 5 on both axes, checks every block face against the intended port
layout, and performs simulated and executed fill/drain operations through
different remote parts. Boundary tests cover the maximum `5x5x11` cistern,
unsupported width 2, over-limit length 12, incompatible fluids, partial fill,
split conservation and re-formation with valid controller references.

## Version-scoped Create workarounds

`create.FluidNetworkMixin` fixes a Create fluid-network defect in all three
currently pinned dependencies:

- Minecraft 1.18.2 / Create `0.5.1.i-435`;
- Minecraft 1.19.2 / Create `0.5.1.i-59`;
- Minecraft 1.20.1 / Create `0.5.1.j-55`.

The exact `FluidNetwork.java` sources inspected under
`.devdocs/tmp/create-sources/<version>` all contain the same loop: every
matching source tank is drained, but each result overwrites `transfer`, so only
the last drained portion reaches the output. The automated
`fluidNetworkConservesMultiTankSourceTransfer` regression invokes the real
Create `FluidNetwork.tick()` with two 40 mB source tanks and a 64 mB transfer
limit. On 2026-09-27, isolated negative runs with
`fixCreateFluidNetwork=false` reproduced the same defect on every version:
the source retained `0 mB`, the target received only `16 mB`, and that was the
only failed required GameTest. With the workaround enabled, every version
retained `16 mB`, delivered `64 mB`, conserved all `80 mB`, and passed all
24 required GameTests.

Therefore the mixin and its `PipeConnectionAccessor` must remain enabled for
all three pinned Create builds. Repeat both the source inspection and the
negative/positive regression whenever a version changes its Create dependency;
gate the mixin out if upstream gains the equivalent accumulation fix, because
double-applying an upstream correction is itself a compatibility risk.

## Support policy

- Minecraft 1.18.2: primary platform, complete feature set and full runtime
  verification.
- Minecraft 1.19.2: additional port on the Create 0.5.1 compatibility family.
- Minecraft 1.20.1: additional port on the Create 0.5.1.j compatibility family.

All configured versions must continue to compile on the main branch. Runtime
publication of an additional port may lag behind the primary 1.18.2 release.

Feature-level verification is tracked in
[`functional-test-matrix.md`](functional-test-matrix.md). Complete the 1.18.2
primary-platform rows before treating a feature as ready; use the reduced port
gate there for 1.19.2 and 1.20.1.

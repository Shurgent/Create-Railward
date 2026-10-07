# Functional test matrix

This matrix is the runtime acceptance checklist for Create: Railward. Minecraft
1.18.2 is the primary platform: every priority P0 row must pass there before a
feature is considered complete. Minecraft 1.19.2, 1.20.1/Create 5 and
1.20.1/Create 6 are ports and use the reduced gate described below.

## Result notation

- `PASS` — performed successfully, with a date and short evidence note.
- `PARTIAL` — a recorded subset passed, but the row still has required work.
- `FAIL` — reproducible defect; include an issue or log reference.
- `BLOCKED` — cannot currently be performed; state the reason.
- `NOT RUN` — no runtime claim has been made.

Store disposable worlds and logs below `.devdocs/tmp/`. Do not use the primary
1.18.2 `run/` world for automated or destructive checks.

## Automated baseline

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Clean compile, unit tests, reobfuscation and JAR collection | PASS (2026-10-04, `clean buildAndCollect`) | PASS (2026-10-04, `clean buildAndCollect`) | PASS (2026-10-04, `clean buildAndCollect`) | PASS (2026-10-04, `clean buildAndCollect`) |
| Dedicated GameTests: cargo/cistern persistence, limits, rules, capabilities, multiblock conservation, unloading, train assembly/movement/NBT and arrival-time selection/projection | PASS (2026-10-04, 25/25) | PASS (2026-10-04, 25/25) | PASS (2026-10-04, 25/25) | PASS (2026-10-04, 25/25; mounted-storage codec and station disassembly included) |
| Optional CC:Tweaked station peripheral: station identity, assigned arrivals, argument validation and schedule/runtime data | PASS (2026-10-07, opt-in 26/26; ordinary no-CC 25/25 retained) | PASS (2026-10-07, opt-in 26/26) | PASS (2026-10-07, opt-in 26/26) | PASS (2026-10-07, opt-in 26/26 against Create 6.0.8) |
| Two-JVM dedicated-server persistence: Create SavedData, carriage entity, station/direction/throttle/schedule, mounted controller/fill, 96 items and 1500 mB fluid | PASS (2026-09-27, prepare/save/stop/restart/verify) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated late join and reconnect: two sequential clients receive restored direction, controller/lights, mounted cargo controller/fill and cistern state | PASS (2026-09-27, both clients and server exited successfully) | NOT RUN | NOT RUN | NOT RUN |
| Two simultaneous dedicated clients receive one live server update, mounted cargo fill change and bounded train movement | PASS (2026-09-27, driver, observer and server exited successfully) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated controller: two clients observe forward/coast/brake/backward/release; driver packets test reverser guards and prompts | PASS (2026-09-21, `ctrl-20260921-h`: server, driver and observer passed without crash) | NOT RUN | NOT RUN | NOT RUN |
| Autonomous scheduled route with seated NPC: two clients observe departure, movement, braking, arrival delay and idle | PASS (2026-09-26, `ctrl03-route-20260926-c`: 32-block Create navigation; server, driver and observer passed without crash) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated coupling topology: two clients observe two one-carriage trains merge and split with valid identities/order/bindings | PASS (2026-09-26, `couple01-20260926-b`: server, driver and observer passed without crash) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated automatic coupling: disabled and over-speed pairs remain separate; permitted approaches from both directions merge for two clients | PASS (2026-09-26, `couple02-20260926-d`: server, driver and observer passed without crash) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated cargo unloading: exact direct and bunker transfers, redstone rejection, synchronized falling-item state and active audio on two clients | PASS (2026-09-27, `cargo03-20260927-c`: server, driver and observer passed without crash) | NOT RUN | NOT RUN | NOT RUN |
| Dedicated server reaches `Done` and Railward server-start handler runs | PASS (2026-09-19) | PASS (2026-09-19) | PASS (2026-09-19) | PASS (2026-10-04, isolated startup smoke) |
| Separate client completes Forge handshake and joins dedicated server | PASS (2026-09-19, `railward:main` accepted) | NOT RUN | NOT RUN | PASS (2026-10-04, isolated client joined the isolated dedicated server twice while exercising both Flywheel backends) |

The unit suite covers physical-station selection, malformed schedule filters,
world-time projection (including B3M modes), and wagon-frame serialized values.
The Create 6 client smoke on 2026-10-04 forced both `flywheel:instancing` and
`flywheel:indirect` against nearby kinetic blocks. Flywheel generated and
compiled a backend-specific fragment pipeline in each run; both dumped sources
contained Railward's projected-light uniforms and call site, and neither client
reported shader compilation or mixin errors. This validates integration with
both Flywheel 1 pipelines; the spotlight's in-game appearance was subsequently
verified by the manual checks recorded under `VISUAL-01`.
On the same date, a client initially crashed while Create 6 constructed the
virtual render world for an assembled migrated train containing a Railward wall
panel. After preventing client-side `BlockEntity#setChanged()` from querying a
real chunk in that virtual world, an isolated client joined a dedicated copy of
the reported world at the saved position beside the same train and remained
connected without the former `UnsupportedOperationException` or a crash report.
The subsequent Create 6 visual-compatibility smoke restored the current
contraption entity around `ContraptionEntityRenderer.render`, allowing mounted
buffer lights and the cab signal to consume their already-synchronized train
state again. It also aligned animated-controller lighting with Create 6 by
combining the virtual block's local light value with the real client level's
transformed light. An isolated client joined beside the migrated train and
rendered it without a mixin, renderer or client crash. Subsequent human checks
confirmed the buffer/cab lights and controller appearance and animation.
The dedicated GameTests add registry-backed server coverage for bulk-cargo
insertion/extraction and filtering, routed item capabilities, cargo/cistern
block-entity NBT persistence, both bulk-cargo horizontal orientations, exact
boundary and layered-fill block states, split/re-form controller recovery, all
three supported cistern widths, every cistern length from 1 through 11 on both
horizontal axes, maximum cargo and cistern dimensions, over-limit and incomplete
shapes, incompatible fluids, every permitted/forbidden cistern capability face,
routed simulated/executed fluid operations, and item/fluid conservation while
multiblocks form, split and re-form. Static unloading tests additionally
verify exact hatch-to-cargo transfer, incompatible-target and redstone rejection,
pull-only timed bunker extraction and synchronized transfer state. A captured carriage contraption
also verifies the mounted bulk-cargo controller, dimensions, routed capacity,
item/fluid mutation while mounted, cargo fill levels, contraption NBT
serialization and restoration into the disassembled world multiblock. A
separate end-to-end test builds a track, station, bogey and controlled carriage,
then verifies station assembly, the registered server-side train entity, mounted
Railward storage updates, short server-side movement of both the carriage entity
and its travelling points, production `Train` NBT write/read with storage
conservation, station disassembly and train deregistration. The suite therefore
fully covers the server-side `FLUID-01` criteria and partially automates
`CARGO-01`, `CARGO-02` and `FLUID-02`, but does not replace extended
player-controlled movement, item/fluid terminal transfer, networking, rendering
or gameplay checks below. The arrival-forecast GameTest uses distinct Create
`GlobalStation` identities to cover exact and wildcard filters, duplicate names,
renames and physical-platform assignment, then exercises production world-time
projection against a real `ServerLevel` with frozen daylight and an observed
two-times day-clock rate. The all-version evidence is in
`.devdocs/tmp/gametest/{1.18.2,1.19.2,1.20.1,1.20.1-create6}/logs/latest.log`.
The opt-in CC-01 test adds each version's pinned full CC:Tweaked artifact only
to the GameTest runtime. It assembles a real station train and invokes the
Mixin-extended Create station peripheral to verify the renamed physical
station UUID, assigned arrival row, limit handling, schedule identity/current
entry/state and Lua exposure. The canonical no-CC run passed separately, so
the integration remains optional rather than becoming a Railward runtime
requirement.
A separate conditional test performs a real two-process 1.18.2
dedicated-server save/restart and restores the assembled carriage from Create
SavedData and chunk NBT. A follow-up dedicated server plus two sequential real
clients verifies the persisted active schedule's authoritative backward
direction and late-join/reconnect synchronization of controller, buffer-light,
mounted cargo controller/fill and cistern state. A concurrent two-client probe then applies one live
server-authoritative update and verifies that independent driver and observer
JVMs both receive the resulting movement, forward direction, controller and
buffer-light state, cargo amount and fill-level change, fluid amount and the station-open to moving-closed
door transition. A separate two-client coupler probe invokes the production
interaction path and verifies `2 x 1 -> 1 x 2 -> 2 x 1` topology, retained and
new train identities, carriage ordering, graph membership, and entity
`trainId`/`carriageIndex` bindings on both clients. Only transient effects remain
manual NET-02 coverage. The live-state evidence is in
`.devdocs/tmp/network/net-02-door-20260926-d/network/{1.18.2,driver,observer}/logs/latest.log`.
The current SAVE-01, NET-01 and NET-02 mounted-cargo evidence is in
`.devdocs/tmp/persistence/save-01-cargo02-20260927-a/{1.18.2,client-1,client-2,driver,observer}/logs/latest.log`.
Coupler evidence is in
`.devdocs/tmp/coupler/couple01-20260926-b/{1.18.2,driver,observer}/logs/latest.log`.
The automatic-coupling probe starts with four isolated pairs. The server first
keeps one pair separate with automatic coupling disabled, then keeps a second
pair separate above the configured speed limit. It couples the remaining two
pairs below the limit from opposite approach directions. Both independent
clients verify the synchronized topology sequence
`8 x 1 -> 6 x 1 + 1 x 2 -> 4 x 1 + 2 x 2`, retained train identities and
carriage entity bindings. Evidence is in
`.devdocs/tmp/coupler/couple02-20260926-d/{1.18.2,driver,observer}/logs/latest.log`.
A further two-client controller
probe uses real driver-to-server reverser packets: stopped changes succeed;
moving, throttle and brake attempts preserve direction and show the denial
prompt to the driver. Both clients observe active, throttle, brake, reversed,
movement and released states. The passing 1.18.2 evidence is in
`.devdocs/tmp/controller/ctrl-20260921-h/{server,driver,observer}.log`.
The current CTRL-03 route probe uses a fresh SAVE-01 train with a glued Create
conductor seat and a second named station, verifies the fixture after a separate
server restart, then seats a real NPC and submits a real Create destination
schedule on a dedicated server. Create calculates a 32-block path and controls
departure, movement, braking, arrival delay and the final idle state. Both
independent client JVMs verify the seated NPC, controller flags, direction and
movement throughout the route. Visual light-color inspection remains manual.
Evidence is in
`.devdocs/tmp/scheduled-route/ctrl03-route-20260926-c/fixture/logs/latest.log`,
`.devdocs/tmp/scheduled-route/ctrl03-route-20260926-c/route/1.18.2/logs/latest.log`
and the corresponding `route/{driver,observer}/logs/latest.log` files.
The dedicated CARGO-03 probe exercises three isolated unloading fixtures. The
server transfers exactly 80 iron ingots through a direct hatch, proves that a
simulated bunker extraction does not mutate its 40-item source, executes a
13-item extraction leaving 27, and keeps a permanently powered source and target
at 24/0. Both independent clients observe the hatch and bunker falling-item
state and active unloading loops, while neither receives an `ItemEntity`.
Evidence is in
`.devdocs/tmp/cargo/cargo03-20260927-c/{1.18.2,driver,observer}/logs/latest.log`.

## Primary 1.18.2 acceptance matrix

| ID | Priority | Area | Procedure and expected result | Result |
| --- | --- | --- | --- | --- |
| NET-01 | P0 | Join/resync | Join a running server after a train already exists. Direction-dependent lights, controller state, cargo contents and cistern fluid must appear correctly without changing the train. Reconnect once and repeat. | PASS (2026-09-27: two sequential dedicated clients received BACKWARD direction, reversed controller, TYPE_3 buffer lights, mounted cargo controller with fill level 1 and 96 iron ingots, plus 1500 mB water) |
| NET-02 | P0 | Two clients | Driver and observer watch the same moving train. Controller, coupler, cargo, fluid, doors, lights and effects must update consistently for both; no state may depend on the integrated server or a static field shared between sides. | PARTIAL (2026-09-27: simultaneous dedicated driver and observer clients matched live direction/controller/lights/cargo/fluid/movement/door updates, including mounted cargo 96 -> 112 and fill level 1 -> 2; a separate real couple/split cycle kept identities, order and bindings consistent; transient effects pending) |
| SAVE-01 | P0 | Persistence | Save and restart the dedicated server with assembled trains, cargo, fluid, controller direction and scheduled controls. After restart, quantities and behavior must be unchanged and reconnecting clients must resync. | PASS (2026-09-27: fresh JVM restored graph/station, carriage, active schedule, backward direction, throttle, mounted cargo controller with 96 items and fill level 1, 1500 mB and both station-open door halves; two sequential clients resynced) |
| CTRL-01 | P0 | Modern controller | Drive forward, coast, brake and drive backward. Lever animation must match server motion for the driver and observer; releasing control must clear the active state. | PARTIAL (2026-09-21: dedicated server and two clients passed synchronized forward/coast/brake/backward/release sequence; visual lever animation inspection pending) |
| CTRL-02 | P0 | Reverser guard | Toggle the reverser while fully stopped, then try while moving, throttling and braking. Only the stopped case may succeed; denied cases must show the prompt and preserve direction. | PASS (2026-09-21: real driver packets; stopped forward/backward accepted; moving/throttle/brake denied with driver prompt and unchanged direction; observer agreed) |
| CTRL-03 | P0 | Scheduled control | Run a schedule with an NPC seated at the controller. Throttle/brake indication and direction-dependent lights must follow the authoritative scheduled train state and return idle at the end. | PARTIAL (2026-09-26: real seated NPC completed a 32-block autonomous station-to-station Create schedule; server plus driver and observer synchronized departure, movement, braking, arrival delay and idle; visual light-color inspection pending) |
| COUPLE-01 | P0 | Manual coupling | Couple two single-carriage trains, then decouple them from the moving contraption interaction. Train identities, carriage order, graph bindings and client visuals must remain valid. | PARTIAL (2026-09-26: production interaction path coupled and split the dedicated fixture; server, driver and observer agreed on train UUID lifecycle, carriage order, graph/entity bindings and all exited successfully; visual model inspection pending) |
| COUPLE-02 | P0 | Automatic coupling | Exercise auto-coupling below the configured speed limit, above it, with auto-coupling disabled, and from both approach directions. Only the permitted case may merge. | PASS (2026-09-26: dedicated server plus Driver and Observer kept disabled/over-limit pairs separate and synchronized both permitted direction merges with valid topology and entity bindings; all processes exited successfully, no crash report) |
| COUPLE-03 | P0 | Topology and storage | Couple and decouple multi-carriage trains containing item and fluid storage. No carriage, passenger, schedule, item or fluid may be duplicated or lost; a reconnecting client must see the resulting topology. | PASS (2026-09-27: fresh dedicated fixture completed `2 x 2 -> 1 x 4 -> 2 x 2`; server, Driver and Observer preserved four carriage entities, four item/fluid signatures and two seated passengers; exactly one paused schedule survived the split; RailwardRejoin received the final named topology and identical storage/passenger state; all four processes exited successfully, no crash report) |
| CARGO-01 | P0 | Static bulk cargo | Form supported container sizes, insert accepted cargo, reject blacklisted cargo, and split/re-form the multiblock. Item count and displayed fill must be conserved. | PARTIAL (2026-09-27: 1.18.2 GameTests formed x-long, z-long, rectangular and maximum containers; validated every representative boundary/orientation state, routed acceptance/rejection, exact `16/4` layered fill, and split/re-form conservation of item type, amount and controller bindings; in-game rendered fill inspection pending) |
| CARGO-02 | P0 | Mounted bulk cargo | Assemble and disassemble a train containing partially filled bulk containers. Controller selection, item count and fill level must survive both transitions and a server restart. | PARTIAL (2026-09-27: captured two-block storage preserved its mounted controller, 54-slot routed capacity, 80 -> 224 items, fill level 1 -> 2, contraption NBT and disassembly state; real dedicated assembly and fresh-JVM restart preserved the controller, 96 items and fill level 1; two late-join and two concurrent clients verified initial and live 96 -> 112/fill 1 -> 2 state; only in-game rendered fill inspection remains) |
| CARGO-03 | P0 | Cargo unloading | Transfer cargo through unloading hatch/bunker to valid and blocked destinations. Server inventory changes must be exact; falling-item visuals/audio must reach tracking clients without creating client-side items. | PASS (2026-09-27: static GameTests cover exact valid, incompatible and redstone-blocked hatch behavior plus pull-only simulated/executed bunker extraction; dedicated server, Driver and Observer passed exact 80-item direct transfer, simulated 40 and executed 13 -> 27 bunker extraction, blocked 24/0 state, synchronized hatch/bunker falling-item state, active unloading loops and no client `ItemEntity`; 2026-10-03 human recheck confirmed the bulk-unloading sounds and particles on all three versions) |
| FLUID-01 | P0 | Static cistern | Form each supported cistern width/length, fill partially, split/re-form, and test sided capabilities. Fluid type and amount must be conserved with no invalid controller references. | PASS (2026-09-27: all three versions passed width-one lengths 1-11 on both axes; widths 1/3/5 formed on both axes with exhaustive per-face capability checks and remote simulated/executed fill/drain; maximum `5x5x11`, unsupported width 2, over-limit length 12 and incompatible fluids behaved correctly; partial water survived split/re-form with valid controller bindings) |
| FLUID-02 | P0 | Mounted fluid transfer | Assemble a partially filled cistern and transfer through hatch/terminal before and after movement. Fluid must remain server-authoritative and resync after reconnect and disassembly. | PASS (2026-09-27: all three versions assembled a real station train, filled its mounted cistern 1000 -> 1500 mB through the connected hatch/terminal, disconnected during movement, reconnected after returning, drained 250 mB through the terminal and disassembled with exactly 1250 mB; SAVE-01 and two NET-01 late joins independently verified fresh-JVM persistence and reconnect synchronization; the multi-tank FluidNetwork regression passed with the workaround enabled) |
| SIGNAL-01 | P0 | Signals and signs | Pass speed signs, whistle signs and each relevant Create signal aspect in both directions. Speed behavior, honk event and cab-signal aspect must change once per authoritative event and match on observing clients. | PARTIAL (2026-09-29: a real seated NPC completed the outbound and return Create schedules through two-sided 20/60 km/h speed signs, whistle signs and a fixed signal; the dedicated server plus Driver and Observer independently observed WHITE/GREEN/YELLOW/YELLOW_RED aspects, both synchronized throttle limits, both movement directions, exactly two honk events and the complete round trip; all three processes exited successfully with no crash report; cab-signal/sign rendering and spatial whistle audio inspection pending) |
| TIME-01 | P1 | Arrival forecast | Use exact and wildcard station names, duplicate platform names, renamed stations, frozen daylight and altered time rates. Only the selected physical station may receive an assigned arrival and projected time must follow the world clock. | PASS (2026-09-29: production selection matched exact/wildcard names, kept duplicate-name physical UUIDs distinct through rename, froze projections with daylight disabled and followed a measured 2x world-clock rate; dedicated GameTests passed 25/25 on all three versions) |
| VISUAL-01 | P1 | Contraption rendering | Inspect panels, roofs, doors, railings, lights, spotlight, cistern and cargo from all block facings while orbiting the camera. World-facing diffuse light must remain stable and animated parts must rotate around the correct local axes. | PARTIAL (2026-10-03 manual recheck confirmed the fixed train-roof material geometry on Railward copycat slices, half-panels and wall panels in 1.19.2/1.20.1, corrected colored/on-off large-signal lamps in 1.20.1, and correctly rendered full and both wedge roof-partition variants in 1.20.1. On 2026-10-04 Create 6 compiled the Railward spotlight patch successfully with both Flywheel backends; a later isolated client smoke loaded the migrated train after restoring Create 6 contraption render context for buffer/cab lights and matching animated-controller lighting to Create 6. Manual Create 6 checks then confirmed those lights and controller visuals. On 2026-10-07 a further manual orbit check confirmed that the spotlight still illuminates geometry in front of it without projecting a reverse cone onto JSON/SuperByteBuffer locomotive parts, and that Railward's custom bogeys use normal world lighting instead of rendering full-bright in darkness, both as placed blocks and on an assembled train. The remaining complete all-model/all-facing visual pass is pending.) |
| AUDIO-01 | P1 | Audio/effects | Verify primary/secondary whistle, arrival sound, unloading loop and diesel exhaust from driver, observer and reconnecting-client perspectives. Sounds must start/stop once and particles must not be emitted by server-only code on the client. | PASS (2026-10-03 manual recheck: after lengthening the test route, station-approach arrival whistles worked on 1.19.2 and 1.20.1, confirming the original report was a fixture issue; diesel exhaust is visible and composites correctly against clouds on 1.20.1; bulk-unloading sounds and particles work on all three versions. Earlier dedicated multi-client probes cover synchronized driver/observer delivery and reconnect behavior.) |
| CC-01 | P2 | ComputerCraft | With CC:Tweaked installed, call the documented station peripheral methods and validate schedule/arrival results. Without CC:Tweaked, Railward must still load normally. | PASS (2026-10-07: all four targets passed the opt-in 26/26 GameTest suite against their pinned full CC:Tweaked runtime, including Create 6.0.8; the real Create station peripheral exposed and returned valid `getStationId`, `getArrivals` and `getTrainSchedule` data, rejected a zero arrival limit, and the separately retained ordinary 1.18.2 no-CC run passed 25/25) |

## Port release gate

After all 1.18.2 P0 rows pass, verify the following on 1.19.2,
1.20.1/Create 5 and 1.20.1/Create 6:

1. Dedicated server startup and a separate client connection.
2. Assemble, drive, save and reload one train containing a modern controller,
   couplers, bulk cargo storage and a partially filled cistern.
3. Perform one coupling/decoupling cycle and one item/fluid transfer while an
   observing client tracks the train.
4. Confirm metadata, recipes, block models and representative render layers.

Any port-specific failure blocks publication of that port, but must not cause a
regression or feature reduction in the canonical 1.18.2 build.

Use [`manual-release-checklist.md`](manual-release-checklist.md) for the compact
human-inspection pass after the automated P0 work is complete.

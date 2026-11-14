# Functional test matrix

This matrix is the runtime acceptance checklist for Create: Railward. Minecraft
1.18.2 is the primary platform: every priority P0 row must pass there before a
feature is considered complete. Minecraft 1.19.2, 1.20.1/Create 5 and
1.20.1/Create 6 are ports and use the reduced gate described below.

## Steam 'n' Rails coupler compatibility - 2026-11-08

Railward recognizes Steam 'n' Rails' standalone decorative couplers and
Wooden/Copycat Headstocks by their registered block and serialized `style`.
Standalone couplers, Wooden Headstocks and Copycat Headstocks may pair with
each other when their head style matches, regardless of the base block.
Railward/SnR, different-style and plain/buffer pairs are excluded.
The production manual, automatic and uncoupling paths use this compatibility
rule before selecting a target, including when an incompatible coupler is
closer. SnR models remain static; separation on curves is expected.

The standalone/Headstock regression test failed before the compatibility fix
and passed after comparing SnR head styles independently of the base block.
Evidence: `.devdocs/tmp/snr-couplers/headstock-regression-before.log`.

Canonical compilation followed by `compileAll` passed. The isolated 1.18.2
dedicated GameTest server passed 37/37 required tests without SnR, including
restoration of Railward's wrench interactor from contraption NBT. Evidence:
`.devdocs/tmp/snr-couplers/headstock-canonical.log`.

After the matching-head fix, the canonical run and remaining `gameTestAll`
targets passed 37/37 required tests each, with fresh isolated worlds and
successful process exits. SnR integration cases explicitly skip on 1.18.2
and 1.19.2 without the decorative SnR blocks. Evidence:
`.devdocs/tmp/snr-couplers/headstock-{canonical,ports}.log` and
`.devdocs/tmp/snr-couplers/headstock-style/<target>/logs/latest.log`.

With the unmodified SnR 1.6.6 artifact on 1.20.1/Create 5 and SnR 1.7.3 on
1.20.1/Create 6.0.8, each dedicated GameTest server passed 37/37 required
tests. Each run completed 90 matching merge/split cycles and rejected ten
incompatible pairs through the production handlers. All nine combinations
of standalone, Wooden Headstock and Copycat Headstock bases were tested in
manual and automatic modes for all five head styles. Positive scenarios also
included a closer incompatible target. Reload checks covered all five head
styles, three block types and four facings, including saved contraptions
without an existing interactor entry. These are in-memory rail-graph/entity
fixtures, not client-driven station assembly or real wrench-packet probes.
Evidence: `.devdocs/tmp/snr-couplers/headstock-ports.log` and
`.devdocs/tmp/snr-couplers/headstock-style/{1.20.1,1.20.1-create6}/logs/latest.log`.

The 1.19.2 installed-SnR run is not verified. SnR 1.6.6 requires the exact
Create version `0.5.1.i`, rejecting the development artifact `0.5.1.i-59`.
An isolated runtime-only retry with the official Create `0.5.1.i` release
passed dependency validation but failed in SnR's `MixinLootTables`
(`static` handler/target mismatch), before GameTests. No dependency ranges,
upstream mixins or release dependencies were changed to bypass this failure.
Evidence: `.devdocs/tmp/snr-couplers/installed-119-release-v2.log`.
SnR 1.4.8 on 1.18.2 does not provide these decorative blocks.

Final `clean buildAndCollect` passed all 53 tasks and produced all four 0.7.2
release JARs; each target passed 24 unit tests with no failures or errors.
Archive inspection confirmed the compatibility class is present and no
GameTest/client-probe classes, bundled SnR classes or SnR dependency metadata
are included. The version file was not changed. Build evidence:
`.devdocs/tmp/snr-couplers/headstock-build-final.log` and
`versions/<target>/build/test-results/test/TEST-*.xml`.

Real-client wrench interaction, driver notifications/audio, both physical
approach directions and static SnR model inspection remain manual acceptance
checks in `manual-release-checklist.md`. SnR is an optional integration and
its artifacts are injected only by an ignored temporary test init script.

## Track sign Ponder orientation - 2026-11-12

The isolated 1.18.2 client and dedicated server exited cleanly. Both sign
storyboards passed checks for initial west-facing rotation, a one-step wrench
turn and retention of the selected rail and direction. The whistle played
once on the forward crossing, and seeking and the return trip stayed silent.
Inspected the Whistle Sign face and wrench prompt in captures 2 and 5. See
`.devdocs/tmp/track-sign-facing-whistle-20261112/` for logs and captures.

The 1.19.2 and 1.20.1/Create 5 dedicated server startup checks passed and
both servers stopped cleanly. The Create 5 startup passed on retry after a
Gradle asset download failure.

The Create 6 dedicated server and speed sign client probe passed on retry
after an initial disconnect before playback. Inspected the turned face,
limit values and value-box prompt in captures 2 and 3. See the logs and
captures in `.devdocs/tmp/track-sign-facing-speed-20261112/`.

Canonical compilation, `compileAll`, `clean buildAndCollect` and
`compileClientTestAll` passed. Each target passed 24 unit tests. All four
JARs contain both scenes and 131 matching Ponder keys per language, with no
test probe classes. See `.devdocs/tmp/track-sign-facing-{build,jars}.log`.

## Whistle Sign Ponder scene - 2026-11-08

The scene reuses the speed sign stage and demonstrates direction-sensitive
placement, the schedule requirement and a selected-direction passage by a
conductor with a Create steam whistle. An actual whistle sound request and
note particles mark the leading wheel crossing. The reverse pass and scene
seeking remain silent. The scene scripts motion and audio locally. Existing
SIGNAL-01 evidence covers the scheduled train's gameplay behavior.

Canonical compilation, `compileAll` and `compileClientTestAll` passed. The
isolated 1.18.2 dedicated server and Russian client passed the sign target,
cart motion and sound event checks and exited cleanly. The client probe
observed one sound event at the crossing across five live previews. It also
passed the speed sign playback assertions after the shared stage extraction.
Inspected the conductor, schedule icon, sign and note effect in native frames.
See `.devdocs/tmp/whistle-sign-ponder-compile-final.log`,
`.devdocs/tmp/whistle-sign-ponder-runtime-118.log` and the launch logs and
`1.18.2-client/screenshots/whistle-sign-ponder-*.png` in
`.devdocs/tmp/whistle-sign-ponder-20261108/`.

The 1.19.2, 1.20.1/Create 5 and 1.20.1/Create 6 runs also passed their sound
and playback assertions with clean client and server exits. All four runs
produced five captures each. Inspected representative native frames in all
three languages. See `.devdocs/tmp/whistle-sign-ponder-runtime-ports.log` and
the per-target result JSON, launch logs and screenshots in the same fixture.

Final `clean buildAndCollect` completed all 53 tasks. All four release JARs
contain the whistle storyboard and 130 matching Ponder keys per language.
Each target passed 19 unit tests and the packaging check excluded probe
classes. Version remains 0.7.2. See
`.devdocs/tmp/whistle-sign-ponder-{build,jars}.log`.

## Speed Limit Sign Ponder scene - 2026-11-08

The nine-block straight stage demonstrates direction-sensitive rail selection,
placement with the native track marker, the value box, a speed reduction at the
leading wheel point, an unchanged setting on the return trip and turning the
sign face while retaining the track target. The cart movement is scripted.
The existing SIGNAL-01 and train passage probes cover actual train behavior.

Canonical compilation, `compileAll` and `compileClientTestAll` passed.
The isolated 1.18.2 dedicated server and separate Russian client passed the
native target, limit, motion and rotation assertions and exited cleanly. Eight
final captures show readable text, numbers and the value box in the standard
isometric view. See `.devdocs/tmp/speed-sign-ponder-compile-final.log`,
`.devdocs/tmp/speed-sign-ponder-runtime-final-118.log` and the server/client logs
and `1.18.2-client/screenshots/speed-sign-ponder-*.png` in
`.devdocs/tmp/speed-sign-ponder-20261108/`.

The 1.19.2, 1.20.1/Create 5 and 1.20.1/Create 6 dedicated-server and client
runs also passed with clean exits and eight captures each. The first Create 5
connection disconnected before scene playback. The second run passed. Its
first-login logs are preserved alongside the accepted logs. Inspected native
Ukrainian, English and Russian previews across the four targets. See
`.devdocs/tmp/speed-sign-ponder-runtime-{ports,retry}.log` and each target's
result JSON, launch logs and captures in the same fixture root for evidence.

Final `clean buildAndCollect` completed all 53 tasks. The four release JARs
contain the speed sign scene and schematic, 124 matching Ponder keys per
language and no probe classes. Each target passed 19 unit tests. The version
remains 0.7.2. See `.devdocs/tmp/speed-sign-ponder-{build,jars}.log`.

## Wagon Frame baked rendering - 2026-11-08

Separate 1.18.2 runs used copies of the TEST save, the same camera position,
1920x1009 resolution and a 12-chunk render distance. With VSync disabled, median frame time
fell from 21.55 to 12.99 ms and mean throughput rose from 45.8 to 71.9 FPS.
With VSync enabled, mean throughput rose from 30.0 to 40.0 FPS. The earlier
run included additional timing hooks. Moving trains and smoke varied
between runs.

Frame geometry was visible in the updated scene. The old renderer recorded
33 placed and 86 assembled frame calls per frame. The new chunk and
contraption meshes recorded zero frame block entity renderer calls.
The original save matched all 34 recorded SHA-256 hashes.

A separate 1.18.2 client joined a dedicated server running a copy of TEST.
It checked baked detail geometry in all four placed orientations and model
data for 120 mounted frames, including 117 with optional parts. Wrench
interactions changed the member, equipment and valve geometry on the client.
Both processes exited successfully. All four targets built cleanly, and the
three ports passed dedicated server startup and shutdown.

Raw timings, logs and screenshots are in
`.devdocs/tmp/wagon-frame-mesh-20261108/`.

## Placed Wagon Frame render distance - 2026-11-08

An isolated 1.18.2 client disassembled the five-car FLAT fixture and compared
the former 64-block cutoff and the new 192-block cutoff from the same camera
position. At about 99 blocks from the reference frame, the client drew zero
Wagon Frame sections with the old cutoff and all 122 with the new cutoff.
Each count held for 50 consecutive frames. Captures show the same car bodies
and bogeys with the frames restored in the updated view.

Evidence is in `.devdocs/tmp/wagon-frame-range-20261108/client-launch.log`
and `.devdocs/tmp/wagon-frame-range-20261108/1.18.2/screenshots/`.

All four targets built cleanly. The three ports passed dedicated server
startup and shutdown. Logs are in
`.devdocs/tmp/wagon-frame-range-20261108/{buildAndCollect.log,ports-smoke.log}`.

## Placed bogey instancing - 2026-11-08

An isolated copy of FLAT ran on 1.18.2 with the original five-car scene,
camera and 1477x783 resolution. Create disassembled the train at the station.
The probe alternated the registered Railward bogey instances and the normal
bogey renderers while keeping Flywheel enabled for the rest of the scene.
Without VSync, median frame time was 14.43-14.92 ms with the normal bogey
renderers and 8.48-8.54 ms with the new instances. With VSync, mean throughput
was 41.4 and 60.0 FPS respectively.

The full scene retained 122 wagon-frame renderer calls. The normal path
rendered two locomotive bogeys and eight freight bogeys per frame. The
instanced path eliminated those calls and added 32 instances carrying 58,880
vertices. Daylight close-ups captured the same bogeys on the GPU path and with
Flywheel fully disabled. A test-driven wheel-angle change visibly rotated the
wheels on the GPU path. The station then reassembled all five cars and restored
the assembled instance counts. All eleven phases passed and the original FLAT
save matched its pre-test file hashes.

Frame CSVs and screenshots are in
`.devdocs/tmp/bogey-instancing-20261108/1.18.2/`. The same directory's parent
contains `results.txt` and `client-launch.log`. The full manual all-facing
and moving-bogey lighting pass remains in the release checklist.

The separate 1.18.2 client passed the existing bogey-model probe against
a dedicated server. All three ports passed dedicated startup and clean
shutdown. The clean build collected four release JARs and compiled the
client probes. Evidence is in
`.devdocs/tmp/bogey-instancing-20261108/{dedicated.log,ports-smoke.log,buildAndCollect.log}`.

## Placed wagon rendering - 2026-11-08

A dedicated 1.18.2 server and a separate client loaded two tank cars and two
empty gondolas from the bundled schematics in an isolated flat world. With
VSync disabled at 1280x720, the two runs facing all four cars averaged 6.86 and
7.75 ms with the previous block-entity renderers, and 6.75 and 6.93 ms with the
updated renderers. The comparison used the updated cistern culling bounds in
both cases. Looking away averaged 0.51-0.52 ms with either renderer. The
two captures facing the wagons matched pixel for pixel.

A separate CPU-side measurement of tessellating and writing a 3-wide,
9-block-long cistern to a vertex buffer dropped from 78-80 to 51-52
microseconds per warmed draw. The old and new buffers matched across 98,784
vertices covering both horizontal axes, all supported widths and lengths,
and two colors. Client and server exited successfully. The reported 60-to-30
FPS drop and the station assembly transition still need a check in the
original scene.

Evidence is in `.devdocs/tmp/cistern-render-20261108/{baseline-client.log,optimized-client.log,visible-optimized-client.log,visible-optimized-server.log}`
and `visible-optimized-client/screenshots/{legacy-wagons,placed-wagons}.png`.

The clean build collected all four release JARs. The 1.19.2, 1.20.1/Create 5
and 1.20.1/Create 6 dedicated servers also started and shut down cleanly in
isolated run directories. Logs are in
`.devdocs/tmp/cistern-render-20261108/buildAndCollect.log` and
`.devdocs/tmp/cistern-render-20261108/ports/`.

## Train passage interception compatibility - 2026-11-05

Removed the exclusive redirect of `Train.frontSignalListener()` inside
`Carriage.travel`. The existing `TrainMixin` now wraps the listener returned
by that factory. Railward keeps the same before/original/after sequence and
propagates the original stop decision. The returned handler is captured once;
the wrapper does not call the factory again. The separate production
`CarriageMixin` and its configuration entry are removed. No new runtime
dependency is required.

A GameTest-only mixin deliberately redirects the former call site, simulating
another train addon. Two additional tests move real Create travelling points
through the native `Carriage.travel` method. They verify one callback per
point in each direction, directional speed-limit behavior, a complete passage
through a one-block relay section and clearing its pulse, and preservation of
Create's signal-stop decision and stop position. Entity anchoring and lifecycle
are stubbed in this focused fixture. The foreign mixin/configuration is enabled
only for `runGameTestServer` and must not be packaged in release JARs.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
The separate 1.18.2 GameTest run passed 35/35 required tests with the foreign
redirect installed. Evidence: `.devdocs/tmp/train-passage-compile118.log`,
`train-passage-compile-all.log` and `train-passage-gametest118.log`.

`gameTestAll`: PASS, 35/35 required no-CC tests on each of the four targets,
with the foreign call-site redirect installed. Evidence:
`.devdocs/tmp/train-passage-gametest-all.log` (7m 16s, 60 tasks). This establishes
compatibility with the simulated competing redirect, rather than certifying
arbitrary mods which replace or cancel the listener factory itself.

Fresh 1.18.2 SAVE-01 prepare/verify and SIGNAL-01: PASS. The ordinary dedicated
server and two separate, concurrently connected clients verified the real NPC
round trip, WHITE/GREEN/YELLOW/YELLOW_RED aspects, synchronized 20/60 km/h
throttle limits and exactly two directional honks. All five invocations exited
successfully, with no crash report. Evidence:
`.devdocs/tmp/train-passage-signal-flat-20261105/result.json` and adjacent
prepare, verify, server, RailwardDriver and RailwardObserver logs. Rendering
and spatial audio quality still require the existing human inspection pass.

Earlier route attempts are not accepted as passes: the temporary 1.18.2
server used `level-type=minecraft:flat`, which actually generated a noise
overworld. The NPC left its seat and Create cancelled navigation with
`missing_driver`; the old call-site redirect reproduced the same failure.
Evidence: `.devdocs/tmp/train-passage-signal-baseline-20261105/server.log`
and `.devdocs/tmp/train-passage-agent/baseline-state.txt`. The successful
fresh fixture uses the legacy `level-type=flat`, confirmed in `level.dat`.
Temporary diagnostic source changes were removed before that successful run.

Final `clean buildAndCollect`: PASS (53 executed tasks, 1m 46s); 19 ordinary
unit tests passed per target. All four release JARs contain the returned-listener
hook, with neither the old `CarriageMixin` nor the test-only foreign mixin,
its configuration or client probes. The cabinet assets remain intact and
version stays 0.7.2. Evidence: `.devdocs/tmp/train-passage-clean-build.log`
and `.devdocs/tmp/train-passage-jars.log`.

## Relay Cabinet - 2026-11-05

The cabinet uses its own boundaries and block sections on Create's track graph.
Create signals, chain modes, route reservations and navigation restrictions are
not part of relay occupancy. A train occupies every relay section covered by
its wheel-to-wheel path, including the body between bogeys and carriages.
All branches continue to the next cabinet boundaries; intersecting track
sections share occupancy without changing Create's signal data.

Redstone output is 15 only for train occupancy. External power changes the
cabinet's occupied/free state but is not retransmitted. Linked crossing
components can read `RelayCaseBlockEntity.isOccupied()`; the state is synced
through ordinary blockstate updates. The original model's north face points
toward the placing player, with four horizontal facings and wrench rotation.
The 32 x 32 source atlas uses normalized Minecraft UVs.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
The canonical dedicated GameTest server passed 33/33 ordinary no-CC tests,
including seven relay tests. Evidence: `.devdocs/tmp/relay-compile118-final.log`,
`relay-compile-all-final.log` and `relay-gametest118-acceptance.log`.

The separate 1.18.2 client and ordinary dedicated server both exited successfully.
The client checked four baked models with 21 faces each, Russian localization,
initial occupied state and the live release after the server removed external
power, with train-only output remaining off. The native housing capture was
inspected for atlas, roof, legs and four facings. Evidence:
`.devdocs/tmp/relay-client-20261104/1.18.2-result.json` and the adjacent launch
logs/screenshots. The two captures show the same viewpoint; they do not verify
camera-dependent lighting.

The same ordinary-server/separate-client check passed on Create 6 in a fresh
fixture, with successful exits and no crash report. Inspected its two native
views, including the side/rear panels and roof overhang. Evidence:
`.devdocs/tmp/relay-client-retry-20261105/1.20.1-create6-result.json` and adjacent
logs/screenshots. The first Create 6 attempt timed out during login before
entering the world; its failed-probe logs remain in the preceding fixture.

The GameTest fixtures exercise real Create graph, train, travelling-point,
block entity, NBT and redstone APIs; they are not a substitute for driving an
assembled train through a crossing in-game. Human placement/wrench interaction
and camera-dependent lighting remain on the manual release checklist.

`gameTestAll`: PASS, 33/33 ordinary no-CC tests on every target, including all
seven relay checks. All dedicated test servers exited successfully. Evidence:
`.devdocs/tmp/relay-gametest-all.log` (6m 12s, 60 tasks). The port runs also
provide dedicated-server startup coverage for 1.19.2 and 1.20.1/Create 5.

Final `clean buildAndCollect`: PASS (53 executed tasks, 2m 7s). All four
release JARs contain the cabinet classes, graph mixin, original texture,
normalized model, four-facing blockstate, recipe, loot, pickaxe tag and all
three localizations. No GameTest/clientTest classes are packaged. The ordinary
unit suite passed 19 tests per target. Version remains 0.7.2. Evidence:
`.devdocs/tmp/relay-clean-build.log` and `.devdocs/tmp/relay-jars.log`.

## Relay Cabinet track markers - 2026-11-14

Dedicated-server and separate-client probes passed on 1.18.2 and 1.20.1/Create
6.0.8. Two cabinets targeting the same track point in opposite directions
shared a graph point. At login the client received one double-arrow marker
and hid the other cabinet's marker. After the server removed the cabinet
drawing the double marker, the remaining cabinet drew a single arrow. Native
captures of both states were inspected on both targets. Both server/client
pairs exited successfully.

Evidence is under `.devdocs/tmp/relay-marker-check-20261114/` for 1.18.2 and
`.devdocs/tmp/relay-marker-retry-20261114/` for Create 6. See the result JSON,
launch logs and `relay-marker-*.png` captures in the client screenshots.

Dedicated servers on 1.19.2 and 1.20.1/Create 0.5.1 passed startup and shutdown
checks. Canonical-first compilation, `compileAll`, `compileClientTestAll` and
`clean buildAndCollect` passed. See the `relay-marker-port-smoke.log`,
`relay-marker-compile*.log` and `relay-marker-clean-build.log` logs in
`.devdocs/tmp/`.

## Rail Crossing Signal

The 1.18.2 item-placement GameTest exercised the Forge `ItemStack.useOn`
path. Shift-right-click cleared the cabinet selection on a block and in
air. Failed placement retained the selection. Successful placement cleared
both cabinet tags on the remaining stack, preserved unrelated item data
and left the placed signal responding to its cabinet. All four targets
passed 39/39 required GameTests. See `gametest-118.log` and
`gametest-all.log` under `.devdocs/tmp/crossing-link-reset`.

The Ponder probe opened the crossing item storyboard on a separate
client after joining a dedicated server. It verified translated keyframes,
both lamp phases ten ticks apart, one looping warning during playback,
silence during seek, release, and the three sign previews. Native warning
and settings captures are under `.devdocs/tmp/crossing-ponder-check`.
The client and dedicated server exited successfully on all four targets.
The 1.20.1/Create 5 fixture required a retry after the first login stalled.
All four targets passed 39/39 GameTests. The clean build and
`compileClientTestAll` passed, and each release JAR includes the storyboard
and its English, Ukrainian and Russian translations.

After turning the cabinet and signal clockwise and mounting the signal on an
andesite post, the 1.18.2 client verified the two facings and support, confirmed
that the warning reached the sound engine during occupancy, and checked that
its channel became inactive after release. The explicit channel stop runs even
when Ponder pauses normal sound ticks in single-player. The dedicated-client
result and native captures are in `.devdocs/tmp/crossing-ponder-post`.

The 1.18.2 dedicated GameTest server passed 39/39 required tests, including
real item selection and placement, refusal without a cabinet, settings and
binding restoration through NBT, left/right alternation at ten-tick intervals,
initial sync of the current phase, and release after the cabinet was removed.
A ray test hit the settings square from the front and rejected the back face
in each of the four orientations. The server log is at
`.devdocs/tmp/crossing-gametest/1.18.2/logs/latest.log`.

The 1.18.2 client joined a separate dedicated server while the cabinet was
occupied. It opened the localized settings screen through the square, applied
Multi-track and Siren, received both settings back from the server, and
observed both flash phases, the vanilla bell event, and release when the
server cleared the external redstone input. Both processes exited successfully.
The three native captures show the left lamp, the right lamp, and both lamps
off at the same viewpoint with the two signs visible. See
`.devdocs/tmp/crossing-client/1.18.2-result.json` and the adjacent logs and
screenshots.

`gameTestAll` passed 39/39 required tests on each of the four targets.
All four dedicated servers started, ran the tests and exited successfully.
The combined log is at `.devdocs/tmp/crossing-gametest/gameTestAll.log`.

The clean release build and `compileClientTestAll` passed. Each release JAR
contains the signal, all ten partial models, the original texture, recipe,
loot table, pickaxe tag and three localizations. Both test source sets are
excluded from the release JARs. See `.devdocs/tmp/crossing-gametest/clean-build.log`
and `.devdocs/tmp/crossing-gametest/release-jars.log`.

The UV check compared the north face of `cross.1`, the south face of
`cross.2`, the red lamp and the translucent lens with their source rectangles
on the 32 x 32 atlas. The train-drive, wrench, camera orbit and spatial audio
checks remain on the manual release checklist.

The custom audio client probe on 1.18.2 joined a separate dedicated server
while the cabinet was occupied. It verified one positional loop per mode at
the signal, selected Ring and Siren through the settings screen, checked that
each previous loop stopped, and confirmed the siren stopped when the
cabinet released. See `.devdocs/tmp/crossing-audio-check/client-server.log`
and the adjacent `client-server` launch logs. Listening for loop seams and
attenuation remains a manual check.

After the audio update, `gameTestAll` again passed 39/39 required tests on all
four targets. The clean release build contains byte-identical copies of all
three supplied OGGs and their sound definitions and subtitles in each JAR.
See `.devdocs/tmp/crossing-audio-check/gametest-all.log`,
`clean-build.log` and `jars.log` in the same directory.

The hover update removes the settings geometry from the block and item
models. A 1.18.2 server test raycast to the existing post in all four facings
and confirmed the settings hit stays on its surface and rejects the back.
The separate client checked that Create's outliner adds and removes both
the settings frame and the cabinet highlight, skips a selection from another
dimension, and opens the settings from the post hit. The native capture
`settings-outline.png` shows the white frame and localized tooltip.
Evidence is under `.devdocs/tmp/crossing-highlight-check`.
All four targets again passed 39/39 required GameTests and the clean release
build. The packaged block and item models have no settings plate elements.

## Controller Ponder reverser direction correction - 2026-11-03

Corrected the scene's travel cues: forward is from the central seat toward
and beyond the controller (north); reverse is toward the free deck end
(south). Both paired lamp colours and passenger yaw now follow that cab-relative
direction. The native lever signs and fixed housing are retained. The probe
also anchors forward to the controller/seat geometry, rather than accepting
an arbitrary compass direction.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
The corrected 1.18.2 and 1.20.1/Create 6 dedicated-server/separate-client
runs passed with successful exits, 24 native captures and no crash report.
Inspected forward with the bee facing the controller and white lamps at that
end, then reverse with both cues swapped. The existing smooth lever/driver,
fixed housing, neutral, throttle/brake, replay and camera assertions passed.
1.19.2 and Create 5 retain their preceding scene-run evidence; this correction
was runtime-checked on the canonical target and Create 6.
Evidence: `.devdocs/tmp/controller-direction-{118,compile,runtime}.log` and
`.devdocs/tmp/controller-direction-20261103/<target>-{result.json,client-launch.log,server-launch.log}`,
with previews under `<target>-client/screenshots/controller-ponder-*.png`.

Final `clean buildAndCollect`: PASS (53 executed tasks, 1m 48s). All four
release artifacts passed class/asset/translation and no-probe packaging checks;
19 unit tests passed per target. Version remains 0.7.2. Evidence:
`.devdocs/tmp/controller-direction-{build,jars}.log`.

## Modern Train Controller Ponder scene - 2026-11-03

Added a six-caption controller/reverser storyboard on the shared one-bogey
3 x 3 inspection cart with one Railward controller, a seated bee and paired
buffer lights. The sequence selects forward, releases throttle/brake, reverses,
returns forward and ends control. The fixed housing and native lever partials
are rendered separately so reverser/throttle/brake can animate continuously.
Two camera orbits preserve the standard isometric pitch.

The scene is scripted and does not execute train-control packets or simulate
server train movement. Gameplay claims follow `ModernTrainControllerState`
and its existing CTRL acceptance probes; source/runtime results below apply
to the Ponder fixture only.

Canonical-first compilation and final `compileAll compileClientTestAll`: PASS.
After correcting the shared seat reveal, the isolated 1.18.2 dedicated server
and separate Russian client passed all fixture assertions and exited cleanly.
Inspected the native forward/reverse views, visible central seat/passenger,
fixed housing, three handles, paired lamps and caption framing in 12 captures.
Evidence: `.devdocs/tmp/controller-ponder-{118-seat,compile-final,final-runtime}.log`
and `.devdocs/tmp/controller-ponder-20261103/1.18.2-{result.json,client-launch.log,server-launch.log}`;
previews are under `1.18.2-client/screenshots/controller-ponder-*.png`.

All three port runs also passed: 1.19.2 (Ukrainian), 1.20.1/Create 5
(English) and 1.20.1/Create 6 (Russian), with successful client/server exits
and no crash report. Each produced twelve previews: 48 final captures total.
Inspected representative native lever/light/seat/caption frames from every
port, including Create 6's GuiGraphics element-render path.
Evidence: `.devdocs/tmp/controller-ponder-final-runtime.log` and per-target
`<target>-result.json`, launch logs and screenshots in the same fixture root.

Final `clean buildAndCollect`: PASS (53 executed tasks, 1m 47s). All four
release artifacts contain both new controller scene/element classes, the
shared stage and 117 matching nonempty Ponder keys in each supported language.
No clientTest or GameTest classes are packaged; 19 unit tests passed on every
target. Version remains 0.7.2. Evidence:
`.devdocs/tmp/controller-ponder-{build,jars}.log`.

## Lighting Ponder persistent control levers - 2026-11-03

Supersedes the selected-controller model switching described below: both
ordinary train controls now retain their virtual lever model throughout both
lighting scenes. Selection changes only the outline/interaction marker and
the smooth passenger turn; the outline lasts for the full direction caption.
The client probe now checks both lever models on every tick from tick 50 to
1480, including reversals and the final lights-off phase, plus native UI frames.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
The isolated 1.18.2 and 1.20.1/Create 6 dedicated-server/separate-client runs
passed, including 32 native UI captures, successful process exits and no crash
report. Inspected both controls before/after reversal: both retain levers,
while the green outline moves to the selected controller. The yaw checks and
existing lamp/panel/filter/camera assertions also passed. The 1.19.2 and Create
5 native scene runs remain evidence from the preceding revision; they were
not repeated for this static-model correction.
Evidence: `.devdocs/tmp/lighting-levers-{118,compile,runtime}.log` and
`.devdocs/tmp/lighting-levers-20261103/<target>-{result.json,client-launch.log,server-launch.log}`,
with previews in `<target>-client/screenshots/lighting-ponder-*.png`.

Final `clean buildAndCollect`: PASS (53 executed tasks, 1m 53s). All four
release artifacts passed the class/asset/translation and no-probe packaging
checks; each target passed 19 unit tests. Version remains 0.7.2.
Evidence: `.devdocs/tmp/lighting-levers-{build,jars}.log`.

## Lighting Ponder paired lamps and selected controls - 2026-11-03

Each cart end now carries two buffer lights at x=2/4, with mirrored type 1/2
housings placing the red lenses outside. Style cycling and the wall-panel
example affect only the first lamp. Spotlights remain centred. Captions name
the selected controller and lighting direction without describing the bee.
Only the selected ordinary controller has Create's virtual extended-lever
model; the other is closed. An outline and interaction prompt mark selection.
The seated passenger uses a resettable, non-blocking 30-tick eased yaw turn.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS
(`.devdocs/tmp/lighting-pairs-{118,compile}.log`). The revised client probe
checks both pairs and outward red lenses, one-lamp style cycling, exactly one
selected controller, a bounded yaw delta on every tick and intermediate yaw
halfway through the reversal, alongside the existing scene checks.

All four isolated dedicated-server/separate-client runs passed with successful
exits, 16 native UI captures per target (64 total) and no crash report. Inspected
paired white/red lamps, one-lamp style changes and selected controller models
on 1.18.2, plus representative 1.19.2, 1.20.1/Create 5 and Create 6 frames.
The first Create 5 login timed out before scene playback; the repeat passed.
Its first-launch logs are preserved as `1.20.1-*-timeout-first.log`.
Evidence: `.devdocs/tmp/lighting-pairs-{118-runner,ports-runner,ports-retry}.log`
and `.devdocs/tmp/lighting-pairs-20261103/<target>-{result.json,client-launch.log,server-launch.log}`,
with captures under `<target>-client/screenshots/lighting-ponder-*.png`.
These scripted Ponder checks do not replace the real-train gameplay checks.

Final `clean buildAndCollect`: PASS (53 executed tasks, 1m 50s), followed by
release-JAR verification: all four artifacts contain the current lighting
classes/assets and 110 matching Ponder keys per language, with no clientTest
or GameTest classes. Each target passed 19 unit tests. Version remains 0.7.2.
Evidence: `.devdocs/tmp/lighting-pairs-{build,jars}.log`.

## Lighting Ponder inspection cart revision - 2026-11-03

Replaced the solid lighting-demo body with a 3 x 3 Railway Casing deck on one
small Z-axis bogey, above seven straight track blocks. A red seat is centred
on the deck; ordinary Create train controls immediately ahead and behind face
the driver. Buffer Lights and Spotlights mount on the opposite deck ends.
The existing camera orbits are retained.

The native bee is an actual passenger of a Create SeatEntity. AI and gravity
are disabled, and its body/head yaw changes from north to south with the
scripted driving direction. The probe checks one bee, its central seat binding
and stable position, both selected directions, the 3 x 3 deck, exactly one
bogey, tracks and opposed ordinary controls, alongside the existing light,
attachment, filter and camera checks.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
The isolated 1.18.2 dedicated server and separate Russian client exited
successfully, with 16 native UI captures and no crash report. Inspected the
front/rear light states, visible bee facing/seat, ordinary controls, bogey,
rails and Spotlight control. This is a scripted Ponder fixture, not a live
assembled-train direction or schedule test.

Evidence: `.devdocs/tmp/lighting-cart-{118,compile-all,client-tests}.log` and
`.devdocs/tmp/lighting-cart-20261103/1.18.2-{server,client}-launch.log`, with
native screenshots under the corresponding client directory.

All three ports subsequently passed the same full client probe: 1.19.2 in
Ukrainian, 1.20.1/Create 5 in English and 1.20.1/Create 6 in Russian. All
dedicated servers and clients exited successfully, with 16 captures per
target (64 total) and no crash report. Representative port captures confirm
the seated bee, opposite light states and readable captions. This successful
Create 6 cart playback supersedes the earlier orbit-only revision's pending
lighting UI check for the current storyboards.

Port evidence: `.devdocs/tmp/lighting-cart-ports-runner.log` and per-target
launch logs/result JSON files under `.devdocs/tmp/lighting-cart-20261103/`.

Final `clean buildAndCollect`: PASS, all 53 tasks executed and all four release
JARs collected without changing version 0.7.2. Each target passed 19 unit tests.
Archive checks confirmed source-matching schematics, 110 matching nonempty
Ponder language keys and exclusion of all probe classes. Evidence:
`.devdocs/tmp/lighting-cart-{build,jars}.log`.

## Lighting Ponder camera orbit revision - 2026-11-03

Both lighting scenes now use opposite ends of one continuous body, with
north/south equipment facings and automatic 180-degree camera orbits. The
initial isometric angle and -35-degree pitch are preserved. Each direction
example holds the first end before orbiting to the opposite end; reversing
direction swaps the lamp states before the camera returns. The Spotlight
control occupies a supported upper corner of the body and faces the rear view.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
Dedicated servers and separate client probes passed on 1.18.2/Russian,
1.19.2/Ukrainian and 1.20.1/Create 5/English, with 16 UI captures each.
The probe checks opposing facings, a shared body, both camera targets before
and after reversal, panel attachments, housing variants, lamp states and the
Spotlight filter. Inspected native front/rear views and the visible supported
control on Create 5.

Create 6 server/client startup passed, but this revision's native Ponder run
did not reach a client world: two attempts disconnected during login, before
scene compilation. The first was stopped early; the sequential retry reached
the probe's waiting-for-world timeout. Do not mark the revised Create 6 UI
playback verified from compilation or from the previous scene's successful
probe. Manual playback remains on the release checklist.

Evidence: `.devdocs/tmp/lighting-orbit-{118,compile-all}.log` and
`.devdocs/tmp/lighting-orbit-20261103/`, including per-target launch logs,
the three successful result JSON files and native screenshots. The first
Create 6 pre-login attempt is retained as `1.20.1-create6-prelogin-*.log`.

Final `clean buildAndCollect`: PASS, 53 tasks executed and all four release
JARs collected at unchanged version 0.7.2. Each target passed 19 unit tests.
Archive checks confirmed matching schematics, 110 matching nonempty Ponder
language keys and exclusion of test classes. Evidence:
`.devdocs/tmp/lighting-orbit-{build,jars}.log`.

## Locomotive lighting Ponder tutorials - 2026-11-03

Added one storyboard each for Buffer Lights and the Locomotive Spotlight.
Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
Isolated dedicated servers and separate clients passed on 1.18.2/Russian,
1.19.2/Ukrainian, 1.20.1/Create 5/English and 1.20.1/Create 6/Russian.

Each `ponder_lighting` probe checked two item registrations, 6/6 localized
captions, all 13 buffer-light and both spotlight variants, native panel
attachments, white/red/off changes, directional spotlight activation, and
Spotlight-filtered Contraption Controls. Preview state was rejected for an
ordinary client-world block entity even when it was explicitly marked virtual.
Standard -35/145 isometry was retained. All accepted clients and servers
exited successfully, with 12 native UI captures per target and no crash report.

Reviewed the complete canonical capture set and representative Ukrainian,
English and Create 6 frames. Panel-mounted equipment, both lamp colours,
the control in front of the stands and captions remain visible at 854 x 480.
The final connected beam diagram was inspected on Create 5 and Create 6.
Ponder 1.0.91's dynamic-BE hook capitalization mismatch initially left new
lights nonvirtual; explicit Ponder-level-scoped initialization fixes this.
The first Create 5 client disconnected before joining; a separate sequential
retry passed without a gameplay-code change.

Direction changes are scripted and the beam is an overlay diagram. These
checks do not create a moving train, exercise the live forward-light shader,
or replace the real train-light visual checks on the manual release checklist.

Evidence: `.devdocs/tmp/lighting-ponder-final-{118,compile-all}.log`;
accepted 1.18.2/1.19.2 logs and screenshots under
`.devdocs/tmp/lighting-ponder-20261103-final/`; accepted Create 5 and Create 6
retries under `.devdocs/tmp/lighting-ponder-20261103-{c5,c6}-retry/`.

Final `clean buildAndCollect`: PASS, 53 tasks executed and all four release JARs
collected without changing version 0.7.2. Each target passed 19 unit tests.
Archive checks confirmed the lighting schematic and scene classes, 110 matching
nonempty Ponder language keys, and exclusion of client/server test classes.
Evidence: `.devdocs/tmp/lighting-ponder-final-{build,jars}.log`.

## Cistern Ponder tutorials - 2026-11-02

Added building and pipe-connection storyboards on all seven Cistern colours.
Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.

Isolated dedicated servers and separate clients exited successfully on all
four targets: 1.18.2/Russian, 1.19.2/Ukrainian, 1.20.1/Create 5/English and
1.20.1/Create 6/Russian. Each opt-in `ponder_cistern` probe checked 14 registrations,
7/4 translated captions, full controller bindings for 1/3/5-block diameters,
11-block length, separate touching colours and both horizontal axes. Real
sided fluid capabilities accepted the displayed ports and rejected end
corners, off-centre top ports, side ports on end sections and thin-tank sides.
The camera remained at Create's standard -35/145 isometry.

Reviewed 11 native UI previews at 854 x 480: captions remain readable, the
9/25-block end-extension examples show matching items, and the near-side pipe
and near-end connection areas remain visible. Formation and extension timing
are scripted for Ponder, so this is not live server placement verification.
Human playback across all colours/languages remains on the manual checklist.

Ports captured the same 11 previews each (44 total), with no crash reports.
Representative Ukrainian, English and Create 6 pipe-connection previews were
also inspected for caption fit, port visibility and consistent framing.

Final `clean buildAndCollect`: PASS, all 53 tasks executed and all four release
JARs collected at version 0.7.2. Each target passed 19 unit tests. Archive checks
confirmed the new schematic and scene class, 96 matching nonempty Ponder keys
in each language, and exclusion of client/server test classes.

Evidence: `.devdocs/tmp/cistern-ponder-final-{118,compile-all}.log` and
`.devdocs/tmp/cistern-ponder-20261102-final/`, containing per-target result JSON,
`<target>-{server,client}-launch.log` and
`<target>-client/screenshots/cistern-ponder-{0..10}.png`.
Build/archive evidence: `.devdocs/tmp/cistern-ponder-final-{build,jars}.log`.

## Automatic coupling across dimensions - 2026-11-02

Automatic coupling now scans loaded server dimensions at the end of the
server tick. Movement history and post-uncoupling suppression are scoped to
each ServerLevel and cleared on server shutdown. Both trains must have all
carriages loaded entirely in the selected dimension; portal-spanning trains
are excluded.

Canonical-first compilation and `compileAll`: PASS. All four targets passed
26/26 ordinary dedicated-server GameTests. The new dimension regression uses
in-memory Create carriage fixtures in the Overworld, Nether and End to verify
independent movement histories, foreign-world scans preserving the Nether
recoupling lock, and rejection of a carriage spanning dimensions. It does not
perform a track-bound approach/merge or verify driver notifications, sound,
rendering, or a custom mod dimension.

Manual gameplay verification (2026-11-02): the user confirmed that automatic
coupling works in both the Nether and End. The reports do not specify the Minecraft/Create
target or server type, or separately verify notifications, sound and recoupling
suppression. Custom-dimension gameplay checks remain pending.

Evidence: `.devdocs/tmp/autocoupler-dimensions-118-final.log`,
`.devdocs/tmp/autocoupler-dimensions-ports-final.log` and isolated run directories
under `.devdocs/tmp/autocoupler-dimensions-20261102/{118-final,ports/}`.
The port `gameTestAll` invocation excludes the already-passed canonical task
and uses a temporary init script to assign fresh per-target run directories.
Release validation: `clean buildAndCollect` PASS, all 53 tasks executed.
All four collected JARs contain no GameTest or client-probe classes.
Build evidence: `.devdocs/tmp/autocoupler-dimensions-build.log`.

## Coupler Ponder visibility revision вЂ” 2026-11-02

All three coupler storyboards now use `run/schematics/cargo_platform.nbt`
instead of the high-sided halfcar. The 11x1x3 platform uses native Railward
bogeys and leaves the coupling exposed. Coupler/bogey coordinates, movement,
camera and view scale are unchanged; the original schematic is unchanged.
The shared tutorial asset is now 415 blocks / 4021 bytes.

`clean buildAndCollect compileClientTestAll`: PASS on all four targets, with
63 executed tasks and 19/19 unit tests each. Collected JAR resources match the
new asset and contain no test classes. All four dedicated-server/client
`ponder_coupler` checks passed, nine previews each (36 total). The Create 6
parallel attempt disconnected during login before scene playback; its
successful isolated retry is recorded separately. All accepted processes
exited successfully with no client crash reports. Rendered 1.18.2 and Create 6
frames confirm that the platform leaves the heads visible during the coupling
and wrench demonstrations.

Evidence: `.devdocs/tmp/coupler-platform-20261102/ports-results.json`,
per-target logs and screenshots, plus
`.devdocs/tmp/coupler-platform-20261102-c6-retry/` for Create 6.
Build/package logs: `.devdocs/tmp/coupler-platform-build.log` and
`.devdocs/tmp/coupler-platform-jars.log`; the unchanged source hash is stored
in `.devdocs/tmp/coupler-platform-source.json`.

## Train Coupler Ponder tutorials вЂ” 2026-11-02

The Train Coupler exposes three storyboards: automatic approach/coupling,
manual wrench coupling/uncoupling, and train-name rules. They use the supplied
halfcar and reversed ChME3 on one track, with compact framing centered on the
coupler pair and standard isometry. Both vehicles depart together after
joining; only the locomotive moves after the manual split. Naming examples
cover controls on one/both/neither part, clicked-train manual naming,
moving-train automatic naming and free numbered names after splitting.

Canonical-first compilation, `compileAll` and `compileClientTestAll` passed.
The opt-in `ponder_coupler` client checks all three registrations, 5/6/19
translated text entries (including name labels), opposing couplers at equal
height, signed wheel animation, section offsets, controls examples and camera
angles. All four targets passed with separate dedicated servers and clients:
1.18.2 in Russian, 1.19.2 in Ukrainian, 1.20.1/Create 5 in English and Create 6
in Russian. Each passing client saved nine previews, 36 in total; all accepted
client/server processes exited successfully with no client crash reports.

Evidence: `.devdocs/tmp/coupler-ponder-20261102-accepted/ports-results.json`,
per-target logs and screenshot directories. The first parallel Create 5
attempt timed out before joining the server; its successful isolated retry is
under `.devdocs/tmp/coupler-ponder-20261102-c5-retry/`. Earlier canonical
previews have superseded label placement. Final text polish shortens the
naming header to avoid a three-line Ukrainian title meeting the name label.
Compile logs: `.devdocs/tmp/coupler-ponder-118.log` and
`.devdocs/tmp/coupler-ponder-compile-all.log`.

These checks exercise virtual storyboard playback and native block renderers,
not actual server train topology. The existing COUPLE gameplay probes remain
separate. Retain the full human navigation, pacing and rendering pass in
`manual-release-checklist.md`.

Release validation: `clean buildAndCollect` passed with 53 executed tasks and
19/19 unit tests per target. All four 0.7.2 JARs contain `CouplerScenes`, the
new schematic, the client depth mixin and 83 matching Ponder language keys,
with no client-test or GameTest classes. Evidence:
`.devdocs/tmp/coupler-ponder-build.log` and
`.devdocs/tmp/coupler-ponder-jars.log`. The pre-existing 0.7.2 version value
was preserved.

## Ponder train wheels and clipping regression вЂ” 2026-10-31

The fluid and bulk-wagon storyboards now animate bogeys with the sign required
for their +X movement. The client probes check every bogey's wrapped angle
change throughout playback, including arrival and departure. The three train
storyboards also use a wider GUI clipping depth without changing screen
position, scale or the standard isometric camera. A separate widget depth
layer keeps the title and navigation above the train on GuiGraphics ports.

Canonical-first compilation, `compileAll` and `compileClientTestAll`: PASS.
Final runtime checks: PASS on 1.18.2, 1.19.2, 1.20.1/Create 5 and
1.20.1/Create 6. Each target ran a dedicated server and two sequential clients
for `ponder_fluid` and `ponder_bulk`; all twelve processes exited successfully
with no client crash reports. Existing transfer, connection, cargo and camera
assertions also passed. Each client saved nine previews, including a new
empty-track fluid preview: 72 captures in total. Inspection confirmed intact
rails and visible title/navigation on 1.18.2 and Create 6. Pixel comparisons
against the accepted close-button icon passed on all four targets, allowing
for GUI scale 2/3 pixel-grid rounding.

Accepted evidence: `.devdocs/tmp/ponder-visual-fix-20261031-final/`, including
`ports-results.json`, per-target server/client logs and screenshot directories.
The verifier is `.devdocs/tmp/verify_ponder_visual_fix.py`; compile logs are
`.devdocs/tmp/ponder-visual-fix-118.log` and
`.devdocs/tmp/ponder-visual-fix-compile-all.log`. The earlier directory without
`-final` is superseded because its expanded depth could obscure GUI widgets.
These checks cover virtual tutorial playback; retain the full human
navigation, reading and audio pass in `manual-release-checklist.md`.

Release validation: `clean buildAndCollect` PASS (53 executed tasks), with
19/19 unit tests on each target. All four collected JARs contain the depth
mixin and its client registration, the unchanged tutorial schematics and
matching language keys, with no client-test or GameTest classes. Evidence:
`.devdocs/tmp/ponder-visual-fix-build.log` and
`.devdocs/tmp/ponder-visual-fix-jars.log`. The mod version remains 0.7.1.

## Create 6 release-client renderer mixin regression вЂ” 2026-10-30

The supplied ordinary-client crash report reproduces the startup failure with
Minecraft 1.20.1, Forge 47.4.28, Create 6.0.8 and Railward 0.7.0. Its root cause
is `InvalidInjectionException` in `railward$setCurrentContraptionEntitySrg`:
the handler takes `AbstractContraptionEntity`, but `m_7392_` takes `Entity`.
`javap` of the pinned Create 6.0.8-289 release dependency confirms that
`m_7392_(Entity, ...)` is `ACC_BRIDGE, ACC_SYNTHETIC` and delegates to
`render(AbstractContraptionEntity, ...)`. The latter retains its name in both
release and deobfuscated dependencies. The release JAR of Railward 0.7.1
before this fix also contains the faulty SRG handlers.

The Create 6 mixin now targets the typed `render` method by full descriptor
at HEAD and RETURN, requires each injection, and removes the SRG alternatives.
The Create 5 branches are unchanged. An ordinary release-client launch is
required in addition to development `runClient` checks; see the port gate in
`manual-release-checklist.md`.

Verification on 2026-10-30: canonical `:1.18.2:compileJava`, then `compileAll`,
then `clean buildAndCollect` all PASS. The clean build executed all 53 tasks;
each of the four targets passed 19 unit tests with zero failures/errors.
`javap` of the newly collected Create 6 JAR confirms both descriptor-qualified
injections with `require=1` and no SRG handlers. Evidence:
`.devdocs/tmp/create6-render-fix-20261030/compileAll.log`,
`buildAndCollect.log` and `release-mixin-javap.txt` in that directory.
Post-fix ordinary-client startup: PASS (2026-10-30, maintainer confirmed the
replacement JAR starts successfully in the isolated Create/Railward instance
that reproduced the crash). Moving-train rendering remains NOT RUN.

## Bulk Cargo Ponder tutorials вЂ” 2026-10-30

Two additional storyboards cover bulk storage and wagon handling. All three
container colours expose both; Unloading Hatch and Unloading Bunker expose
the wagon scene (eight registrations). Eight storage captions show square,
rectangular and tall assembly, complete-layer placement, adjacent colours that
retain separate controllers/walls, top item intake, empty-hand stack extraction
and inventory automation. Six wagon captions cover overhead supply, 1вЂ“3-block
hatch reach, loading, a floor hatch aligned 1вЂ“3 blocks above a bunker, active
extraction through its pull-only capability and automatic disconnection.
EN/RU/UK translations contain 50 matching Ponder keys in total.

The supplied halfcar and reversed ChME3 share one moving section. All 54 cargo
cells are rebound to controller (4,5,6), size 9x3x2. The view retains standard
isometry and a compact platform; the locomotive may leave the viewport. The
overhead supply is behind the loading hatch, and pale sand makes the native
falling-item visuals distinct. The train moves four blocks to the unloading
stop, then departs empty. Existing signal/fluid scenes and gameplay logic are
unchanged.

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Canonical-first compile, compileAll, clean buildAndCollect, 19/19 unit tests and compileClientTestAll | PASS | PASS | PASS | PASS |
| Eight registrations, localized 8+6 captions and square/rectangular/tall dimensions | PASS | PASS | PASS | PASS |
| Separate touching colours, 1152 thrown items, exactly 64 extracted by hand, 512 inserted/extracted through capabilities | PASS | PASS | PASS | PASS |
| 54 normalized wagon cells, one moving consist, 46656 items conserved through full load/unload and disconnected departure | PASS | PASS | PASS | PASS |
| Native hatch/bunker falling visuals during real UI playback, no transfer ItemEntity, nine rendered previews, default camera | PASS (ru_ru) | PASS (uk_ua) | PASS (en_us) | PASS (ru_ru) |
| Collected JARs contain both new schematics, BulkCargoScenes, matching 50 language keys and no probe classes | PASS | PASS | PASS | PASS |
| Full human navigation, reading pace, rendering/particle/audio inspection in every language | NOT RUN | NOT RUN | NOT RUN | NOT RUN |

Evidence: `.devdocs/tmp/bulk-ponder-compile-118.log`,
`bulk-ponder-compile-all.log`, `bulk-ponder-clean-build.log` and the updated
`verify_ponder_jars.py`. Accepted runtime evidence is under
`.devdocs/tmp/bulk-ponder-20261030-final/`: per-target launch logs, result JSON,
client/server directories and nine `screenshots/bulk-ponder-*.png` captures.
Require `Railward BULK-PONDER client probe passed`, successful client/server
exits and no client crash report. The earlier coal/overhanging-supply layout
under `.devdocs/tmp/bulk-ponder-20261030/` is superseded.

These are virtual tutorial-world checks. Formation, hand interactions and
transfer timing are scripted while item counts, capabilities, multiblock NBT
and native renderers are exercised. The bunker uses its actual pull-only
handler; the visible hopper represents extraction at compressed tutorial
speed. No dedicated-server gameplay or live train/hopper-network change is
claimed. Retain the human pass in `manual-release-checklist.md`.

## Cistern Hatch and Fluid Terminal Ponder tutorials вЂ” 2026-10-27

Both items offer two pair-focused scenes, five captions each. They explain the
analogy with Create's Portable Fluid Interface, the terminal directly above the
hatch at a block-height difference of 1 or 2 (zero or one empty block between),
automatic lid/hose movement, carriage-local storage, comparator connection
output and redstone control. Assembly, pump operation, tank-level explanations,
station scheduling and train-stopping tutorials were removed.

The supplied ChME3 leads the supplied cistern on one track. Their couplers are
adjacent at x=13/14; both vehicles occupy one moving section, with all four
bogeys animated during arrival and departure. The decorative iron pillar,
gantry and pump pedestals were removed. The comparator/support sits screen-left (east); the lever/support sits
screen-right (west), with the comparator output facing east to read the terminal. Both
controls are visible beside the pair. The compact platform is 14 by 8 blocks,
with a 16-block framing region and view scale 0.8 instead of 0.35. The hatch
and terminal are the focus; the decorative locomotive may extend beyond the
platform and viewport. Both scenes retain Create's standard isometric
camera (-35/145 degrees). The locomotive is rotated 180 degrees, with its cab
at the rear beside the wagon and its long hood forward. All supply equipment
is behind the wagon at z=11; the pump at x=3 is beyond its end toward the right
edge of the view. Facing states, copycat materials, secondary panel facings,
whistle rotation and railing parts rotate with the locomotive.

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Canonical-first compilation, compileAll, clean buildAndCollect, 19/19 unit tests and compileClientTestAll | PASS | PASS | PASS | PASS |
| Four storyboard registrations, 81 normalized cistern parts, empty-supply playback, 16000 mB conserved and 8000 mB exchanged both ways | PASS | PASS | PASS | PASS |
| Adjacent couplers, one moving section, all four bogeys animated, no iron station supports, flanking controls, correct comparator facing, rear cab, background supply and standard camera | PASS | PASS | PASS | PASS |
| Rendered lid/hose, redstone disconnect/reconnect, comparator 15/0, closed departure and eight UI captures | PASS (ru_ru) | PASS (uk_ua) | PASS (en_us) | PASS (ru_ru) |
| Collected JARs: matching schematics, scene classes, 34 Ponder keys per language, no test classes | PASS | PASS | PASS | PASS |
| Full human W-key navigation/reading-pace/visual pass in every language | NOT RUN | NOT RUN | NOT RUN | NOT RUN |

Evidence: `.devdocs/tmp/fluid-swap-compile-118.log`,
`fluid-swap-compile-all.log`, `fluid-swap-clean-build.log` (63 tasks) and
`verify_ponder_jars.py`. Runtime evidence lives in
`.devdocs/tmp/fluid-swap-20261027/`: `<target>-client/`,
`<target>-server/`, their launch logs and `<target>-result.json` files. Require
the `Railward FLUID-PONDER client probe passed` marker including
`standard isometric camera, rear-facing cab, supply behind wagon`, successful
client/server exits, no crash report in the accepted client directory and
eight `screenshots/fluid-ponder-*.png` captures.

These scenes illustrate inventory exchange in a virtual world; gameplay train
and fluid-network code is unchanged. Keep the human checks in
`manual-release-checklist.md`.

## Signal Ponder tutorials вЂ” 2026-10-26

Follow-up from human review: the original renderless disconnected-state check
missed the cached source retained by a rendered repeater. `SchematicWorld`
removed the source from its map without setting its removed flag, so subsequent
visible-section ticks could restore INVALID and flashing lights. The scene now
marks the source removed before replacing it with air. The new regression
warms each cache in an actually rendered invalid scene, then removes the source
and checks that the repeater remains dark for 80 client ticks. The wrench scene
also turns all four parts through 15 sequential steps (337.5 degrees).

Three shared-source tutorials cover installation, tall-signal aspects and
ground-signal aspects. Both items register installation plus their own aspect
scene. Captions explain the actual 1вЂ“4-block downward source lookup, four-block
tall-head clearance, wrench rotation, Create-controlled routing and the ground
head's green/yellow-to-white mapping. Gameplay signal logic is unchanged.

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Canonical-first compilation, `compileAll`, clean `buildAndCollect`, 19/19 unit tests and `compileClientTestAll` | PASS | PASS | PASS | PASS |
| Collected JAR: matching schematic, scene classes, 22 signal Ponder keys in each of three languages, no test classes | PASS | PASS | PASS | PASS |
| Isolated dedicated server plus separate-client Ponder playback: four registrations, real schematic/BE loading, all four animated aspects and disconnected state | PASS | PASS | PASS | PASS |
| Rendered wrench demonstration observes all 15 sequential steps, 16 distinct renderer states and consistent rotation of all four parts | PASS | PASS | PASS | PASS |
| Both rendered repeaters warm their invalid-source cache, then stay dark for 80 client ticks after source removal | PASS | PASS | PASS | PASS |
| Client UI rendered six preview frames and exited successfully, no crash report | PASS (ru_ru) | PASS (uk_ua) | PASS (en_us) | PASS (ru_ru) |
| Human W-key entry, full tutorial/navigation/reading-pace review in all languages | NOT RUN | NOT RUN | NOT RUN | NOT RUN |

Build evidence: `.devdocs/tmp/ponder-fix-compile-118.log`,
`ponder-fix-compile-all.log`, `ponder-fix-clean-build.log` and
`verify_ponder_jars.py`. The clean build executed all 63 tasks successfully.
Runtime evidence is under `.devdocs/tmp/ponder-fix-20261026/`. Each target uses
`<target>-client/`, `<target>-server/` and corresponding
`<target>-client-launch.log` / `<target>-server-launch.log` files. All four
clients logged the `Railward PONDER client probe passed` marker, saved six
previews and exited zero. The marker explicitly includes
`15 rendered wrench steps` and
`both cached-source disconnects stayed dark for 80 client ticks`.
All four servers then stopped cleanly over RCON with exit code zero.
`ports-results.json` records the successful target/language pairs. The
six screenshots per client show installation, each head's normal aspect,
15-step rotation, tall head disconnected and ground head disconnected.
The canonical rotation and both disconnected screenshots were inspected.
The probe clears vanilla join toasts and hides the background HUD in its
isolated client so these do not obscure the tutorial captures.

Initial attempts and the earlier three-preview probe are retained separately
under `.devdocs/tmp/ponder-20261026/`; they do not verify the cached-source fix.
Create 5's `PonderWorld` needs a client level, so probes wait for a dedicated
server connection. Ponder playback drives illustrated state changes explicitly;
it does not claim a new verification of real railway signaling or a complete
human visual/reading-pace pass. Retain the human checks in
`manual-release-checklist.md`.

## Triple-axle locomotive bogey v2 вЂ” 2026-10-09

The approved v2 OBJ frame, centered wheelset and 256Г—256 atlas replace the
placeholder in shared `src/main/resources` under the original resource IDs.
Wheel radius, axle spacing, renderer offsets, connector anchors, bogey style
registration and train stopping/navigation logic are unchanged. Forge 40/43
use explicit legacy OBJ lighting/UV keys; resource processing translates those
keys to Forge 47's native schema for both 1.20.1 targets.

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Clean `buildAndCollect`, 19/19 unit tests, and separate `compileClientTestAll` | PASS | PASS | PASS | PASS |
| Collected JAR contains current OBJ/MTL/PNG, target-native OBJ schema, no test classes | PASS | PASS | PASS | PASS |
| Isolated dedicated server startup and separate-client Forge join | PASS | PASS | PASS (fresh client retry) | PASS |
| Actual baked OBJ counts: 3788 frame quads, 200 wheelset quads; outward normals, sprite UV bounds, shaded/non-emissive surfaces | PASS | PASS | PASS | PASS |
| Production buffer renderer: fixed frame, three centered 0/90-degree wheel rotations, matching rotated normals, dark/full packed light | PASS | PASS | PASS | PASS |
| Separate Flywheel 1 `SpecialModels.smoothLit` frame/wheelset mesh construction | N/A | N/A | N/A | PASS |
| Human placed/moving bogey appearance, rail height, all facings and Flywheel GPU on/off comparison | NOT RUN | NOT RUN | NOT RUN | NOT RUN |

Evidence: `.devdocs/tmp/bogey-v2-install-20261009/<target>/server/logs/`
and `client/logs/` (`client-retry/logs/` for 1.20.1/Create 5). Each successful
client logged `Railward BOGEY-MODEL client probe passed` and exited with code
zero; none of those successful client directories contains a crash report.
`clean buildAndCollect compileClientTestAll` exited successfully with all
63 tasks executed. `.devdocs/tmp/bogey-v2-install-20261009/verify_jars.py`
compared the collected OBJ/MTL/PNG bytes to the source assets and checked
the per-target JSON flags and absence of test classes in all four release JARs.
The initial 1.20.1/Create 5 client timed out during login, before any model
assertion; a fresh client completed the handshake and all checks. The probe
does not claim GPU backend or assembled-train visual verification. Repeat
the bogey item in `manual-release-checklist.md` before visual acceptance.

## Result notation

- `PASS` вЂ” performed successfully, with a date and short evidence note.
- `PARTIAL` вЂ” a recorded subset passed, but the row still has required work.
- `FAIL` вЂ” reproducible defect; include an issue or log reference.
- `BLOCKED` вЂ” cannot currently be performed; state the reason.
- `NOT RUN` вЂ” no runtime claim has been made.

Store disposable worlds and logs below `.devdocs/tmp/`. Do not use the primary
1.18.2 `run/` world for automated or destructive checks.

## Automated baseline

| Check | 1.18.2 | 1.19.2 | 1.20.1/Create 5 | 1.20.1/Create 6 |
| --- | --- | --- | --- | --- |
| Clean compile, unit tests, reobfuscation and JAR collection | PASS (2026-10-09, `clean buildAndCollect`) | PASS (2026-10-09, `clean buildAndCollect`) | PASS (2026-10-09, `clean buildAndCollect`) | PASS (2026-10-09, `clean buildAndCollect`) |
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
| Dedicated server reaches `Done` and Railward server-start handler runs | PASS (2026-10-09, isolated bogey-model fixture) | PASS (2026-10-09, isolated bogey-model fixture) | PASS (2026-10-09, isolated bogey-model fixture) | PASS (2026-10-09, isolated bogey-model fixture) |
| Separate client completes Forge handshake and joins dedicated server | PASS (2026-10-09, `bogey_models`) | PASS (2026-10-09, `bogey_models`) | PASS (2026-10-09, `bogey_models`, fresh client retry) | PASS (2026-10-09, `bogey_models`; earlier 2026-10-04 both-backend shader smoke retained) |

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
| RELAY-01 | P0 | Relay Cabinet | Independently delimit blocks with cabinets; pass a long train in both directions, clear after its tail and preserve occupancy across topology changes and reload. Verify state-only external input, train-only output, adjacent release, unrestricted AI and all four model facings. | PARTIAL (2026-11-05: 33/33 dedicated GameTests on all four targets and separate 1.18.2/Create 6 clients passed occupancy-state synchronization and model checks; assembled-train drive, placement/wrench interaction and camera-dependent lighting remain manual) |
| CROSSING-01 | P0 | Rail Crossing Signal | Select a Relay Cabinet before placing. Verify each facing and its settings square, all sign options, ten-tick lamp alternation, all three sounds, release and saved settings. | PARTIAL (Dedicated GameTests passed on all four targets. A separate 1.18.2 client confirmed live settings, lamps, all three sound loops and release. See the Rail Crossing Signal section for evidence and the manual checks.) |
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

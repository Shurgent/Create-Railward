# Manual release checklist

Run this short human-inspection pass after the automated 1.18.2 P0 probes are
complete. Use a dedicated 1.18.2 server with separate driver and observer
clients and a disposable world. Record the date and evidence in
`functional-test-matrix.md`; do not mark an item from compilation or logs alone.

## Canonical 1.18.2 visual and interaction pass

- [ ] Drive forward, coast, brake and reverse. The modern-controller lever is
  smooth and agrees for driver and observer; releasing control returns it to
  idle.
- [ ] Run the NPC schedule in both directions. Buffer-light colors follow the
  authoritative direction and do not flicker or disagree between clients.
- [ ] Run through representative speed signs, whistle signs and all four cab
  signal aspects in both directions. Driver and observer see matching sign and
  cab-signal models; each whistle is heard once from the correct world position.
- [ ] Manually couple, decouple and automatically couple from both approach
  directions. Coupler models animate and remain attached to the correct outer
  carriage after each topology change.
- [ ] Watch one mounted cargo fill, fluid, door and controller state change from
  both clients and after one reconnect. The cargo renderer follows the stored
  fill level; sounds and particles start and stop once with no stale or
  duplicated transient effect.
- [ ] Form representative x-long and z-long bulk-cargo containers, then inspect
  empty, partially filled, full, split and re-formed states. The visible fill
  matches the stored amount and no inner/outer wall is missing or duplicated.
- [ ] Orbit representative panels, roof pieces, doors, railings, lights,
  spotlight, cistern and cargo blocks in every supported facing. Lighting stays
  world-facing and stable; animated parts use the correct local pivot.
- [ ] Inspect the v2 triple-axle locomotive bogey placed on both track axes
  and on a moving assembled train: approved geometry/texture, all suspension
  and motor parts present, wheels centered on the rails and normal day/night
  lighting. Repeat on the ports, including Create 6 with Flywheel enabled
  and disabled; the automated model/buffer probe is not a GPU visual pass.
- [ ] Exercise the primary and secondary whistle, arrival sound, unloading
  falling-item stream and loop, and diesel exhaust as driver and observer. Each
  effect looks correct, starts/stops once and remains spatially consistent.
- [ ] Hold W over each Railward signal item and play both Ponder scenes in
  Russian, Ukrainian and English. Check camera/framing, captions, navigation,
  tall green/red/yellow/flashing-red/dark states, and ground blue/white/
  flashing-blue/dark states. The ground head must show white for both green
  and yellow Create source states. Repeat on the ports, especially Create 6.
  In the wrench demonstration, the whole tall signal takes 15 steps through
  almost one full turn; both heads stay completely dark throughout the
  disconnected-source caption after their invalid flashing example.
- [ ] Hold W over both the Cistern Hatch and Fluid Terminal and play both fluid
  tutorials. Inspect the locomotive leading the cistern on the same track as
  one moving consist, cab at the rear and long hood forward, with all four bogeys
  animated. Keep the standard isometric view with the hatch/terminal large and
  central; allow the decorative locomotive beyond the compact platform and
  viewport. Check the comparator/support on the left and lever/support on the
  right, both unobscured. The supply pipe/tank sit behind
  the wagon and the pump is visible beyond its end toward the right edge. Check readable
  captions, visible lid opening/closure and hose extension/retraction, reservoir
  level changes in both flow directions, redstone disconnect/reconnect and
  closed departure, the explicit 1–2-block terminal height, clear hose/hatch
  sightlines and absence of the old support pillar.
  Repeat framing and text checks in all three languages and
  on the ports; the illustrative scenes do not replace live fluid-transfer tests.

- [ ] Hold W over each Bulk Cargo Container colour, the Unloading Hatch and the
  Unloading Bunker. Inspect square/rectangular/tall assembly, the complete-layer
  right-click prompt, separate touching colours, thrown-item intake, empty-hand
  stack extraction and hopper input/output. In the wagon scene, check the
  supplied halfcar travelling with ChME3, clear overhead loading and falling
  cargo into the bunker, active extraction and empty departure. Retain standard
  isometry and focus on the cargo mechanisms. Check reading pace, navigation,
  particles and audio in all three languages and on the ports.

## Port gate: repeat for 1.19.2, 1.20.1/Create 5 and 1.20.1/Create 6

- [ ] Launch the collected release JAR in an isolated ordinary Forge client,
  with the matching Create release and required dependencies, and reach the
  main menu. In particular, check Create 6.0.8: `runClient` uses deobfuscated
  dependencies and cannot rule out the `m_7392_(Entity, ...)` bridge-method
  mixin crash. Then render a moving train with Railward couplers and spotlight.
- [ ] Confirm the recorded `create.FluidNetworkMixin` audit still matches the
  exact packaged Create version. Re-run the disabled/enabled multi-tank
  regression whenever the dependency changes; keep the mixin only while the
  upstream bug remains.
- [ ] On Create 6, keep `fixCreate6SignalOverlayLighting=true` and compare
  signal markers on straight and curved track in the same scene. Disable it
  only to diagnose a confirmed `SignalVisual` mixin conflict.
- [ ] Join a dedicated server with a separate client; assemble, drive, save and
  reload a train containing a modern controller, couplers, bulk cargo and a
  partially filled cistern.
- [ ] Perform one manual couple/decouple, one permitted automatic coupling, one
  rejected over-speed automatic coupling and one item/fluid transfer while an
  observer tracks the train.
- [ ] Check metadata, recipes, representative block/item models and render
  layers. A port-specific defect blocks only that port's publication and must
  not reduce 1.18.2 behavior.
- [x] Recheck the port regressions found on 2026-10-03: arrival sound with both
  Create and modern whistles; train-roof material geometry on Railward slices,
  half-panels and wall panels; full and wedge roof partitions; and, on 1.20.1,
  colored on/off large-signal lamps plus diesel exhaust from both driver and
  observer views, including correct depth/compositing against clouds.

## Evidence record

- Date and Minecraft/Create versions:
- Dedicated server, driver and observer log directories:
- Screenshots or short recording:
- Failed checklist item and reproduction steps, if any:

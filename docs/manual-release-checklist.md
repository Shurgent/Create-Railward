# Manual release checklist

Run this short human-inspection pass after the automated 1.18.2 P0 probes are
complete. Use a dedicated 1.18.2 server with separate driver and observer
clients and a disposable world. Record the date and evidence in
`functional-test-matrix.md`; do not mark an item from compilation or logs alone.

## Canonical 1.18.2 visual and interaction pass

- [ ] Bind Relay Cabinets to a track in both directions. Place the cabinet
  from all four horizontal directions: the model's original north face must
  face the player. Wrench each face and confirm horizontal quarter-turns,
  matching selection/collision shapes, correct atlas UVs and stable lighting.
  Drive through a cabinet-delimited block in both directions; check train-only
  redstone output and the occupied/free goggles status. Power an empty cabinet
  externally and confirm it changes status without powering its neighbor;
  two adjacent cabinets must release after the train clears both tracks.

- [ ] Select a Relay Cabinet with the Rail Crossing Signal item and place the
  signal from all four directions. Confirm the selected cabinet gets a gold
  outline while holding the item and the outline disappears when switching
  items. Wrench the signal through four quarter-turns and check that aiming
  at its front shows the white square and clicking inside opens settings. Apply
  None, Single-track and Multi-track and inspect the signs from both sides.
  Drive a train through the cabinet's section and check alternating red lamps,
  both lamps off after release, stable lighting while orbiting, and the
  positional audio for the bell, ring and siren. Switch sounds while occupied
  and verify that playback stops when the section clears. Move away from the
  signal and check that sound fades to silence at 32 blocks.

- [ ] Play the Whistle Sign Ponder scene. Check that the sign faces the
  approaching train and that turning it with a wrench preserves the track
  selection. Check the schedule prompt, the whistle at the marked crossing,
  and the silent return and seek across the marker.

- [ ] Play the Rail Crossing Signal Ponder scene from the item. Check the
  cabinet highlight, alternating lamps and warning as the train passes,
  release, the white settings square and all three sign previews. Replay
  and scrub the occupied segment, then leave Ponder during the warning.

- [ ] Play the Speed Limit Sign Ponder scene. Check the track selection arrow,
  the sign facing the approaching train, the readable number and value box,
  slowdown at the marked wheel crossing,
  retained limit on the return trip and the marker after turning the sign.

- [ ] Play the Modern Train Controller Ponder scene. Check the one-controller
  cart, all three smoothly animated levers, forward/reverse/forward/off lamp
  colours (forward toward the controller; reverse toward the free deck end),
  a fixed controller housing, smooth seated-passenger turns, camera
  orbits and readable captions about the reverser key and stopping/releasing
  both traction and brake. Repeat on the ports and in all three languages.

- [ ] Play the Buffer Lights and Locomotive Spotlight Ponder scenes. Inspect
  all 13/2 housing variants, direct panel attachments, white/red/off states,
  both spotlight directions, control-filter disabling/re-enabling and readable
  captions. Inspect the 3 x 3 cart on rails, its single bogey, central seat,
  two ordinary train controls with permanently visible levers (an outline
  marks the selected one) and
  the seated bee smoothly turning toward it. Check paired buffer lights on
  the outer blocks with red lenses outside; only one changes housing style.
  Lights must face away from opposite cart ends; smooth
  half-turns must reveal each end while preserving isometric pitch. Repeat
  on the ports and in all three languages.

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
  carriage after each topology change. Repeat automatic coupling in the Nether
  and End; after wrench uncoupling, confirm that the pair remains separated
  until the couplers move more than 1.5 blocks apart.
- [ ] With Steam 'n' Rails installed, use matching heads across standalone
  decorative couplers, Wooden Headstocks and Copycat Headstocks for automatic
  and wrench coupling and uncoupling, including mixed bases in both orders.
  Confirm that different styles and Railward/SnR pairs remain separate.
  Check a saved assembled wagon after
  reloading, the driver's prompt/sound, and both approach directions. SnR
  models remain static; visual separation on curves is expected.
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
- [ ] Inspect the triple-axle locomotive and 18-100 freight bogeys on both
  track axes before assembly, on a moving train and after disassembly. Check
  rail alignment, wheel rotation, geometry, textures and day/night lighting.
  Repeat on the ports with Flywheel enabled and disabled.
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
  rolling in the direction of travel. Check that long rails and vehicles are
  not sliced inside the viewport. Keep the standard isometric view with the
  hatch/terminal large and
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
- [ ] Hold W over each Cistern colour and play its building and pipe-connection
  scenes. Check the solid 3 x 3 cross-section, 9/25-block end extension,
  1/3/5 diameters, 11-block length, separate colours and both axes. Inspect the
  end/side/top pipe models and highlighted connection areas, readable captions
  and standard isometric framing in all three languages and on the ports.

- [ ] Hold W over each Bulk Cargo Container colour, the Unloading Hatch and the
  Unloading Bunker. Inspect square/rectangular/tall assembly, the complete-layer
  right-click prompt, separate touching colours, thrown-item intake, empty-hand
  stack extraction and hopper input/output. In the wagon scene, check the
  supplied halfcar travelling with ChME3, wheels rolling with the train, no
  clipping inside the viewport, clear overhead loading and falling
  cargo into the bunker, active extraction and empty departure. Retain standard
  isometry and focus on the cargo mechanisms. Check reading pace, navigation,
  particles and audio in all three languages and on the ports.

- [ ] Play all three Train Coupler Ponder scenes. Check the close-up view of
  facing heads beside the open cargo platform, slow locomotive approach,
  joint departure, wrench prompts and
  the wagon remaining behind after splitting. Wheels must follow both travel
  directions. Inspect the name labels and the visible controls in the
  one-controls, both-controls and neither-controls examples. Confirm readable
  captions, intact offstage rails and visible navigation at normal GUI scales,
  keeping standard isometry. Repeat on the ports and in EN/RU/UK.

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

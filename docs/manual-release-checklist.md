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
- [ ] Exercise the primary and secondary whistle, arrival sound, unloading
  falling-item stream and loop, and diesel exhaust as driver and observer. Each
  effect looks correct, starts/stops once and remains spatially consistent.

## Port gate: repeat for 1.19.2 and 1.20.1

- [ ] Confirm the recorded `create.FluidNetworkMixin` audit still matches the
  exact packaged Create version. Re-run the disabled/enabled multi-tank
  regression whenever the dependency changes; keep the mixin only while the
  upstream bug remains.
- [ ] Join a dedicated server with a separate client; assemble, drive, save and
  reload a train containing a modern controller, couplers, bulk cargo and a
  partially filled cistern.
- [ ] Perform one manual couple/decouple, one permitted automatic coupling, one
  rejected over-speed automatic coupling and one item/fluid transfer while an
  observer tracks the train.
- [ ] Check metadata, recipes, representative block/item models and render
  layers. A port-specific defect blocks only that port's publication and must
  not reduce 1.18.2 behavior.

## Evidence record

- Date and Minecraft/Create versions:
- Dedicated server, driver and observer log directories:
- Screenshots or short recording:
- Failed checklist item and reproduction steps, if any:

# Railward Ponder scenes

Hold Create's Ponder key (W by default) over a Railward signal, Cistern Hatch or
Fluid Terminal item. Signals offer installation plus their own aspect scene;
both fluid components offer two scenes focused on their paired connection.
Captions and titles are provided in English, Russian and Ukrainian.

| Scene | Available on | Teaches |
| --- | --- | --- |
| Installing Railward signals | Tall and ground signals | Select track with a Create signal, place the source below the Railward base, four-block search range, tall head clearance, wrench rotation, source configuration |
| Reading the tall signal | Tall signal | Green, red, chain-signal yellow, invalid flashing red, disconnected dark head, manual driving responsibility |
| Reading the ground signal | Ground signal | Red maps to blue; green and yellow map to white; invalid flashing blue; disconnected dark head |
| The hatch and terminal pair | Cistern Hatch and Fluid Terminal | Analogy with Create's Portable Fluid Interface, exact 1–2-block height, automatic lid/hose, per-carriage storage |
| Fluid exchange through the pair | Cistern Hatch and Fluid Terminal | The paired connection in both transfer directions, connection comparator output and redstone control |
| Building and using bulk storage | All three Bulk Cargo Container colours | Square, rectangular and tall multiblocks; complete layers; separate colours; top intake, empty-hand stack extraction and inventory automation |
| Loading and unloading a bulk wagon | All three containers, Unloading Hatch and Unloading Bunker | Overhead loading into the supplied halfcar, its floor hatch above a bunker, active extraction through the bunker and departure |

Shared scene code lives in `client/ponder/`. Create 5 registers storyboards with
`PonderRegistrationHelper`; Create 6 registers a client-only `PonderPlugin`.
Stonecutter handles the small scene API/package differences.

The shipped `assets/railward/ponder/signals.nbt` is a vanilla structure with
DataVersion 2975 (1.18.2), a 7 x 7 base at y=0, east-west track at z=4/y=1,
Create source signals at (2,1,2) and (4,1,2), a tall Railward signal at (2,2,2)
with all four parts, and a ground signal at (4,2,2). All ports use this asset.
Both heads use rotation 14, facing the Ponder camera; its pitch is lowered to
-15 degrees so the opaque lamp hoods do not hide the lit lenses.
Visible Ponder sections tick their block entities once rendering initializes
their ticker lists. Initial aspects are also set explicitly for fast-forward
before the first rendered frame. The scene invalidates the Create source before
removing it: the schematic world drops block entities from its map without
marking them removed, so a repeater could otherwise retain a cached invalid
source and keep flashing. Playback does not simulate a live railway graph or
change gameplay signal behavior.

The wrench demonstration cycles all four tall-signal parts through 15 steps,
one every six ticks (337.5 degrees over 4.5 seconds), while its caption is shown.

## Fluid tutorial assets

`assets/railward/ponder/cistern_transfer.nbt` derives its wagon and locomotive
from the supplied `tank_car_lgray.nbt` and `chme3.nbt`. The originals remain
unchanged. The local authoring helper `tools/generate_fluid_ponder.py` (in the
repository-ignored `tools/` directory) reads those files and bakes one 38 x 10 x 12
vanilla 1.18.2 structure, with no external Python dependencies:

```powershell
python tools/generate_fluid_ponder.py run/schematics/tank_car_lgray.nbt run/schematics/chme3.nbt
```

The wagon is translated by (3,2,6). The locomotive is rotated 180 degrees around
its structure's center, then translated by (14,1,5), placing the cab at the rear
beside the wagon and the long hood forward. Facing states, copycat materials,
secondary panel facings and railing parts rotate with the locomotive. Their couplers
are adjacent at x=13/14 on one track at z=7. All 81
cistern blocks are rebound to the scene controller at (4,3,6), with normalized
`LastKnownPos` and `Controller` tags. Inventory contents and original-world
coordinates are excluded. The hatch is at (8,6,7), the stationary terminal at
(8,8,7), and a windowed station reservoir at (1,1,11). The supply pipe, pump and
reservoir sit behind the wagon at z=11. The pump at (3,3,11) is beyond the wagon
end toward the right edge of the standard isometric view. Both fluid scenes
retain Create's default camera rotations; visibility comes from the layout.
Both vehicles stay in one
independent section throughout both scenes and move together; all four bogeys
animate. The platform covers only x=1..14/z=4..11; the decorative locomotive
extends beyond it. A 16-block framing region and 0.8 view scale keep the hatch
and terminal prominent, without fitting the locomotive into the view.
The lever sits screen-right (west), with its support at (7,8,7), and
the comparator sits screen-left (east), with its support at (9,7,7) and its
output facing east so it reads the terminal. There is no industrial-
iron pillar, gantry or pump pedestal. The locomotive's own original blocks
are retained.

The scenes initialize the virtual terminal before applying scripted connection
NBT. Its real block-entity renderer and the hatch's normal tick animate hose
extension and lid movement. The complete consist and four bogey animations illustrate
arrival/departure. For predictable tutorial playback, connection NBT and fluid
inventory changes are scripted rather than creating a live train or server
pipe network. Fluid exchange is a background illustration of the pair; captions
do not teach Create assembly, pump direction, reservoir levels, station waits
or train stopping. Both scenes state that the terminal's block is 1 or 2 blocks
above the hatch's block, leaving zero or one empty block between them.
With default Create capacity settings, the example conserves 16000 mB, loads
8000 mB into the wagon, then returns it. Transfer instructions simulate the
available source amount and target space before moving fluid, so empty supplies
or reduced tank capacities remain valid no-op/partial transfers.

## Automated client check

Run 1.18.2 first, then repeat with each other target. Start a dedicated server
in a separate disposable directory first (with an accepted EULA); Ponder on
Create 5 needs a client world as context. From Windows PowerShell:

```powershell
.\gradlew.bat :1.18.2:runClient '-PrailwardRunDirectory=.devdocs\tmp\ponder-check\1.18.2' -PrailwardClientProbe=true -PrailwardClientProbeMode=ponder -PrailwardConsoleLogLevel=info
```

The separate `clientTest` probe compiles all four registered item/storyboard
combinations, loads the real schematic, plays every instruction, checks all
four aspects and the dark disconnected state, checks current-language titles
and captions, then renders six previews and exits. The additional previews
observe all 15 wrench steps through the renderer's block state and first warm
each repeater's cache on an invalid source, remove that source, and require its
lights to stay off for 80 subsequent client ticks. This exercises the rendered
section tickers that a renderless fast-forward alone does not initialize. Require
`Railward PONDER client probe passed`, exit code zero and no crash report.
Previews are saved in the isolated run directory's `screenshots/` folder.
Set `lang:ru_ru` or `lang:uk_ua` in that directory's options to check those
languages. Automated clients have music muted by the existing probe setup.

Use `-PrailwardClientProbeMode=ponder_fluid` for the separate fluid-scene probe.
It loads all four item/storyboard combinations, validates all 81 cistern parts
and localized text, checks conserved volume and both transfer directions, then
renders eight checkpoints: open hatch, loading, unloading, redstone-disabled,
reconnected, departing wagon, closed hatch and the arrival view centered on the pair.
It also checks adjacent couplers, one moving section, all four animated bogeys,
the flanking controls, absence of industrial-iron station supports, default
isometric camera, rear cab and relocated background supply.
It requires the real lid/hose
animations, comparator output 15/0, correct power state and unchanged total
volume, and separately plays an empty-supply scene to catch exhausted-source
errors. Require `Railward FLUID-PONDER client probe passed`, eight
`screenshots/fluid-ponder-*.png` captures, exit code zero and no crash report.

This check does not verify actual track signaling or replace a person checking
the full tutorials, camera angles, reading pace, scene navigation and W-key
entry from each item. Keep those checks in the manual release checklist.

## Bulk cargo tutorials

`bulk_storage.nbt` supplies a compact nine-block stage. The storyboard forms
1x1x1, 3x1x3, 5x1x2 and 3x3x3 examples (X/Y/Z), then adjacent green and blue
volumes with independent controllers. The interactive example collects 1152
sand from thrown stacks, removes exactly 64 for the empty-hand right-click,
then transfers 512 in and 512 out through inventory capabilities. It finishes
with 1088 in the container, 512 in the output barrel and an empty supply barrel.

`bulk_transport.nbt` uses the supplied `halfcar_green.nbt` at (3,4,6) and the
same reversed ChME3 at (14,3,5). All 54 wagon cells are normalized to controller
(4,5,6), with a 9x3 footprint and height 2. Both vehicles move in one section;
all four bogeys animate. The platform stays compact and the standard camera
keeps the loader, cargo and bunker prominent, allowing the locomotive offscreen.
The train moves four blocks from its loading stop to align its floor hatch
with bunker (11,2,6), two blocks below it. The overhead supply contains 46656
sand; loading fills half of the wagon's capacity and unloading moves all of it
into a light-blue 9x3x1 receiving container through the bunker. The visible
hopper below the bunker represents the inventory extractor. Inventory movement
is scripted at tutorial speed; it is not a live server hopper-network simulation.

The local ignored authoring helper is `tools/generate_bulk_ponder.py`. It reads
both supplied schematics without changing them. Multiblock states and inventory
geometry are baked/illustrated in the client scene, without changing gameplay
connectivity. Native hatch and bunker falling-item renderers show train transfers;
the latter uses its real pull-only capability. Only the manual throw/pickup
demonstration creates temporary `ItemEntity` objects.

Use `-PrailwardClientProbeMode=ponder_bulk` to check all eight registrations,
8+6 localized captions, multiblock geometry, controller normalization, colour
separation, item conservation, native transfer effects and standard isometry.
The opt-in client saves nine `screenshots/bulk-ponder-*.png` previews and exits
with `Railward BULK-PONDER client probe passed`. It remains in `clientTest` and
is excluded from release JARs. Keep the human rendering/reading-pace checks.

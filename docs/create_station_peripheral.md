# Create Station Data in CC:Tweaked

When CC:Tweaked is installed, Create: Railward extends Create's built-in
`Create_Station` peripheral. A wired modem can connect directly to the station
block; a Target Block and Display Link are not required to retrieve the data.

Without CC:Tweaked, the integration mixin is skipped and Create: Railward loads
normally.

## `getArrivals([limit])`

Returns the predicted calls at this station, ordered by arrival time. The
optional `limit` argument restricts the number of returned entries and must be
greater than zero.

Each entry contains:

- `train_id` - the train UUID;
- `train_name` - the train name;
- `destination` - the direction displayed by Create (the schedule title,
  normally the next station on the route, but it may be changed by a Rename
  instruction in the schedule);
- `station_name` - the station or filter for which the arrival was calculated;
- `arrival_ticks`, `arrival_seconds` - the remaining time until arrival;
- `create_arrival_ticks`, `create_arrival_seconds` - Create's unadjusted
  prediction, exposed for diagnostics;
- `arrival_estimate_source` - `live` on the first traversal, `historical` when
  a learned route time is available, `create` when no current leg needs
  adjustment, or `unpredictable` while blocked by a signal;
- `clock_rate` - the number of Minecraft day-time ticks advanced per server
  tick (`1` in vanilla and approximately `0.3333` with B3M
  `TimeMultiplier: 3`);
- `clock_source` - `b3m` when B3M's API supplied the rate, `observed` when it
  was measured for another time-control mod, `vanilla` before a measurement is
  available, or `frozen` when `doDaylightCycle` is disabled;
- `predictable`, `arrival_predictable` - whether Create could calculate the
  arrival time. `predictable` is retained as a short alias;
- `arrival_day`, `arrival_day_time` - the projected Minecraft day and raw
  day-time value;
- `arrival_hour`, `arrival_minute`, `arrival_time` - the projected Minecraft
  clock time. `arrival_time` uses the `HH:MM` format;
- `dwell_ticks`, `dwell_seconds` - the calculated stop duration. For a train
  already at the station, this is the remaining stop duration;
- `departure_predictable` - whether a departure can be calculated safely;
- `departure_ticks`, `departure_seconds` - the remaining time until departure;
- `departure_day`, `departure_day_time`, `departure_hour`,
  `departure_minute`, `departure_time` - the projected Minecraft departure
  time.

Minecraft raw day time uses `0` for 06:00. The formatted `HH:MM` fields use a
normal clock where `00:00` is midnight. Arrival and dwell durations are server
ticks, while the projected clock fields account for the actual rate at which
the world's day time advances.

Unknown numeric values are `-1`, unknown formatted times are empty strings, and
the corresponding `*_predictable` field is `false`.

Create 0.5.1 applies a conservative `distance / speed * 2` estimate on the
first traversal of a route. This estimate counts down roughly twice as fast as
world time while the train is moving at a steady speed. Create: Railward
removes that safety factor for the first traversal. After a completed
traversal, it uses Create's learned transit time together with the current
elapsed transit ticks, producing a stable projected clock time.

An occupied signal has no deterministic release time. While a train is waiting
for one, the adjusted arrival and departure fields are marked unpredictable;
the unadjusted `create_arrival_*` fields remain available.

B3M is detected through its optional public API and is not a required
dependency. For example, a five-second Scheduled Delay is 100 server ticks. At
`TimeMultiplier: 3`, those 100 ticks advance the Minecraft clock by about 33
day-time ticks, or two displayed Minecraft minutes. Other time-control mods are
supported by measuring the change in `dayTime` against ordinary server ticks.
Sleeping and `/time` command jumps are discarded as clock-rate samples.

```lua
local station = peripheral.find("Create_Station")

for _, arrival in ipairs(station.getArrivals(8)) do
    if arrival.arrival_predictable then
        local departure = arrival.departure_predictable
            and arrival.departure_time
            or "unknown"

        print(("%s -> %s: %s, departs %s"):format(
            arrival.train_name,
            arrival.destination,
            arrival.arrival_time,
            departure
        ))
    else
        print(("%s -> %s: unknown"):format(
            arrival.train_name,
            arrival.destination
        ))
    end
end
```

### Departure prediction rules

Create can predict travel time independently of station wait conditions.
Create: Railward only reports a future departure when all alternative wait
condition branches consist entirely of Scheduled Delay conditions. Since a
train leaves when the first alternative branch completes, the shortest timed
branch is used.

For a train already at the station, completed conditions and the live progress
of its current Scheduled Delay are taken into account. A paused schedule or an
unresolved condition such as cargo, redstone, player presence, or time of day
makes the departure unpredictable.

### Refresh behavior

`getArrivals()` asks Create for live predictions and schedule progress on every
call. It does not use the Display Link forecast cache, which Create refreshes
every 100 game ticks (about five seconds). Sleeping or changing the world time
with a command is therefore reflected by the next Lua call.

When `doDaylightCycle` is disabled, the projected Minecraft clock remains at
its current value while relative arrival and departure tick counts continue to
be reported. Polling once per second is usually sufficient for a display,
allows an unknown time-control mod's rate to be measured, and avoids scanning
all active trains every game tick.

## `getTrainSchedule()`

Returns the schedule of the train currently present at the station. It uses the
same structure as Create's built-in `getSchedule()` method and adds live runtime
state:

- `train_id`, `train_name`;
- `current_entry` - the current schedule entry, starting at 1;
- `entry_count` - the number of schedule entries;
- `state` - `pre_transit`, `in_transit`, or `post_transit`;
- `paused`, `completed`, `auto_schedule`;
- `current_destination` - the current navigation target, or an empty string;
- `departure_predictable`, `departure_ticks`, `departure_seconds`;
- `clock_rate`, `clock_source`;
- `departure_day`, `departure_day_time`, `departure_hour`,
  `departure_minute`, `departure_time`;
- `entries`, `cyclic`, and the other fields from Create's standard schedule
  table.

The departure fields use the current progress of the train's wait conditions.
They are unknown unless the train is waiting at the station and its remaining
wait can be calculated from Scheduled Delay conditions.

If there is no train at the station, or the train has no schedule, the method
raises a normal Lua error. Use `pcall` when this condition should be handled.

```lua
local station = peripheral.find("Create_Station")
local ok, schedule = pcall(station.getTrainSchedule)

if ok then
    print(schedule.train_name)
    print(("%d/%d, %s"):format(
        schedule.current_entry,
        schedule.entry_count,
        schedule.state
    ))

    if schedule.departure_predictable then
        print("Departure: " .. schedule.departure_time)
    end
end
```

All existing Create station methods (`getSchedule`, `setSchedule`,
`getTrainName`, `isTrainPresent`, and others) remain unchanged.

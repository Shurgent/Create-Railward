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

## Networked train information board

The following program discovers every color monitor and `Create_Station`
peripheral visible on the computer's modem network. It combines and sorts the
arrivals from all stations, then draws the same board on every monitor. Each
monitor is laid out independently, so the `Destination` column uses all space
left by the fixed-width columns.

With one connected station, the platform is always `1`. With multiple
stations, the last number in each station name is used. Consequently, platform
station names should end in a number, for example `Spawn Central P1`. A station
whose name does not end in a number is shown as `--`.

While a train is present at its station, its `Arr` value is shown as ` now `.
Every refresh discovers the peripherals again. If no stations are currently
loaded, connected monitors show an empty board; if no monitors are loaded, the
refresh does nothing. A peripheral which unloads in the middle of a refresh is
skipped and can be discovered again by the next pass.

Save the program as `startup.lua` to start it automatically:

```lua
local BOARD_TITLE = "TRAIN DEPARTURES"
local TEXT_SCALE = 0.5
local REFRESH_SECONDS = 1

local TRAIN_WIDTH = 8
local ARRIVAL_WIDTH = 5
local DEPARTURE_WIDTH = 5
local PLATFORM_WIDTH = 2
local SEPARATOR_WIDTH = 1
local FIXED_WIDTH = TRAIN_WIDTH
    + ARRIVAL_WIDTH
    + DEPARTURE_WIDTH
    + PLATFORM_WIDTH
    + SEPARATOR_WIDTH * 4
local MINIMUM_WIDTH = FIXED_WIDTH + 1

local function fit(value, width)
    local text = tostring(value or "")
    if #text > width then
        return text:sub(1, width)
    end
    return text .. string.rep(" ", width - #text)
end

local function writeAt(monitor, x, y, text)
    monitor.setCursorPos(x, y)
    monitor.write(text)
end

local function findColorMonitors()
    local monitors = {}

    for _, monitor in ipairs({ peripheral.find("monitor") }) do
        local ok, width, height = pcall(function()
            if not monitor.isColor() then
                return nil, nil
            end

            monitor.setTextScale(TEXT_SCALE)
            return monitor.getSize()
        end)

        if ok and type(width) == "number" and type(height) == "number" then
            table.insert(monitors, {
                device = monitor,
                width = width,
                height = height
            })
        end
    end

    return monitors
end

local function findStations()
    local stations = {}

    for _, station in ipairs({ peripheral.find("Create_Station") }) do
        local ok, stationName = pcall(station.getStationName)
        if ok and type(stationName) == "string" then
            table.insert(stations, {
                device = station,
                name = stationName
            })
        end
    end

    return stations
end

local function getPlatform(stationInfo, stationCount)
    if stationCount == 1 then
        return "1"
    end

    return stationInfo.name:match("(%d+)%s*$") or "--"
end

local function sortableArrivalTicks(arrival)
    local ticks = tonumber(arrival.arrival_ticks)
    if not ticks or ticks < 0 then
        return math.huge
    end
    return ticks
end

local function collectArrivals(stations, limit)
    local result = {}

    for _, stationInfo in ipairs(stations) do
        local station = stationInfo.device
        local platform = getPlatform(stationInfo, #stations)
        local scheduleOk, presentSchedule = pcall(station.getTrainSchedule)
        local presentTrainId = scheduleOk
            and type(presentSchedule) == "table"
            and presentSchedule.train_id
            or nil
        local ok, arrivals = pcall(station.getArrivals, limit)

        if ok and type(arrivals) == "table" then
            for _, arrival in ipairs(arrivals) do
                if type(arrival) == "table" then
                    table.insert(result, {
                        train_id = arrival.train_id,
                        train_name = arrival.train_name,
                        destination = arrival.destination,
                        arrival_ticks = arrival.arrival_ticks,
                        arrival_time = arrival.arrival_time,
                        arrival_predictable = arrival.arrival_predictable,
                        departure_time = arrival.departure_time,
                        departure_predictable = arrival.departure_predictable,
                        platform = platform,
                        at_station = presentTrainId ~= nil
                            and arrival.train_id == presentTrainId
                    })
                end
            end
        end
    end

    table.sort(result, function(a, b)
        local aTicks = sortableArrivalTicks(a)
        local bTicks = sortableArrivalTicks(b)

        if aTicks ~= bTicks then
            return aTicks < bTicks
        end
        return tostring(a.train_name or "") < tostring(b.train_name or "")
    end)

    return result
end

local function displayTime(value, predictable)
    if predictable and type(value) == "string" and #value == 5 then
        return value
    end
    return "--:--"
end

local function displayArrivalTime(arrival)
    if arrival.at_station then
        return " now "
    end
    return displayTime(arrival.arrival_time, arrival.arrival_predictable)
end

local function drawBoard(monitorInfo, arrivals)
    local monitor = monitorInfo.device
    local width = monitorInfo.width
    local height = monitorInfo.height

    monitor.setBackgroundColor(colors.black)
    monitor.setTextColor(colors.white)
    monitor.clear()

    local clock = " " .. textutils.formatTime(os.time(), true) .. " "
    local clockX = math.max(1, width - #clock + 1)

    monitor.setBackgroundColor(colors.blue)
    monitor.setTextColor(colors.white)
    writeAt(monitor, 1, 1, fit(BOARD_TITLE, width))

    monitor.setBackgroundColor(colors.yellow)
    monitor.setTextColor(colors.black)
    writeAt(monitor, clockX, 1, clock)

    if width < MINIMUM_WIDTH or height < 3 then
        return
    end

    local destinationWidth = width - FIXED_WIDTH
    local trainX = 1
    local destinationX = trainX + TRAIN_WIDTH + SEPARATOR_WIDTH
    local arrivalX = destinationX + destinationWidth + SEPARATOR_WIDTH
    local departureX = arrivalX + ARRIVAL_WIDTH + SEPARATOR_WIDTH
    local platformX = departureX + DEPARTURE_WIDTH + SEPARATOR_WIDTH

    monitor.setBackgroundColor(colors.black)
    monitor.setTextColor(colors.white)
    writeAt(monitor, trainX, 2, fit("Train", TRAIN_WIDTH))
    writeAt(monitor, destinationX, 2, fit("Destination", destinationWidth))
    writeAt(monitor, arrivalX, 2, fit("Arr", ARRIVAL_WIDTH))
    writeAt(monitor, departureX, 2, fit("Dep", DEPARTURE_WIDTH))
    writeAt(monitor, platformX, 2, fit("Plat", PLATFORM_WIDTH))

    monitor.setTextColor(colors.green)
    for row = 3, height do
        local arrival = arrivals[row - 2]
        if arrival then
            writeAt(monitor, trainX, row, fit(arrival.train_name, TRAIN_WIDTH))
            writeAt(
                monitor,
                destinationX,
                row,
                fit(arrival.destination, destinationWidth)
            )
            writeAt(
                monitor,
                arrivalX,
                row,
                fit(
                    displayArrivalTime(arrival),
                    ARRIVAL_WIDTH
                )
            )
            writeAt(
                monitor,
                departureX,
                row,
                fit(
                    displayTime(
                        arrival.departure_time,
                        arrival.departure_predictable
                    ),
                    DEPARTURE_WIDTH
                )
            )
            writeAt(
                monitor,
                platformX,
                row,
                fit(arrival.platform, PLATFORM_WIDTH)
            )
        end
    end
end

local function refreshBoard()
    local monitors = findColorMonitors()
    local stations = findStations()
    local maximumRows = 0

    for _, monitorInfo in ipairs(monitors) do
        maximumRows = math.max(maximumRows, monitorInfo.height - 2)
    end

    local arrivals = {}
    if maximumRows > 0 then
        arrivals = collectArrivals(stations, maximumRows)
    end

    for _, monitorInfo in ipairs(monitors) do
        pcall(drawBoard, monitorInfo, arrivals)
    end
end

while true do
    -- A peripheral can unload between discovery and use. Never let one failed
    -- refresh terminate startup.lua; the next pass discovers everything again.
    pcall(refreshBoard)
    sleep(REFRESH_SECONDS)
end
```

`peripheral.find()` includes peripherals reached through an attached wired
modem network, so the program does not depend on computer side names and also
handles peripherals being added or removed while it runs. In CC:Tweaked,
wireless modems exchange messages between computers but do not expose remote
peripherals to `peripheral.find()`. A wireless link therefore requires a
computer at the remote wired network to forward station data or display
commands over `rednet`.

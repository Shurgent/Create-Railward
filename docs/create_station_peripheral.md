# Данные станции Create в CC:Tweaked

Если CC:Tweaked установлен, Create: Railward расширяет штатную периферию
станции Create с типом `Create_Station`. Проводной модем можно подключить прямо
к блоку станции; Target Block и Display Link для получения данных не нужны.

Без CC:Tweaked интеграционный Mixin не применяется, и Create: Railward
загружается в обычном режиме.

## `getArrivals([limit])`

Возвращает прогнозируемые прибытия на эту станцию, отсортированные по времени.
Необязательный `limit` ограничивает число строк.

Каждая строка содержит:

- `train_id` — UUID поезда;
- `train_name` — имя поезда;
- `destination` — направление, показываемое Create на табло (schedule title;
  обычно следующая станция маршрута, но оно может быть изменено инструкцией
  Rename в расписании);
- `station_name` — станция/фильтр, для которого рассчитано прибытие;
- `arrival_ticks` — время до прибытия в тиках;
- `arrival_seconds` — время до прибытия в секундах;
- `predictable` — удалось ли Create рассчитать время.

Если время пока неизвестно, `arrival_ticks` и `arrival_seconds` равны `-1`, а
`predictable` равен `false`.

```lua
local station = peripheral.find("Create_Station")

for _, arrival in ipairs(station.getArrivals(8)) do
    if arrival.predictable then
        print(("%s -> %s: %.0f sec"):format(
            arrival.train_name,
            arrival.destination,
            arrival.arrival_seconds
        ))
    else
        print(("%s -> %s: unknown"):format(
            arrival.train_name,
            arrival.destination
        ))
    end
end
```

## `getTrainSchedule()`

Возвращает расписание поезда, который сейчас стоит на станции, в том же формате,
что и штатный `getSchedule()`, и добавляет состояние его выполнения:

- `train_id`, `train_name`;
- `current_entry` — текущий пункт расписания, начиная с 1;
- `entry_count` — количество пунктов;
- `state` — `pre_transit`, `in_transit` или `post_transit`;
- `paused`, `completed`, `auto_schedule`;
- `current_destination` — текущая фактическая цель навигации или пустая строка;
- `entries`, `cyclic` и остальные поля штатного расписания Create.

Если поезда на станции или расписания у него нет, функция завершится обычной
Lua-ошибкой, которую при необходимости можно обработать через `pcall`.

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
end
```

Все прежние функции станции Create (`getSchedule`, `setSchedule`,
`getTrainName`, `isTrainPresent` и другие) остаются без изменений.

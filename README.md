# what is zpm?

ZPM is a mobile app / web server bundle that lets you gather event occurence data.
We include the timestamp and the geo location per event occurency. We may put in the 'event type' in the future.

Finally, it clusters nearby occurence data at a given interval to get occurence / minute metric (zpm).


# Why?

The idea is to do geospatial visualizations that change as time passses. 
* Heatmaps that change color (red is high `zpm`, green is low `zpm`)
* dotted maps that change with `tap` data?

This is just a tool to let us do the data gathering.

# table schema
The server is doing the operations in PostgreSQL. here are the schemas:

### tap table

| `timestamp`  | `lat` | `long` | `zpm_id` |
| ------------ | ----- | ------ | -------- |
| 124012412499 | 102   | 90     | 101      |
| 125125192512 | 102   | 90.1   | 101      |

`zpm_id` is a foreign key reference to `zpm_id` in zpm table.

### zpm table

measure zpm in time intervals.

| `zpm_id` | `timestamp_start` (when we start recording this zpm) | `start_lat` | `start_long` | `end_lat` | `end_long` | `zpm` | `interval` (interval of measurement in seconds) |
| -------- | ---------------------------------------------------- | ----------- | ------------ | --------- | ---------- | ------------------------------------------------------------ | ----------------------------------------------- |
| 101      | 12401240124                                          | 102         | 90.1         | 102       | 90.2       | 2                                                            | 15                                              |
| 2        | 124012409124                                         | 102         | 90.1         | 102       | 90.1       | 0                                                            | 30                                              |

Each zpm table entry (one) has zero or more tap entries (many).

NOTE: `zpm_id `(calculated by finding # of taps with same zpm_id, then multiplying by `60 / interval` 

POSSIBLE DEPRECATION: `zpm` may or may not be deleted, depending whether we want to optimize for space (then delete) or computation (keep so that we don't have to do count zpm queries)

# built with
* Android SDK (mobile client)
* Node.js express server (server prototype)

# TODO
* add event type when recording taps
* Change zpm table schema, as well as android app, to record `start_lat/long` and `end_lat/long`. 

# Analytics map

The starter records the raw facts needed to compare Lyft and Uber without
guessing how either platform defines “booked.” OPS can derive these metrics
after JSON import or API synchronization.

| Metric | Source |
|---|---|
| Door-to-door time | `SHIFT_STARTED` through `HOME_ARRIVED` |
| Online time | `SHIFT_STARTED` through `SHIFT_ENDED`, less breaks |
| Pickup approach time | `NEXT_PICKUP_STARTED` through `PASSENGER_PICKED_UP` |
| Passenger time | `PASSENGER_PICKED_UP` through `PASSENGER_DROPPED_OFF` |
| Return-home deadhead | `SHIFT_ENDED` through `HOME_ARRIVED` |
| Queued offer outcome | queued event followed by start, disappear, or cancel |
| Manual vs auto performance | ride acquisition mode plus queue-mode changes |
| Total outing miles | ending odometer minus starting odometer |
| Phase GPS miles | filtered location trail grouped by `phase` and `rideId` |
| FN outbound travel | `FN_TRIP_STARTED` through `FN_ARRIVED_SITE` |
| FN onsite/work time | arrival, check-in, work-complete, and checkout events |
| FN return travel | `FN_RETURN_STARTED` through `FN_RETURN_COMPLETED` |

Earnings and platform statement values are deliberately not guessed by the
tracker. They should be added during OPS review/import so the raw driving facts
remain independent of Lyft or Uber’s reporting definitions.

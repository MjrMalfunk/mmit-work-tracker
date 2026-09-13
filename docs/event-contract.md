# MMIT Work Tracker event contract v2

Every event is immutable and carries a device-generated UUID, shift UUID,
optional ride UUID, UTC occurrence time, location, accuracy, and payload.

```json
{
  "contract_version": "mmit.tracker.events.v2",
  "device_id": "note10-local-id",
  "events": [
    {
      "client_event_id": "uuid-v4",
      "shift_id": "uuid-v4",
      "ride_id": "uuid-v4-or-null",
      "event_type": "PASSENGER_DROPPED_OFF",
      "occurred_at": "2026-09-10T21:17:42.120Z",
      "location": {
        "latitude": 41.0793,
        "longitude": -85.1394,
        "accuracy_meters": 7.2
      },
      "payload": {}
    }
  ]
}
```

The eventual OPS endpoint must enforce uniqueness on
`(device_id, client_event_id)` and return `ACCEPTED`, `DUPLICATE`, or `REJECTED`
for every submitted event.

# Outing mileage and offline return

Gig exports retain the `mmit.work-tracker.v2` raw packet and add `mileageSummary.version = 1`.
FieldNation exports retain their existing workflow and do not receive the gig summary.

- `totalOdometerMiles` = final odometer at home minus starting odometer. It includes return travel.
- An optional `shift.offlineOdometer` is the reading **at going offline**, not a later reading.
- With that reading, `beforeOfflineOdometerMiles` plus `offlineOdometerMiles` equals the total.
- Without it, both odometer segments remain null. Do not convert GPS estimates into odometer readings.
- `onlineWindowGps` and `offlineGps` contain independently labelled GPS estimates and quality counts.
  GPS links that cross the offline timestamp are allocated proportionally by time; this is an estimate.
  Duplicate timestamps do not add distance. Invalid fixes and implausible speeds are rejected without
  bridging across them. Gaps longer than 60 seconds and uncovered window edges remain explicit.
- The pre-offline odometer/GPS window can include off-duty breaks. `beforeOfflineIncludesBreaks`
  marks this; those miles require review rather than automatic business classification.
- `onlineMinutes` excludes recorded breaks. `offlineMinutes` runs from going offline to effective arrival.
- A late arrival correction is driver-entered; it is never inferred from missing GPS or stillness.
  Original home-arrival and completion timestamps remain intact. The corrected timestamp is stored
  separately and audit events retain the original tap and each correction.

## OPS integration

Display the full outing odometer total once. Offline return is a component of that total, never an
additional distance. The existing OPS form's additive offline-mile field must not be populated
alongside full-outing odometers until its calculation is updated. Keep platform booked miles separate.
If an offline boundary reading is absent, show the odometer split as unknown and show the GPS split
as estimates. Do not manufacture an offline odometer from the GPS estimate or scale GPS to match it.

## Upgrade

Database migration 3 to 4 adds two nullable fields and leaves existing outings and raw events intact.
Old outings may be re-exported for a GPS summary without inventing missing odometer boundaries.
New version: 0.6.4 (11). Install a release signed with the same key as the installed app; do not
uninstall the current app to resolve signing issues because its outings are stored locally.

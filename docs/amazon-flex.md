# Amazon Flex tracking — v0.7.0

Choose Flex. Enter the scheduled block date, 24-hour start time, hours, offered pay and optional station. Enter the starting odometer, then tap **Start trip to pickup** when leaving home. This starts the outing and GPS, not the scheduled block clock.

At pickup, tap **Arrived at pickup station**. Waiting/loading are work time. Tap **Loaded — start deliveries** when ready to leave. When finished, choose **Deliveries done — go home**, or **Deliveries done — return packages to station** if a station return is required. Required returns count as work until **Packages returned — go home**. A station release without a route has its own action and audit event.

Record the odometer when all Flex work finishes if safely available, then tap **Arrived home — lock time** at home and complete the final readings. Home travel is included in the outing odometer total once. Starting another gig requires finishing this outing first.

Scheduled time, actual station-to-finish time and actual door-to-door time are separate. Rate figures use driver-entered offered pay and are labelled gross; they are not confirmed payout or profit. Add final earnings, tips/adjustments and vehicle costs in OPS. The existing OPS manual Flex form supports this. The older automatic JSON preview accepts Lyft/Uber only, so Flex JSON is retained as supporting evidence until the OPS importer is extended; do not select Lyft to force an import.

JSON retains the v2 envelope with platform AMAZON_FLEX, adds a flexBlock object, and preserves raw events, GPS diagnostics, location phases and odometer summary. Room 5→6 adds five nullable columns; existing outings are preserved. Android application ID and signing remain unchanged.

For the first block: tomorrow, 04:15, 3.5 hours, $79; scheduled finish 07:45. Enter actual readings yourself. The app does not create that outing until you tap start.

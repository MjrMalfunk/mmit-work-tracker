# MMIT Work Tracker

Offline-first Android ride tracker for Midwest Managed IT's Gig Work workflow.

## MVP scope

- Native Kotlin + Jetpack Compose UI.
- Explicit shift start with platform, queue mode, and odometer.
- Foreground GPS recording only while a shift is active.
- Manual-accept and auto-queue ride acquisition.
- Pending queued rides that do not overlap current passenger mileage.
- One-action `Drop off + next pickup` transition.
- Encrypted-device local Room persistence and idempotent UUID event records.
- JSON shift export for review before OPS integration.
- Return-home tracking after going offline, completed by the home odometer.
- Notification actions for pickup, drop-off, queued ride, and drop-off + next.
- FieldNation work-order mode with outbound, onsite, work, checkout, and return phases.
- Round-trip or one-way/next-stop FieldNation tracking.
- Database migration from v0.1 that preserves existing outings.

## State rule

Accepting or auto-queuing another ride while a passenger is onboard creates a
`PENDING` ride. The next ride begins accumulating pickup time and mileage only
after the current ride is dropped off and the driver confirms that the queued
ride was awarded.

## Development

Open this directory in Android Studio with JDK 17, install Android SDK 35, and
run the `app` configuration on the Note10 or an emulator running Android 8+.

The Note10 does not need a hotspot while driving. All events and GPS points are
stored locally; use **Export JSON for OPS** after the outing when Wi-Fi is
available. Leave the app exempt from Samsung battery optimization during road
testing so Android does not suspend its foreground location service.

## First road-test checklist

1. Grant location and notification permissions.
2. Enter the starting odometer and choose Lyft or Uber plus Accept/Auto queue.
3. Confirm the persistent notification appears after starting.
4. Run one short outing and exercise a queued-ride-disappeared case if possible.
5. Go offline at the final drop, drive home, then enter the ending odometer.
6. Export the JSON and retain it until OPS confirms a successful import.

This is a source-ready starter, not a signed production APK. Build and install
it from Android Studio first; signing and automatic OPS synchronization belong
in the next milestone after the road-test controls are verified.

This source package intentionally does not contain signing keys, credentials,
API tokens, build output, or a production OPS endpoint.

## Upgrade from v0.1

Build and run v0.2 with the same application ID. Room migration 1→2 adds the
FieldNation fields while retaining previously recorded rideshare outings.

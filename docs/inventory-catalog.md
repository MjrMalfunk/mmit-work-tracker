# Inventory catalog in v0.6.6

FieldNation material entry searches a locally saved catalog by SKU and description. Selecting a result records its exact SKU; the quantity label follows its unit. The picker shows last-reported stock and the catalog's sync time. An over-stock warning does not block field capture because that snapshot may be stale. OPS V2 validates actual stock when the completed outing is imported.

## Loading the catalog

- With a reachable HTTPS OPS V2 origin: connect using the existing V2 email, password and six-digit OTP. The app refreshes after login, on resume and after connectivity events, with a five-minute freshness throttle. Refresh stock bypasses the throttle.
- With desktop-only V2: Download catalog in its Inventory panel, transfer the JSON to the phone, then Import catalog JSON in the tracker. This path requires no phone network or login. The desktop HTTP origin is file metadata; the app never sends credentials to it.

The original outing JSON contract remains unchanged: `materials` contains `{sku, quantity, notes?}`. Capture adds to an existing SKU line and limits its total to 10,000 with three-decimal quantity precision. It does not reserve or deduct central stock. Import a completed outing against the matching FieldNation work order in V2 to apply its usage.

## Authentication and persistence

The live client calls `/api/v1/auth/login`, `/api/v1/inventory` and `/api/v1/auth/logout`. OWNER, ADMIN and OPERATOR accounts can read inventory. Only root HTTPS origins are request targets, redirects are rejected, normal platform TLS verification applies, and requests have bounded timeouts and response sizes. Passwords and OTPs are ephemeral. Session tokens use AES-GCM with an Android Keystore key; they are absent from catalog files and outing exports. App backup remains disabled.

The validated catalog is written atomically to app-private storage. Connection failures or malformed responses preserve the last good snapshot. An expired session retains offline search and requires sign-in to refresh. Changing account/source clears the previous catalog; disconnect clears both session and catalog. A manual file cannot cross an active account boundary or replace a newer snapshot from that account/source. No Room schema change is needed; existing v0.6.5 outing history remains in database version 5.

Catalog file schema: `mmit.inventory-catalog.v1`, with `serverUrl`, `userId`, `email`, `syncedAtEpochMs`, and `items` containing `sku`, `description`, `unit`, `quantityOnHand`, `unitCost`, `active`. It contains no tokens, passwords, OTPs, stock movements or user lists. Decimal stock and cost values may be JSON strings, matching V2's API representation.

## Verification

Unit coverage includes offline search, invalid stock/SKUs, exact origins, quantity bounds, network failure, malformed response, expiry, cancellation, account separation, inactive filtering, zero stock, refresh throttling, offline disconnect and desktop-file import. Browser coverage exercises the actual V2 download code against fixtures shaped like the real API, including `/auth/me`'s flat `data` object, read-only requests, secret exclusion and error recovery. Live server login and physical-phone UI checks still need the user's environment.

Primary API/UI references: [Compose menus](https://developer.android.com/develop/ui/compose/components/menu), [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [connectivity state](https://developer.android.com/training/monitoring-device-state/connectivity-status-type).

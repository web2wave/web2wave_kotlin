## 1.2.1

- Fix `cancelSubscription` and `chargeUser` reporting failure on success: the backend returns `"success": true`; `true`, `1`, `"1"` and `"true"` are now all accepted
- Failure messages now come from the backend's `error_msg` (then `message`), including for non-2xx responses (e.g. 403 missing API key scope, 400 cancel/refund failure, 422 validation); network errors keep their original exception
- `updateUserProperty` / `set*ProfileID` failures carry the backend's error message instead of "Unexpected response"
- `chargeUser` sends `price_id` as a JSON integer
- Send `Accept: application/json` so validation errors come back as JSON
- Treat any 2xx status as success

## 1.2.0

- Add `setApphudProfileID` and `setSuperwallProfileID` (sync web subscriptions to Apphud / Superwall)

## 1.1.0

- Send `device_model` header for identify fingerprinting (`Build.MODEL`)

## 1.0.1

- Fingerprint headers and `identify()` deferred deeplink support

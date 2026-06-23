---
"@phantom/react-native-webview": patch
---

Honor `mediaCapturePermissionGrantType="deny"` on Android. Previously the Android
setter was a no-op and the value was ignored. Now `RNCWebChromeClient.onPermissionRequest`
short-circuits and calls `request.deny()` before reading the requested resources, showing
the site-attributed `AlertDialog`, or triggering an OS CAMERA/RECORD_AUDIO permission
request. Other grant-type values (and the default `prompt`) preserve the existing Android
prompt behavior, and iOS behavior is unchanged.

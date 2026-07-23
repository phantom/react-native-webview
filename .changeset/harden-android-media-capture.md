---
"@phantom/react-native-webview": patch
---

Honor `mediaCapturePermissionGrantType="deny"` on Android by rejecting camera
and microphone resources before the existing permission flow can display a
prompt. Other permission resources and grant-type values retain their current
behavior.

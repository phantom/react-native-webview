---
"@phantom/react-native-webview": patch
---

Quote `injectedJavaScriptObject` JSON as a JavaScript string on Android and
Apple platforms. This preserves nested JSON and control characters while
preventing template-literal interpolation from executing injected code.

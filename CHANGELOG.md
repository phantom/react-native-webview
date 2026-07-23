# @phantom/react-native-webview

## 1.2.4

### Patch Changes

- 78dc86f: Honor `mediaCapturePermissionGrantType="deny"` on Android by rejecting camera
  and microphone resources before the existing permission flow can display a
  prompt. Other permission resources and grant-type values retain their current
  behavior.
- a45290f: Quote `injectedJavaScriptObject` JSON as a JavaScript string on Android and
  Apple platforms. This preserves nested JSON and control characters while
  preventing template-literal interpolation from executing injected code.

## 1.2.3

### Patch Changes

- 4f46080: Fixes and improvements

## 1.2.2

### Patch Changes

- c71924c: Prevent showing beforeunload event alerts and always allow proceeding

## 1.2.1

### Patch Changes

- dbe76c7: Implement MIME type blacklist.

## 1.2.0

### Minor Changes

- 176afdb: Added a new prop that allows marking webviews as active or inactive.

## 1.1.1

### Patch Changes

- 1388353: Add isMainFrame to new arch serialization layer

## 1.1.0

### Minor Changes

- a08be33: - Merged 16 commits from upstream/master
  - Upgraded androidx.webkit:webkit from 1.4.0 to 1.14.0
  - Added SSL error handling for sub-resources
  - Added Payment Request API support (disabled downloads for security)
  - Preserved Phantom's custom changes:
    - Package name and version (1.0.2)
    - Download blocking with toast message
    - All existing security configurations

## 1.0.2

### Patch Changes

- 9052aae: Implement android alerts when permissions are requested by the webpage

## 1.0.1

### Patch Changes

- 17a37dc: added isMainFrame to onMessage callbacks

## 1.0.0

### Major Changes

- 141fb62: Fork complete

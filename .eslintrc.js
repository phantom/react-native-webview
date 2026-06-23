module.exports = {
  extends: ['@react-native', 'prettier'],
  rules: {
    'prettier/prettier': [
      'error',
      {
        quoteProps: 'consistent',
        singleQuote: true,
        tabWidth: 2,
        trailingComma: 'es5',
        useTabs: false,
      },
    ],
    // Security guard (AGE-347): ban the `injectedJavaScriptObject` prop.
    // On Android the native bridge serializes the value and wraps it in a
    // backtick template literal without escaping (see
    // `injectJavascriptObject` in
    // android/src/main/java/com/reactnativecommunity/webview/RNCWebView.java),
    // so any value containing a backtick or `${...}` can break out of the
    // literal and execute arbitrary JavaScript inside the WebView (UXSS).
    // Until the native escaping is fixed, the prop must not be used.
    'react/forbid-component-props': [
      'error',
      {
        forbid: [
          {
            propName: 'injectedJavaScriptObject',
            message:
              'Do not use the `injectedJavaScriptObject` prop: on Android its ' +
              'value is wrapped in an unescaped backtick template literal in ' +
              'the native bridge, allowing template-literal breakout and ' +
              'arbitrary JS execution in the WebView (UXSS). See AGE-347.',
          },
        ],
      },
    ],
  },
};

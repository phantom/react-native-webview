package com.reactnativecommunity.webview;

final class RNCJavaScriptString {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private RNCJavaScriptString() {}

    static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder quoted = new StringBuilder(value.length() + 2);
        quoted.append('"');
        for (int index = 0; index < value.length(); index += 1) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    quoted.append("\\\"");
                    break;
                case '\\':
                    quoted.append("\\\\");
                    break;
                case '\b':
                    quoted.append("\\b");
                    break;
                case '\f':
                    quoted.append("\\f");
                    break;
                case '\n':
                    quoted.append("\\n");
                    break;
                case '\r':
                    quoted.append("\\r");
                    break;
                case '\t':
                    quoted.append("\\t");
                    break;
                default:
                    if (character <= 0x1f || character == '\u2028' || character == '\u2029') {
                        appendUnicodeEscape(quoted, character);
                    } else {
                        quoted.append(character);
                    }
            }
        }
        return quoted.append('"').toString();
    }

    private static void appendUnicodeEscape(StringBuilder output, char character) {
        output.append("\\u");
        output.append(HEX[(character >> 12) & 0xf]);
        output.append(HEX[(character >> 8) & 0xf]);
        output.append(HEX[(character >> 4) & 0xf]);
        output.append(HEX[character & 0xf]);
    }
}

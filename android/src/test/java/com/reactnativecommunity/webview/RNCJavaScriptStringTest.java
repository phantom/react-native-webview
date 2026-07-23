package com.reactnativecommunity.webview;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RNCJavaScriptStringTest {
    @Test
    public void quotesTemplateLiteralInjectionCharacters() {
        assertEquals(
            "\"`${alert(1)}`;globalThis.pwned=true;//\"",
            RNCJavaScriptString.quote("`${alert(1)}`;globalThis.pwned=true;//")
        );
    }

    @Test
    public void preservesNestedJsonEscapes() {
        assertEquals(
            "\"{\\\"state\\\":\\\"{\\\\\\\"key\\\\\\\":\\\\\\\"value\\\\\\\"}\\\"}\"",
            RNCJavaScriptString.quote("{\"state\":\"{\\\"key\\\":\\\"value\\\"}\"}")
        );
    }

    @Test
    public void escapesControlAndLineSeparatorCharacters() {
        assertEquals(
            "\"line\\nnext\\tvalue\\u0000\\u2028\\u2029\"",
            RNCJavaScriptString.quote("line\nnext\tvalue\u0000\u2028\u2029")
        );
    }

    @Test
    public void returnsJavaScriptNullForNull() {
        assertEquals("null", RNCJavaScriptString.quote(null));
    }
}

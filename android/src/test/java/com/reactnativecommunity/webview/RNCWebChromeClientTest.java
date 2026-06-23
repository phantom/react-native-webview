package com.reactnativecommunity.webview;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.webkit.PermissionRequest;

import org.junit.Test;

/**
 * Unit tests for {@link RNCWebChromeClient#onPermissionRequest(PermissionRequest)} covering the
 * Android enforcement of the {@code mediaCapturePermissionGrantType="deny"} prop.
 */
public class RNCWebChromeClientTest {

  private RNCWebChromeClient createClient() {
    RNCWebView webView = mock(RNCWebView.class);
    return new RNCWebChromeClient(webView);
  }

  @Test
  public void denyShortCircuitsPermissionRequest() {
    RNCWebChromeClient client = createClient();
    client.setMediaCapturePermissionGrantType("deny");

    PermissionRequest request = mock(PermissionRequest.class);
    client.onPermissionRequest(request);

    // The request is denied immediately...
    verify(request, times(1)).deny();
    // ...and never reaches the resource-mapping / OS-permission / AlertDialog path,
    // which always starts by reading the requested resources.
    verify(request, never()).getResources();
    verify(request, never()).grant(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void nullGrantTypePreservesExistingPromptFlow() {
    RNCWebChromeClient client = createClient();
    // No grant type configured (default behavior).

    PermissionRequest request = mock(PermissionRequest.class);
    when(request.getResources()).thenReturn(new String[] {});

    client.onPermissionRequest(request);

    // It does NOT take the deny short-circuit: the existing flow always inspects
    // the requested resources first.
    verify(request, times(1)).getResources();
  }

  @Test
  public void unknownGrantTypePreservesExistingPromptFlow() {
    RNCWebChromeClient client = createClient();
    // An unsupported value must preserve existing prompt behavior, not deny.
    client.setMediaCapturePermissionGrantType("grant");

    PermissionRequest request = mock(PermissionRequest.class);
    when(request.getResources()).thenReturn(new String[] {});

    client.onPermissionRequest(request);

    verify(request, times(1)).getResources();
  }
}

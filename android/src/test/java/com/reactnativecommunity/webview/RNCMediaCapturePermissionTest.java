package com.reactnativecommunity.webview;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;

import android.webkit.PermissionRequest;

import org.junit.Test;

public class RNCMediaCapturePermissionTest {
    @Test
    public void denyRemovesCameraAndMicrophoneResources() {
        String[] resources = {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        };

        assertArrayEquals(
            new String[0],
            RNCMediaCapturePermission.filterRequestedResources("deny", resources)
        );
    }

    @Test
    public void denyPreservesUnrelatedResources() {
        String[] resources = {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
        };

        assertArrayEquals(
            new String[] { PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID },
            RNCMediaCapturePermission.filterRequestedResources("deny", resources)
        );
    }

    @Test
    public void otherPoliciesPreserveExistingBehavior() {
        String[] resources = { PermissionRequest.RESOURCE_VIDEO_CAPTURE };

        assertSame(
            resources,
            RNCMediaCapturePermission.filterRequestedResources("prompt", resources)
        );
    }
}

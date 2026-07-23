package com.reactnativecommunity.webview;

import android.webkit.PermissionRequest;

import java.util.ArrayList;
import java.util.List;

final class RNCMediaCapturePermission {
    private static final String GRANT_TYPE_DENY = "deny";

    private RNCMediaCapturePermission() {}

    static String[] filterRequestedResources(String grantType, String[] resources) {
        if (!GRANT_TYPE_DENY.equals(grantType)) {
            return resources;
        }

        List<String> filteredResources = new ArrayList<>(resources.length);
        for (String resource : resources) {
            if (!isMediaCaptureResource(resource)) {
                filteredResources.add(resource);
            }
        }
        return filteredResources.toArray(new String[0]);
    }

    private static boolean isMediaCaptureResource(String resource) {
        return PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
            || PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource);
    }
}

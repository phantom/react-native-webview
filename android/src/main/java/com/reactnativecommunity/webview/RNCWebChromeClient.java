package com.reactnativecommunity.webview;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.util.Log;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.uimanager.UIManagerHelper;
import com.reactnativecommunity.webview.events.TopLoadingProgressEvent;
import com.reactnativecommunity.webview.events.TopOpenWindowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RNCWebChromeClient extends WebChromeClient implements LifecycleEventListener {
    private static final String TAG = RNCWebChromeClient.class.getSimpleName();
    protected static final FrameLayout.LayoutParams FULLSCREEN_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);

    protected static final int FULLSCREEN_SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    protected static final int COMMON_PERMISSION_REQUEST = 3;

    protected RNCWebView mWebView;

    protected View mVideoView;
    protected WebChromeClient.CustomViewCallback mCustomViewCallback;

    /*
     * - Permissions -
     * As native permissions are asynchronously handled by the PermissionListener, many fields have
     * to be stored to send permissions results to the webview
     */

    // Webview camera & audio permission callback
    protected PermissionRequest permissionRequest;
    // Webview camera & audio permission already granted
    protected List<String> grantedPermissions;
    // Webview requested permissions from the OS
    protected List<String> requestedAndroidPermissions;
    // Webview granted permissions requested separately through isDialog
    protected List<String> alertPermissions;

    // Webview geolocation permission callback
    protected GeolocationPermissions.Callback geolocationPermissionCallback;
    // Webview geolocation permission origin callback
    protected String geolocationPermissionOrigin;

    // true if native permissions dialog is shown, false otherwise
    protected boolean permissionsRequestShown = false;
    // Pending Android permissions for the next request
    protected List<String> pendingPermissions = new ArrayList<>();

    protected RNCWebView.ProgressChangedFilter progressChangedFilter = null;
    protected boolean mAllowsProtectedMedia = false;

    protected boolean mHasOnOpenWindowEvent = false;

    public RNCWebChromeClient(RNCWebView webView) {
        this.mWebView = webView;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

        final WebView newWebView = new WebView(view.getContext());

        if(mHasOnOpenWindowEvent) {
            newWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading (WebView subview, String url) {
                WritableMap event = Arguments.createMap();
                event.putString("targetUrl", url);

                ((RNCWebView) view).dispatchEvent(
                    view,
                    new TopOpenWindowEvent(RNCWebViewWrapper.getReactTagFromWebView(view), event)
                );

                return true;
            }
            });
        }

        final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();

        return true;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage message) {
        if (ReactBuildConfig.DEBUG) {
            return super.onConsoleMessage(message);
        }
        // Ignore console logs in non debug builds.
        return true;
    }

    @Override
    public void onProgressChanged(WebView webView, int newProgress) {
        super.onProgressChanged(webView, newProgress);
        final String url = webView.getUrl();
        if (progressChangedFilter.isWaitingForCommandLoadUrl()) {
            return;
        }
        int reactTag = RNCWebViewWrapper.getReactTagFromWebView(webView);
        WritableMap event = Arguments.createMap();
        event.putDouble("target", reactTag);
        event.putString("title", webView.getTitle());
        event.putString("url", url);
        event.putBoolean("canGoBack", webView.canGoBack());
        event.putBoolean("canGoForward", webView.canGoForward());
        event.putDouble("progress", (float) newProgress / 100);

        UIManagerHelper.getEventDispatcherForReactTag(this.mWebView.getThemedReactContext(), reactTag).dispatchEvent(new TopLoadingProgressEvent(reactTag, event));
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        permissionRequest = request;
        grantedPermissions = new ArrayList<>();
        alertPermissions = new ArrayList<>();
        // Permissions that we need to ask permission for from the OS
        requestedAndroidPermissions = new ArrayList<>();

        for (String requestedResource : request.getResources()) {
            String androidPermission = null;
            String requestPermissionIdentifier = null;

            if (requestedResource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                androidPermission = Manifest.permission.RECORD_AUDIO;
                requestPermissionIdentifier = "microphone";
            } else if (requestedResource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                androidPermission = Manifest.permission.CAMERA;
                requestPermissionIdentifier = "camera";
            } else if(requestedResource.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                if (mAllowsProtectedMedia) {
                    grantedPermissions.add(requestedResource);
                } else {
                    // TODO: Make sure this is properly taken care of too
                    /**
                    * Legacy handling (Kept in case it was working under some conditions (given Android version or something))
                    *
                    * Try to ask user to grant permission using Activity.requestPermissions
                    *
                    * Find more details here: https://github.com/react-native-webview/react-native-webview/pull/2732
                    */
                    androidPermission = PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID;
                }
            }

            // TODO: RESOURCE_MIDI_SYSEX, RESOURCE_PROTECTED_MEDIA_ID.
            if (androidPermission != null) {
                if (ContextCompat.checkSelfPermission(this.mWebView.getThemedReactContext(), androidPermission) == PackageManager.PERMISSION_GRANTED) {
                    Activity activity = this.mWebView.getThemedReactContext().getCurrentActivity();
                    if (activity == null) {
                        // We just skip this one, but it should never happen
                        Log.w(TAG, "Current activity is null, cannot show AlertDialog. Denying permission request.");
                        continue;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(this.makeAlertMessage(request, requestPermissionIdentifier));
                    builder.setCancelable(false);
                    String finalAndroidPermission = androidPermission;
                    builder.setPositiveButton("Allow", (dialog, which) -> {
                        this.grantedPermissions.add(requestedResource);
                        this.alertPermissions.remove(finalAndroidPermission);
                        if (!this.alertPermissions.isEmpty()) {
                            // There will be more alerts to show, so we just return
                            return;
                        }
                        this.alertPermissions = null;
                        if (this.requestedAndroidPermissions.isEmpty()) {
                            // Wrapping up the permission request
                            this.permissionRequest.grant(this.grantedPermissions.toArray(new String[0]));
                            this.permissionRequest = null;
                            this.grantedPermissions = null;
                        } else {
                            // Need to request permissions from the OS, will grant or deny later
                            this.requestPermissions(this.requestedAndroidPermissions);
                        }
                    });
                    builder.setNegativeButton("Deny", (dialog, which) -> {
                        this.alertPermissions.remove(finalAndroidPermission);
                        if (!this.alertPermissions.isEmpty()) {
                            // There will be more alerts to show, so we just return
                            return;
                        }
                        this.alertPermissions = null;
                        if (!this.requestedAndroidPermissions.isEmpty()) {
                            // Need to request permissions from the OS, will grant or deny later and return now
                            this.requestPermissions(this.requestedAndroidPermissions);
                            return;
                        }
                        // Wrapping up the permission request, this time with grant or deny
                        if (!this.grantedPermissions.isEmpty()) {
                            this.permissionRequest.grant(this.grantedPermissions.toArray(new String[0]));
                        } else {
                            this.permissionRequest.deny();
                        }
                        this.permissionRequest = null;
                        this.grantedPermissions = null;
                    });                  
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    this.alertPermissions.add(finalAndroidPermission);
                    //Delay making `allow` clickable for 500ms to avoid unwanted presses.
                    Button posButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    posButton.setEnabled(false);
                    this.runDelayed(() -> posButton.setEnabled(true), 500);
                } else {
                    requestedAndroidPermissions.add(androidPermission);
                }
            }
        }

        // if we have alerts in progress we will handle below with the last alert
        if (!this.alertPermissions.isEmpty()) {
            return;
        }

        // If all the permissions are already granted, send the response to the WebView synchronously
        if (requestedAndroidPermissions.isEmpty()) {
            if (!grantedPermissions.isEmpty()) {
                request.grant(grantedPermissions.toArray(new String[0]));
                grantedPermissions = null;
            } else {
                request.deny();
            }
            return;
        }

        // Otherwise, ask to Android System for native permissions asynchronously
        requestPermissions(requestedAndroidPermissions);
    }

    private String makeAlertMessage(PermissionRequest request, String requestIdentifier) {
      Uri originUri = request.getOrigin();
      String host = originUri.getHost();
      return String.format("Allow " + host  + " to use your " + requestIdentifier + "?");
    }

    private void runDelayed(Runnable function, long delayMillis) {
        Handler handler = new Handler();
        handler.postDelayed(function, delayMillis);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {

        if (ContextCompat.checkSelfPermission(this.mWebView.getThemedReactContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            /*
             * Keep the trace of callback and origin for the async permission request
             */
            geolocationPermissionCallback = callback;
            geolocationPermissionOrigin = origin;

            requestPermissions(Collections.singletonList(Manifest.permission.ACCESS_FINE_LOCATION));

        } else {
            String alertMessage = String.format("Allow %s to use your location?", origin);
            Activity activity = this.mWebView.getThemedReactContext().getCurrentActivity();
            if (activity == null) {
                Log.w(TAG, "Current activity is null, cannot show AlertDialog. Calling negative callback.");
                callback.invoke(origin, false, false);
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(alertMessage);
            builder.setCancelable(false);
            builder.setPositiveButton("Allow", (dialog, which) -> {
                callback.invoke(origin, true, false);
            });
            builder.setNegativeButton("Don't allow", (dialog, which) -> {
                callback.invoke(origin, false, false);
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //Delay making `allow` clickable for 500ms to avoid unwanted presses.
            Button posButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            posButton.setEnabled(false);
            this.runDelayed(() -> posButton.setEnabled(true), 500);

        }
    }

    private PermissionAwareActivity getPermissionAwareActivity() {
        Activity activity = this.mWebView.getThemedReactContext().getCurrentActivity();
        if (activity == null) {
            throw new IllegalStateException("Tried to use permissions API while not attached to an Activity.");
        } else if (!(activity instanceof PermissionAwareActivity)) {
            throw new IllegalStateException("Tried to use permissions API but the host Activity doesn't implement PermissionAwareActivity.");
        }
        return (PermissionAwareActivity) activity;
    }

    private synchronized void requestPermissions(List<String> permissions) {
        /*
         * If permissions request dialog is displayed on the screen and another request is sent to the
         * activity, the last permission asked is skipped. As a work-around, we use pendingPermissions
         * to store next required permissions.
         */

        if (permissionsRequestShown) {
            pendingPermissions.addAll(permissions);
            return;
        }

        PermissionAwareActivity activity = getPermissionAwareActivity();
        permissionsRequestShown = true;

        activity.requestPermissions(
                permissions.toArray(new String[0]),
                COMMON_PERMISSION_REQUEST,
                webviewPermissionsListener
        );

        // Pending permissions have been sent, the list can be cleared
        pendingPermissions.clear();
    }


    private PermissionListener webviewPermissionsListener = (requestCode, permissions, grantResults) -> {

        permissionsRequestShown = false;

        /*
         * As a "pending requests" approach is used, requestCode cannot help to define if the request
         * came from geolocation or camera/audio. This is why shouldAnswerToPermissionRequest is used
         */
        boolean shouldAnswerToPermissionRequest = grantedPermissions != null && !grantedPermissions.isEmpty() && permissionRequest != null;

        for (int i = 0; i < permissions.length; i++) {

            String permission = permissions[i];
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && geolocationPermissionCallback != null
                    && geolocationPermissionOrigin != null) {

                if (granted) {
                    geolocationPermissionCallback.invoke(geolocationPermissionOrigin, true, false);
                } else {
                    geolocationPermissionCallback.invoke(geolocationPermissionOrigin, false, false);
                }

                geolocationPermissionCallback = null;
                geolocationPermissionOrigin = null;
            }

            if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                if (granted && grantedPermissions != null) {
                    grantedPermissions.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
                }
                shouldAnswerToPermissionRequest = true;
            }

            if (permission.equals(Manifest.permission.CAMERA)) {
                if (granted && grantedPermissions != null) {
                    grantedPermissions.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                }
                shouldAnswerToPermissionRequest = true;
            }

            if (permission.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                if (granted && grantedPermissions != null) {
                    grantedPermissions.add(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);
                }
                shouldAnswerToPermissionRequest = true;
            }
        }

        if (shouldAnswerToPermissionRequest
                && permissionRequest != null
                && grantedPermissions != null) {
            permissionRequest.grant(grantedPermissions.toArray(new String[0]));
            permissionRequest = null;
            grantedPermissions = null;
        }

        if (!pendingPermissions.isEmpty()) {
            requestPermissions(pendingPermissions);
            return false;
        }

        return true;
    };

    protected void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType) {
      this.mWebView.getThemedReactContext().getNativeModule(RNCWebViewModule.class).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback) {
      this.mWebView.getThemedReactContext().getNativeModule(RNCWebViewModule.class).startPhotoPickerIntent(filePathCallback, "");
    }

    protected void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType, String capture) {
      this.mWebView.getThemedReactContext().getNativeModule(RNCWebViewModule.class).startPhotoPickerIntent(filePathCallback, acceptType);
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        boolean allowMultiple = fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;

        return this.mWebView.getThemedReactContext().getNativeModule(RNCWebViewModule.class).startPhotoPickerIntent(filePathCallback, acceptTypes, allowMultiple, fileChooserParams.isCaptureEnabled());
    }

    @Override
    public void onHostResume() {
        if (mVideoView != null && mVideoView.getSystemUiVisibility() != FULLSCREEN_SYSTEM_UI_VISIBILITY) {
            mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
        }
    }

    @Override
    public void onHostPause() { }

    @Override
    public void onHostDestroy() { }

    protected ViewGroup getRootView() {
        return this.mWebView.getThemedReactContext().getCurrentActivity().findViewById(android.R.id.content);
    }

    public void setProgressChangedFilter(RNCWebView.ProgressChangedFilter filter) {
        progressChangedFilter = filter;
    }

    /**
     * Set whether or not protected media should be allowed
     * /!\ Setting this to false won't revoke permission already granted to the current webpage.
     * In order to do so, you'd need to reload the page /!\
     */
    public void setAllowsProtectedMedia(boolean enabled) {
      mAllowsProtectedMedia = enabled;
    }

    public void setHasOnOpenWindowEvent(boolean hasEvent) {
      mHasOnOpenWindowEvent = hasEvent;
    }
}
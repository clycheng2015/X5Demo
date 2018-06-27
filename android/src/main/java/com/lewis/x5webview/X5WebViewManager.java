package com.lewis.x5webview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.webview.events.TopLoadingErrorEvent;
import com.facebook.react.views.webview.events.TopLoadingFinishEvent;
import com.facebook.react.views.webview.events.TopLoadingStartEvent;
import com.facebook.react.views.webview.events.TopMessageEvent;
import com.lewis.x5webview.webview.OverrideUrlEvent;
import com.lewis.x5webview.webview.ProgressMessageEvent;
import com.lewis.x5webview.webview.X5WebViewConfig;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 项目名称：buyer
 * 类描述：
 * 创建人：Administrator
 * 创建时间：2018-03-14
 *
 * @version ${VSERSION}
 */

public class X5WebViewManager extends SimpleViewManager<WebView> {

    public static final String REACT_CLASS = "X5WebView";

    protected static final String HTML_ENCODING = "UTF-8";
    protected static final String HTML_MIME_TYPE = "text/html";
    protected static final String BRIDGE_NAME = "__REACT_WEB_VIEW_BRIDGE";

    protected static final String HTTP_METHOD_POST = "POST";

    public static final int COMMAND_GO_BACK = 1;
    public static final int COMMAND_GO_FORWARD = 2;
    public static final int COMMAND_RELOAD = 3;
    public static final int COMMAND_STOP_LOADING = 4;
    public static final int COMMAND_POST_MESSAGE = 5;
    public static final int COMMAND_INJECT_JAVASCRIPT = 6;
    public static final int COMMAND_SYNC_COOKIE = 7;
    public static final int COMMAND_DESTORY = 8;


    public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
    public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
    public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;

    // Use `webView.loadUrl("about:blank")` to reliably reset the view
    // state and release page resources (including any running JavaScript).
    protected static final String BLANK_URL = "about:blank";

    protected X5WebViewConfig mWebViewConfig;
    protected Context mContext;
    protected @Nullable
    WebView.PictureListener mPictureListener;


    protected static class ReactWebViewClient extends WebViewClient {

        protected boolean mLastLoadFailed = false;
        protected boolean mOverrideUrlEnable = false;
        protected @Nullable
        ReadableArray mUrlPrefixesForDefaultIntent;

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);

            if (!mLastLoadFailed) {
                X5WebViewManager.ReactWebView reactWebView = (X5WebViewManager.ReactWebView) webView;
                reactWebView.callInjectedJavaScript();
                reactWebView.linkBridge();
                emitFinishEvent(webView, url);
            }
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);
            mLastLoadFailed = false;

            dispatchEvent(
                    webView,
                    new TopLoadingStartEvent(
                            webView.getId(),
                            createWebViewEvent(webView, url)));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            boolean useDefaultIntent = false;
            if (mUrlPrefixesForDefaultIntent != null && mUrlPrefixesForDefaultIntent.size() > 0) {
                ArrayList<Object> urlPrefixesForDefaultIntent =
                        mUrlPrefixesForDefaultIntent.toArrayList();
                for (Object urlPrefix : urlPrefixesForDefaultIntent) {
                    if (url.startsWith((String) urlPrefix)) {
                        useDefaultIntent = true;
                        break;
                    }
                }
            }

            if (mOverrideUrlEnable) {
                //发消息到rn端
                dispatchEvent(view, new OverrideUrlEvent(view.getId(), url));
                return true;
            }

            if (!useDefaultIntent &&
                    (url.startsWith("http://") || url.startsWith("https://") ||
                            url.startsWith("file://") || url.equals("about:blank"))) {
                return false;
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    view.getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    FLog.w(ReactConstants.TAG, "activity not found to handle uri scheme for: " + url, e);
                }
                return true;
            }
        }

        @Override
        public void onReceivedError(
                WebView webView,
                int errorCode,
                String description,
                String failingUrl) {
            super.onReceivedError(webView, errorCode, description, failingUrl);
            mLastLoadFailed = true;

            // In case of an error JS side expect to get a finish event first, and then get an error event
            // Android WebView does it in the opposite way, so we need to simulate that behavior
            emitFinishEvent(webView, failingUrl);

            WritableMap eventData = createWebViewEvent(webView, failingUrl);
            eventData.putDouble("code", errorCode);
            eventData.putString("description", description);

            dispatchEvent(
                    webView,
                    new TopLoadingErrorEvent(webView.getId(), eventData));
        }

        protected void emitFinishEvent(WebView webView, String url) {
            dispatchEvent(
                    webView,
                    new TopLoadingFinishEvent(
                            webView.getId(),
                            createWebViewEvent(webView, url)));
        }

        protected WritableMap createWebViewEvent(WebView webView, String url) {
            WritableMap event = Arguments.createMap();
            event.putDouble("target", webView.getId());
            // Don't use webView.getUrl() here, the URL isn't updated to the new value yet in callbacks
            // like onPageFinished
            event.putString("url", url);
            event.putBoolean("loading", !mLastLoadFailed && webView.getProgress() != 100);
            event.putString("title", webView.getTitle());
            event.putBoolean("canGoBack", webView.canGoBack());
            event.putBoolean("canGoForward", webView.canGoForward());
            return event;
        }

        public void setUrlPrefixesForDefaultIntent(ReadableArray specialUrls) {
            mUrlPrefixesForDefaultIntent = specialUrls;
        }

        public void setOverrideUrlEnable(boolean overrideUrlEnable) {
            mOverrideUrlEnable = overrideUrlEnable;
        }
    }

    /**
     * Subclass of {@link android.webkit.WebView} that implements {@link LifecycleEventListener} interface in order
     * to call {@link android.webkit.WebView#destroy} on activty destroy event and also to clear the client
     */
    protected static class ReactWebView extends WebView implements LifecycleEventListener {
        protected @Nullable
        String injectedJS;
        protected boolean messagingEnabled = false;
        protected @Nullable
        X5WebViewManager.ReactWebViewClient mReactWebViewClient;

        protected class ReactWebViewBridge {
            X5WebViewManager.ReactWebView mContext;

            ReactWebViewBridge(X5WebViewManager.ReactWebView c) {
                mContext = c;
            }

            @JavascriptInterface
            public void postMessage(String message) {
                mContext.onMessage(message);
            }
        }

        /**
         * WebView must be created with an context of the current activity
         * <p>
         * Activity Context is required for creation of dialogs internally by WebView
         * Reactive Native needed for access to ReactNative internal system functionality
         */
        public ReactWebView(ThemedReactContext reactContext) {
            super(reactContext);
        }

        @Override
        public void onHostResume() {
            // do nothing
        }

        @Override
        public void onHostPause() {
            // do nothing
        }

        @Override
        public void onHostDestroy() {
            cleanupCallbacksAndDestroy();
        }

        @Override
        public void setWebViewClient(WebViewClient client) {
            super.setWebViewClient(client);
            mReactWebViewClient = (X5WebViewManager.ReactWebViewClient) client;
        }

        public @Nullable
        X5WebViewManager.ReactWebViewClient getReactWebViewClient() {
            return mReactWebViewClient;
        }

        public void setInjectedJavaScript(@Nullable String js) {
            injectedJS = js;
        }

        protected X5WebViewManager.ReactWebView.ReactWebViewBridge createReactWebViewBridge(X5WebViewManager.ReactWebView webView) {
            return new X5WebViewManager.ReactWebView.ReactWebViewBridge(webView);
        }

        public void setMessagingEnabled(boolean enabled) {
            if (messagingEnabled == enabled) {
                return;
            }

            messagingEnabled = enabled;
            if (enabled) {
                addJavascriptInterface(createReactWebViewBridge(this), BRIDGE_NAME);
                linkBridge();
            } else {
                removeJavascriptInterface(BRIDGE_NAME);
            }
        }

        public void callInjectedJavaScript() {
            if (getSettings().getJavaScriptEnabled() &&
                    injectedJS != null &&
                    !TextUtils.isEmpty(injectedJS)) {
                loadUrl("javascript:(function() {\n" + injectedJS + ";\n})();");
            }
        }

        public void linkBridge() {
            if (messagingEnabled) {
                if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // See isNative in lodash
                    String testPostMessageNative = "String(window.postMessage) === String(Object.hasOwnProperty).replace('hasOwnProperty', 'postMessage')";
                    evaluateJavascript(testPostMessageNative, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (value.equals("true")) {
                                FLog.w(ReactConstants.TAG, "Setting onMessage on a WebView overrides existing values of window.postMessage, but a previous value was defined");
                            }
                        }
                    });
                }

                loadUrl("javascript:(" +
                        "window.originalPostMessage = window.postMessage," +
                        "window.postMessage = function(data) {" +
                        BRIDGE_NAME + ".postMessage(String(data));" +
                        "}" +
                        ")");
            }
        }

        public void onMessage(String message) {
            dispatchEvent(this, new TopMessageEvent(this.getId(), message));
        }

        protected void cleanupCallbacksAndDestroy() {
            setWebViewClient(null);
            destroy();
        }
    }

    public X5WebViewManager() {
        mWebViewConfig = new X5WebViewConfig() {
            public void configWebView(WebView webView) {
            }
        };
    }

    public X5WebViewManager(X5WebViewConfig webViewConfig, Context context) {
        mWebViewConfig = webViewConfig;
        mContext = context;
    }


    public X5WebViewManager(X5WebViewConfig webViewConfig) {
        mWebViewConfig = webViewConfig;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    protected X5WebViewManager.ReactWebView createReactWebViewInstance(ThemedReactContext reactContext) {
        return new X5WebViewManager.ReactWebView(reactContext);
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        X5WebViewManager.ReactWebView webView = createReactWebViewInstance(reactContext);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                if (ReactBuildConfig.DEBUG) {
                    return super.onConsoleMessage(message);
                }
                // Ignore console logs in non debug builds.
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissionsCallback callback) {
                callback.invoke(origin, true, false);
            }

            /*添加进度变化监听*/
            @Override
            public void onProgressChanged(WebView webView, int i) {
                //发消息到rn端
                dispatchEvent(webView, new ProgressMessageEvent(webView.getId(), i));
                super.onProgressChanged(webView, i);
            }
        });
        reactContext.addLifecycleEventListener(webView);
        mWebViewConfig.configWebView(webView);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);

        // Fixes broken full-screen modals/galleries due to body height being 0.
        webView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        return webView;
    }

    @ReactProp(name = "javaScriptEnabled")
    public void setJavaScriptEnabled(WebView view, boolean enabled) {
        view.getSettings().setJavaScriptEnabled(enabled);
    }

    @ReactProp(name = "thirdPartyCookiesEnabled")
    public void setThirdPartyCookiesEnabled(WebView view, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, enabled);
        }
    }

    @ReactProp(name = "scalesPageToFit")
    public void setScalesPageToFit(WebView view, boolean enabled) {
        view.getSettings().setUseWideViewPort(!enabled);
    }

    @ReactProp(name = "domStorageEnabled")
    public void setDomStorageEnabled(WebView view, boolean enabled) {
        view.getSettings().setDomStorageEnabled(enabled);
    }

    @ReactProp(name = "userAgent")
    public void setUserAgent(WebView view, @Nullable String userAgent) {
        if (userAgent != null) {
            // TODO(8496850): Fix incorrect behavior when property is unset (uA == null)
            view.getSettings().setUserAgentString(userAgent);

        }
    }

    @ReactProp(name = "mediaPlaybackRequiresUserAction")
    public void setMediaPlaybackRequiresUserAction(WebView view, boolean requires) {
        view.getSettings().setMediaPlaybackRequiresUserGesture(requires);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(android.webkit.WebView view, boolean allow) {
        view.getSettings().setAllowUniversalAccessFromFileURLs(allow);
    }

    @ReactProp(name = "saveFormDataDisabled")
    public void setSaveFormDataDisabled(WebView view, boolean disable) {
        view.getSettings().setSaveFormData(!disable);
    }

    @ReactProp(name = "injectedJavaScript")
    public void setInjectedJavaScript(WebView view, @Nullable String injectedJavaScript) {
        ((X5WebViewManager.ReactWebView) view).setInjectedJavaScript(injectedJavaScript);
    }

    @ReactProp(name = "onMessage")
    public void setOnMessage(WebView view, @Nullable boolean message) {
        ((X5WebViewManager.ReactWebView) view).onMessage(String.valueOf(message));
    }

    @ReactProp(name = "onError")
    public void setOnError(WebView view, @Nullable String error) {
        view.getWebViewClient().onReceivedError(view, null, null);
    }

    @ReactProp(name = "onLoadStart")
    public void setOnLoadStart(WebView view, @Nullable String onLoadStart) {
        view.getWebViewClient().onPageStarted(view, onLoadStart, null);
    }

    @ReactProp(name = "onLoadEnd")
    public void setOnLoadEnd(WebView view, @Nullable String onLoadEnd) {
        view.getWebViewClient().onPageFinished(view, onLoadEnd);
    }

    @ReactProp(name = "overrideUrlEnabled")
    public void setOverrideUrlEnabled(WebView view, @Nullable boolean onLoadEnd) {
        X5WebViewManager.ReactWebViewClient client = ((X5WebViewManager.ReactWebView) view).getReactWebViewClient();
        if (client != null) {
            client.setOverrideUrlEnable(onLoadEnd);
        }
    }

    @ReactProp(name = "messagingEnabled")
    public void setMessagingEnabled(WebView view, boolean enabled) {
        ((X5WebViewManager.ReactWebView) view).setMessagingEnabled(enabled);
    }

    @ReactProp(name = "source")
    public void setSource(WebView view, @Nullable ReadableMap source) {
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                if (source.hasKey("baseUrl")) {
                    view.loadDataWithBaseURL(
                            source.getString("baseUrl"), html, HTML_MIME_TYPE, HTML_ENCODING, null);
                } else {
                    view.loadData(html, HTML_MIME_TYPE, HTML_ENCODING);
                }
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                String previousUrl = view.getUrl();
                if (previousUrl != null && previousUrl.equals(url)) {
                    return;
                }
                if (source.hasKey("method")) {
                    String method = source.getString("method");
                    if (method.equals(HTTP_METHOD_POST)) {
                        byte[] postData = null;
                        if (source.hasKey("body")) {
                            String body = source.getString("body");
                            try {
                                postData = body.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                postData = body.getBytes();
                            }
                        }
                        if (postData == null) {
                            postData = new byte[0];
                        }
                        view.postUrl(url, postData);
                        return;
                    }
                }
                HashMap<String, String> headerMap = new HashMap<>();
                if (source.hasKey("headers")) {
                    ReadableMap headers = source.getMap("headers");
                    ReadableMapKeySetIterator iter = headers.keySetIterator();
                    while (iter.hasNextKey()) {
                        String key = iter.nextKey();
                        if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
                            if (view.getSettings() != null) {
                                view.getSettings().setUserAgentString(headers.getString(key));
                            }
                        } else {
                            headerMap.put(key, headers.getString(key));
                        }
                    }
                }
                view.loadUrl(url, headerMap);
                return;
            }
        }
        view.loadUrl(BLANK_URL);
//        syncCookie();
    }

    @ReactProp(name = "onContentSizeChange")
    public void setOnContentSizeChange(WebView view, boolean sendContentSizeChangeEvents) {
        if (sendContentSizeChangeEvents) {
            view.setPictureListener(getPictureListener());
        } else {
            view.setPictureListener(null);
        }
    }

    @ReactProp(name = "mixedContentMode")
    public void setMixedContentMode(WebView view, @Nullable String mixedContentMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mixedContentMode == null || "never".equals(mixedContentMode)) {
                view.getSettings().setMixedContentMode(MIXED_CONTENT_NEVER_ALLOW);
            } else if ("always".equals(mixedContentMode)) {
                view.getSettings().setMixedContentMode(MIXED_CONTENT_ALWAYS_ALLOW);
            } else if ("compatibility".equals(mixedContentMode)) {
                view.getSettings().setMixedContentMode(MIXED_CONTENT_COMPATIBILITY_MODE);
            }
        }
    }

//    @ReactProp(name = "urlPrefixesForDefaultIntent")
//    public void setUrlPrefixesForDefaultIntent(
//            WebView view,
//            @javax.annotation.Nullable ReadableArray urlPrefixesForDefaultIntent) {
//        X5WebViewManager.ReactWebViewClient client = ((X5WebViewManager.ReactWebView) view).getReactWebViewClient();
//        if (client != null && urlPrefixesForDefaultIntent != null) {
//            client.setUrlPrefixesForDefaultIntent(urlPrefixesForDefaultIntent);
//        }
//    }


    /**
     * 同步cookie
     */
    private void syncCookie() {
        if (mContext != null) {
            CookieSyncManager.createInstance(mContext);
            CookieSyncManager.getInstance().sync();
        }
    }

    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
        // Do not register default touch emitter and let WebView implementation handle touches
        view.setWebViewClient(new X5WebViewManager.ReactWebViewClient());
    }

    @Override
    public @Nullable
    Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "goBack", COMMAND_GO_BACK,
                "goForward", COMMAND_GO_FORWARD,
                "reload", COMMAND_RELOAD,
                "stopLoading", COMMAND_STOP_LOADING,
                "postMessage", COMMAND_POST_MESSAGE,
                "injectJavaScript", COMMAND_INJECT_JAVASCRIPT,
                "syncCookie", COMMAND_SYNC_COOKIE
        );
    }

    @Override
    public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_GO_BACK:
                root.goBack();
                break;
            case COMMAND_GO_FORWARD:
                root.goForward();
                break;
            case COMMAND_RELOAD:
                root.reload();
                break;
            case COMMAND_DESTORY:
                break;
            case COMMAND_STOP_LOADING:
                root.stopLoading();
                root.clearCache(true);
                root.clearHistory();
                root.destroy();
                break;
            case COMMAND_POST_MESSAGE:
                try {
                    JSONObject eventInitDict = new JSONObject();
                    eventInitDict.put("data", args.getString(0));
                    root.loadUrl("javascript:(function () {" +
                            "var event;" +
                            "var data = " + eventInitDict.toString() + ";" +
                            "try {" +
                            "event = new MessageEvent('message', data);" +
                            "} catch (e) {" +
                            "event = document.createEvent('MessageEvent');" +
                            "event.initMessageEvent('message', true, true, data.data, data.origin, data.lastEventId, data.source);" +
                            "}" +
                            "document.dispatchEvent(event);" +
                            "})();");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                break;
            case COMMAND_INJECT_JAVASCRIPT:
                root.loadUrl("javascript:" + args.getString(0));
                break;
            case COMMAND_SYNC_COOKIE:
                String accessToken = args.getString(2);
                String refreshToken = args.getString(3);
                int accessTokenExpire = args.getInt(4);
                int refreshTokenExpire = args.getInt(5);
                String tokenCity = args.getString(6);
                String cityCode = args.getString(7);
//                CookieUtils.syncX5Cookie(mContext, args.getString(0), args.getString(1),
//                        accessToken, refreshToken, accessTokenExpire, refreshTokenExpire, tokenCity, cityCode);
        }
    }

    @Override
    public void onDropViewInstance(WebView webView) {
        super.onDropViewInstance(webView);
        ((ThemedReactContext) webView.getContext()).removeLifecycleEventListener((X5WebViewManager.ReactWebView) webView);
        ((X5WebViewManager.ReactWebView) webView).cleanupCallbacksAndDestroy();
    }

    protected WebView.PictureListener getPictureListener() {
        if (mPictureListener == null) {
            mPictureListener = new WebView.PictureListener() {
                @Override
                public void onNewPicture(WebView webView, Picture picture) {
                    dispatchEvent(
                            webView,
                            new ContentSizeChangeEvent(
                                    webView.getId(),
                                    webView.getWidth(),
                                    webView.getContentHeight()));
                }
            };
        }
        return mPictureListener;
    }

    protected static void dispatchEvent(WebView webView, Event event) {
        ReactContext reactContext = (ReactContext) webView.getContext();
        EventDispatcher eventDispatcher =
                reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
        eventDispatcher.dispatchEvent(event);
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("progressMessage", MapBuilder.of("registrationName", "onProgress"))
                .put("shouldOverrideUrlLoading", MapBuilder.of("registrationName", "shouldOverrideUrlLoading"))
                .build();
    }
}
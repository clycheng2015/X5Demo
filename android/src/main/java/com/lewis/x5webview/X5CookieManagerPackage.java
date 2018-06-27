package com.lewis.x5webview;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.lewis.x5webview.webview.X5WebViewConfig;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class X5CookieManagerPackage implements ReactPackage {
    private X5WebViewConfig webViewConfig = new X5WebViewConfig() {
        @Override
        public void configWebView(WebView webView) {
            WebSettings settings = webView.getSettings();
            String ua = settings.getUserAgentString();
            //添加标记用于在区分平台的来源（IOS、android、H5）
            settings.setUserAgentString(ua + " HMCHybridAndroid HMCIMSUPPORT");
        }
    };

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new X5CookieManagerModule(reactApplicationContext));
        modules.add(new X5Module(reactApplicationContext));
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(new X5WebViewManager(webViewConfig, reactContext));
    }
}

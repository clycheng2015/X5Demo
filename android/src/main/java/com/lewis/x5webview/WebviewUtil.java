package com.lewis.x5webview;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.io.File;

/**
 * 项目名称：app_android_buyer
 * 类描述：
 * 创建人：Administrator
 * 创建时间：2017-03-09
 *
 * @version ${VSERSION}
 */


public class WebviewUtil {
    private static final String TAG = WebviewUtil.class.getSimpleName();
    private static final String APP_CACAHE_DIRNAME = "/webcache";
    private static final String WEBVIEW_CACAHE_DIRNAME = "/webviewCache";

    /**
     * 判断当前的webview是否滚动到底部
     *
     * @param webView
     * @return
     */
    public static boolean isOnTop(WebView webView) {
        double exactContentHeight = Math.floor(webView.getContentHeight() * webView.getScale());
        if (exactContentHeight - (webView.getHeight() + webView.getScrollY()) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前的webview是否滚动到顶部
     *
     * @param webView
     * @return
     */
    public static boolean isOnBtton(WebView webView) {
        if (webView.getScrollY() == 0) {
            return true;
        }
        return false;
    }

    /**
     * 1、Android 中的 WebView 如何获取服务器页面的 jsessionid 的值
     * 2、Android 的 WebView 又是如何把得到的 jsessionid 的值在 set 到服务器中,
     * 一致达到他们在同一个 jsessionid 的回话中.
     *
     * @param url
     * @param taggetCookie
     */
    public static void setSessionId(String url, String taggetCookie) {
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookie();
        cm.getCookie(url);
        cm.setCookie(url, taggetCookie);
    }

    /***
     * 如果用户已经登录，则同步本地的cookie到webview中
     */
    public static void synCookies(Context context, String url) {
        String cookies = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context).sync();
        } else {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.removeSessionCookie();//移除
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {

                }
            });
            //存储在SP文件中的上次会话的Cookie_key
            //String cookies = PreferenceHelper.readString(this, AppConfig.COOKIE_KEY, AppConfig.COOKIE_KEY);
            cookieManager.setCookie(url, cookies);
        }
    }

    /**
     * 配置webview 的缓存策略
     *
     * @param context
     */
    public static void configWebviewSetting(Context context, WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setAllowFileAccess(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAppCacheMaxSize(Long.MAX_VALUE);
        settings.setAppCachePath(context.getDir("appcache", 0).getPath());
        settings.setDatabasePath(context.getDir("databases", 0).getPath());
        settings.setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());
        // settings.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
        settings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        //设置 缓存模式（读取缓存）
        if (isNetworkConnected(context)) {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        int screenDensity = context.getResources().getDisplayMetrics().densityDpi;
        Log.d(TAG, "==========================screenDensity = " + screenDensity);

        String Scale = String.valueOf(webView.getScale());
        Log.d(TAG, "-------Scale:--" + Scale);
        Log.d(TAG, "-------new Scale:--" + webView.getScale());
        WebSettings.ZoomDensity zoomDensity;
        switch (screenDensity) {
            case DisplayMetrics.DENSITY_LOW:
                zoomDensity = WebSettings.ZoomDensity.CLOSE;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                zoomDensity = WebSettings.ZoomDensity.MEDIUM;
                break;
            case DisplayMetrics.DENSITY_HIGH:
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_XXHIGH:
            default:
                zoomDensity = WebSettings.ZoomDensity.FAR;
                break;
        }
        settings.setDefaultZoom(zoomDensity);
    }

    /**
     * 清除webview 的缓存
     *
     * @param context
     */
    public static void clearWebviewCache(Context context) {
        //清理ebview缓存数据库
        try {
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //WebView 缓存文件
        File appCacheDir = new File(context.getFilesDir().getAbsolutePath() + APP_CACAHE_DIRNAME);
        Log.d(TAG, "appCacheDir path=" + appCacheDir.getAbsolutePath());

        File webviewCacheDir = new File(context.getCacheDir().getAbsolutePath() + WEBVIEW_CACAHE_DIRNAME);
        Log.d(TAG, "webviewCacheDir path=" + webviewCacheDir.getAbsolutePath());
        //删除webview 缓存目录
        if (webviewCacheDir.exists()) {
            deleteFile(webviewCacheDir);
        }
        //删除webview 缓存 缓存目录
        if (appCacheDir.exists()) {
            deleteFile(appCacheDir);
        }
    }

    /**
     * 递归删除 文件/文件夹
     *
     * @param file
     */
    public static void deleteFile(File file) {

        Log.i(TAG, "delete file path=" + file.getAbsolutePath());

        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i]);
                }
            }
            file.delete();
        } else {
            Log.i(TAG, "delete file no exists " + file.getAbsolutePath());
        }
    }

    /**
     * 本方法复写webview的对应方法
     * <p>
     * 非超链接的请求加上请求头或其他参数，可以拦截到所有的网页中资源请求，比如加载JS，图片以及Ajax请求
     *
     * @param view
     * @param url
     * @return
     */
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        String imei = "";//手机唯一标识符
        // 非超链接(如Ajax)请求无法直接添加请求头，现拼接到url末尾,这里拼接一个imei作为示例

        String ajaxUrl = url;
        // 如标识:req=ajax
        if (url.contains("req=ajax")) {
            ajaxUrl += "&imei=" + imei;
        }
        return null;
//        return super.shouldInterceptRequest(view, ajaxUrl);

    }
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }
}

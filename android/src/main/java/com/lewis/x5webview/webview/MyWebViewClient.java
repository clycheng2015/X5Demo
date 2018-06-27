package com.lewis.x5webview.webview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lewis.x5webview.WebviewUtil;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.IOException;
import java.io.InputStream;


/**
 * webview 的WebViewClient
 * <p>
 * 1、注册android提供给H5调用的方法
 * <p>
 * 2、调用H5提供给android调用的方法。
 */
public class MyWebViewClient extends WebViewClient {
    private final static String TAG = MyWebViewClient.class.getSimpleName();
    private final static String TEL = "tel:";
    private final static String NAME_NORMALIZE = "normalize.min";
    private final static String NAME_POLYFILL = "polyfill.min";
    public static final int MSG_PAGE_TOTAL_FINISH = 2001;
    public static final int MSG_PAGE_NO_FOUND = 404;
    public static final int MSG_PAGE_SERVER_ERROR = 400;
    private Activity context;
    private Handler handler;
    //private JPushManager mJPushManager;

    public MyWebViewClient(final Activity mContext, WebView webView, Handler handler) {

//        // support js send
//        //构建了一个默认的WVJBHandler
//        super(webView, new WVJBHandler() {
//
//            @Override
//            public void request(Object data, WVJBResponseCallback callback) {
//                GlobalUtils.toast(mContext, "ObjC got response! :" + data);
//                //callback接口实例被实际的回调代替，并调用这里的callback（data）方法，将其中的参数
//                //作为data封装到WVJBMessage中，返回给H5
//                callback.callback("Response for message from Native!");
//            }
//        });
        // not support js send super(webView);
        this.context = mContext;
        this.handler = handler;

//        if (context != null) {
//            mJPushManager = JPushManager.getJpushManager(context);
//        }
        // 配置webview的setting及cache
        WebviewUtil.configWebviewSetting(context, webView);
        //注册给H5调用的方法
        registeMethodForH5();
    }

    /**
     * 1、注册给h5调用的方法
     * <p>
     * 2、接收前端传给Native端的数据
     * <p>
     * 3、将每次调用需要的数据返回给H5
     */
    private void registeMethodForH5() {

    }


    /**
     * 页面加载开始
     *
     * @param view
     * @param url
     * @param favicon
     */
    @Override
    public void onPageStarted(final WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(TEL)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
            return true;
        }
        //view.loadUrl(url);
        return super.shouldOverrideUrlLoading(view, url);
    }

    /**
     * 文件找不到，网络连不上，服务器找不到等问题
     * <p>
     * 不能调用其super方法
     *
     * @param view
     * @param errorCode
     * @param description
     * @param failingUrl
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onReceivedError=" +
                "errorCode=" + errorCode + "\n" +
                "description=" + description + "\n"
                + "failingUrl=" + failingUrl);
        //去掉默认的出错页面
        view.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        Message msg = handler.obtainMessage();
        msg.what = MSG_PAGE_SERVER_ERROR;
        handler.sendMessage(msg);
    }

    /**
     * HTTPS 协议的Url证书错误，处理代码里应该让其忽略证书认证
     *
     * @param view
     * @param sslErrorHandler
     * @param error
     */
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler sslErrorHandler, SslError error) {
        //super.onReceivedSslError(view, handler, error);
        Log.e(TAG, "onReceivedSslError---------" + "error=" + error.toString());
        //接收所有证书
        sslErrorHandler.proceed();
        Message msg = handler.obtainMessage();
        msg.what = MSG_PAGE_SERVER_ERROR;
        handler.sendMessage(msg);
    }


    /**
     * 加装本地
     *
     * @param webView
     * @param url
     * @return
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
        if (url.endsWith("normalize.min.css")) {
            InputStream is = null;
            try {
                is = context.getApplicationContext().getAssets().open(NAME_NORMALIZE);
                return new WebResourceResponse("text/css", "UTF-8", is);
            } catch (IOException e) {
                e.printStackTrace();
                return super.shouldInterceptRequest(webView, url);
            }
        } else if (url.endsWith("polyfill.min.js")) {
            InputStream is = null;
            try {
                is = context.getApplicationContext().getAssets().open(NAME_POLYFILL);
                return new WebResourceResponse("text/javascript", "UTF-8", is);
            } catch (IOException e) {
                e.printStackTrace();
                return super.shouldInterceptRequest(webView, url);
            }
        }
        return super.shouldInterceptRequest(webView, url);
    }
}

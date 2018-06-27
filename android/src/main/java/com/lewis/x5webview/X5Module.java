package com.lewis.x5webview;

import android.content.Context;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

/**
 * 项目名称：buyer
 * 类描述：
 * 创建人：Administrator
 * 创建时间：2018-01-15
 *
 * @version ${VSERSION}
 */


public class X5Module extends ReactContextBaseJavaModule {

    private Context mContext;

    public X5Module(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
    }


    @Override
    public String getName() {
        return "X5Module";
    }

    /**
     * rnCallNativePay为RN需要调用的方法
     *
     * @param url
     * @param tokenName
     * @param tokenCity
     * @param authData
     * @param cityData
     * @param callback  RN调用此本地方法时的回调
     *                  WritableMap map = Arguments.createMap();
     *                  map.putBoolean("success", true);
     *                  callback.invoke(map);
     */
    @ReactMethod
    public void syncX5Cookie(String url, String tokenName, String tokenCity, String authData, String cityData, Callback callback) {

        CookieUtils.syncX5Cookie(mContext,url,tokenName, tokenCity, authData,cityData);
    }
}

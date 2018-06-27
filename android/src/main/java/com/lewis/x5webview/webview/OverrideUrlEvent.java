package com.lewis.x5webview.webview;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

/**
 * 项目名称：webuserapp
 * 类描述：自定义的X5webView shouldOverrideUrlLoading拦截a标签的URL
 * 创建人：Administrator
 * 创建时间：2018-04-02
 *
 * @version ${VSERSION}
 */


public class OverrideUrlEvent extends Event<OverrideUrlEvent> {

    public static final String EVENT_NAME = "shouldOverrideUrlLoading";
    private String mData;

    public OverrideUrlEvent(int viewId,String data) {
        super(viewId);
        mData = data;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public boolean canCoalesce() {
        return false;
    }

    @Override
    public short getCoalescingKey() {
        // All events for a given view can be coalesced.
        return 0;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        WritableMap data = Arguments.createMap();
        data.putString("data", mData);
        rctEventEmitter.receiveEvent(getViewTag(), EVENT_NAME, data);
    }
}
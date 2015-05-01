package com.fujitsu.jp.komoachi;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Created by yokoi on 2015/05/02.
 */
public class WebViewJavascriptInterface {

    Context mContext;

    /** Instantiate the interface and set the context */
    WebViewJavascriptInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void pushButton(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }
}

package com.fujitsu.jp.komoachi;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Created by yokoi on 2015/05/02.
 */
public class WebViewJavascriptInterface {

    Context mContext;
    private IrrcUsbDriver irrcUsbDriver;

    /** Instantiate the interface and set the context */
    WebViewJavascriptInterface(Context c, IrrcUsbDriver driver) {
        mContext = c;
        irrcUsbDriver = driver;

    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void pushButton(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    /** Setting Mode ====================*/

    //設定開始
    @JavascriptInterface
    public void settingStart(){

        Toast.makeText(mContext, "リモコンの設定を開始します", Toast.LENGTH_SHORT).show();

    }

    //ボタン押下
    @JavascriptInterface
    public void pushButtonInSettingMode(String id){
        Toast.makeText(mContext, "ボタン"+ id +"の設定を開始します", Toast.LENGTH_SHORT).show();

        //赤外線の受信モードON
        //受信モードで待機
        if(irrcUsbDriver.isReady()) {
            Toast.makeText(mContext,"USBデバイスを認識", Toast.LENGTH_SHORT).show();

            irrcUsbDriver.startReceiveIr(new IrrcUsbDriver.IrrcResponseListener() {
                @Override
                public void onIrrcResponse(byte[] data) {
                    irrcUsbDriver.getReceiveIrData(new IrrcUsbDriver.IrrcResponseListener() {
                        @Override
                        public void onIrrcResponse(byte[] data) {
                            Toast.makeText(mContext, data.toString(), Toast.LENGTH_SHORT).show();
                            irrcUsbDriver.endReceiveIr(null);
                        }
                    }, 5000);
                }
            });
        }
        else {
            Toast.makeText(mContext, "USBデバイスが見つかりません", Toast.LENGTH_SHORT).show();

        }

    }

    //設定完了
    @JavascriptInterface
    public void settingSave(){
        Toast.makeText(mContext, "リモコンの設定を保存します", Toast.LENGTH_SHORT).show();
    }
}

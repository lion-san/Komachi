package com.fujitsu.jp.komachi;

import android.app.Activity;
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

    /** Active Mode ====================*/

    /**
     * 赤外線データ送信処理
     */
    @JavascriptInterface
    public void pushButton(String id) {
        Toast.makeText(mContext, id, Toast.LENGTH_SHORT).show();

        //データをプット
        byte[] data = (byte[])((RemoconApplication) ((Activity) (mContext)).getApplication()).getObject(id);

            if (irrcUsbDriver.isReady() == false || data == null) {
                Toast.makeText(mContext, "Not ready.", Toast.LENGTH_SHORT).show();
                return;
            }
            irrcUsbDriver.sendData(data);
    }

    /** Setting Mode ====================*/

    //設定開始
    @JavascriptInterface
    public void settingStart(){

        Toast.makeText(mContext, "リモコンの設定を開始します", Toast.LENGTH_SHORT).show();

        //赤外線の受信モードON
        //受信モードで待機
        if(irrcUsbDriver.isReady()) {
            Toast.makeText(mContext,"USBデバイスを認識", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(mContext, "USBデバイスが見つかりません", Toast.LENGTH_SHORT).show();
        }

    }

    //ボタン押下
    @JavascriptInterface
    public void pushButtonInSettingMode(String id){
        Toast.makeText(mContext, "ボタン"+ id +"を押してください", Toast.LENGTH_SHORT).show();

        final String key = id;

        irrcUsbDriver.startReceiveIr(new IrrcUsbDriver.IrrcResponseListener() {
            @Override
            public void onIrrcResponse(byte[] data) {
                irrcUsbDriver.getReceiveIrData(new IrrcUsbDriver.IrrcResponseListener() {
                    @Override
                    public void onIrrcResponse(byte[] data) {

                        if(data != null) {
                            Toast.makeText(mContext, data.toString(), Toast.LENGTH_SHORT).show();

                            //データをプット
                            ((RemoconApplication) ((Activity) (mContext)).getApplication()).putObject(key, data);
                        }

                        irrcUsbDriver.endReceiveIr(null);
                    }
                }, 5000);
            }
        });

    }

    //設定完了
    @JavascriptInterface
    public void settingSave(){
        Toast.makeText(mContext, "リモコンの設定を保存します", Toast.LENGTH_SHORT).show();
    }
}

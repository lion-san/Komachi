package com.fujitsu.jp.komachi;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by yokoi on 2015/05/02.
 */
public class WebViewJavascriptInterface {

    Context mContext;
    private IrrcUsbDriver irrcUsbDriver;
    private RemoconApplication app;

    /** Instantiate the interface and set the context */
    WebViewJavascriptInterface(Context c, IrrcUsbDriver driver) {
        mContext = c;
        irrcUsbDriver = driver;

        app =
        (RemoconApplication) ((Activity) (mContext)).getApplication();

    }

    /** Active Mode ====================*/

    /**
     * 赤外線データ送信処理
     */
    @JavascriptInterface
    public void pushButton(String id) {

        if(irrcUsbDriver.isReady()) {

            //データをプット
            byte[] data = (byte[]) app.getObject(id);

            if (irrcUsbDriver.isReady() == false || data == null) {
                Toast.makeText(mContext, "チャンネルが設定されてません", Toast.LENGTH_SHORT).show();
                return;
            }
            else {
                irrcUsbDriver.sendData(data);
                Toast.makeText(mContext, id, Toast.LENGTH_SHORT).show();
                Log.sendLog("0000", "remocon", id, "");
            }

        }
        else{

            app.getMainHandler((MainActivity)mContext).doTalk("USBデバイスが見つかりません", false);
        }
    }

    /** Setting Mode ====================*/
    //設定開始
    @JavascriptInterface
    public void settingStart(){

        app.getMainHandler((MainActivity)mContext).doTalk("リモコンの設定を開始します", true);

        //赤外線の受信モードON
        //受信モードで待機
        if(irrcUsbDriver.isReady()) {
            app.getMainHandler((MainActivity)mContext).doTalk("USBデバイスを認識しました", false);
        }
        else {
            app.getMainHandler((MainActivity)mContext).doTalk("USBデバイスが見つかりません", false);
        }
    }

    //ボタン押下
    @JavascriptInterface
    public void pushButtonInSettingMode(String id){
        app.getMainHandler((MainActivity)mContext).doTalk("ボタン"+ id +"を押してください", true);

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
                            app.putObject(key, data);
                        }

                        irrcUsbDriver.endReceiveIr(null);
                    }
                }, 5000);
            }
        });

    }

    //設定完了
    @JavascriptInterface
    public void settingSave()  {
        app.getMainHandler((MainActivity)mContext).doTalk("リモコンの設定を保存します", true);


        //JSONの作成
        Map map = app.getSaveObjects();
        String json = "";

        json = "{\"maker\":\"SONY\", \"buttons\":[";
       // String json = "[";

        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();

            //メモリ上からロード
            Object key = entry.getKey();
            byte[] value = (byte[])entry.getValue();

            //KeyのJson化
            String btnId = "{\"btnId\":\"" + key.toString() + "\"";

            //ValueのJSON化(16進数に変換）
            String btnCode = "\"btnCode\":\"" + Utility.bin2hex(value) + "\"}";


            json += btnId + "," + btnCode;

            if(it.hasNext()){
                json += ",";
            }
        }

        json += "]}";

        //WebAPI保存
        SendHttpRequest http = new SendHttpRequest();
        if(http.saveRemocon(json)){
            app.getMainHandler((MainActivity)mContext).doTalk("クラウドに保存しました", true);
        }
        else {
            app.getMainHandler((MainActivity)mContext).doTalk("保存に失敗しました", true);
        }


    }


    /** Test Mode ====================*/
    //ボタン押下
    @JavascriptInterface
    public void pushButtonInTestMode(String id){
        Toast.makeText(mContext, id, Toast.LENGTH_SHORT).show();

        //データをプット
        byte[] data = (byte[])app.getObject(id);

        if (irrcUsbDriver.isReady() == false || data == null) {
            Toast.makeText(mContext, "Not ready.", Toast.LENGTH_SHORT).show();
            return;
        }
        irrcUsbDriver.sendData(data);
    }

}//Class

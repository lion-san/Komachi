package com.fujitsu.jp.komachi;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by clotcr_23 on 2015/05/11.
 */
public class Log  {

    static MyAsyncTask task;

    public static void sendLog(String userId, String action, String val1, String val2) {

        // サブスレッドで実行するタスクを作成
        task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {

                try {
                    Calendar now = Calendar.getInstance();
                    //フォーマットパターンを指定して表示する
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                    String json = "{";
                    json += "\"time\":\"" + sdf.format(now.getTime()) + "\",";
                    json += "\"userid\":\"" + params[0] + "\",";
                    json += "\"action\":\"" + params[1] + "\",";
                    json += "\"val1\":\"" + params[2] + "\",";
                    json += "\"val2\":\"" + params[3] + "\"";
                    json += "}";

                    SendHttpRequest http = new SendHttpRequest();

                    if (http.sendLog(json) ){
                        //OK
                    }
                    else {//error

                    }

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                return null;
            }

            @Override
            protected void onPostExecute(String json_org) {

            }
        };

        try {
            //非同期処理開始
            task.execute( userId, action, val1, val2 );
        }catch( Exception e){
            e.printStackTrace();
        }
    }
}

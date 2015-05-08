package com.fujitsu.jp.komachi;

import android.app.Activity;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;

/**
 * Created by clotcr_22 on 2015/02/16.
 */
public class MyAsyncTask extends AsyncTask<String, Void, String> {

    private String param;
    private Activity activity;
    private TextToSpeech tts;


    /**
     * コンストラクタ
     */
    public MyAsyncTask() {

    }

    /*

     */
    protected String doInBackground(String... params) {
        return "";
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public TextToSpeech getTts() {
        return tts;
    }

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }
}




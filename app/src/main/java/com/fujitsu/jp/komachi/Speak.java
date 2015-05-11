package com.fujitsu.jp.komachi;

import android.app.Activity;
import android.speech.tts.TextToSpeech;

/**
 * Created by clotcr_23 on 2015/05/11.
 */
public class Speak implements  TextToSpeech.OnInitListener {

    private TextToSpeech tts;

    public Speak(Activity activity){
        tts = new TextToSpeech(activity.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {

    }
}

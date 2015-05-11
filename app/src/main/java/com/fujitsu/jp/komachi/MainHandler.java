package com.fujitsu.jp.komachi;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by clotcr_23 on 2015/04/28.
 */
public class MainHandler implements TextToSpeech.OnInitListener, StaticParams{

    /** 変数 */
    private Activity mainActivity;
    private TextToSpeech tts;
    private ProgressDialog progressBar;
    private MyAsyncTask task;
    private String res = null;
    private WebView web;
    //プロジェクトリスト
    private CharSequence[] items;
    private ArrayList<String> list = new ArrayList<String>();
    private ArrayList<String> pjId = new ArrayList<String>();
    AlertDialog dialog;
    private ActionHandler act;
    /** カメラのハードウェアを操作する {@link android.hardware.Camera} クラスです。 */
    private Camera mCamera;
    /** カメラのプレビューを表示する {@link android.view.SurfaceView} です。 */
    private SurfaceView mView;
    private CameraOverlayView mCameraOverlayView;
    private Date lasttime = null;
    private Date starttime = null;

    /**
     * コンストラクタ
     */
    public MainHandler(MainActivity mainActivity){

        this.mainActivity = mainActivity;

        //ぐるぐる
        progressBar = new ProgressDialog(mainActivity);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMessage("処理を実行中しています");
        progressBar.setCancelable(true);
        progressBar.show();

        //カメラ
        //mView = (SurfaceView) mainActivity.findViewById(R.id.surfaceView);
        //顔検知用重畳ビュー
        //mCameraOverlayView = new CameraOverlayView(mainActivity);
        //mainActivity.addContentView(mCameraOverlayView, new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
        //コールバック関数をセット
        //SurfaceHolder holder = mView.getHolder();
        //holder.addCallback(surfaceHolderCallback);
        //holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //TTSの初期化
        tts = new TextToSpeech(mainActivity.getApplicationContext(), this);



    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            //String ready = "準備OKです";
            //on.speak(ready, TextToSpeech.QUEUE_FLUSH, null);
            //Toast.makeText(this, ready, Toast.LENGTH_SHORT).show();

            //ロードするプロジェクト一覧の表示
            loadProjectList();

        } else {

        }
    }

    /**
     * プロジェクトリストを取得、表示
     */

    private void loadProjectList(){
        // サブスレッドで実行するタスクを作成
        task = new MyAsyncTask() {

            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {
                    // Twitter フォロー実行
                    SendHttpRequest http = new SendHttpRequest();
                    String json_org = http.getProjectList();

                    res = json_org;//インスタンス変数にＪＳＯＮ(命令セット)をセット

                    return json_org;
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                return null;
            }

            @Override
            protected void onPostExecute(String json_org) {
                try {
                    JSONArray jsons = new JSONArray(json_org);

                    for (int i = 0; i < jsons.length(); i++) {
                        // 情報を取得
                        JSONObject event = jsons.getJSONObject(i);

                        // pjname
                        String pjname = event.getString("pjname");
                        String id = event.getString("id");

                        list.add(pjname);
                        pjId.add(id);
                    }

                    //リストの表示
                    items = list.toArray(new CharSequence[list.size()]);
                    dialog = new AlertDialog.Builder(mainActivity)
                            .setTitle("Select Project")
                            .setSingleChoiceItems(
                                    items,
                                    0, // Initial
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which){
                                            Toast.makeText(mainActivity, list.get(which) + "をロードします", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                            //ロボナイゼーヨンイニシャライズ
                                            //Robotプログラムスタート
                                            initRobot( pjId.get(which) );
                                        }
                                    })
                            .setPositiveButton("Close", null)
                            .show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(mainActivity, "Network Busy!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //ぐるぐる
                progressBar.dismiss();//消去
            }

        };

        task.execute("");
    }

    private void initRobot ( String projectName ){
        // サブスレッドで実行するタスクを作成
        task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {
                    SendHttpRequest http = new SendHttpRequest();
                    String json_org = http.sendRequestToGarako( resultsString );

                    res = json_org;//インスタンス変数にＪＳＯＮ(命令セット)をセット

                    this.setParam(resultsString);

                    //リモコンのロード
                    String remocon = http.getRemocon();
                    JSONObject jsonObject = new JSONObject(remocon);
                    JSONArray jsons = jsonObject.getJSONArray("buttons");

                    //プロパティの取得
                    String maker = jsonObject.getString("maker");

                    RemoconApplication app =
                    ((RemoconApplication) ((Activity) (mainActivity)).getApplication());

                    for (int i = 0; i < jsons.length(); i++) {
                        // リモコン情報を取得
                        JSONObject btn = jsons.getJSONObject(i);
                        //メモリ上にロード
                       app.putObject(btn.getString("btnId"), btn.getString("btnCode").getBytes());
                    }

                    progressBar.dismiss();//消去

                    return json_org;
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                return null;
            }

            @Override
            protected void onPostExecute(String json_org) {
                String ready = "小町プログラム起動しました";
                tts.speak(ready, TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(this.getActivity(), ready, Toast.LENGTH_SHORT).show();

            }
        };

        try {
            //アクションハンドラの生成
            act = new ActionHandler(mainActivity);

            task.setActivity(mainActivity);
            task.setTts(this.tts);
            //act.setmCam(mCamera);
            act.setContext(mainActivity.getApplicationContext());


            //非同期処理開始
            task.execute( projectName );
        }catch( Exception e){
            e.printStackTrace();
        }

    }

    /**
     * executeRobot
     */
    protected void executeRobot( String resultsString ){

        //表示
        progressBar.show();
        starttime = new Date();

        // サブスレッドで実行するタスクを作成
        task = new MyAsyncTask() {
            @Override
            protected String doInBackground(String... params) {
                String resultsString = params[0];
                try {

                    if(res == null) {

                        // Twitter フォロー実行
                        SendHttpRequest http = new SendHttpRequest();
                        String json_org = http.sendRequestToGarako(resultsString);
                        res = json_org;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(this.getActivity(), "Network Busy!", Toast.LENGTH_SHORT).show();

                }
                progressBar.dismiss();//消去
                this.setParam(resultsString);
                return res;
            }

            @Override
            protected void onPostExecute(String json_org) {

                // トーストを使って結果を表示
                //Toast.makeText(this.getActivity(), json_org, Toast.LENGTH_SHORT).show();

                //WebView webView = (WebView) findViewById(R.id.webView);
                //webView.loadUrl(url);
                //webView.loadData(data, "text/html", null);
                //webView.loadDataWithBaseURL(null, json_org, "text/html", "UTF-8", null);

                String resultsString = this.getParam();


                act.setTts(this.getTts());
                //act.setContext(context);

                //----------------------------------
                //-- JSONの振り分け処理
                //----------------------------------

                act.analyzeJson(resultsString, json_org);

            }
        };

        task.setActivity(mainActivity);
        task.setTts( this.tts );
        //アクションハンドラの生成
        //act = new ActionHandler( this );
        act.setContext(mainActivity.getApplicationContext());
        act.setmCam( mCamera );
        //act.setWeb( web );

        task.execute( resultsString );
    }


//--------------------------------------------------------------------------------
//--- Camera -----------------------------------------------------------------------------
//--------------------------------------------------------------------------------

    /** カメラのコールバックです。 */
    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            try {
                // 生成されたとき
                mCamera = Camera.open(1);

                // リスナをセット  // 顔検出の開始
                //mCamera.setFaceDetectionListener(faceDetectionListener);

                //mCamera.stopFaceDetection();

                // プレビューをセットする
                //mCamera.setPreviewDisplay(holder);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                Log.d("onFaceDetection", "顔検出数:" + faces.length);
                // View に渡す
                //mCameraOverlayView.setFaces(faces);

                //会話中画像を消す
                /*if (starttime != null && (new Date()).getTime() - starttime.getTime() > 3000 ){
                    web.loadUrl(StaticParams.STOP_ANIMATION);
                    web.reload();
                }*/

                if(faces.length > 0){

                    lasttime = null;

                    if (!act.getFace_ditect()) {

                        act.setFace_ditect(true);
                        //tts.speak("侵入者を検知しました", TextToSpeech.QUEUE_FLUSH, null);
                        // 画像取得
                        //mCamera.takePicture(null, null, mPicJpgListener);
                        executeRobot(StaticParams.FACE_DETECT);

                        lasttime = null;
                    }
                }
                else{
                    if(lasttime == null )
                        lasttime = new Date();

                        //検知ゼロが指定ミリ秒以上続くまで、処理しない
                    else if ( ((new Date()).getTime() - lasttime.getTime() > 10000)){
                        act.setFace_ditect(false);//フラグをもどす
                        if( lasttime != null){
                            long a = (( new Date()).getTime() - lasttime.getTime());
                            Log.d("#######################", "時間（ミリ秒）"+ a);}
                    }

                }

            }
        };

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            // 変更されたとき
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            //parameters.setPreviewSize(previewSize.width, previewSize.height);
            parameters.setPreviewSize(640, 480);
            // width, heightを変更する
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // 破棄されたとき
            mCamera.release();
            mCamera = null;
        }

    };


//--------------------------------------------------------------------------------
//--------------------------------------------------------------------------------
//--------------------------------------------------------------------------------



}

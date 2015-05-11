package com.fujitsu.jp.komachi;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;

/**
 * Created by yokoi.shinya on 2015/01/19.
 */
public class SendHttpRequest implements WebServiceUrls {

    public String sendRequestToGarako(String projectId) {

        HttpClient httpClient = new DefaultHttpClient();

        //StringBuilder uri = new StringBuilder("http://ec2-54-65-250-88.ap-northeast-1.compute.amazonaws.com/python/querygyarako.py?foo=" + msg);
        StringBuilder uri = new StringBuilder(ROBO_PAAS_URL + "/projects/"+ projectId + "/events.json");
        HttpGet request = new HttpGet(uri.toString());

        HttpResponse httpResponse = null;

        try {
            httpResponse = httpClient.execute(request);
        } catch (Exception e) {
            Log.d("HttpSampleActivity", "Error Execute");
        }


        //レスポンスの処理
        String url = "";
        int status = httpResponse.getStatusLine().getStatusCode();

        if (HttpStatus.SC_OK == status) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                httpResponse.getEntity().writeTo(outputStream);
                url = outputStream.toString();
            } catch (Exception e) {
                Log.d("HttpSampleActivity", "Error");
            }
        } else {
            Log.d("HttpSampleActivity", "Status" + status);
        }

        return url;
    }

    public String getProjectList(){
        HttpClient httpClient = new DefaultHttpClient();

        //StringBuilder uri = new StringBuilder("http://ec2-54-65-250-88.ap-northeast-1.compute.amazonaws.com/python/querygyarako.py?foo=" + msg);
        StringBuilder uri = new StringBuilder(ROBO_PAAS_URL + "/projects.json");
        HttpGet request = new HttpGet(uri.toString());

        HttpResponse httpResponse = null;

        try {
            httpResponse = httpClient.execute(request);
        } catch (Exception e) {
            Log.d("HttpSampleActivity", "Error Execute");
        }


        //レスポンスの処理
        String list = "";
        int status = httpResponse.getStatusLine().getStatusCode();

        if (HttpStatus.SC_OK == status) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                httpResponse.getEntity().writeTo(outputStream);
                list = outputStream.toString();
            } catch (Exception e) {
                Log.d("HttpSampleActivity", "Error");
            }
        } else {
            Log.d("HttpSampleActivity", "Status" + status);
        }

        return list;
    }

    public boolean saveRemocon(String json) {
        HttpClient httpClient = new DefaultHttpClient();

        //StringBuilder uri = new StringBuilder("http://ec2-54-65-250-88.ap-northeast-1.compute.amazonaws.com/python/querygyarako.py?foo=" + msg);
        StringBuilder uri = new StringBuilder(USER_MANAGEMENT_URL + "/remocons/save");
        HttpPost request = new HttpPost(uri.toString());
        HttpResponse httpResponse = null;

        try {
            StringEntity params = new StringEntity(json);
            request.addHeader("content-type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");

            request.setEntity(params);

            //request.setEntity(new ByteArrayEntity(json.toString().getBytes("UTF8")));

            httpResponse = httpClient.execute(request);

            //レスポンスの処理
            int status = httpResponse.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK == status) {
                try {
                    return true;
                } catch (Exception e) {
                    Log.d("HttpSampleActivity", "Error");

                }
            } else {
                Log.d("HttpSampleActivity", "Status" + status);

            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("HttpSampleActivity", "Error Execute");
        }

        return false;
    }

    /**
     * getRemocon
     */
    public String getRemocon(){

        String json = "";

        HttpClient httpClient = new DefaultHttpClient();

        StringBuilder uri = new StringBuilder(USER_MANAGEMENT_URL + "/remocons/0000.json");

        HttpGet request = new HttpGet(uri.toString());
        HttpResponse httpResponse = null;



        try {

            httpResponse = httpClient.execute(request);

            //レスポンスの処理
            int status = httpResponse.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK == status) {
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    httpResponse.getEntity().writeTo(outputStream);
                    json = outputStream.toString();
                    return json;
                } catch (Exception e) {
                    Log.d("HttpSampleActivity", "Error");
                }
            } else {
                Log.d("HttpSampleActivity", "Status" + status);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("HttpSampleActivity", "Error Execute");
        }

        return  json;

    }

    public boolean sendLog(String json){
        HttpClient httpClient = new DefaultHttpClient();

        StringBuilder uri = new StringBuilder(USER_MANAGEMENT_URL + "/logs");
        HttpPost request = new HttpPost(uri.toString());
       // request.setHeader("Content-Type", "text/plain; charset=UTF-8");
        HttpResponse httpResponse = null;

        try {
            StringEntity params = new StringEntity(json);
            request.addHeader("content-type", "application/json; charset=UTF-8");
            request.setHeader("Accept", "application/json; charset=UTF-8");
            request.setHeader("Content-Type", "application/json; charset=UTF-8");


            //文字コード変換（重要）
            request.setEntity(new ByteArrayEntity(json.toString().getBytes("UTF8")));

            httpResponse = httpClient.execute(request);

            //レスポンスの処理
            int status = httpResponse.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK == status) {
                try {
                    return true;
                } catch (Exception e) {
                    Log.d("HttpSampleActivity", "Error");

                }
            } else {
                Log.d("HttpSampleActivity", "Status" + status);

            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("HttpSampleActivity", "Error Execute");
        }

        return false;

    }
}

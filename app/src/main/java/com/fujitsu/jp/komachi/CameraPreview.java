package com.fujitsu.jp.komachi;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by clotcr_22 on 2015/02/17.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    protected Camera camera;

    CameraPreview(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {


        try {
            camera = Camera.open();
            camera.setPreviewDisplay(holder);
        }
        catch (Exception e){}

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.stopPreview();
        Camera.Parameters params = camera.getParameters();
        params.setPreviewFormat(format);
        params.setPreviewSize(width, height);
        camera.setParameters(params);
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }
}
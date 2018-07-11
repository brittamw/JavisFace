package com.britta.javisface.ui.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

public class CameraSourcePreview extends ViewGroup {

    private static final String TAG = "CameraSourcePreview";
    private Context mcontext;
    private SurfaceView msurfaceView;
    private boolean mstartRequested;
    private boolean msurfaceAvailable;
    private CameraSource mcameraSource;

    private GraphicOverlay moverlay;

    public CameraSourcePreview(Context context, AttributeSet attrs){
        super(context, attrs);
        mcontext =context;
        mstartRequested = false;
        msurfaceAvailable =false;

        msurfaceView = new SurfaceView(context);
        msurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(msurfaceView);

    }
    public void start(CameraSource cameraSource)throws IOException{
        if(cameraSource== null){
            stop();
        }
        mcameraSource =cameraSource;

        if(mcameraSource != null){
            mstartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException{
        moverlay =overlay;
        start(cameraSource);
    }

    public void stop(){
        if(mcameraSource != null){
            mcameraSource.stop();
        }
    }

    public void release(){
        if(mcameraSource !=null){
            mcameraSource.release();
            mcameraSource = null;
        }
    }

    private void startIfReady()throws IOException{
        if(mstartRequested && msurfaceAvailable){
            mcameraSource.start(msurfaceView.getHolder());
            if (moverlay !=null){
               Size size = mcameraSource.getPreviewSize();
               int min = Math.min(size.getWidth(), size.getHeight());
               int max = Math.max(size.getWidth(), size.getHeight());
               if(isPortraitMode()){
                   moverlay.setCameraInfo(min, max, mcameraSource.getCameraFacing());

               }
               else{
                   moverlay.setCameraInfo(max, min, mcameraSource.getCameraFacing());
               }
               moverlay.clear();

            }
            mstartRequested=false;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback{


        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            msurfaceAvailable = true;
            try {
                startIfReady();
            }catch(IOException e){
                Log.e(TAG, "Kamera konnte nciht gestartet werden", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            msurfaceAvailable = false;
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if(mcameraSource != null){
            Size size = mcameraSource.getPreviewSize();
            if(size != null){
                width = size.getWidth();
                height = size.getHeight();

            }
        }
        if(isPortraitMode()){
            int temp = width;
            width = height;
            height = temp;
        }

        final int layoutWidth = right -left;
        final int layoutHeight = bottom - top;

        int childWidth =layoutWidth;
        int childHeight =(int)(((float)layoutWidth/(float)width)*height);

        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Kamera konnte nicht gestartet werden.", e);
        }
    }




    private boolean isPortraitMode(){
        int orientation = mcontext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }
        return false;
    }
}

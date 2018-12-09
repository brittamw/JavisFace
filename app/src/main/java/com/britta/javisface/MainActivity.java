package com.britta.javisface;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.britta.javisface.ui.camera.CameraSourcePreview;
import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class MainActivity extends AppCompatActivity {

    public static final String TAG ="JavisFace";
    private Context context;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int HANDLE_CAMERA_STORAGE_PERM = 2;
    private Button snapButton;
    private Button switchButton;
    private Button filterButton;
    private Button smileButton;
    private static boolean isSmiledetectorActive = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        context=getApplicationContext();
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);
        snapButton = findViewById(R.id.captureBtn);
        switchButton = findViewById(R.id.switchBtn);
        filterButton = findViewById(R.id.filterBtn);
        smileButton = findViewById(R.id.smileBtn);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int bc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(rc == PackageManager.PERMISSION_GRANTED && bc ==PackageManager.PERMISSION_GRANTED){
            createCameraSource(1);
        }
        else {
            requestCameraAndStoragePermissions();
        }
        snapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes) {
                        snapPhoto(bytes);
                    }

                });
            }
        });
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();

            }
        });
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                putMustacheOnFace();
            }
        });
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectSmile();
            }
        });
    }



    private void requestCameraAndStoragePermissions() {
        Log.w(TAG, "App benötigt Zugriff auf die Kamera und den Speicher");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, HANDLE_CAMERA_STORAGE_PERM);
            return;
        }

        final Activity recentActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(recentActivity, permissions,
                        HANDLE_CAMERA_STORAGE_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, "Gesichterkennung benötigt Kamera- und Speicherzugriff", Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok", listener)
                .show();

    }

    private void createCameraSource(int facing){

        FaceDetector detector = new FaceDetector.Builder(context).setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build();

        detector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if(!detector.isOperational()){
            Log.w(TAG, "Gesichterkennung noch nicht vollständig geladen");
        }

        mCameraSource = new CameraSource.Builder(context, detector).setRequestedPreviewSize(640, 480)
                .setFacing(facing).setRequestedFps(30.0f).build();
    }

    @Override
    protected void onResume(){
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mCameraSource !=null){
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode != HANDLE_CAMERA_STORAGE_PERM){
            Log.d(TAG, "unerwartetes Zugriffsergebnis: "+requestCode);
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            return;
        }

        if(grantResults.length != 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            createCameraSource(1);
            return;
        }
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Javis Face").setMessage("App kann nicht ausgeführt werden. Kamera- oder Speicher Zugriff nicht gegeben")
                .setPositiveButton("OK", listener).show();
    }


    private void startCameraSource(){

        if(mCameraSource !=null){
            try{
                mPreview.start(mCameraSource,mGraphicOverlay);
            }
            catch (IOException e){
                Log.e(TAG, "Kamera kann nicht geöffnet werden", e);
                mCameraSource.release();
                mCameraSource =null;
            }
        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face>{
        @Override
        public Tracker<Face> create(Face face){
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face>{
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay){
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, context);
        }

        @Override
        public void onNewItem(int faceID, Face item){
            mFaceGraphic.setID(faceID);
        }
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face){
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);

        }
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults){
            mOverlay.remove(mFaceGraphic);
        }

        @Override
        public void onDone(){
            mOverlay.remove(mFaceGraphic);
        }

    }

    private void snapPhoto(byte[] bytes){

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap cameraData = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        mGraphicOverlay.setDrawingCacheEnabled(true);
        Bitmap overlay = mGraphicOverlay.getDrawingCache();

            try {
                String pathToFolder = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ) + File.separator;
                File file = new File(pathToFolder);
                if(!file.exists()){
                    file.mkdirs();
                }
                String pathToFile = pathToFolder + "foto_"+getPhotoTime()+".jpg";
                File photo = new File(pathToFile);
                photo.createNewFile();
                if (!photo.exists()){
                    photo.createNewFile();
                }
                FileOutputStream stream = new FileOutputStream(photo);

                if(mCameraSource.getCameraFacing()==CameraSource.CAMERA_FACING_FRONT){
                    Matrix mtx = new Matrix();
                    mtx.preScale(-1.0f, 1.0f);
                    Bitmap frontFace = Bitmap.createBitmap(cameraData, 0, 0, cameraData.getWidth(), cameraData.getHeight(), mtx, true);
                    if(isSmiledetectorActive){
                        frontFace.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }else{
                        Bitmap frontCameraAndOverlay = combineOverlay(frontFace, overlay);
                        frontCameraAndOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }
                }else{
                    if(isSmiledetectorActive){
                        cameraData.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    }else{
                        Bitmap rearCameraAndOverlay = combineOverlay(cameraData, overlay);
                        rearCameraAndOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    }
                }

                stream.flush();
                stream.close();
                mGraphicOverlay.setDrawingCacheEnabled(false);

            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    private String getPhotoTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy_hhmmss");

        return dateFormat.format(new Date());
    }



    private void switchCamera(){

        if(mCameraSource.getCameraFacing()==CameraSource.CAMERA_FACING_FRONT){

            if (mCameraSource != null) {
                mCameraSource.release();
            }
            createCameraSource(0);
        }
        else{

            if (mCameraSource != null) {
                mCameraSource.release();
            }
            createCameraSource(1);
        }

        startCameraSource();
    }

    public Bitmap combineOverlay(Bitmap cameraData, Bitmap overlay) {
      
        int width = cameraData.getWidth();
        int height = cameraData.getHeight();
        Bitmap combinedBM = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Rect cameraRect = new Rect(0,0,width,height);
        Rect overlayRect = new Rect(0,0,overlay.getWidth(),overlay.getHeight());
        Canvas canvas = new Canvas(combinedBM);
        canvas.drawBitmap(cameraData, cameraRect, cameraRect, null);
        canvas.drawBitmap(overlay, overlayRect, cameraRect, null);
        return combinedBM;
    }


    private void putMustacheOnFace(){

        FaceGraphic.setFilterEnabled();
    }

    private void detectSmile() {
        FaceGraphic.setSmilingEnabled();
        if (!isSmiledetectorActive){
            isSmiledetectorActive = true;
        }
        else{
            isSmiledetectorActive=false;
        }
    }

}

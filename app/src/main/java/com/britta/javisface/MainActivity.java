package com.britta.javisface;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.britta.javisface.ui.camera.CameraSourcePreview;
import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class MainActivity extends AppCompatActivity {

    public static final String TAG ="JavisFace";
    private Context context;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private Button snapButton;
    private Button switchButton;
    private Button filterButton;
    private Button smileButton;

    private static boolean isSmiledetectorActive = false;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

    };






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
        if(rc == PackageManager.PERMISSION_GRANTED){
            createCameraSource(1);
            verifyStoragePermissions(this);
            snapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes) {
                            mGraphicOverlay.setDrawingCacheEnabled(true);
                            Bitmap overlay = mGraphicOverlay.getDrawingCache();
                            snapPhoto(bytes);
                        }

                    });
                    Log.d(TAG, "onClick: Hello");
                }
            });
            switchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchCamera();
                    Log.d(TAG, "onClick: hallo?");

                }
            });
            filterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    putMustacheOnFace();
                    Log.d(TAG, "hallo mustache");
                }
            });
            smileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    detectSmile();
                }
            });
        }
        else{
            requestCameraPermission();
        }

    }



    private void requestCameraPermission() {
        Log.w(TAG, "App benötigt Zugriff auf die Kamera");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;

        }

        final Activity recentActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(recentActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, "Gesichterkennung benötigt Kamerazugriff", Snackbar.LENGTH_INDEFINITE)
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
        if(requestCode != RC_HANDLE_CAMERA_PERM){
            Log.d(TAG, "unerwartetes Zugriffsergebnis: "+requestCode);
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            return;
        }

        if(grantResults.length != 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Zugriff erlaubt. Kamera wird initialisiert");
            createCameraSource(1);
            return;
        }

        Log.e(TAG, "Zugriff nicht erlaubt:  " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Javis Face").setMessage("App kann nicht ausgeführt werden, da kein Kamerazugriff gewährt")
                .setPositiveButton("OK", listener).show();
    }



    private void startCameraSource(){

        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());

        if(code!= ConnectionResult.SUCCESS){
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dialog.show();
        }

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



    public static void verifyStoragePermissions(Activity activity) {

        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void snapPhoto(byte[] bytes){

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap face = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        mGraphicOverlay.setDrawingCacheEnabled(true);
        Bitmap overlay = mGraphicOverlay.getDrawingCache();


            try {
                String mainpath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
                File basePath = new File(mainpath);
                if (!basePath.exists())
                    Log.d("CAPTURE_BASE_PATH", basePath.mkdirs() ? "Success" : "Failed");
                String path = mainpath + "photo_" + getPhotoTime() + ".jpg";
                File captureFile = new File(path);
                captureFile.createNewFile();
                if (!captureFile.exists())
                    Log.d("CAPTURE_FILE_PATH", captureFile.createNewFile() ? "Success" : "Failed");
                FileOutputStream stream = new FileOutputStream(captureFile);
                if(mCameraSource.getCameraFacing()==CameraSource.CAMERA_FACING_FRONT){
                    Matrix mtx = new Matrix();
                    //this will prevent mirror effect
                    mtx.preScale(-1.0f, 1.0f);
                    Bitmap newImage = Bitmap.createBitmap(face, 0, 0, face.getWidth(), face.getHeight(), mtx, true);
                    if(isSmiledetectorActive){
                        newImage.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }else{
                        Bitmap result = mergeBitmaps(newImage, overlay);
                        result.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }
                }else{
                    if(isSmiledetectorActive){
                        face.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }else{
                        Bitmap result = mergeBitmaps(face, overlay);
                        result.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    }
                }

                stream.flush();
                stream.close();

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

    public Bitmap mergeBitmaps(Bitmap face, Bitmap overlay) {
      
        int width = face.getWidth();
        int height = face.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Rect faceRect = new Rect(0,0,width,height);
        Rect overlayRect = new Rect(0,0,overlay.getWidth(),overlay.getHeight());


        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(face, faceRect, faceRect, null);
        canvas.drawBitmap(overlay, overlayRect, faceRect, null);
        return newBitmap;
    }


    public void putMustacheOnFace(){

        GraphicOverlay mOverlay = new GraphicOverlay(context);
        GraphicFaceTracker tracker = new GraphicFaceTracker(mOverlay);
        tracker.mFaceGraphic.isFilterenabled();
        Log.d(TAG, "putMustacheOnFace: check");



    }

    private void detectSmile() {

        GraphicOverlay overlay = new GraphicOverlay(context);
        GraphicFaceTracker tracker = new GraphicFaceTracker(overlay);
        tracker.mFaceGraphic.isSmiling();
        Log.d(TAG, "detectSmile: hello");
        if (!isSmiledetectorActive){
            isSmiledetectorActive = true;
        }
        else{
            isSmiledetectorActive=false;
        }
    }

}

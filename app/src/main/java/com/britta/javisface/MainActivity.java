package com.britta.javisface;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.britta.javisface.ui.camera.CameraSourcePreview;
import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    public static final String TAG ="JavisFace";
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreview =(CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(rc == PackageManager.PERMISSION_GRANTED){
            createCameraSource();
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

    private void createCameraSource(){

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context).setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build();

        if(!detector.isOperational()){
            Log.w(TAG, "Gesichterkennung noch nicht vollständig geladen");
        }

        mCameraSource = new CameraSource.Builder(context, detector).setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK).setRequestedFps(30.0f).build();
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
            createCameraSource();
            return;
        }

        Log.e(TAG, "Zugriff nicht erlaubt: results len = " + grantResults.length +
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
            mFaceGraphic = new FaceGraphic(overlay);
        }
    }
}

package com.britta.javisface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

public class FaceGraphic extends GraphicOverlay.Graphic {


    private static final float TEXT_SIZE = 40.0f;
    private static final int TEXT_COLOR = Color.GREEN;
    private Paint mSmilePaint;
    private Paint mLandmarkPaint;
    private Bitmap bmapMustache;
    private BitmapFactory.Options options;
    private Resources resources;
    private static boolean isFilterEnabled;
    private static boolean isSmiling;
    private boolean smiling = true;

    private volatile Face mFace;
    private int mFaceID;
    private float mSmiling;
    

    public FaceGraphic(GraphicOverlay overlay, Context context){
        super(overlay);

        options=new BitmapFactory.Options();
        options.inScaled = false;
        resources = context.getResources();
        bmapMustache = BitmapFactory.decodeResource(resources, R.drawable.mustache);

        mSmilePaint = new Paint();
        mSmilePaint.setColor(TEXT_COLOR);
        mSmilePaint.setTextSize(TEXT_SIZE);

        mLandmarkPaint = new Paint();

    }

    void setID(int id) {
        mFaceID = id;
    }

    void updateFace(Face face){
        mFace = face;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {

        Face face = mFace;
        if(face == null){
            return;
        }

        float x = translateX(face.getPosition().x + face.getWidth()/2);
        float y = translateY(face.getPosition().y + face.getHeight()/2);
       
        int scaleFactorFilterWidth =(int)face.getWidth()/2;
        int scaleFactorFilterheight = (int)face.getHeight()/6;

       if(isFilterEnabled){
           //Log.d(TAG, "draw: helloo filterenabled is true");

           for (Landmark landmark: face.getLandmarks()){
               int cx = (int) translateX(landmark.getPosition().x );
               int cy = (int) translateY(landmark.getPosition().y );
               //canvas.drawCircle(cx,cy,10f,mLandmarkPaint);
               if(landmark.getType() ==Landmark.NOSE_BASE){
                   Bitmap scaledMustacheBM = Bitmap.createScaledBitmap(bmapMustache, scaleFactorFilterWidth,scaleFactorFilterheight,true);
                   canvas.drawBitmap(scaledMustacheBM, cx-(scaleFactorFilterWidth/2),cy, mLandmarkPaint);
               }
           }
       }
       else{
          // Log.d(TAG, "draw: nothing to draw");
       }

       if(isSmiling) {
           if(smiling) {
               mSmiling = face.getIsSmilingProbability() * 100;
               if(mSmiling<=0){
                   mSmiling = 0;
               }
               canvas.drawText("Happiness: " +Math.floor(mSmiling)+ " %", x-100,y-100,mSmilePaint);
               if (mSmiling > 70) {
                   canvas.drawText("Smiling! Snap a photo! ",x-100,y-200,mSmilePaint);
               } else {
                   canvas.drawText("Say CHEEEESE!",x-100,y-200,mSmilePaint);
               }
            }
        }
    }

    public static void setFilterEnabled() {
        if(!isFilterEnabled){
            isFilterEnabled =true;
        }
        else{
           isFilterEnabled =false;
        }
    }
    
    public static void setSmilingEnabled(){
       if(isSmiling){
           isSmiling =false;
       }
       else{
           isSmiling = true;
       }
    }
}



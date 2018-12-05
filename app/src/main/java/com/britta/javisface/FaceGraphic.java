package com.britta.javisface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

public class FaceGraphic extends GraphicOverlay.Graphic {


    private static final float TEXT_SIZE = 40.0f;
    private static final String TAG ="FaceGraphic";
    private static final int COLOR_CHOICES[]={
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.RED,
            Color.MAGENTA,
            Color.WHITE,
            Color.BLACK,
            Color.YELLOW

    };
    private static int mCurrentColorIndex = 0;
    private Paint mSmilePaint;
    private Paint mLandmarkPaint;

    private Bitmap bmapMustache;
    private BitmapFactory.Options options;
    private Resources resources;
    public static boolean isFilterenabled;
    public static boolean isSmiling;
    public boolean smiling = true;

    private volatile Face mFace;
    private int mFaceID;
    private float mHappiness;
    

    public FaceGraphic(GraphicOverlay overlay, Context context){
        super(overlay);

        options=new BitmapFactory.Options();
        options.inScaled = false;
        resources = context.getResources();
        bmapMustache = BitmapFactory.decodeResource(resources, R.drawable.mustache);

        mCurrentColorIndex =(mCurrentColorIndex+1)% COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mSmilePaint = new Paint();
        mSmilePaint.setColor(selectedColor);
        mSmilePaint.setTextSize(TEXT_SIZE);

        mLandmarkPaint = new Paint();
        mLandmarkPaint.setColor(selectedColor);

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

       if(isFilterenabled){
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
               mHappiness = face.getIsSmilingProbability() * 100;
               canvas.drawText("Happines: " +Math.floor(mHappiness)+ " %", x-100,y-100,mSmilePaint);
               if (mHappiness > 70) {
                   canvas.drawText("Smiling! Snap a photo! ",x-100,y-200,mSmilePaint);
               } else {
                   canvas.drawText("Say CHEEEESE!",x-100,y-200,mSmilePaint);
               }
            }
        }
    }

    public static boolean isFilterenabled() {
        if(!isFilterenabled){
            isFilterenabled=true;
        }
        else{
           isFilterenabled =false;
        }
        return isFilterenabled;
    }
    
    public static boolean isSmiling(){
       if(isSmiling){
           isSmiling =false;
       }
       else{
           isSmiling = true;
       }
        return isSmiling;
    }
}



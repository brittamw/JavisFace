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

    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

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
    private Paint mFacePositionPaint;
    private Paint mIDPaint;
    private Paint mBoxPaint;
    private Paint mLandmarkPaint;

    private Bitmap bmapGreenEye;
    private Bitmap bmapEyepatch;
    private BitmapFactory.Options options;
    private Resources resources;
    public static boolean isFilterenabled;


    private volatile Face mFace;
    private int mFaceID;
    private float mHappiness;
    

    public FaceGraphic(GraphicOverlay overlay, Context context){
        super(overlay);


        options=new BitmapFactory.Options();
        options.inScaled = false;
        resources = context.getResources();
        bmapGreenEye = BitmapFactory.decodeResource(resources, R.drawable.akiszalia, options);
        bmapEyepatch = BitmapFactory.decodeResource(resources, R.drawable.eyepatch);

        mCurrentColorIndex =(mCurrentColorIndex+1)% COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIDPaint = new Paint();
        mIDPaint.setColor(selectedColor);
        mIDPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

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
        mHappiness = face.getIsSmilingProbability()*100;



        float x = translateX(face.getPosition().x + face.getWidth()/2);
        float y = translateY(face.getPosition().y + face.getHeight()/2);
        //canvas.drawCircle(x,y, FACE_POSITION_RADIUS,mFacePositionPaint);

       //canvas.drawText("Happines: "+Math.floor(mHappiness)+ "%", x,y,mIDPaint);

        float xOffset = scaleX(face.getWidth()/2.0f);
        float yOffset = scaleY(face.getHeight()/2.0f);

        float left = x -xOffset;
        float top = y-yOffset;
        float right = x + xOffset;
        float bottom = y+yOffset;



       if(isFilterenabled){
           Log.d(TAG, "draw: helloo filterenabled is truee");
           canvas.drawRect(left, top, right,bottom, mBoxPaint);
           for (Landmark landmark: face.getLandmarks()){
               int cx = (int) translateX(landmark.getPosition().x );
               int cy = (int) translateY(landmark.getPosition().y );

               if(landmark.getType()== Landmark.LEFT_EYE){
                   Bitmap scaledGreenEyeBm = Bitmap.createScaledBitmap(bmapGreenEye,50,50,true);

                   canvas.drawBitmap(scaledGreenEyeBm,cx,cy,mBoxPaint);
               }
           }
       }
       else{
           Log.d(TAG, "draw: nothing to draw");
       }
    }

    public static boolean isFilterenabled() {
        if(!isFilterenabled){
            isFilterenabled=true;
           // MustacheEnabled mustache = new MustacheEnabled();
            //mustache.setMustacheThere();


            Log.d(TAG, "choseFilter: is false, set true");
        }
        else{
           isFilterenabled =false;
        }
        return isFilterenabled;
    }
}



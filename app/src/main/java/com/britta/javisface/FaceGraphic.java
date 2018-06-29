package com.britta.javisface;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.graphics.drawable.BitmapDrawable;
import android.app.Activity;

import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    //private Landmark landmark;



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

    private volatile Face mFace;
    private int mFaceID;
    //private float mHappiness;

    FaceGraphic(GraphicOverlay overlay){
        super(overlay);
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

        float x = translateX(face.getPosition().x + face.getWidth()/2);
        float y = translateY(face.getPosition().y + face.getHeight()/2);
        canvas.drawCircle(x,y, FACE_POSITION_RADIUS,mFacePositionPaint);
        canvas.drawText("ID "+ mFaceID, x+ID_X_OFFSET, y+ID_Y_OFFSET,mIDPaint);

        float xOffset = scaleX(face.getWidth()/2.0f);
        float yOffset = scaleY(face.getHeight()/2.0f);

        float left = x -xOffset;
        float top = y-yOffset;
        float right = x + xOffset;
        float bottom = y+yOffset;
        canvas.drawRect(left, top, right,bottom, mBoxPaint);


        for (Landmark landmark: face.getLandmarks()){
            int cx = (int) (landmark.getPosition().x);
            int cy = (int) (landmark.getPosition().y);
            //zum Landamrks markieren:
            canvas.drawCircle(cx, cy, 10.0f, mLandmarkPaint);

            //landmark mit IDs
            //String type = String.valueOf(landmark.getType());
            //mBoxPaint.setTextSize(50.0f);
            //canvas.drawText(type, cx,cy, mBoxPaint);



          // if(landmark.getType()== 4){
               // Bitmap bMap = BitmapFactory.decodeFile("C:\\Users\\Britta\\Desktop\\BA\\akiszalia.png");
              //  canvas.drawBitmap(bMap,cx,cy,mBoxPaint);
          // }





        }


    }
}

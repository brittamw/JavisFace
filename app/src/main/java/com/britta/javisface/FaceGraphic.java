package com.britta.javisface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.britta.javisface.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;



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
    private float mHappiness;

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
        mHappiness = face.getIsSmilingProbability()*100;
        float x = translateX(face.getPosition().x + face.getWidth()/2);
        float y = translateY(face.getPosition().y + face.getHeight()/2);
        canvas.drawCircle(x,y, FACE_POSITION_RADIUS,mFacePositionPaint);
        //canvas.drawText("ID "+ mFaceID, x+ID_X_OFFSET, y+ID_Y_OFFSET,mIDPaint);
        canvas.drawText("Happines: "+mHappiness+ "%", x,y,mIDPaint);

        float xOffset = scaleX(face.getWidth()/2.0f);
        float yOffset = scaleY(face.getHeight()/2.0f);

        float left = x -xOffset;
        float top = y-yOffset;
        float right = x + xOffset;
        float bottom = y+yOffset;
        canvas.drawRect(left, top, right,bottom, mBoxPaint);


        for (Landmark landmark: face.getLandmarks()){
            int cx = (int) translateX(landmark.getPosition().x );
            int cy = (int) translateY(landmark.getPosition().y );
            //zum Landamrks markieren:
            canvas.drawCircle(cx, cy, 10.0f, mLandmarkPaint);


            //landmark mit TypeIDs
            //String type = String.valueOf(landmark.getType());
            //mLandmarkPaint.setTextSize(50.0f);
            //canvas.drawText(type, cx,cy, mLandmarkPaint);

        /*landmark typeIDs:
            linkes auge: 4
            rechtes auge: 10
            nasenspitze: 6
            wange links : 1
            wange rechts: 7
            mundwinkel links: 5
            mundwinkel rechts: 11
            mund center: 0
            */

          /*if(landmark.getType()== 4){
              Bitmap bmap = Bitmap.createBitmap();
              canvas.drawBitmap(bmap,cx,cy,mBoxPaint);
           }*/


        }



    }

}

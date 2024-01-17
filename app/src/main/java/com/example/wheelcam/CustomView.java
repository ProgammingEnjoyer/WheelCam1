package com.example.wheelcam;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class CustomView extends View {
    private Paint paint;
    private float horizontalLineY = 0;
    private float verticalLineX = 0;


    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        Log.d("CustomView", "setVisibility called with: " + visibility);
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw vertical line
        canvas.drawLine(0, horizontalLineY, getWidth(), horizontalLineY, paint);
        // draw horizontal line
        canvas.drawLine(verticalLineX, 0, verticalLineX, getHeight(), paint);
    }

    public void setHorizontalLineY(float y) {
        this.horizontalLineY = y;
        invalidate();
    }

    public void setVerticalLineX(float x) {
        this.verticalLineX = x;
        invalidate();
    }

    public float[] getIntersectionPoint() {
        //get coordinate of central point
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float adjustedX = verticalLineX - centerX;
        float adjustedY = centerY - horizontalLineY;

        return new float[]{adjustedX, adjustedY};
    }

}




package com.example.joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class JoystickView extends View {

    private float centerX = 0f;
    private float centerY = 0f;
    private float baseRadius = 0f;
    private float hatRadius = 0f;


    private float hatX= 0f;
    private float hatY= 0f;


    private Paint basePaint;
    private Paint hatPaint;

    private float thumbX;
    private float thumbY;
    private final float thumbRadius = 100f;



    private OnJoystickMoveListener onJoystickMoveListener;



    public JoystickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init()
    {
        setFocusable(true);
        setClickable(true);

        basePaint=new Paint();
        basePaint.setColor(Color.parseColor("#281E5D"));
        basePaint.setStyle(Paint.Style.FILL);

        hatPaint=new Paint();
        hatPaint.setColor(Color.parseColor("#3F51B5"));
        hatPaint.setStyle(Paint.Style.FILL);
        hatPaint.setShadowLayer(15f, 10f, 10f, Color.BLACK);

        thumbX = getWidth() / 2f;
        thumbY = getHeight() / 2f;

        //thumbX = 0f;
        //thumbY = 0f;


    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(hatX, hatY, hatRadius, hatPaint);
    }

    //Movimiento


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;


        double distance = Math.sqrt(dx * dx + dy * dy);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (distance < baseRadius){
                    hatX = event.getX();
                    hatY = event.getY();
                } else{
                    float ratio= baseRadius / (float) distance;
                    hatX = centerX + dx * ratio;
                    hatY = centerY + dy * ratio;
                }
                invalidate();
                updateListener();
                break;
            case MotionEvent.ACTION_UP:
                resetHatPosition();
                invalidate();
                updateListener();
                break;
        }
        return true;
    }

    private  void  updateListener(){
        float xPercent = (hatX - centerX) / baseRadius;
        float yPercent = (hatY - centerY) / baseRadius;
        String direction = calculateDirection(xPercent, yPercent);

        if (onJoystickMoveListener != null)
        {
            onJoystickMoveListener.onMove(xPercent, yPercent, direction);
        }
    }

    private String calculateDirection(float xPercent, float yPercent) {
        final float deadZone = 0.1f;
        if (Math.abs(xPercent) < 0.3 && Math.abs(yPercent) < 0.3) {
            return "Idle";
        }

        if (Math.abs(xPercent) > Math.abs(yPercent)) {
            return (xPercent > 0) ? "Right" : "Left";
        } else {
            return (yPercent < 0) ? "Up" : "Down";
        }

    }

    @Override
    protected  void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        baseRadius = Math.min(getWidth(), getHeight()) /3f;
        hatRadius = baseRadius / 2.5f;
        resetHatPosition();

    }

    private void resetHatPosition(){
        hatX = centerX;
        hatY = centerY;
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener listener)
    {
       this.onJoystickMoveListener = listener;
    }


}

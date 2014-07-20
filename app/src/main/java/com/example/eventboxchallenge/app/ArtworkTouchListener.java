package com.example.eventboxchallenge.app;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by roinir.
 */
public abstract class ArtworkTouchListener implements View.OnTouchListener {
    private static final long MIN_DRAG_DIST = 10;
    private int downX;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                downX = (int) motionEvent.getRawX();
                return true;
            case MotionEvent.ACTION_UP:
                if(Math.abs(motionEvent.getRawX() - downX) <= MIN_DRAG_DIST){
                    onClick(view);
                } else {
                    onDragStop(view,((int) motionEvent.getRawX())-downX, motionEvent);
                }

                return true;
            case MotionEvent.ACTION_MOVE:
                int dragX = (int) motionEvent.getRawX();
                if(Math.abs(motionEvent.getRawX() - downX) > MIN_DRAG_DIST){
                    onDrag(view, dragX - downX);
                }

                return true;
        }
        return false;
    }

    public abstract void onClick(View v);
    public abstract void onDrag(View v, int deltaX);
    public abstract void onDragStop(View v, int deltaX, MotionEvent e);
}

package com.example.spaceinvaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

public class Defender {
    RectF rect;

    // The player ship will be represented by a Bitmap
    private Bitmap bitmap;
    private Bitmap bitmap_stationary;
    private Bitmap bitmap_left;
    private Bitmap bitmap_right;

    // How long and high our paddle will be
    private float length;
    private float height;

    // so can manipulate defender on screen
    private float swidth;

    // excess is the ExcessX
    private int excess;

    // X is the far left of the rectangle which forms our paddle
    private float x;

    // Y is the top coordinate
    private float y;

    // This will hold the pixels per second speed that the paddle will move
    private float defenderSpeed;

    // Which ways can the paddle move
    public final int STOPPED = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    // Is the ship moving and in which direction
    private int defenderMoving = STOPPED;

    // This is the constructor method
    // When we create an object from this class we will pass
    // in the screen width and height
    public Defender(Context context, int screenX, int screenY, int ExcessX){

        // Initialize a blank RectF
        rect = new RectF();

        this.excess = ExcessX;
        this.swidth = screenX + ExcessX;

        length = screenX/12;
        height = screenX/12;

        // Start ship in roughly the screen centre
        x = ExcessX + ((screenX / 2) - (length / 2));
        y = screenY - height;

        // Initialize the bitmap
        bitmap_stationary = BitmapFactory.decodeResource(context.getResources(), R.drawable.defender);
        bitmap_left = BitmapFactory.decodeResource(context.getResources(), R.drawable.defender_left);
        bitmap_right = BitmapFactory.decodeResource(context.getResources(), R.drawable.defender_right);

        // stretch the bitmap to a size appropriate for the screen resolution
        bitmap_stationary = Bitmap.createScaledBitmap(bitmap_stationary, (int) (length), (int) (height), false);
        bitmap_left = Bitmap.createScaledBitmap(bitmap_left, (int) (length), (int) (height), false);
        bitmap_right = Bitmap.createScaledBitmap(bitmap_right, (int) (length), (int) (height), false);
        bitmap = bitmap_stationary;

        // How fast is the paddle in pixels per second
        defenderSpeed = 800;
    }

    public RectF getRect(){
        return rect;
    }

    // This is a getter method to make the rectangle that
    // defines our paddle available in BreakoutView class
    public Bitmap getBitmap(){
        return bitmap;
    }

    public float getX(){
        return x;
    }

    public float getY() {
        return y;
    }

    public float getLength(){
        return length;
    }

    public float getHeight() {
        return height;
    }
    // This method will be used to change/set if the paddle is going left, right or nowhere
    public void setMovementState(int state){
        defenderMoving = state;
    }

    // This update method will be called from update in SpaceInvadersView
    // It determines if the player ship needs to move and changes the coordinates
    // contained in x if necessary
    public void update(long fps){
        if(defenderMoving == LEFT){
            x = x - defenderSpeed / fps;
            this.bitmap = bitmap_left;
        } else if(defenderMoving == RIGHT){
            x = x + defenderSpeed / fps;
            this.bitmap = bitmap_right;
        } else {
            this.bitmap = bitmap_stationary;
        }

        if (x < excess) {
            setMovementState(STOPPED);
            x = excess;
        } else if (x > (swidth - length)) {
            setMovementState(STOPPED);
            x = swidth - length;
        }

        // Update rect which is used to detect hits
        rect.top = y;
        rect.bottom = y + height;
        rect.left = x;
        rect.right = x + length;
    }
}

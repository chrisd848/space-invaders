package com.example.spaceinvaders;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

public class SpaceInvadersActivity extends Activity {
    SpaceInvadersView spaceInvadersView;
    boolean paused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        spaceInvadersView = new SpaceInvadersView(this, size.x, size.y);
        setContentView(spaceInvadersView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        spaceInvadersView.resume();
        paused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        spaceInvadersView.pause();
        paused = true;
    }

    @Override
    public void onBackPressed() {
        if (paused) {
            onResume();
        } else  {
            onPause();
        }
    }
}
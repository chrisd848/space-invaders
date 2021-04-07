package com.example.spaceinvaders;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.io.InputStream;

public class SpaceInvadersView extends SurfaceView implements Runnable {
    Context context;

    // This is our thread
    private Thread gameThread = null;

    // Our SurfaceHolder to lock the surface before we draw our graphics
    private SurfaceHolder ourHolder;

    // A boolean which we will set and unset
    // when the game is running- or not.
    private volatile boolean playing;

    // Game is paused at the start
    private boolean paused = true;
    private boolean openMainMenu = true;
    private boolean openGameOverMenu = false;
    private boolean openUpgradesMenu = false;
    private boolean openPauseMenu = false;

    // A Canvas and a Paint object
    private Canvas gameCanvas;
    private Canvas menuCanvas;
    private Canvas effectsCanvas;
    private Paint paint;

    // This variable tracks the game frame rate
    private long fps;

    // Animation timer
    private int animationTimer_explosion;
    private int animationTimer_damage;
    private int animationTimer_level;
    private long animationFrames_explosion = 0;
    private long animationFrames_damage = 0;
    private long animationFrames_level = 0;

    private boolean damageAnimation = false;
    private boolean explosionAnimation = false;
    private boolean levelAnimation = true;
    private float explosionX;
    private float explosionY;

    // This is used to help calculate the fps
    private long timeThisFrame;

    // The size of the screen in pixels
    private int screenX;
    private int screenY;

    private int PlayableX;
    private int PlayableY;
    private int ExcessX;

    // HUD Layout
    private RectF leftSideRect;
    private RectF middleSideRect;
    private RectF rightSideRect;
    private RectF hudRect;
    private RectF pauseRect;

    // New background
    private Bitmap btBgLeft;
    private Bitmap btBgMiddle;
    private Bitmap btBgRight;

    // New explosion
    private Bitmap[] explosionEffect;

    //game object

    private Defender defender;

    // The player's bullet
    private Bullet bullet;

    // Invaders bullets
    private Bullet[] invadersBullets = new Bullet[200];
    private int nextBullet;
    private int maxInvaderBullets = 10;

    // Up to 60 invaders
    Invader[] invaders = new Invader[60];
    int numInvaders = 0;

    // For sound FX
    private SoundPool soundPool;
    private int playerShootID = -1;
    private int invaderShootID = -1;
    private int invaderExplodeID = -1;
    private int playerExplodeID = -1;
    private int shieldImpactID = -1;
    private int severeDamageID = -1;
    private int uhID = -1;
    private int ohID = -1;

    // The score
    private int finalScore = 0;
    private int totalScore = 0;
    private int score = 0;

    // Score numbers
    private Bitmap[] scoreNumbers;

    // Cash
    private int cash = 0;

    // Multiplier
    private int multiplier = 1;

    // No hit streak
    private int noHitStreak = 0;

    // Shields
    private boolean alive = true;
    private int shields = 3;

    // Level
    private int wavesSurvived = 0;
    private int level = 1;

    // invaders column and row
    private int invadersColumn = 5;
    private int invadersRow = 3;

    // invaders left
    private int invadersLeft;

    // How menacing should the sound be?
    private long menaceInterval = 1000;

    // When did we last play a menacing sound
    private long lastMenaceTime = System.currentTimeMillis();

    // Which menace sound should play next
    private boolean uhOrOh;

    public SpaceInvadersView(Context context, int x, int y) {
        super(context);
        this.context = context;

        // Initialize ourHolder and paint objects
        ourHolder = getHolder();
        paint = new Paint();

        // Set typeface
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.barcade_brawl));

        screenX = x;
        screenY = y;

        PlayableX = (y / 3) * 4;
        PlayableY = (y / 3) * 3;
        ExcessX = (x - PlayableX) / 2;

        // left
        leftSideRect = new RectF();
        leftSideRect.set(0, 0, ExcessX, screenY);

        // middle
        middleSideRect = new RectF();
        middleSideRect.set(leftSideRect.right, 0, (leftSideRect.right + PlayableX), screenY);

        // right
        rightSideRect = new RectF();
        rightSideRect.set(middleSideRect.right, 0, screenX, screenY);

        // hud rect
        hudRect = new RectF();
        hudRect.set(ExcessX, 0, ExcessX+PlayableX, PlayableY/10);

        // pause rect
        pauseRect = new RectF();
        pauseRect.set(leftSideRect.left + (ExcessX/100)*5, leftSideRect.top + (ExcessX/100)*5, leftSideRect.right - (ExcessX/100)*10, (PlayableY/100)*15);

        // Background bitmap
        btBgLeft = BitmapFactory.decodeResource(getResources(), R.drawable.left_side);
        btBgLeft = Bitmap.createScaledBitmap(btBgLeft, ExcessX, PlayableY, true);

        btBgMiddle = BitmapFactory.decodeResource(getResources(), R.drawable.middle_side);
        btBgMiddle = Bitmap.createScaledBitmap(btBgMiddle, PlayableX, PlayableY, true);

        btBgRight = BitmapFactory.decodeResource(getResources(), R.drawable.right_side);
        btBgRight = Bitmap.createScaledBitmap(btBgRight, ExcessX, PlayableY, true);

        // This SoundPool is deprecated but don't worry
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try{
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Load our fx in memory ready for use
            descriptor = assetManager.openFd("sounds/player_shoot.wav");
            playerShootID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/invader_shoot.wav");
            invaderShootID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/invader_explode.wav");
            invaderExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/player_explode.wav");
            playerExplodeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/player_impact2.ogg");
            shieldImpactID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/severe_damage.wav");
            severeDamageID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/uh.ogg");
            uhID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sounds/oh.ogg");
            ohID = soundPool.load(descriptor, 0);

        }catch(IOException e){
            // Print an error message to the console
            Log.e("error", "failed to load sound files");
        }

        try {
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            scoreNumbers = new Bitmap[10];
            for (int i=0; i < 10; i++) {
                scoreNumbers[i] = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(assetManager.open("numbers/" + (i) + ".png")), PlayableX/32, PlayableX/32, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Print an error message to the console
            Log.e("error", "failed to load explosion effect image files");
        }

        prepareLevel();
    }
    private void prepareLevel(){

        // Level animation set to true
        levelAnimation = true;

        // Reset the menace level
        menaceInterval = 1200;

        // Here we will initialize all the game objects
        // Make a new player space ship
        defender = new Defender(context, PlayableX, PlayableY, ExcessX);

        // Prepare the players bullet
        bullet = new Bullet(context, PlayableX, PlayableY, (float) 1.2);

        // Initialize the invadersBullets array
        for(int i = 0; i < invadersBullets.length; i++){
            invadersBullets[i] = new Bullet(context, PlayableX, PlayableY, (float) 0.80);
        }

        // Build an army of invaders
        numInvaders = 0;
        for(int column = 0; column < invadersColumn; column ++ ){
            for(int row = 0; row < invadersRow; row ++ ){
                invaders[numInvaders] = new Invader(context, row, column, PlayableX, PlayableY, ExcessX);
                numInvaders ++;
            }
        }
        invadersLeft = numInvaders;

        try {
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            explosionEffect = new Bitmap[12];
            for (int i=0; i < 12; i++) {
                explosionEffect[i] = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(assetManager.open("explosion/" + (i + 1) + ".png")), (int) (invaders[0].getLength()*1.75), (int) (invaders[0].getLength()*1.75), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Print an error message to the console
            Log.e("error", "failed to load explosion effect image files");
        }

    }

    @Override
    public void run() {
        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Update the frame
            if(!paused){
                update();
            }

            // Draw the frame
            draw();

            // Calculate the fps this frame
            // We can then use the result to
            // time animations and more.
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }

            // We will do something new here towards the end of the project
            // Play a sound based on the menace level
            if(!paused) {
                if ((startFrameTime - lastMenaceTime) > menaceInterval) {
                    if (uhOrOh) {
                        // Play Uh
                        soundPool.play(uhID, 1, 1, 0, 0, 1);
                    } else {
                        // Play Oh
                        soundPool.play(ohID, 1, 1, 0, 0, 1);
                    }

                    // Reset the last menace time
                    lastMenaceTime = System.currentTimeMillis();
                    // Alter value of uhOrOh
                    uhOrOh = !uhOrOh;
                }
            }
        }
    }

    private void update(){

        // Did an invader bump into the side of the screen
        boolean bumped = false;

        // Has the player lost
        boolean lost = false;

        // Move the player's ship
        defender.update(fps);

        // Update the invaders if visible
        for(int i = 0; i < numInvaders; i++){

            if(invaders[i].getVisibility()) {
                // Move the next invader
                invaders[i].update(fps);

                // Does he want to take a shot?
                if(invaders[i].takeAim(defender.getX(),
                        defender.getLength())){

                    // If so try and spawn a bullet
                    if(invadersBullets[nextBullet].shoot(invaders[i].getX() + invaders[i].getLength() / 2, invaders[i].getY(), bullet.DOWN)) {
                        soundPool.play(invaderShootID, 1, 1, 0, 0, 1);

                        // Shot fired
                        // Prepare for the next shot
                        nextBullet++;

                        // Loop back to the first one if we have reached the last
                        if (nextBullet == maxInvaderBullets) {
                            // This stops the firing of another bullet until one completes its journey
                            // Because if bullet 0 is still active shoot returns false.
                            nextBullet = 0;
                        }
                    }
                }
                // If that move caused them to bump the screen change bumped to true
                if (invaders[i].getX() > screenX - ExcessX - invaders[i].getLength()
                        || invaders[i].getX() < ExcessX){

                    bumped = true;
                }
            }
        }

        // Update all the invaders bullets if active
        for(int i = 0; i < invadersBullets.length; i++){
            if(invadersBullets[i].getStatus()) {
                invadersBullets[i].update(fps);
            }
        }

        // Did an invader bump into the edge of the screen
        if(bumped){

            // Move all the invaders down and change direction
            for(int i = 0; i < numInvaders; i++){
                invaders[i].dropDownAndReverse();
                // Have the invaders landed
                if(invaders[i].getY() > screenY - screenY / 10){
                    lost = true;
                }
            }

            // Increase the menace level
            // By making the sounds more frequent
            menaceInterval = menaceInterval - 80;
        }

        if(lost){
            prepareLevel();
        }

        // Update the players bullet
        if(bullet.getStatus()){
            bullet.update(fps);
        }

        // Has the player's bullet hit the top of the screen
        if(bullet.getImpactPointY() < 0){
            bullet.setInactive();
        }

        // Has an invaders bullet hit the bottom of the screen
        for(int i = 0; i < invadersBullets.length; i++){
            if(invadersBullets[i].getImpactPointY() > screenY){
                invadersBullets[i].setInactive();
            }
        }

        // Has the player's bullet hit an invader
        if(bullet.getStatus()) {
            for (int i = 0; i < numInvaders; i++) {
                if (invaders[i].getVisibility()) {
                    if (RectF.intersects(bullet.getRect(), invaders[i].getRect())) {
                        invaders[i].setInvisible();
                        explosionAnimation = true;
                        explosionX = invaders[i].getX() - invaders[i].getLength()/2;
                        explosionY = invaders[i].getY() - invaders[i].getHeight()/2;
                        soundPool.play(invaderExplodeID, 1, 1, 0, 0, 1);
                        bullet.setInactive();
                        score = (10 * level) * multiplier;
                        totalScore = totalScore + score;
                        cash = cash + 10;
                        invadersLeft = invadersLeft - 1;

                        // Has the player won
                        if(invadersLeft == 0){
                            shields = shields + 1;
                            cash = cash + (10 * multiplier);
                            totalScore = totalScore + ((100 * level) * multiplier);
                            level = level + 1;
                            noHitStreak = noHitStreak + 1;
                            if (noHitStreak > 0) {
                                multiplier = noHitStreak * 2;
                                if (multiplier > 16) {
                                    multiplier = 16;
                                }
                            } else {
                                multiplier = 1;
                            }
                            noHitStreak = noHitStreak + 1;
                            openUpgradesMenu = true;
                            prepareLevel();
                        }
                    }
                }
            }
        }

        // Has an invader bullet hit the player ship
        for(int i = 0; i < invadersBullets.length; i++){
            if(invadersBullets[i].getStatus()){
                if(RectF.intersects(defender.getRect(), invadersBullets[i].getRect())){
                    invadersBullets[i].setInactive();
                    damageAnimation = true;
                    if (shields == 0) {
                        alive = false;
                    } else if (shields == 1) {
                        soundPool.play(severeDamageID, 1, 1, 0, 0, 1);
                        shields = shields - 1;
                    } else {
                        shields = shields - 1;
                    }
                    soundPool.play(shieldImpactID, 1, 1, 0, 0, 1);
                    noHitStreak = 0;
                    multiplier = 1;

                    // Is it game over?
                    if(!alive){
                        soundPool.play(playerExplodeID, 1, 1, 0, 0, 1);
                        wavesSurvived = level-1;
                        finalScore = totalScore;
                        shields = 3;
                        alive = true;
                        cash = 0;
                        multiplier = 1;
                        totalScore = 0;
                        level = 1;
                        invadersColumn = 5 ;
                        invadersRow = 3;
                        openGameOverMenu = true;
                        prepareLevel();
                    }
                }
            }
        }
    }

    private void draw(){

        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {

            if (openMainMenu) {
                mainMenu();
            } else if (openGameOverMenu) {
                gameOverMenu();
            } else if (openUpgradesMenu) {
                upgradeMenu();
            } else if (openPauseMenu) {
                pauseMenu();
            } else {
                // Lock the canvas ready to draw
                gameCanvas = ourHolder.lockCanvas();

                // Draw the background
                gameCanvas.drawBitmap(btBgLeft, leftSideRect.left, leftSideRect.top, null);
                gameCanvas.drawBitmap(btBgMiddle, middleSideRect.left, middleSideRect.top, null);
                gameCanvas.drawBitmap(btBgRight, rightSideRect.left, rightSideRect.top, null);

                // HUD Rectangle
                String padding = " | ";
                paint.setTextSize(30);
                paint.setColor(Color.argb(125,  0, 0, 0));
                gameCanvas.drawRect(hudRect, paint);
                paint.setColor(Color.argb(255,  255, 255, 255));
                gameCanvas.drawText("Bonus: " + multiplier + padding + "Score: " + totalScore + padding + "Shields: " + shields + padding + "Cash: £" + cash, hudRect.left + (hudRect.right/25), hudRect.bottom - (hudRect.height()/3), paint);

                // Draw the invaders bullets if active
                for(int i = 0; i < invadersBullets.length; i++){
                    paint.setColor(Color.argb(255,  228, 53, 37));
                    if(invadersBullets[i].getStatus()) {
                        gameCanvas.drawRect(invadersBullets[i].getRect(), paint);
                    }
                }

                // Draw the players bullet if active
                if(bullet.getStatus()){
                    paint.setColor(Color.argb(255,  30, 127, 224));
                    gameCanvas.drawRect(bullet.getRect(), paint);
                }

                // Draw the player spaceship
                paint.setColor(Color.argb(255,  255, 255, 255));
                gameCanvas.drawBitmap(defender.getBitmap(), defender.getX(), screenY - defender.getHeight(), paint);

                // Draw the invaders
                for(int i = 0; i < numInvaders; i++){
                    if(invaders[i].getVisibility()) {
                        if(uhOrOh) {
                            gameCanvas.drawBitmap(invaders[i].getBitmap(), invaders[i].getX(), invaders[i].getY(), paint);
                        }else{
                            gameCanvas.drawBitmap(invaders[i].getBitmap2(), invaders[i].getX(), invaders[i].getY(), paint);
                        }
                    }
                }

                // Pause button
                paint.setColor(Color.argb(125,  0, 0, 0));
                gameCanvas.drawRect(pauseRect, paint);
                paint.setTextSize((float) (screenX*0.0138));
                paint.setColor(Color.argb(255,  255, 255, 255));
                gameCanvas.drawText("PAUSE GAME", pauseRect.left + (pauseRect.width()/10), pauseRect.bottom - (pauseRect.height()/3), paint);

                // Animations
                renderAnimations();

                // Draw everything to the screen
                ourHolder.unlockCanvasAndPost(gameCanvas);
            }

        }
    }

    private void renderAnimations() {

        // Animations
        paint.setColor(Color.argb(255,  0, 0, 0));
        double time;
        long frames;

        if (explosionAnimation) {
            time = 0.4;
            frames = (long) (fps * time);
            if (animationFrames_explosion == 0) {
                animationFrames_explosion = frames / 12;
            }

            paint.setColor(Color.argb(255,  255, 255, 255));
            animationTimer_explosion = animationTimer_explosion + 1;
            for (int i=0; i<12;i++) {
                if (animationTimer_explosion > frames) {
                    animationTimer_explosion = 0;
                    animationFrames_explosion = 0;
                    explosionAnimation = false;
                    break;
                } else if (animationTimer_explosion < (animationFrames_explosion * i+1)) {
                    gameCanvas.drawBitmap(explosionEffect[i], explosionX, explosionY, paint);
                    String number = String.valueOf(score);
                    for(int k = 0; k < number.length(); k++) {
                        int j = Character.digit(number.charAt(k), 10);
                        gameCanvas.drawBitmap(scoreNumbers[j], explosionX + (50 * k), explosionY-(6 * i), paint);
                    }
                    break;
                }
            }

        }

        if (levelAnimation) {
            time = 5;
            frames = (long) (fps * time);
            if (animationFrames_level == 0) {
                animationFrames_level = frames / 255;
            }

            animationTimer_level = animationTimer_level + 1;
            for (int i=0; i<255;i++) {
                if (animationTimer_level > frames) {
                    animationTimer_level = 0;
                    animationFrames_level = 0;
                    levelAnimation = false;
                    break;
                } else if (animationTimer_level < (animationFrames_level * i)) {
                    paint.setTextSize(75);
                    paint.setColor(Color.argb(255-i,  255, 255, 255));
                    gameCanvas.drawText("WAVE " + level, ExcessX+(PlayableX/3), ((PlayableY/10)*6)-i, paint);
                    break;
                }
            }

        }

        if (damageAnimation) {
            time = 1;
            frames = (long) (fps * time);
            if (animationFrames_damage == 0) {
                animationFrames_damage = frames / 30;
            }

            animationTimer_damage = animationTimer_damage + 1;
            for (int i=0; i<30;i++) {
                if (animationTimer_damage > frames) {
                    animationTimer_damage = 0;
                    animationFrames_damage = 0;
                    damageAnimation = false;
                    break;
                } else if (animationTimer_damage < (animationFrames_damage * i+1)) {
                    gameCanvas.drawColor(Color.argb(150-(i*5),  230, 25, 25));
                    break;
                }
            }

        }

        if (shields == 0) {
            if (uhOrOh) {
                for (int i=0;i<50;i++) {
                    gameCanvas.drawColor(Color.argb(50-i,  230, 25, 25));
                    paint.setColor(Color.argb(255,  255, 255, 255));
                    paint.setTextSize(50);
                    gameCanvas.drawText("NO SHIELDS", ExcessX + (PlayableX/3), (PlayableY/2), paint);
                }
            } else {
                gameCanvas.drawColor(Color.argb(20,  230, 25, 25));
            }
        }

    }

    private void mainMenu() {
        if (ourHolder.getSurface().isValid()) {
            menuCanvas = ourHolder.lockCanvas();
            menuCanvas.drawColor(Color.argb(255,  255, 255, 255));
            paint.setColor(Color.argb(255,  0, 0, 0));
            paint.setTextSize(75);
            int menuCurrent = (screenY/9);
            menuCanvas.drawText("MAIN MENU", ExcessX+PlayableX/4, (float) (menuCurrent*3-menuCurrent*0.5), paint);
            paint.setTextSize(50);
            menuCanvas.drawText("NEW GAME", ExcessX+PlayableX/4,menuCurrent*4, paint);
            menuCanvas.drawText("CONTROLS", ExcessX+PlayableX/4,menuCurrent*5, paint);
            menuCanvas.drawText("HIGH SCORES", ExcessX+PlayableX/4,menuCurrent*6, paint);
            ourHolder.unlockCanvasAndPost(menuCanvas);
        }
        pause();
    }

    private void gameOverMenu() {
        if (ourHolder.getSurface().isValid()) {
            menuCanvas = ourHolder.lockCanvas();
            menuCanvas.drawColor(Color.argb(255,  255, 255, 255));
            paint.setColor(Color.argb(255,  0, 0, 0));
            paint.setTextSize(75);
            int menuCurrent = (screenY/9);
            menuCanvas.drawText("GAME OVER", ExcessX+PlayableX/4, (float) (menuCurrent*3-menuCurrent*0.5), paint);
            paint.setTextSize(50);
            menuCanvas.drawText("FINAL SCORE: " + finalScore, ExcessX+PlayableX/4,menuCurrent*4, paint);
            menuCanvas.drawText("WAVES SURVIVED: " + wavesSurvived, ExcessX+PlayableX/4,menuCurrent*5, paint);
            menuCanvas.drawText("NEW GAME?", ExcessX+PlayableX/4,menuCurrent*6, paint);
            ourHolder.unlockCanvasAndPost(menuCanvas);
        }
        pause();
    }

    private void upgradeMenu() {
        if (ourHolder.getSurface().isValid()) {
            menuCanvas = ourHolder.lockCanvas();
            menuCanvas.drawColor(Color.argb(255,  255, 255, 255));
            paint.setColor(Color.argb(255,  0, 0, 0));
            paint.setTextSize(75);
            int menuCurrent = (screenY/9);
            menuCanvas.drawText("WAVE " + (level-1) + " COMPLETED", ExcessX+PlayableX/4, (float) (menuCurrent*3-menuCurrent*0.5), paint);
            paint.setTextSize(50);
            menuCanvas.drawText("BUY SHIELDS", ExcessX+PlayableX/4,menuCurrent*4, paint);
            menuCanvas.drawText("INCREASE SPEED", ExcessX+PlayableX/4,menuCurrent*5, paint);
            menuCanvas.drawText("EXTRA BULLETS", ExcessX+PlayableX/4,menuCurrent*6, paint);
            menuCanvas.drawText("NEXT WAVE", ExcessX+PlayableX/4,menuCurrent*7, paint);
            ourHolder.unlockCanvasAndPost(menuCanvas);
        }
        explosionAnimation = false;
        damageAnimation = false;
        pause();
    }

    private void pauseMenu() {
        if (ourHolder.getSurface().isValid()) {
            menuCanvas = ourHolder.lockCanvas();
            menuCanvas.drawColor(Color.argb(150,  0, 0, 0));
            paint.setColor(Color.argb(255,  255, 255, 255));
            paint.setTextSize(75);
            int menuCurrent = (screenY/9);
            menuCanvas.drawText("PAUSE MENU", ExcessX+PlayableX/4, (float) (menuCurrent*3-menuCurrent*0.5), paint);
            paint.setTextSize(50);
            menuCanvas.drawText("NEW GAME", ExcessX+PlayableX/4,menuCurrent*4, paint);
            menuCanvas.drawText("CONTROLS", ExcessX+PlayableX/4,menuCurrent*5, paint);
            menuCanvas.drawText("HIGH SCORES", ExcessX+PlayableX/4,menuCurrent*6, paint);
            menuCanvas.drawText("RESUME GAME", ExcessX+PlayableX/4,menuCurrent*7, paint);
            ourHolder.unlockCanvasAndPost(menuCanvas);
        }
        pause();
    }

    // If SpaceInvadersActivity is paused/stopped
    // shutdown our thread.
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }
    }

    // If SpaceInvadersActivity is started then
    // start our thread.
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:
                paused = false;
                // Pause game if pause button is touched
                if (playing && motionEvent.getX() > pauseRect.left && motionEvent.getX() < pauseRect.right && motionEvent.getY() > pauseRect.top && motionEvent.getY() < pauseRect.bottom) {
                    openPauseMenu = true;
                }
                if (motionEvent.getX() < ExcessX) {
                    defender.setMovementState(defender.LEFT);
                } else if (motionEvent.getX() > (ExcessX + PlayableX)) {
                    defender.setMovementState(defender.RIGHT);
                } else {
                    if(bullet.shoot(defender.getX()+ defender.getLength()/2,screenY, bullet.UP)){
                        soundPool.play(playerShootID, 1, 1, 0, 0, 1);
                    }
                }
                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                // Start game for first time
                if (openMainMenu && motionEvent.getX() > ExcessX && motionEvent.getX() < (ExcessX + PlayableX)) {
                    openMainMenu = false;
                    resume();
                }
                // Start new game
                if (openGameOverMenu && motionEvent.getX() > ExcessX && motionEvent.getX() < (ExcessX + PlayableX)) {
                    openGameOverMenu = false;
                    resume();
                }
                // Continue game
                if (openUpgradesMenu && motionEvent.getX() > ExcessX && motionEvent.getX() < (ExcessX + PlayableX)) {
                    openUpgradesMenu = false;
                    resume();
                }
                // Play game if paused
                if (!playing && motionEvent.getX() > ExcessX && motionEvent.getX() < (ExcessX + PlayableX)) {
                    openPauseMenu = false;
                    resume();
                }
                if(motionEvent.getX() > 0 && motionEvent.getX() < screenX) {
                    defender.setMovementState(defender.STOPPED);
                }
                break;
        }
        return true;
    }
}

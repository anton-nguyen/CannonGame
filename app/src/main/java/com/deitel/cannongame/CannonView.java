package com.deitel.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.util.HashMap;
import java.util.Map;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {
        //implements SurfaceHolder.Callback

    // to test apk for takehome final, kill cannongame already in emulator
    // settings/apps, find cannongame, uninstall
    // go to command prompt/terminal and find adb to put on phone

    // play another sound when cannonball is left of cannon base and in the center of cannon base
    // add your own wav file
    // get rid of "boing" noise

    private CannonThread cannonThread; // controls the game loop
    private Activity activity; // to display Game Over dialog in GUI thread
    private boolean dialogIsDisplayed = false;

    // constants for game play
    public static final int TARGET_PIECES = 5; // sections in the target
    public static final int MISS_PENALTY = 2; // seconds deducted on a miss
    public static final int HIT_REWARD = 3; // seconds added on a hit

    // variables for the game loop and tracking statistics
    private boolean gameOver; // is the game over?
    private double timeLeft; // the amount of time left in seconds
    private int shotsFired; // the number of shots the user has fired
    private double totalElapsedTime; // the number of seconds elapsed

    // variables for the blocker and target
    private Line blocker; // start and end points of the blocker
    private int blockerDistance; // blocker distance from left
    private int blockerBeginning; // blocker top-edge distance from top
    private int blockerEnd; // blocker bottom-edge distance from top
    private int initialBlockerVelocity; // initial blocker speed multiplier
    private float blockerVelocity; // blocker speed multiplier during game

    private Line target; // start and end points of the target
    private int targetDistance; // target distance from left
    private int targetBeginning; // target distance from top
    private double pieceLength; // length of a target piece
    private int targetEnd; // target bottom's distance from top
    private int initialTargetVelocity; // initial target speed multiplier
    private float targetVelocity; // target speed multiplier during game

    private int lineWidth; // width of the target and blocker
    private boolean[] hitStates; // is each target piece hit?
    private int targetPiecesHit; // number of target pieces hit (out of 7)

    // variables for the cannon and cannonball
    private Point cannonball; // cannonball image's upper-left corner
    private int cannonballVelocityX; // cannonball's x velocity
    private int cannonballVelocityY; // cannonball's y velocity
    private boolean cannonballOnScreen; // is the cannonball on the screen
    private int cannonballRadius; // cannonball radius
    private int cannonballSpeed; // cannonball speed
    private int cannonBaseRadius; // cannon base radius
    private int cannonLength; // cannon barrel length
    private Point barrelEnd; // the endpoint of the cannon's barrel
    private int screenWidth; // width of the screen
    private int screenHeight; // height of the screen

    // constants and variables for managing sounds
    private static final int TARGET_SOUND_ID = 0;
    private static final int CANNON_SOUND_ID = 1;
    private static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private Map<Integer, Integer> soundMap; // maps IDs to SoundPool

    // Paint variables used when drawing each item on the screen
    private Paint textPaint; // Paint used to draw text
    private Paint cannonballPaint; // Paint used to draw the cannonball
    private Paint cannonPaint; // Paint used to draw the cannon
    private Paint blockerPaint; // Paint used to draw the blocker
    private Paint targetPaint; // Paint used to draw the target
    private Paint backgroundPaint; // Paint used to clear the drawing area

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;
        getHolder().addCallback(this);

        blocker = new Line();
        target = new Line();
        cannonball = new Point();

        hitStates = new boolean[TARGET_PIECES];

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundMap = new HashMap<Integer, Integer>();
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        cannonPaint = new Paint();
        cannonballPaint = new Paint();
        blockerPaint = new Paint();
        targetPaint = new Paint();
        backgroundPaint = new Paint();
    }

    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false);
    }

    public void releaseResources() {
        soundPool.release(); // release all resources used by the SoundPool
        soundPool = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height)
    {
    } // end method surfaceChanged

    // called when surface is first created
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (!dialogIsDisplayed)
        {
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start(); // start the game loop thread
        } // end if
    } // end method surfaceCreated

    // called when the surface is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // ensure that thread terminates properly
        boolean retry = true;
        cannonThread.setRunning(false);

        while (retry)
        {
            try
            {
                cannonThread.join();
                retry = false;
            } // end try
            catch (InterruptedException e)
            {
            } // end catch
        } // end while
    } // end method surfaceDestroyed

    public void fireCannonball(MotionEvent event) {
        if (cannonballOnScreen)
            return;

        double angle = alignCannon(event);

        cannonball.x = cannonballRadius;
        cannonball.y = screenHeight / 2;

        cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));

        cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));
        cannonballOnScreen = true;
        ++shotsFired;

        soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
    }

    public double alignCannon(MotionEvent event) {
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());
        double centerMinusY = (screenHeight / 2 - touchPoint.y);
        double angle = 0;
        if (centerMinusY != 0)
            angle = Math.atan((double) touchPoint.x / centerMinusY);
        if (touchPoint.y > screenHeight / 2)
            angle += Math.PI;
        barrelEnd.x = (int) (cannonLength * Math.sin(angle));
        barrelEnd.y =
                (int) (-cannonLength * Math.cos(angle) + screenHeight / 2);
        return angle;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        cannonBaseRadius = h / 18;
        cannonLength = w / 8;
        cannonballRadius = w / 38;
        cannonballSpeed = w * 3 / 2;
        lineWidth = w / 24;
        blockerDistance = w * 5 / 8;
        blockerBeginning = h / 8;
        blockerEnd = h * 3 / 8;
        initialBlockerVelocity = h / 2;
        blocker.start = new Point(blockerDistance, blockerBeginning);
        blocker.end = new Point(blockerDistance, blockerEnd);

        targetDistance = w * 7 / 8;
        targetBeginning = h / 8;
        targetEnd = h * 7 / 8;
        pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
        initialTargetVelocity = -h / 4;
        target.start = new Point(targetDistance, targetBeginning);
        target.end = new Point(targetDistance, targetEnd);

        barrelEnd = new Point(cannonLength, h / 2);

        textPaint.setTextSize(w / 20);
        textPaint.setAntiAlias(true);
        cannonPaint.setStrokeWidth(lineWidth * 1.5f);
        blockerPaint.setStrokeWidth(lineWidth);
        targetPaint.setStrokeWidth(lineWidth);
        backgroundPaint.setColor(Color.WHITE);

        newGame();
    }

    public void newGame() {
        for (int i = 0; i < TARGET_PIECES; ++i)
            hitStates[i] = false;
        targetPiecesHit = 0;
        blockerVelocity = initialBlockerVelocity;
        targetVelocity = initialTargetVelocity;
        timeLeft = 60;
        cannonballOnScreen = false;
        shotsFired = 0;
        totalElapsedTime = 0.0;
        blocker.start.set(blockerDistance, blockerBeginning);
        blocker.end.set(blockerDistance, blockerEnd);
        target.start.set(targetDistance, targetBeginning);
        target.end.set(targetDistance, targetEnd);

        if (gameOver) {
            gameOver = false;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }
    }

    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0;
         if(cannonballOnScreen) {
             cannonball.x += interval * cannonballVelocityX;
             cannonball.y += interval * cannonballVelocityY;
             if (cannonball.x + cannonballRadius > blockerDistance &&
                     cannonball.x - cannonballRadius < blockerDistance &&
                     cannonball.y + cannonballRadius > blocker.start.y &&
                     cannonball.y - cannonballRadius < blocker.end.y) {
                 cannonballVelocityX *= -1;
                 timeLeft -= MISS_PENALTY;

                 soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
             }
             else if (cannonball.x + cannonballRadius > screenWidth || cannonball.x - cannonballRadius < 0)
                 cannonballOnScreen = false;
             else if (cannonball.y + cannonballRadius < 0)
                 cannonballOnScreen = false;
             else if (cannonball.x + cannonballRadius > targetDistance &&
                     cannonball.x - cannonballRadius < targetDistance &&
                     cannonball.y + cannonballRadius > target.start.y &&
                     cannonball.y - cannonballRadius < target.end.y) {
                 int section = (int) ((cannonball.y - target.start.y) / pieceLength);
                 if ((section >= 0 && section < TARGET_PIECES) && !hitStates[section]) {
                     hitStates[section] = true;
                     cannonballOnScreen = false;
                     timeLeft += HIT_REWARD;
                     soundPool.play(soundMap.get(TARGET_SOUND_ID), 1, 1, 1, 0, 1f);
                     if (++targetPiecesHit == TARGET_PIECES) {
                         cannonThread.setRunning(false);
                         showGameOverDialog(R.string.win);
                         gameOver = true;
                     }
                 }
             }
         }

         double blockerUpdate = interval * blockerVelocity;
         blocker.start.y += blockerUpdate;
         blocker.end.y += blockerUpdate;

         double targetUpdate = interval * targetVelocity;
         target.start.y += targetUpdate;
         target.end.y += targetUpdate;

         if (blocker.start.y < 0 || blocker.end.y > screenHeight)
             blockerVelocity *= -1;

         if (target.start.y < 0 || target.end.y > screenHeight)
             targetVelocity *= -1;

         timeLeft -= interval;

         if (timeLeft <= 0) {
             timeLeft = 0.0;
             gameOver = true;
             cannonThread.setRunning(false);
             showGameOverDialog(R.string.lose);
         }
    }

    public void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
                backgroundPaint);

        // display time remaining
        canvas.drawText(getResources().getString(
                R.string.time_remaining_format, timeLeft), 30, 50, textPaint);

        // if a cannonball is currently on the screen, draw it
        if (cannonballOnScreen)
            canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,
                    cannonballPaint);

        // draw the cannon barrel
        canvas.drawLine(0, screenHeight / 2, barrelEnd.x, barrelEnd.y,
                cannonPaint);

        // draw the cannon base
        canvas.drawCircle(0, (int) screenHeight / 2,
                (int) cannonBaseRadius, cannonPaint);

        // draw the blocker
        canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x,
                blocker.end.y, blockerPaint);

        Point currentPoint = new Point(); // start of current target section

        // initialize curPoint to the starting point of the target
        currentPoint.x = target.start.x;
        currentPoint.y = target.start.y;

        // draw the target
        for (int i = 1; i <= TARGET_PIECES; ++i)
        {
            // if this target piece is not hit, draw it
            if (!hitStates[i - 1])
            {
                // alternate coloring the pieces yellow and blue
                if (i % 2 == 0)
                    targetPaint.setColor(Color.YELLOW);
                else
                    targetPaint.setColor(Color.BLUE);

                canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
                        (int) (currentPoint.y + pieceLength), targetPaint);
            } // end if

            // move curPoint to the start of the next piece
            currentPoint.y += pieceLength;
        }
    }

    private void showGameOverDialog(int messageId)
    {
        // create a dialog displaying the given String
        final AlertDialog.Builder dialogBuilder =
                new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle(getResources().getString(messageId));
        dialogBuilder.setCancelable(false);

        // display number of shots fired and total time elapsed
        dialogBuilder.setMessage(getResources().getString(
                R.string.results_format, shotsFired, totalElapsedTime));
        dialogBuilder.setPositiveButton(R.string.reset_game,
                new DialogInterface.OnClickListener()
                {
                    // called when "Reset Game" Button is pressed
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialogIsDisplayed = false;
                        newGame(); // set up and start a new game
                    } // end method onClick
                } // end anonymous inner class
        ); // end call to setPositiveButton

        activity.runOnUiThread(
                new Runnable() {
                    public void run()
                    {
                        dialogIsDisplayed = true;
                        dialogBuilder.show(); // display the dialog
                    } // end method run
                } // end Runnable
        ); // end call to runOnUiThread
    } // end method showGameOverDialog

    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private boolean threadIsRunning = true;

        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("OldCannonThread");
        }

        public void setRunning(boolean running)
        {
            threadIsRunning = running;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();
            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        // atomic code
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS /  1000.0;
                        updatePositions(elapsedTimeMS);
                        drawGameElements(canvas);
                        previousFrameTime = currentTime;
                    }
                }
                finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

} // end CannonView

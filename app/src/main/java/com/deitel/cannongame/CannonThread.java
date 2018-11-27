package com.deitel.cannongame;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class CannonThread extends Thread {
    private SurfaceHolder surfaceHolder;
    private boolean threadIsRunning = true;

    public CannonThread(SurfaceHolder holder) {
        surfaceHolder = holder;
        setName("CannonThread");
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


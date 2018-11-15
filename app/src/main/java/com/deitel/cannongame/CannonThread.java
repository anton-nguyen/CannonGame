package com.deitel.cannongame;

import android.view.SurfaceHolder;

public class CannonThread extends Thread {
    private SurfaceHolder surfaceHolder;

    public CannonThread(SurfaceHolder holder) {
        surfaceHolder = holder;
        setName("CannonThread");
    }

    @Override
    public void run() {

    }
}


package com.jpl.lneat.lowfluxserver;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {


    private static final String LOGKEY = "MainActivity";
    private static final int PORT = 6000;
    private ImageView myDisplay;
    private BlockingQueue<Bitmap> myMessengerQueue;
    private SocketManager mySocketManager;
    private int xMax;
    private int yMax;
    private UIThread myUIThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDisplay = (ImageView)findViewById(R.id.imageView);
        hideSystemUI();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        xMax = size.x;
        yMax = size.y;
        logMes(xMax+"");
        logMes(yMax+"");


        myMessengerQueue = new LinkedBlockingQueue<>();
        mySocketManager  = new SocketManager(PORT, myMessengerQueue);
        myUIThread       = new UIThread(myMessengerQueue,myDisplay);
        mySocketManager.start();
        myUIThread.start();
    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }


    // Used to rase an error in the form of an android log
    protected void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }
    protected void logMes(String lMes) { Log.i(LOGKEY, lMes);}
}

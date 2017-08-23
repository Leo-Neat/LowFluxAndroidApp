package com.jpl.lneat.lowfluxserver;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;


/*
 *  Leo Neat
 *  Jet Propulsion Laboratory
 *  Division 38 Optics
 *  leo.s.neat@jpl.nasa.gov
 *
 *
 *  MainActivity.java - This class acts as a distributor for the rest of the android application.
 *  It kicks off the two different threads, and the linked blocking queues that are used to
 *  communicate between the threads. Once the Refrences are passed the job of this class is done.
 *  The most important code in this is the hideSystemUI function, which allows the screen to be set
 *  to full.
 */


public class MainActivity extends AppCompatActivity {

    // Global Constants
    private static final String LOGKEY  = "MainActivity";
    private static final int PORT       = 6000;

    // Global State Variables
    private ImageView myDisplay;
    private BlockingQueue<Bitmap> myMessengerQueue;             // Image communication link
    private BlockingQueue<Integer[]> myTimeQueue;               // Time communication link
    private SocketManager mySocketManager;                      // Socket Thread
    private UIThread myUIThread;                                // Display Thread
    private TimeDelayThread myTimeDelayThread;                  // Timing Thread
    private int xMax;
    private int yMax;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        setContentView(R.layout.activity_main);
        hideSystemUI();                                         //Turns full screen mode
        settingPermission();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        xMax                = size.x;
        yMax                = size.y;
        myDisplay           = (ImageView)findViewById(R.id.imageView);  // Links Java and XML screen
        myMessengerQueue    = new LinkedBlockingQueue<>();              // Display reference
        myTimeQueue         = new LinkedBlockingDeque<>();              // Time reference
        Integer[] empty     = new Integer[2];                           // Init for time reference
        empty[0]            = 0;
        empty[1]            = 0;
        mContext            = getApplicationContext();
        myTimeQueue.add(empty);
        // Create Threads, pass communication references, kick off threads
        myUIThread          = new UIThread(myMessengerQueue, myDisplay);
        myTimeDelayThread   = new TimeDelayThread(myTimeQueue, myDisplay);
        mySocketManager     = new SocketManager(PORT, myMessengerQueue, myTimeQueue, xMax, yMax, mContext);
        mySocketManager.start();
        myTimeDelayThread.start();
        myUIThread.start();
    }


    @Override
    protected void onResume() {
        // Make sure the system turns the app back to fullscreen after  it is paused
        // Ideally this should never be called
        super.onResume();
        hideSystemUI();
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


    // Debug Information
    // Used to raise an Error in the form of an android Log.
    private void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }


    public void settingPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);

            }
        }
    }


    // Used to print output in the form of an android Log.
    private void logMes(String lMes) {
        Log.i(LOGKEY, lMes);
    }


    // Closes a system message if it interupts the android application
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (! hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }
}

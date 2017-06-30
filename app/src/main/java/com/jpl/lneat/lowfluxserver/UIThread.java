package com.jpl.lneat.lowfluxserver;


import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import java.util.concurrent.BlockingQueue;


/*
 *  Leo Neat
 *  Jet Propulsion Laboratory
 *  Division 38 Optics
 *  leo.s.neat@jpl.nasa.gov
 *
 *
 *  UIThread.java - This thread is used to update the screen based upon the messages that are
 *  received by the socket manager thread. It sets the screen based upon a reference that was
 *  passed on the creation of this thread. The priority is set to max priority because it is the
 *  point of the application.
 */


public class UIThread extends Thread {

    // Global Constants
    private static final String LOGKEY = "UIThread";

    // Global State Variables
    private BlockingQueue<Bitmap> myMessengerQueue;
    private BlockingQueue<Integer[]> myTimeQueue;
    private ImageView   myDisplay;
    private Bitmap currentDisplay = null;
    private int waitTime;
    private int showTime;
    private long nextDisplayTime    = 0;
    private long oldtime            = 0;


    public UIThread(BlockingQueue myMQ, BlockingQueue time, ImageView myDisp){
        super("UIThread");
        logMes("UI Thread Starting up");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        myMessengerQueue    = myMQ;                 // Linking References
        myTimeQueue         = time;
        myDisplay           = myDisp;
        waitTime            = 0;
        showTime            = 0;
    }


    @Override
    public void run() {
        // This is where a majority of the code is located for this class.
        // The run method should last forever and check results on each iteration of the loop
        // If their is new results then it updates the display.

        super.run();
        while(true){
            if(!myTimeQueue.isEmpty() && myTimeQueue.element()[0] != waitTime ){
                waitTime = myTimeQueue.element()[0];
                logMes("Wait time Changed to: " + waitTime);    // Debug
            }
            if(!myMessengerQueue.isEmpty()){
                try {
                    currentDisplay = myMessengerQueue.take();
                } catch (InterruptedException e) {
                    raiseError("Error taking from blocking Queue: " + e.toString());
                }
                // Set the display to the newly received Bitmap
                final Bitmap tempDisplay = currentDisplay;
                myDisplay.post(new Runnable() {
                    @Override
                    public void run() {
                        myDisplay.setImageBitmap(tempDisplay);
                    }
                });
                // Sets the timing data
                 nextDisplayTime = System.currentTimeMillis() + waitTime + myTimeQueue.element()[1];
                logMes(" The next show time is: "+ nextDisplayTime);        // Debug info
            }else{
                if(System.currentTimeMillis() > nextDisplayTime){
                    oldtime = System.currentTimeMillis();
                    // Set the Display to be visible if the time permits
                    myDisplay.post(new Runnable() {
                        @Override
                        public void run() {
                            if(!myDisplay.isShown()) {
                                myDisplay.setVisibility(View.VISIBLE);
                            }
                            }
                    });
                    try {
                        currentThread().sleep(myTimeQueue.element()[1]);
                    } catch (InterruptedException e) {
                        raiseError("Thread sleep error: " + e.toString());
                    }
                    nextDisplayTime = System.currentTimeMillis() + waitTime;
                }else{
                    if(waitTime != 0) {
                        // Set the display to invisible if the time permits
                        myDisplay.post(new Runnable() {
                            @Override
                            public void run() {
                                if(myDisplay.isShown()) {
                                    myDisplay.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            }
        }
    }


    // Debug Information
    // Used to raise an Error in the form of an android Log.
    private void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }


    // Used to print output in the form of an android Log.
    private void logMes(String lMes) {
        Log.i(LOGKEY, lMes);
    }
}

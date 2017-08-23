package com.jpl.lneat.lowfluxserver;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.concurrent.BlockingQueue;

/**
 * Created by lneat on 8/22/2017.
 */

public class TimeDelayThread extends Thread {

    private String LOGKEY = "Time Delay";

    private BlockingQueue<Integer[]> myTimeQueue;
    private ImageView myDisplay;
    private long waitTime    = 0;
    private long showTime            = 0;

    public TimeDelayThread(BlockingQueue time, ImageView myDisp){
        super("UIThread");
        logMes("Time Delay Thread Starting up");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        myTimeQueue         = time;                             // Linking Refrence
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
        while (true) {
            if (!myTimeQueue.isEmpty()) {
                showTime = myTimeQueue.element()[0];
                waitTime = myTimeQueue.element()[1];
                myTimeQueue.clear();
            }
            if(waitTime != 0) {
                try {
                    myDisplay.post(new Runnable() {
                        @Override
                        public void run() {
                            myDisplay.setVisibility(View.INVISIBLE);
                        }
                    });
                    Thread.sleep(waitTime);
                    myDisplay.post(new Runnable() {
                        @Override
                        public void run() {
                            myDisplay.setVisibility(View.VISIBLE);
                        }
                    });
                    Thread.sleep(showTime);
                } catch (InterruptedException e) {
                    raiseError("Error in Time Thread: " + e.toString());
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

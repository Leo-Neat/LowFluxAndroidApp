package com.jpl.lneat.lowfluxserver;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.concurrent.BlockingQueue;

/**
 * Created by lneat on 6/26/2017.
 */

public class UIThread extends Thread {

    private static final String LOGKEY = "UIThread";
    BlockingQueue<Bitmap> myMessengerQueue;
    BlockingQueue<Integer[]> myTimeQueue;
    long oldtime = 0;
    ImageView   myDisplay;
    Bitmap currentDisplay = null;
    int waitTime;
    int showTime;
    long nextDisplayTime = 0;

    public UIThread(BlockingQueue myMQ, BlockingQueue time, ImageView myDisp){
        super("UIThread");
        logMes("UI Thread Starting up");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        myMessengerQueue= myMQ;
        myTimeQueue      = time;
        myDisplay   = myDisp;
        waitTime = 0;
        showTime = 0;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            if(!myTimeQueue.isEmpty() && myTimeQueue.element()[0] != waitTime ){
                waitTime = myTimeQueue.element()[0];
                logMes("Wait time Changed to: " + waitTime);
            }
            if(!myMessengerQueue.isEmpty()){

                try {
                    logMes("Here");
                    currentDisplay = myMessengerQueue.take();
                } catch (InterruptedException e) {
                    raiseError("Error taking from blocking Queue: " + e.toString());
                }
                final Bitmap tempDisplay = currentDisplay;
                myDisplay.post(new Runnable() {
                    @Override
                    public void run() {
                        myDisplay.setImageBitmap(tempDisplay);
                    }
                });
                 nextDisplayTime = System.currentTimeMillis() + waitTime + myTimeQueue.element()[1];
                logMes(" The next show time is: "+ nextDisplayTime);
            }else{
                if(System.currentTimeMillis() > nextDisplayTime){
                    //logMes("Time diffrence: " + ( System.currentTimeMillis() - oldtime ));
                    oldtime = System.currentTimeMillis();
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



    // Used to rase an error in the form of an android log
    protected void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }
    protected void logMes(String lMes) { Log.i(LOGKEY, lMes);}
}

package com.jpl.lneat.lowfluxserver;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.widget.ImageView;

import java.util.concurrent.BlockingQueue;

/**
 * Created by lneat on 6/26/2017.
 */

public class UIThread extends Thread {

    private static final String LOGKEY = "UIThread";
    BlockingQueue<Bitmap> myMessengerQueue;
    ImageView   myDisplay;


    public UIThread(BlockingQueue myMQ, ImageView myDisp){
        super("UIThread");
        logMes("UI Thread Starting up");
        myMessengerQueue= myMQ;
        myDisplay   = myDisp;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            if(!myMessengerQueue.isEmpty()){
                Bitmap nextDisplay = null;
                try {
                    logMes("Here");
                    nextDisplay = myMessengerQueue.take();
                } catch (InterruptedException e) {
                    raiseError("Error taking from blocking Queue: " + e.toString());
                }
                final Bitmap tempDisplay = nextDisplay;
                myDisplay.post(new Runnable() {
                    @Override
                    public void run() {
                        myDisplay.setImageBitmap(tempDisplay);
                    }
                });

            }
        }
    }

    // Used to rase an error in the form of an android log
    protected void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }
    protected void logMes(String lMes) { Log.i(LOGKEY, lMes);}
}

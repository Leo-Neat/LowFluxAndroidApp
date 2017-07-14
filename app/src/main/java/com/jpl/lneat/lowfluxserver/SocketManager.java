package com.jpl.lneat.lowfluxserver;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Base64;
import android.util.Log;
import android.view.Display;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;


/*
 *  Leo Neat
 *  Jet Propulsion Laboratory
 *  Division 38 Optics
 *  leo.s.neat@jpl.nasa.gov
 *
 *  SocketManager.java - This class acts as the communication line between the android server and the
 *  computer client. It uses a standard TCP socket to communicate and does so with specially
 *  formatted strings. The communication would be significantly faster with a byte array but I am
 *  still working on implementing it. This class uses specific opt codes that allow the two sides
 *  to determine which information should be relayed at what time.
 *
 *  NOTE: abd port forwarding must be done before this class is able to communicate with a its
 *  USB host.
 */


public class SocketManager extends Thread{

    // Opt Codes
    private final int NEWDATA       = 1;
    private final int SUCCESS       = 2;
    private final int DISCONNECT    = 3;
    private final int SETTIME       = 4;
    private final int GETSCREENDIM  = 5;

    // Global Constants
    private static final String LOGKEY = "Socket Manager: ";
    private int PORT;
    private int screenWidth;
    private int screenHeight;
    private static final String IP = "127.0.0.1";

    // Global State variables
    private BlockingQueue<Bitmap> myMessengerQueue;
    private BlockingQueue<Integer[]> myTimeQueue;
    private Socket sock;


    public SocketManager(int port, BlockingQueue mMMQ, BlockingQueue time, int sW, int sH){
        // This function is used to link the references, allowing for communication between threads.
        super("SocketManager");
        PORT                = port;
        myMessengerQueue    = mMMQ;
        myTimeQueue         = time;
        screenWidth         = sW;
        screenHeight        = sH;
    }


    private void serverSetup(){
        // This function sets up the socket that polls for a user to connect over a USB connection.
        // It is a blocking function so it will not continue until a connection is recived

        // Debug Data
        logMes("Attempting to connect Socket From");
        logMes("Port: " + PORT);
        logMes("IP: " + IP);

        ServerSocket myServerSocket = null;
        try {
            myServerSocket = new ServerSocket(PORT);                        // Bind Socket to port
        } catch (IOException e) {
            raiseError("Error Binding Server Socket: " + e.toString());
        }
        try {
            sock = myServerSocket.accept();                                 // Blocking function
        } catch (IOException e) {
            raiseError("Error Accepting the Client Socket: "+e.toString());
        }
        logMes("Connection Created");
    }


    private int checkOpt(){
        // This function is called when ever a process is finished.
        // It returns an integer that the user has specified. Each Integer corresponds to a different
        // process that needs to be completed

        String message = "";
        try {
            InputStream is          = sock.getInputStream();
            InputStreamReader isr   = new InputStreamReader(is);
            BufferedReader br       = new BufferedReader(isr);
            message = br.readLine();
        } catch (IOException e) {
            raiseError("Error reading Message in CheckOpt: " + e.toString());
        }
        try {
            logMes(message);
        }catch(Exception e){
            raiseError("Python Client has failed, Resetting server ...");
            run();
        }
        return Integer.parseInt(message);
    }

    private void sendConformation(){
        // This function is called to send the user a signal that it has successful received the
        // Message that the user has sent

        try {
            OutputStream out;
            DataOutputStream dos;
            out     = sock.getOutputStream();
            dos     = new DataOutputStream(out);
            dos.write(SUCCESS);
            dos.flush();
        } catch (IOException e) {
            raiseError("Error in send Conformation: "+ e.toString());
        }
    }



    // Expects width and height
    // Expects a line of numbers following those dimensions
    private Bitmap getImage(){
        // This is where a majorty of the thread time is spent. The function is able to receive an
        // Image in the form of a string, and then pass that image as a bitmap to the UI Thread.
        // It uses JSON formatting in order to communicate an array across languages.

        String message;
        int width;
        int height;
        Bitmap ret = null;
        try {
            InputStream is          = sock.getInputStream();
            InputStreamReader isr   = new InputStreamReader(is);
            BufferedReader br       = new BufferedReader(isr);
            message                 = br.readLine();
            logMes("Message Data Received:");
            logMes(message);
            sendConformation();
            String[] parts  = message.split(":");           // Receive the dimensions of the image
            width           = Integer.parseInt(parts[0]);
            height          = Integer.parseInt(parts[1]);
            ret             =  Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            JSONObject jobj;
            message    = br.readLine();
            logMes("Message Received");                     // Debug
            // New Base64 Method, More Resource efficent
            byte[] temp = Base64.decode(message,Base64.DEFAULT);
            ret = BitmapFactory.decodeByteArray(temp, 0, temp.length);
            logMes(ret.getWidth()+ " " + ret.getHeight());
            /*
            try {                                           // Read the JSON object as a list
                jobj = new JSONObject(message);
                JSONArray j1= jobj.getJSONArray("list");
                JSONArray j2;
                for(int x = 0; x < width; x++){
                    j2 = j1.getJSONArray(x);
                    for(int y = 0; y< height; y++){
                        ret.setPixel(x,y,Color.argb(255,0,j2.getInt(y),0)); // Set Bitmap to JSON
                    }
                }
                logMes("Message Sent To Array");            // Debug
            } catch (JSONException e) {
                raiseError("Json Error: " +  e.toString());
            }*/
        } catch (IOException e) {
            raiseError("Error in recive Image: " + e.toString());
        }
        return ret;
    }


    public Integer[] getWaitTime(){
        // This function is used to recive the wait time for the image. It is passed in the form
        // of a specially formatted string from the computer client.

        Integer[] res = new Integer[2];
        try {
            InputStream is          = sock.getInputStream();
            InputStreamReader isr   = new InputStreamReader(is);
            BufferedReader br       = new BufferedReader(isr);
            String strWait = null;
            strWait = br.readLine();
            String[] temp = strWait.split(":");                 // Read formatted String
            res[0] = Integer.parseInt(temp[0]);
            res[1] = Integer.parseInt(temp[1]);
            return res;
        } catch (IOException e) {
            raiseError("Error in Get Wait time: " + e.toString());
        }
        res[0] = 0;                                             // Set time to zero
        res[1] = 0;
        return res;
    }

    private void sendScreenDim(){
        try {
            OutputStream out;
            DataOutputStream dos;
            out     = sock.getOutputStream();
            dos     = new DataOutputStream(out);
            dos.writeUTF(screenWidth+","+screenHeight);
            //dos.write(screenHeight);
            dos.flush();
        } catch (IOException e) {
            raiseError("Error in send ScreenDim: "+ e.toString());
        }
    }



    @Override
    public void run() {
        // This is the body of the thread and is theoretic infinite. It should not stop running
        // because it is tail recurive. The function is used to read the opt codes and call the
        // necessary functions.

        super.run();
        serverSetup();
        while(sock.isConnected()){
            int opt = checkOpt();
            if(opt == NEWDATA){
                sendConformation();
                Bitmap next = getImage();                       // Get Sent bitmap
                myMessengerQueue.add(next);                     // Send it to UIThread
                logMes("Message Sent to UITHREAD");             // Debug
            }
            else if(opt == SUCCESS){
                continue;
            }
            else if(opt == DISCONNECT){
                try {                                           // Look for new clients to connect
                    sock.close();
                } catch (IOException e) {
                    raiseError("Error Closing Socket: " + e.toString());
                }
                break;
            }
            else if(opt == GETSCREENDIM){
                    sendScreenDim();
            }
            else if(opt == SETTIME){
                sendConformation();
                Integer[] waittime = getWaitTime();             // Set a new delay time
                logMes("Wait time: "+ waittime[0]+ ' ' + waittime[1]);
                myTimeQueue.clear();                            // Send the new time to the UIThread
                myTimeQueue.add(waittime);
            }
            else{
                raiseError("Error recived invalid opt code...");
            }
        }
        if(sock.isConnected()){
            try {
                sock.close();
            } catch (IOException e) {
                raiseError("Error in end of thread RUN: " + e.toString());
            }
        }
        run();                              // Tail recursion
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

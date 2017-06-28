package com.jpl.lneat.lowfluxserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lneat on 6/26/2017.
 */

public class SocketManager extends Thread{


    private final int NEWDATA = 1;
    private final int SUCCESS = 2;
    private final int DISCONNECT = 3;
    private final int SETTIME = 4;


    private static final String LOGKEY = "Socket Manager: ";
    private BlockingQueue<Bitmap> myMessengerQueue;
    private BlockingQueue<Integer[]> myTimeQueue;
    private int PORT;
    private String IP = "127.0.0.1";
    private Socket sock;

    public SocketManager(int port, BlockingQueue mMMQ, BlockingQueue time){
        super("SocketManager");
        PORT = port;
        myMessengerQueue = mMMQ;
        myTimeQueue = time;
    }

    private void serverSetup(){
        logMes("Attempting to connect Socket From");
        logMes("Port: " + PORT);
        logMes("IP: " + IP);
        ServerSocket myServerSocket = null;
        try {
            myServerSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            raiseError("Error Binding Server Socket: " + e.toString());
        }
        try {
            sock = myServerSocket.accept();
        } catch (IOException e) {
            raiseError("Error Accepting the Client Socket: "+e.toString());
        }
        logMes("Connection Created");
    }


    private int checkOpt(){
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
        try {
            OutputStream out;
            DataOutputStream dos;
            out = sock.getOutputStream();
            dos    = new DataOutputStream(out);
            dos.write(SUCCESS);
            dos.flush();
        } catch (IOException e) {
            raiseError("Error in send Conformation: "+ e.toString());
        }
    }



    // Expects width and height
    // Expects a line of numbers following those dimensions
    private Bitmap getImage(){
        String message = "";
        int width = 0;
        int height = 0;
        Bitmap ret = null;
        try {
            InputStream is          = sock.getInputStream();
            InputStreamReader isr   = new InputStreamReader(is);
            BufferedReader br       = new BufferedReader(isr);
            message    = br.readLine();
            logMes("Message Recived:");
            logMes(message);
            sendConformation();

            String[] parts = message.split(":");
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
            ret =  Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            JSONObject jobj;
            message    = br.readLine();
            try {
                jobj = new JSONObject(message);
                JSONArray j1= jobj.getJSONArray("list");
                JSONArray j2;
                for(int x = 0; x < width; x++){
                    j2 = j1.getJSONArray(x);
                    for(int y = 0; y< height; y++){
                        ret.setPixel(x,y,Color.argb(255,0,j2.getInt(y),0));
                    }
                }

            } catch (JSONException e) {
                raiseError("Json Error: " +  e.toString());
            }





        } catch (IOException e) {
            raiseError("Error in recive Image: " + e.toString());
        }

        /*
        int counter = 0;
        Bitmap nextScreen = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        logMes(parts2.length+"");
        for(int x = 0; x < width; x++){
            for(int y = 0; y <height; y++){
                nextScreen.setPixel(x,y, Color.argb(255, 0, Integer.parseInt(parts2[counter]), 0));
                counter ++;
            }
        }
        */
        return ret;



    }


    public Integer[] getWaitTime(){
        Integer[] res = new Integer[2];
        try {
            InputStream is          = sock.getInputStream();
            InputStreamReader isr   = new InputStreamReader(is);
            BufferedReader br       = new BufferedReader(isr);
            String strWait = null;
            strWait = br.readLine();
            String[] temp = strWait.split(":");
            res[0] = Integer.parseInt(temp[0]);
            res[1] = Integer.parseInt(temp[1]);
            return res;
        } catch (IOException e) {
            raiseError("Error in Get Wait time: " + e.toString());
        }
        res[0] = 0;
        res[1] = 0;
        return res;
    }

    @Override
    public void run() {
        super.run();
        serverSetup();
        while(sock.isConnected()){
            int opt = checkOpt();
            if(opt == NEWDATA){
                sendConformation();
                Bitmap next = getImage();
                myMessengerQueue.add(next);
            }
            else if(opt == SUCCESS){
                continue;
            }
            else if(opt == DISCONNECT){
                try {
                    sock.close();
                } catch (IOException e) {
                    raiseError("Error Closing Socket: " + e.toString());
                }
                break;
            }
            else if(opt == SETTIME){
                sendConformation();
                Integer[] waittime = getWaitTime();
                logMes("Wait time: "+ waittime[0]+ ' ' + waittime[1]);
                myTimeQueue.clear();
                myTimeQueue.add(waittime);
            }
            else{
                raiseError("Error recived invalid opt code...");
            }

        }
        run();

    }

    // Used to rase an error in the form of an android log
    protected void raiseError(String eMessage){
        Log.e(LOGKEY, eMessage);
    }
    protected void logMes(String lMes) { Log.i(LOGKEY, lMes);};
}

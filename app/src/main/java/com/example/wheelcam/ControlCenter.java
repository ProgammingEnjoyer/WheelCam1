package com.example.wheelcam;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class ControlCenter {
    private static final String TAG = "ControlCenter";
    private static ControlCenter instance;
    private static Context context;

    private String btnClicked = "";
    private int moveExtent =0; // range of 1-4
    private static int panDegree = 135; // degree
    private static int tiltDegree = 135; //degree
    private static int rotateDegree = 0; //degree
    final static private int maxPan = 195;
    final static private int minPan = 75;
    final static private int maxTilt = 225;
    final static private int minTilt = 95;
    final static private int maxRot = 90;
    private boolean maxPanFlag = false;
    private boolean maxTiltFlag = false;

    final static private int MidPan = 135;
    final static private int MidTilt = 135;
    final static private int MidRot = 0;

    private int motor = 1; // 1 = pan, 2 = tilt, 3 = rotate
    private int degree = 0;

    private boolean isPortrait = true;


    final static Handler delayhandler = new Handler();

    //A = tilt145, pan145

    private ControlCenter(){}

    public static ControlCenter getInstance(Context c){ //making ControlCenter a singleton
        if (instance == null){
            context = c;
            instance = new ControlCenter();
        }
        return instance;
    }

    public void setBtnClicked(String btn){btnClicked = btn;}
    public void setMoveExtent(int extent){
        moveExtent =extent;
        extentToDegree();
    }
    public static void setCenter(){
        sendSignal_specific("#CENTER");
        panDegree = MidPan;
        tiltDegree = MidTilt;
        rotateDegree = MidRot;
    }

    public static void setGridA(){
        panDegree = angleChange(panDegree, 10, maxPan, minPan);
        tiltDegree = angleChange(tiltDegree, 13, maxTilt, minTilt);
        sendSignal_specific("#PAN: " + panDegree);
        delayhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSignal_specific("#TILT: " + tiltDegree);
            }
        }, 1500);
    }
    public static void setGridB(){
        //panDegree = 135;
        tiltDegree = angleChange(tiltDegree, 13, maxTilt, minTilt);
        sendSignal_specific("#TILT: " + tiltDegree);
//        sendSignal_specific("#PAN: " + panDegree);
//        delayhandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendSignal_specific("#TILT: " + tiltDegree);
//            }
//        }, 1500);
    }

    public static void setGridC(){
        panDegree = angleChange(panDegree, -10, maxPan, minPan);
        tiltDegree = angleChange(tiltDegree, 13, maxTilt, minTilt);
        sendSignal_specific("#PAN: " + panDegree);
        delayhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSignal_specific("#TILT: " + tiltDegree);
            }
        }, 1500);
    }

    public static void setGridD(){
        panDegree = angleChange(panDegree, 13, maxPan, minPan);
        //tiltDegree = 135;
        sendSignal_specific("#PAN: " + panDegree);
//        delayhandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendSignal_specific("#TILT: " + tiltDegree);
//            }
//        }, 1500);
    }
    public static void setGridF(){
        panDegree = angleChange(panDegree, -10, maxPan, minPan);
//        tiltDegree = 135;
        sendSignal_specific("#PAN: " + panDegree);
//        delayhandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendSignal_specific("#TILT: " + tiltDegree);
//            }
//        }, 1500);
    }
    public static void setGridG(){
        panDegree = angleChange(panDegree, 12, maxPan, minPan);
        tiltDegree = angleChange(tiltDegree, -14, maxTilt, minTilt);
        sendSignal_specific("#PAN: " + panDegree);
        delayhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSignal_specific("#TILT: " + tiltDegree);
            }
        }, 1500);
    }

    public static void setGridH(){
        panDegree = angleChange(panDegree, 2, maxPan, minPan);
        tiltDegree = angleChange(tiltDegree, -14, maxTilt, minTilt);
        sendSignal_specific("#PAN: " + panDegree);
        delayhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSignal_specific("#TILT: " + tiltDegree);
            }
        }, 1500);
    }

    public static void setGridI(){
        panDegree = angleChange(panDegree, -8, maxPan, minPan);
        tiltDegree = angleChange(tiltDegree, -14, maxTilt, minTilt);
        sendSignal_specific("#PAN: " + panDegree);
        delayhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSignal_specific("#TILT: " + tiltDegree);
            }
        }, 1500);
    }

    public void setIsPortrait(boolean portrait){isPortrait = portrait;}

    public void sendSignal_basic(){
        String msg = "#" + btnClicked + ": " + ((btnClicked == "ROTATE")? ((moveExtent == 1)? "clockwise": "anticlockwise"): moveExtent);
        Toast.makeText(context.getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
        if (MainActivity.btOutputStream != null) {
            try {
                MainActivity.btOutputStream.write((msg+"\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendSignal_general(){ //send the form #a=010,b=020,c=180;
        String msg = "#a=" + intTo3dp(panDegree) + ",b=" + intTo3dp(tiltDegree) + ",c=" + intTo3dp(rotateDegree) +";";
        Toast.makeText(context.getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
        if (MainActivity.btOutputStream != null) {
            try {
                MainActivity.btOutputStream.write((msg+"\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendSignal_specific(String msg){ // send #PAN: 180 OR #TILT: 270 OR #ROTATE: 90
        Toast.makeText(context.getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
        if (MainActivity.btOutputStream != null) {
            try {
                MainActivity.btOutputStream.write((msg+"\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void extentToDegree(){
        switch (btnClicked){
            case "RIGHT":
                switch (moveExtent) {
                    case 1:
                        panDegree = angleChange(panDegree, -5, maxPan, minPan);
                        break;
                    case 2:
                        panDegree = angleChange(panDegree, -15, maxPan, minPan);
                        break;
                    case 3:
                        panDegree = angleChange(panDegree, -30, maxPan, minPan);
                        break;
                    case 4:
                        panDegree = angleChange(panDegree, -40, maxPan, minPan);
                        break;
                }
                Log.d(TAG,(panDegree == maxPan)? "Max Reach" : ("#PAN: " + panDegree));
                sendSignal_specific("#PAN: " + panDegree);
                break;
            case "LEFT":
                switch (moveExtent) {
                    case 1:
                        panDegree = angleChange(panDegree, 5, maxPan, minPan);
                        break;
                    case 2:
                        panDegree = angleChange(panDegree, 15, maxPan, minPan);
                        break;
                    case 3:
                        panDegree = angleChange(panDegree, 30, maxPan, minPan);
                        break;
                    case 4:
                        panDegree = angleChange(panDegree, 40, maxPan, minPan);
                        break;
                }
                Log.d(TAG,(panDegree == maxPan)? "Max Reach" : ("#PAN: " + panDegree));
                sendSignal_specific("#PAN: " + panDegree);
                break;
            case "UP":
                switch (moveExtent) {
                    case 1:
                        tiltDegree = angleChange(tiltDegree, 5, maxTilt, minTilt);
                        break;
                    case 2:
                        tiltDegree = angleChange(tiltDegree, 15, maxTilt, minTilt);
                        break;
                    case 3:
                        tiltDegree = angleChange(tiltDegree, 30, maxTilt, minTilt);
                        break;
                    case 4:
                        tiltDegree = angleChange(tiltDegree, 40, maxTilt, minTilt);
                        break;
                }
                Log.d(TAG,(tiltDegree == maxTilt)? "Max Reach" : ("#TILT: " + tiltDegree));
                sendSignal_specific("#TILT: " + tiltDegree);
                break;
            case "DOWN":
                switch (moveExtent) {
                    case 1:
                        tiltDegree = angleChange(tiltDegree, -5, maxTilt, minTilt);
                        break;
                    case 2:
                        tiltDegree = angleChange(tiltDegree, -15, maxTilt, minTilt);
                        break;
                    case 3:
                        tiltDegree = angleChange(tiltDegree, -30, maxTilt, minTilt);
                        break;
                    case 4:
                        tiltDegree = angleChange(tiltDegree, -40, maxTilt, minTilt);
                        break;
                }
                Log.d(TAG,(tiltDegree == maxTilt)? "Max Reach" : ("#TILT: " + tiltDegree));
                sendSignal_specific("#TILT: " + tiltDegree);
                break;
            case "ROTATE":
                switch (moveExtent) {
                    case 1:
                        rotateDegree = 0;
                        break;
                    case 2:
                        rotateDegree = 90;
                        break;
                }
                Log.d(TAG,"#ROTATE: " + rotateDegree);
                sendSignal_specific("#ROTATE: " + rotateDegree);
                break;
        }
    }

    private String intTo3dp(int num){// integer converted to 3 d.p.
        int length = String.valueOf(num).length();
        if (length == 1){
            return "00"+ String.valueOf(num);
        }else if (length == 2){
            return "0"+ String.valueOf(num);
        }else if (length == 3){
            return String.valueOf(num);
        }
        return "0";
    }

    private static int angleChange (int current, int change, int maximum, int minimum){
        int tmp = current + change;
        if (tmp > maximum){
            return maximum;
        }else if (tmp < minimum){
            return minimum;
        }
        return tmp;
    }


//    private String motorType(){
//        switch ()
//    }
    //mapping the button to the


}

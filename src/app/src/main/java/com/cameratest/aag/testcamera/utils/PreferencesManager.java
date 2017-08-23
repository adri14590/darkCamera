package com.cameratest.aag.testcamera.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Adrian on 21/08/2017.
 */

public class PreferencesManager {

    private static String SHOW_ACTION_BAR_KEY = "show_actionBar";
    private static String SERVER_PORT_KEY = "server_port";
    private static String RTSP_CAMERA_ADDRESS = "rtsp_camera_address";
    private static String ALPHAVIDEO_ACCURACY_KEY= "alphaVideo_accuracy";
    private static String ALPHAVIDEO_ALPHACOLOR_KEY = "alphaVideo_alphaColor";

    public static boolean getShowActionBar(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SHOW_ACTION_BAR_KEY, true);
    }

    public static int getServerPort(Context context) {
        int serverPort = 5000;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String serverPort_s = prefs.getString(SERVER_PORT_KEY, "5000");
            serverPort = Integer.parseInt(serverPort_s);
        }
        catch(Exception e) {}

        return serverPort;
    }

    public static String getRTSPCameraAddress(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(RTSP_CAMERA_ADDRESS, "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_175k.mov");
    }

    public static float getAlphaVideoAccuracy(Context context){
        float alphaVideoAccuracy = 0.92F;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String alphaVideoAccuracy_s = prefs.getString(ALPHAVIDEO_ACCURACY_KEY, "0.92F");
            alphaVideoAccuracy = Float.parseFloat(alphaVideoAccuracy_s);
        }
        catch(Exception e) {}

        return alphaVideoAccuracy;
    }

    public static int getAlphaVideoAlphaColor(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(ALPHAVIDEO_ALPHACOLOR_KEY, 0xff1e852a);
    }
}
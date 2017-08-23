package com.cameratest.aag.testcamera.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alphamovie.lib.AlphaMovieView;
import com.cameratest.aag.testcamera.R;
import com.cameratest.aag.testcamera.utils.Constants;
import com.cameratest.aag.testcamera.utils.MediaUtils;
import com.cameratest.aag.testcamera.utils.PreferencesManager;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends AppCompatActivity {
    private static int PLAY_PERIOD_SECONDS = 30;

    private Camera mCamera;
    private CameraPreview mPreview;
    private AlphaMovieView mVideoView;
    private boolean mVideoPlaying;

    //private long mLastPlayed;
    boolean mIsLandscape;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int MY_PERMISSIONS_REQUEST_SOUND = 1;
    private static final int MY_PERMISSIONS_REQUEST_EXTERNALSTORAGE = 2;

    boolean m_acceptCamera = false;
    boolean m_acceptSound = false;
    boolean m_acceptExternalStorage = false;

    private SensorManager mSensorManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*** Layout Setting ***/
        int alphaColor = PreferencesManager.getAlphaVideoAlphaColor(getApplicationContext());
        float accuracy = PreferencesManager.getAlphaVideoAccuracy(getApplicationContext());
        mVideoView = new AlphaMovieView(this, alphaColor, accuracy);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, 0);
        mVideoView.setVisibility(View.INVISIBLE);

        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        mVideoView.setLayoutParams(lp);
        RelativeLayout rL = (RelativeLayout) findViewById(R.id.relativeLayout);
        rL.addView(mVideoView);

        /*** Set RTSP ***/
        if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
            return;

        /*** Server HTTP ***/
        int serverPort = PreferencesManager.getServerPort(getApplicationContext());
        setAndStartServer(serverPort);

        /*** Start Sensor Manager ***/
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        //mLastPlayed = System.currentTimeMillis();

        /*** Check permissions ***/
        m_acceptCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        m_acceptSound = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        m_acceptExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        checkInitApp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferencesIntent = new Intent(this, SettingsActivity.class);
                startActivity(preferencesIntent);
                // User chose the "Settings" item, show the app settings UI...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void checkInitApp() {
        if(m_acceptCamera && m_acceptSound && m_acceptExternalStorage) {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            // Add a listener to the Capture button
            Button captureButton = (Button) findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
            );

            //VideoView rtspVideo = (VideoView) findViewById(R.id.camera_preview);

            /*
             * Alternatively,for streaming media you can use
             * mVideoView.setVideoURI(Uri.parse(URLstring));
             */
            //rtspVideo.setVideoPath("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_175k.mov");
            //rtspVideo.requestFocus();
//            rtspVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                @Override
//                public void onPrepared(MediaPlayer mediaPlayer) {
//                    // optional need Vitamio 4.0
//                    mediaPlayer.setPlaybackSpeed(1.0f);
//                }
//            });

            // Add a listener to the Capture button
            Button captureVideoButton = (Button) findViewById(R.id.button_capture_video);
            captureVideoButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(!mVideoPlaying) {
                                mVideoPlaying = true;
                                mVideoView.setVisibility(View.VISIBLE);
                                mVideoView.setVideoFromAssets("video3.mp4");
                                mVideoView.setLooping(false);
                            }
                        }
                    }
            );

            //mVideoView = (AlphaMovieView) findViewById(R.id.videoView);

            mVideoView.setAlpha(0.5F);
            mVideoPlaying = false;

            mVideoView.setOnVideoStartedListener(new AlphaMovieView.OnVideoStartedListener() {
                @Override
                public void onVideoStarted() {
                    mVideoPlaying = true;
                }
            });

            mVideoView.setOnVideoEndedListener(new AlphaMovieView.OnVideoEndedListener() {
                @Override
                public void onVideoEnded() {
                    mVideoView.setVisibility(View.INVISIBLE);
                    mVideoPlaying = false;
                }
            });
        }
        else if(!m_acceptCamera){
            requestPermissions(Manifest.permission.CAMERA, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else if(!m_acceptSound) {
            requestPermissions(Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_REQUEST_SOUND);
        }
        else if(!m_acceptExternalStorage) {
            requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_EXTERNALSTORAGE);
        }
        else {
            Toast.makeText(this, "La aplicación no funcionará sin estos permisos", Toast.LENGTH_SHORT).show();
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d(Constants.TAG, "Error accesing camera: " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    /*** Callbacks and Listeners ***/
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = MediaUtils.getOutputMediaFile(MediaUtils.MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(Constants.TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(Constants.TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(Constants.TAG, "Error accessing file: " + e.getMessage());
            }
            mCamera.startPreview();
        }
    };

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float X_Axis = sensorEvent.values[0];
            float Y_Axis = sensorEvent.values[1];

            if((X_Axis >= 8 && X_Axis <= 11) && Y_Axis >= -6 && Y_Axis <= 6){
                mIsLandscape = true;
            }
            else {
                mIsLandscape = false;
            }

            //long timeNow = System.currentTimeMillis();
            //long timeOffset = (timeNow - mLastPlayed) / 1000;

//            if(!mVideoPlaying && ((isLandscape && (timeOffset >= PLAY_PERIOD_SECONDS)) || mToPlay == true)) {
//                mLastPlayed = System.currentTimeMillis();
//                mToPlay = false;
//                mVideoPlaying = true;
//                mVideoView.setVisibility(View.VISIBLE);
//                mVideoView.setVideoFromAssets("video3.mp4");
//                mVideoView.setLooping(false);
//            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void setAndStartServer(int port) {
        AsyncHttpServer server = new AsyncHttpServer();

        List<WebSocket> _sockets = new ArrayList<WebSocket>();

        server.post("/video", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                if(!mVideoPlaying && mIsLandscape) {
                    final UrlEncodedFormBody requestBody = (UrlEncodedFormBody) request.getBody();
                    final String videoName = requestBody.get().getString("video");
                    boolean videoExists = false;
                    try {
                        videoExists = Arrays.asList(getResources().getAssets().list("")).contains(videoName);
                    } catch (IOException e) {}

                    if(videoExists) {
                        mVideoPlaying = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mVideoView.setVisibility(View.VISIBLE);
                                mVideoView.setVideoFromAssets(videoName);
                                mVideoView.setLooping(false);
                            }
                        });

                        response.code(200);
                        response.send("Played");
                    }
                    else {
                        response.code(404);
                        response.send("Video Not Exists");
                    }
                }
                response.code(400);
                response.send("Not Played");
            }
        });

        // listen on port 5000
        server.listen(port);
    }

    /*** Permissions ***/
    private void requestPermissions(String permission, int requestCode) {
        if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.

                m_acceptCamera = (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED);

                if(m_acceptCamera) {
                    checkInitApp();
                }
                else {
                    Toast.makeText(this, "La aplicación no funcionará sin estos permisos", Toast.LENGTH_SHORT).show();
                }

                return;
            }
            case MY_PERMISSIONS_REQUEST_SOUND: {
                // If request is cancelled, the result arrays are empty.

                m_acceptSound = (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);

                if(m_acceptSound) {
                    checkInitApp();
                }
                else {
                    Toast.makeText(this, "La aplicación no funcionará sin estos permisos", Toast.LENGTH_SHORT).show();
                }

                return;
            }
            case MY_PERMISSIONS_REQUEST_EXTERNALSTORAGE: {
                // If request is cancelled, the result arrays are empty.

                m_acceptExternalStorage = (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);

                if(m_acceptExternalStorage) {
                    checkInitApp();
                }
                else {
                    Toast.makeText(this, "La aplicación no funcionará sin estos permisos", Toast.LENGTH_SHORT).show();
                }

                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //mVideoView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //mVideoView.onPause();
    }
}

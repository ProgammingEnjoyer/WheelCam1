package com.example.wheelcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;


import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Button;
import java.util.ArrayList;import android.view.animation.AnimationUtils;


public class MainActivity extends AppCompatActivity {
    private boolean isRotating = false;

    private void completeRotation() {
        Log.d(TAG, "Exiting rotation mode");
        // reset isrotating
        isRotating = false;

        //highlightHandler.removeCallbacks(highlightRunnable);

        //initialiseHighlightableButtons();


        // reset currentButtonIndex
        currentButtonIndex = 0;
        //highlightButton(currentButtonIndex);
        //highlightHandler.postDelayed(highlightRunnable, 3000);
    }

    /*private Runnable highlightRunnable = new Runnable() {
        @Override
        public void run() {
            // check if rotating
            Log.d(TAG, "Running highlightRunnable, currentButtonIndex: " + currentButtonIndex);
            highlightButton(currentButtonIndex);
            if (isRotating) {
                currentButtonIndex = (currentButtonIndex + 1) % 2; //rotate mode
            }
            else{
                currentButtonIndex = (currentButtonIndex + 1) % highlightableButtons.size();//not rotating
            }
            Log.d(TAG, "New currentButtonIndex: " + currentButtonIndex);
            highlightHandler.postDelayed(this, 3000);
        }
    };*/

    //private ArrayList<View> highlightableButtons;
    //private Handler highlightHandler = new Handler();
    private int currentButtonIndex = 0;
    private CameraControl cameraControl;
    private VideoCapture videoCapture;
    private boolean isRecording = false;
    final private String TAG = "MainActivity";
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int PERMISSION_REQUEST_CODE = 200;
    // for bluetooth
    BluetoothDevice selectedBTDevice = null;
    BluetoothDevice  connectedBTDevice = null;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    public static OutputStream btOutputStream;
    public static InputStream btInputStream;
    boolean isBtConnected = false;
    boolean isFlashedEnabled = false;
    boolean isVideoMode = false;

    Drawable btOffImg, btOnImg, flashOnImg, flashOffImg, videoStartImg, videoEndImg;
    private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;

    //initialise UI in main activity layout
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private FloatingActionButton capturePhoto_Btn;
    private FloatingActionButton startVideo_Btn;
    private FloatingActionButton stopVideo_Btn;
    private ImageButton arrowBtnR, arrowBtnUp, arrowBtnDown, arrowBtnL;
    private Button bluetooth_Btn, scanCenter_Btn, gridCenter_Btn, moveDoneBtn, orientBtn, modeDoneBtn, flipBtn, galleryBtn, /*settingsBtn,*/ flash_Btn;
    private Button grid_A, grid_B, grid_C, grid_D, grid_E, grid_F, grid_G, grid_H, grid_I;
    private Button zoom05_Btn, zoom1_Btn, zoom15_Btn, zoom2_Btn, zoom3_Btn;
    private Button video_Btn, photo_Btn;
    private Button motorBtn_1, motorBtn_2, motorBtn_3, motorBtn_4, clkwiseBtn, antiClkBtn;
    private PreviewView previewView;
    private LinearLayout directionLO, levelLO, orientLO, gridLO,zoomLO, modeLO, recordingLO;
    private TextView moveDirTV;

    // camera
    private CameraSelector lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
    ProcessCameraProvider cameraProvider;
    Uri ImageUri;


    //control
    ControlCenter controlCenter = ControlCenter.getInstance(this);

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialiseHighlightableButtons();
        //highlightHandler.postDelayed(highlightRunnable, 3000);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (!checkPermission())
            requestPermission();
        //check if bluetooth is available & enabled

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG);

        //set filter for constantly update bluetooth connection status
        setFilter();
        initialiseUI();
        setUI();

        gridLO.setVisibility(View.GONE);

        if (isFinishing()){
            if (bluetoothSocket!= null) {
                try {
                    closeBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        try {
            cameraProvider = cameraProviderFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /*private void updateHighlightableButtonsForRotation() {
        Log.d(TAG, "Entering rotation mode");
        highlightHandler.removeCallbacks(highlightRunnable);
        highlightableButtons.clear();
        highlightableButtons.add(findViewById(R.id.clockwiseBtn));
        highlightableButtons.add(findViewById(R.id.anticlockwiseBtn));
        isRotating = true;
        currentButtonIndex = 0;
        highlightButton(currentButtonIndex);
        highlightHandler.postDelayed(highlightRunnable, 3000);
    }*/

    /*private void initialiseHighlightableButtons() {
        highlightableButtons = new ArrayList<>();
        highlightableButtons.add((View) findViewById(R.id.flashBtn));
        highlightableButtons.add((View) findViewById(R.id.scanCenterBtn));
        highlightableButtons.add((View) findViewById(R.id.gridCenterBtn));
        highlightableButtons.add((View) findViewById(R.id.rotateBtn));
        highlightableButtons.add((View) findViewById(R.id.bluetooth));
        //highlightableButtons.add((View) findViewById(R.id.settingsBtn));
        highlightableButtons.add((View) findViewById(R.id.gridA));
        highlightableButtons.add((View) findViewById(R.id.gridB));
        highlightableButtons.add((View) findViewById(R.id.gridC));
        highlightableButtons.add((View) findViewById(R.id.gridD));
        highlightableButtons.add((View) findViewById(R.id.gridE));
        highlightableButtons.add((View) findViewById(R.id.gridF));
        highlightableButtons.add((View) findViewById(R.id.gridG));
        highlightableButtons.add((View) findViewById(R.id.gridH));
        highlightableButtons.add((View) findViewById(R.id.gridI));
        highlightableButtons.add((View) findViewById(R.id.zoom05_Btn));
        highlightableButtons.add((View) findViewById(R.id.zoom1_Btn));
        highlightableButtons.add((View) findViewById(R.id.zoom15_Btn));
        highlightableButtons.add((View) findViewById(R.id.zoom2_Btn));
        highlightableButtons.add((View) findViewById(R.id.zoom3_Btn));
        highlightableButtons.add((View) findViewById(R.id.video_Btn));
        highlightableButtons.add((View) findViewById(R.id.photo_Btn));
        highlightableButtons.add((View) findViewById(R.id.galleryBtn));
        highlightableButtons.add((View) findViewById(R.id.capture_photo));
        highlightableButtons.add((View) findViewById(R.id.flipBtn));
        Log.d(TAG, "Total number of highlightable buttons: " + highlightableButtons.size());
        //add more buttons
    }*/
    /*private void highlightButton(int index) {
        Log.d(TAG, "Highlighting button at index: " + index);
        for (int i = 0; i < highlightableButtons.size(); i++) {
            View view = highlightableButtons.get(i);

            if (i == index) {
                // begin anime
                Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink_animation);
                view.startAnimation(blinkAnimation);
                Log.d(TAG, "Button at index " + i + " started animation.");
            } else {
                // stop anime
                view.clearAnimation();
                Log.d(TAG, "Button at index " + i + " cleared animation.");
            }
        }
    }*/


    /*public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            int indexToSelect = currentButtonIndex;

            if (indexToSelect == 0) {
                indexToSelect = highlightableButtons.size() - 1;
            } else {
                indexToSelect -= 1;
            }
            selectHighlightedButton(indexToSelect);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }*/

    /*private void selectHighlightedButton(int indexToSelect) {

        View selectedView = highlightableButtons.get(indexToSelect);

        if (selectedView instanceof Button) {
            Button selectedButton = (Button) selectedView;
            selectedButton.performClick();
        }
    }*/


    @Override

    protected void onResume() {
        checkBtEnabled();
        super.onResume();
        // highlightHandler.postDelayed(highlightRunnable, 3000);
        if (cameraProvider != null) {
            startCameraX(cameraProvider);
        }
    }
    protected void onPause() {
        super.onPause();
        //highlightHandler.removeCallbacks(highlightRunnable);
    }

    public boolean checkBtEnabled() {
        if (bluetoothAdapter.isEnabled()){
            Toast.makeText(this, "Bluetooth is Enabled", Toast.LENGTH_LONG);
            return true;
        }else {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_LONG);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }

    }

    void initialiseUI(){
        previewView = findViewById(R.id.previewView);
        capturePhoto_Btn = findViewById(R.id.capture_photo);
        startVideo_Btn = findViewById(R.id.startVideo_Btn);
        stopVideo_Btn = findViewById(R.id.stopVideo_Btn);
        arrowBtnL = findViewById(R.id.arrowBtn_Left);
        arrowBtnUp = findViewById(R.id.arrowBtn_Up);
        arrowBtnDown = findViewById(R.id.arrowBtn_Down);
        arrowBtnR = findViewById(R.id.arrowBtn_Right);
        bluetooth_Btn = findViewById(R.id.bluetooth);
        scanCenter_Btn = findViewById(R.id.scanCenterBtn);
        gridCenter_Btn = findViewById(R.id.gridCenterBtn);
        flash_Btn = findViewById(R.id.flashBtn);
        //settingsBtn = findViewById(R.id.settingsBtn);
        directionLO = findViewById(R.id.directionLayout);
        levelLO = findViewById(R.id.levelLayout);
        orientLO = findViewById(R.id.orientationLayout);
        zoomLO=findViewById(R.id.zoomLayout);
        modeLO=findViewById(R.id.modeLayout);
        recordingLO=findViewById(R.id.recordingLayout);
        moveDoneBtn = findViewById(R.id.moveDone);
        moveDirTV = findViewById(R.id.moveDir);
        orientBtn = findViewById(R.id.rotateBtn);
        modeDoneBtn = findViewById(R.id.modeDone);
        motorBtn_1 = findViewById(R.id.moveLevel1);
        motorBtn_2 = findViewById(R.id.moveLevel2);
        motorBtn_3 = findViewById(R.id.moveLevel3);
        motorBtn_4 = findViewById(R.id.moveLevel4);
        clkwiseBtn = findViewById(R.id.clockwiseBtn);
        antiClkBtn = findViewById(R.id.anticlockwiseBtn);
        gridLO = findViewById(R.id.gridLayout);
        //gridCenter_Btn.setText("grid on");
        flipBtn = findViewById(R.id.flipBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        grid_A = findViewById(R.id.gridA);
        grid_B = findViewById(R.id.gridB);
        grid_C = findViewById(R.id.gridC);
        grid_D = findViewById(R.id.gridD);
        grid_E = findViewById(R.id.gridE);
        grid_F = findViewById(R.id.gridF);
        grid_G = findViewById(R.id.gridG);
        grid_H = findViewById(R.id.gridH);
        grid_I = findViewById(R.id.gridI);
        zoom05_Btn=findViewById(R.id.zoom05_Btn);
        zoom1_Btn=findViewById(R.id.zoom1_Btn);
        zoom15_Btn=findViewById(R.id.zoom15_Btn);
        zoom2_Btn=findViewById(R.id.zoom2_Btn);
        zoom3_Btn=findViewById(R.id.zoom3_Btn);
        video_Btn=findViewById(R.id.video_Btn);
        photo_Btn=findViewById(R.id.photo_Btn);
        btOffImg= this.getResources().getDrawable( R.drawable.ic_bluetooth_disabled);
        btOnImg= this.getResources().getDrawable( R.drawable.ic_bluetooth);
        flashOnImg=this.getResources().getDrawable(R.drawable.ic_flash_on);
        flashOffImg=this.getResources().getDrawable(R.drawable.ic_flash_off);
        videoStartImg=this.getResources().getDrawable(R.drawable.ic_video_start);
        videoEndImg=this.getResources().getDrawable(R.drawable.ic_video_stop);

    }

    void setUI(){ //This updates the User Interface
        if (isBtConnected){
            bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOnImg, null, null);
        }else{
            bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOffImg, null, null);
        }

        //camera
        capturePhoto_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto();
            }
        });
        startVideo_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureVideo();
            }
        });

        stopVideo_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto_Btn.setVisibility(View.GONE);
                stopVideo_Btn.setVisibility(View.GONE);
                startVideo_Btn.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                recordingLO.setVisibility(View.GONE);

            }
        });

        if(isVideoMode == false) {
            photo_Btn.setSelected(true); //By default, start in photo mode
        };

        flash_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {toggleFlash(); }
        });

        //If video mode is selected, show the start video icon and activate isVideoMode
        video_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //directionLO.setVisibility(View.VISIBLE);
                capturePhoto_Btn.setVisibility(View.GONE);
                startVideo_Btn.setVisibility(View.VISIBLE);
                stopVideo_Btn.setVisibility(View.GONE);
                modeLO.setVisibility(View.VISIBLE);
                isVideoMode=true;
                // If the button is now selected, change its background to the selected state
                video_Btn.setSelected(true);
                photo_Btn.setSelected(false);
            }
        });

        //If photo mode is selected, show the capture photo icon and inactivate isVideoMode
        photo_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto_Btn.setVisibility(View.VISIBLE);
                startVideo_Btn.setVisibility(View.GONE);
                stopVideo_Btn.setVisibility(View.GONE);
                modeLO.setVisibility(View.VISIBLE);
                isVideoMode=false;
                photo_Btn.setSelected(true);
                video_Btn.setSelected(false);
            }
        });

        /*video_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {setVideoMode(); }
        });*/

        /*zoom2_Btn.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraControl.setZoomRatio(2.0f);
            }
        }));*/


        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            startCameraX(cameraProvider);
        }, getExecutor());

        //bluetooth button
        bluetooth_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomSheetDialog();
            }
        });

/*
        arrowBtnL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setBtnClicked("LEFT");
                // since gridBtn is removed，do "grid on" directly
                directionLO.setVisibility(View.GONE);
                moveDirTV.setText("LEFT");
                levelLO.setVisibility(View.VISIBLE);
            }
        });


        arrowBtnR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setBtnClicked("RIGHT");
                // Since grid is always on. modified
                directionLO.setVisibility(View.GONE);
                moveDirTV.setText("RIGHT");
                levelLO.setVisibility(View.VISIBLE);
            }
        });

        arrowBtnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setBtnClicked("UP");
                // Since grid is always on，modified
                directionLO.setVisibility(View.GONE);
                moveDirTV.setText("UP");
                levelLO.setVisibility(View.VISIBLE);
            }
        });


        arrowBtnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setBtnClicked("DOWN");
                //Since grid is always on, modified
                directionLO.setVisibility(View.GONE);
                moveDirTV.setText("DOWN");
                levelLO.setVisibility(View.VISIBLE);
            }
        });
*/

        moveDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);
            }
        });

        orientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //directionLO.setVisibility(View.GONE);
                zoomLO.setVisibility(View.GONE);
                modeLO.setVisibility(View.GONE);

                orientLO.setVisibility(View.VISIBLE);
                controlCenter.setBtnClicked("ROTATE");
                //updateHighlightableButtonsForRotation();
                currentButtonIndex = 0;
                //highlightButton(currentButtonIndex);
                //highlightHandler.removeCallbacks(highlightRunnable);
                //highlightHandler.postDelayed(highlightRunnable, 3000);
            }
        });


        modeDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);
            }
        });

        motorBtn_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(1);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(2);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(3);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(4);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });
        clkwiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(1);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);
                completeRotation();

            }
        });
        antiClkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(2);
                //directionLO.setVisibility(View.VISIBLE);
                zoomLO.setVisibility(View.VISIBLE);
                modeLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);
                completeRotation();

            }
        });
        gridCenter_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ("Grid Center".equals(gridCenter_Btn.getText().toString())) {
                    gridCenter_Btn.setText("Grid off");
                    gridLO.setVisibility(View.VISIBLE);

                    modeLO.setVisibility(View.GONE);
                    flash_Btn.setVisibility(View.GONE);
                    scanCenter_Btn.setVisibility(View.GONE);
                    orientBtn.setVisibility(View.GONE);
                    bluetooth_Btn.setVisibility(View.GONE);
                    galleryBtn.setVisibility(View.GONE);
                    flipBtn.setVisibility(View.GONE);
                    zoomLO.setVisibility(View.GONE);



                } else {
                    gridCenter_Btn.setText("Grid Center");
                    gridLO.setVisibility(View.GONE);

                    modeLO.setVisibility(View.VISIBLE);
                    flash_Btn.setVisibility(View.VISIBLE);
                    scanCenter_Btn.setVisibility(View.VISIBLE);
                    orientBtn.setVisibility(View.VISIBLE);
                    bluetooth_Btn.setVisibility(View.VISIBLE);
                    galleryBtn.setVisibility(View.VISIBLE);
                    flipBtn.setVisibility(View.VISIBLE);
                    zoomLO.setVisibility(View.VISIBLE);
                }
            }
        });


        flipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
                else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;
                startCameraX(cameraProvider);
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                if (ImageUri != null){
                    String url = ImageUri.toString();
                int trimIndex = url.lastIndexOf("/");
                url = url.substring(0, trimIndex);
                ImageUri = Uri.parse(url);
                //Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();
                intent.setDataAndType(ImageUri, "image/*");
                }else{
                    intent.setType("image/*");
                }

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        scanCenter_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, OldMainActivity.class);
                startActivity(intent);
            }
        });

        grid_A.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 6) - ((float) screenWidth / 2);
                        float centerY = ((float) gridHeight / 2) - ((float) gridHeight / 6);


                        Toast.makeText(MainActivity.this, "Grid A Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        grid_B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridB();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridB
                        float centerX = 0;
                        float centerY = ((float) gridHeight / 2) - ((float) gridHeight / 6);

                        Toast.makeText(MainActivity.this, "Grid B Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });


        grid_C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 2) - ((float) screenWidth / 6);
                        float centerY = ((float) gridHeight / 2) - ((float) gridHeight / 6);

                        Toast.makeText(MainActivity.this, "Grid C Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        grid_D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 6) - ((float) screenWidth / 2);
                        float centerY = 0;

                        Toast.makeText(MainActivity.this, "Grid D Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        grid_E.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = 0;
                        float centerY = 0;

                        Toast.makeText(MainActivity.this, "Grid E Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        grid_F.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 2) - ((float) screenWidth / 6);
                        float centerY = 0;

                        Toast.makeText(MainActivity.this, "Grid F Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        grid_G.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 6) - ((float) screenWidth / 2);
                        float centerY = ((float) gridHeight / 6) - ((float) gridHeight / 2);

                        Toast.makeText(MainActivity.this, "Grid G Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        grid_H.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 6) - ((float) screenWidth / 2);
                        float centerY = 0;

                        Toast.makeText(MainActivity.this, "Grid H Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        grid_I.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();

                // acquire size of screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenHeight = displayMetrics.heightPixels;
                int screenWidth = displayMetrics.widthPixels;

                // calculate height of top and bottom
                View topBar = findViewById(R.id.topBar);
                View bottomBar = findViewById(R.id.bottomBar);

                topBar.post(new Runnable() {
                    @Override
                    public void run() {
                        int topBarHeight = topBar.getHeight();
                        int bottomBarHeight = bottomBar.getHeight(); // 140dp in pixels

                        // Calculate height of grid
                        int gridHeight = screenHeight - topBarHeight - bottomBarHeight;

                        // calculate central point of gridA
                        float centerX = ((float) screenWidth / 2) - ((float) screenWidth / 6);
                        float centerY = ((float) gridHeight / 6) - ((float) gridHeight / 2);

                        Toast.makeText(MainActivity.this, "Grid I Center: (" + centerX + ", " + centerY + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector;
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();
        }else {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        }
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

    }
    // Toggle flash when the flash_Btn is clicked
    private void toggleFlash(){
        //Toggle flash state
        isFlashedEnabled=!isFlashedEnabled; // When clicked, becomes the opposite
        if (isFlashedEnabled){
            flash_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, flashOnImg, null, null);
        }
        else {
            flash_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, flashOffImg, null, null);
        };
        // Set flash mode for the ImageCapture use case
        imageCapture.setFlashMode(
                isFlashedEnabled ? ImageCapture.FLASH_MODE_ON:ImageCapture.FLASH_MODE_OFF
        );
    }

    /*private void setVideoMode(){
        captureVideo_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, videoStartImg, null, null);
    }*/

    private void capturePhoto() {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");



        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(getApplicationContext(), "Photo has been saved successfully.", Toast.LENGTH_SHORT).show();
                        ImageUri = outputFileResults.getSavedUri();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(getApplicationContext(), "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
        );

    }
        Executor getExecutor() {
            return ContextCompat.getMainExecutor(this);
        }
    private void captureVideo(){
        startVideo_Btn.setVisibility(View.GONE);
        capturePhoto_Btn.setVisibility(View.GONE);
        stopVideo_Btn.setVisibility(View.VISIBLE);
        modeLO.setVisibility(View.GONE);
        recordingLO.setVisibility(View.VISIBLE);
    }
    void showBottomSheetDialog(){
        //for bluetooth connection
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        bottomSheetDialog.setContentView(R.layout.bsheet_bluetooth);
        TextView btDeviceTV = bottomSheetDialog.findViewById(R.id.btDevice);
        ListView btDeviceLV = bottomSheetDialog.findViewById(R.id.device_list);
        Button btConnectBtn = bottomSheetDialog.findViewById(R.id.bt_connect);
        Button cancelBtn = bottomSheetDialog.findViewById(R.id.bt_cancel);
        ProgressBar progressBar = bottomSheetDialog.findViewById(R.id.bt_progress);

        //get btdevice list
        btDeviceTV.setText(isBtConnected? "Disconnect " + connectedBTDevice.getName() + "?": btDeviceRefresh());

        //bluetooth list
        listAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, listItems) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if (view == null)

                    view = getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.btDeviceName);
                text1.setText(device.getName());
                return view;
            }
        };
        btDeviceLV.setAdapter(listAdapter);
        btDeviceLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (checkBtEnabled()) {
                    selectedBTDevice = listItems.get(i);
                    btDeviceTV.setText("Selected: " + selectedBTDevice.getName());
                }
            }
        });

        // button set for open or close socket
        btConnectBtn.setText(isBtConnected? "Disconnect" :"Connect");
        btConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBtConnected) {
                    try {
                        closeBT();
                        btDeviceTV.setText("Disconnected");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    new ProgressAsyncTask(progressBar, btDeviceTV, btConnectBtn).execute();
                   // ControlCenter.setCenter();
                }
                btConnectBtn.setText(isBtConnected? "Disconnect" :"Connect");
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
            }
        });

        bottomSheetDialog.show();
    }

    private String btDeviceRefresh(){ //refresh bluetooth device list
        if(bluetoothAdapter == null){
            return getResources().getString(R.string.bluetooth_no_support);
        }else if (!bluetoothAdapter.isEnabled())
            return getResources().getString(R.string.bluetooth_disabled);
        listItems.clear();
        if(bluetoothAdapter!= null){
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    listItems.add(device);
            }
        }
        if (listItems == null){
            return getResources().getString(R.string.bluetooth_no_device);
        }else if (listItems != null && bluetoothAdapter.isEnabled())
            return getResources().getString(R.string.bluetooth_select);

        return null;
    }

    private boolean openBT() {
        if (selectedBTDevice != null && selectedBTDevice.getUuids() != null){
            ParcelUuid[] uuids = selectedBTDevice.getUuids();
            try {
                bluetoothSocket = selectedBTDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                bluetoothSocket.connect();
                btOutputStream = bluetoothSocket.getOutputStream();
                btInputStream = bluetoothSocket.getInputStream();
                connectedBTDevice = selectedBTDevice;
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "bluetooth is not connected");
                return false;
            }
        }
        return false;

    }

    private void closeBT() throws IOException {
        if(btOutputStream != null && btInputStream != null && bluetoothSocket != null){
            //stopworker
            btOutputStream.close();
            btInputStream.close();
            bluetoothSocket.close();
            isBtConnected = false;

        }
    }
    private class ProgressAsyncTask extends AsyncTask<Void, Void, Void> {
        //to show and hide progress bar and implement openbt

        ProgressBar btprogress;
        TextView btTV;
        Button btConnectBtn;
        public ProgressAsyncTask(ProgressBar pb, TextView tv, Button btBtn){
            super();
            btprogress = pb;
            btTV = tv;
            btConnectBtn = btBtn;
        }
        @Override
        protected Void doInBackground(Void... args) {
            isBtConnected = openBT();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isBtConnected){
                btTV.setText("Connected:" + connectedBTDevice.getName());
                btConnectBtn.setText(isBtConnected? "Disconnect" :"Connect");
            } else{
                btTV.setText(getResources().getText(R.string.bluetooth_connect_fail));
            }
            btprogress.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btprogress.setVisibility(View.VISIBLE);
        }
    }

    private void setFilter() { //to constantly check bt connection status
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { //check bt status constantly

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                isBtConnected = true;
                Toast.makeText(context, deviceName + " is connected", Toast.LENGTH_LONG).show();
                bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOnImg, null, null);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                isBtConnected = false;
                //endlogging could be done here
                Toast.makeText(context, deviceName + " is disconnected", Toast.LENGTH_LONG).show();
                bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOffImg, null, null);
            }
        }
    };

// for checking and asking for permission
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
package com.example.wheelcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
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

    Drawable btOffImg, btOnImg;
    private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;

    //initialise UI in main activity layout
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private FloatingActionButton captureBtn;
    private ImageButton arrowBtnR, arrowBtnUp, arrowBtnDown, arrowBtnL;
    private Button bluetooth_Btn, center_Btn, moveDoneBtn, orientBtn, modeDoneBtn, flipBtn, galleryBtn;
    private Button grid_A, grid_B, grid_C, grid_D, grid_E, grid_F, grid_G, grid_H, grid_I;
    private Button motorBtn_1, motorBtn_2, motorBtn_3, motorBtn_4, clkwiseBtn, antiClkBtn;
    private PreviewView previewView;
    private LinearLayout directionLO, levelLO, orientLO, gridLO;
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
    @Override
    protected void onResume() {
        checkBtEnabled();
        super.onResume();
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
        captureBtn = findViewById(R.id.capture);
        arrowBtnL = findViewById(R.id.arrowBtn_Left);
        arrowBtnUp = findViewById(R.id.arrowBtn_Up);
        arrowBtnDown = findViewById(R.id.arrowBtn_Down);
        arrowBtnR = findViewById(R.id.arrowBtn_Right);
      // grid_Btn = findViewById(R.id.gridBtn);
        bluetooth_Btn = findViewById(R.id.bluetooth);
        center_Btn = findViewById(R.id.centerBtn);
        directionLO = findViewById(R.id.directionLayout);
        levelLO = findViewById(R.id.levelLayout);
        orientLO = findViewById(R.id.orientationLayout);
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
       // grid_Btn.setText("grid on");
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
        btOffImg= this.getResources().getDrawable( R.drawable.ic_bluetooth_disabled);
        btOnImg= this.getResources().getDrawable( R.drawable.ic_bluetooth);
    }

    void setUI(){
        if (isBtConnected){
            bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOnImg, null, null);
        }else{

            bluetooth_Btn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, btOffImg, null, null);
        }

        //camera
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto();
            }
        });

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


        moveDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                directionLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);
            }
        });

        orientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                directionLO.setVisibility(View.GONE);
                orientLO.setVisibility(View.VISIBLE);
                controlCenter.setBtnClicked("ROTATE");
            }
        });

        modeDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                directionLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);
            }
        });

        motorBtn_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(1);
                directionLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(2);
                directionLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(3);
                directionLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });

        motorBtn_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(4);
                directionLO.setVisibility(View.VISIBLE);
                levelLO.setVisibility(View.GONE);

            }
        });
        clkwiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(1);
                directionLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);

            }
        });
        antiClkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlCenter.setMoveExtent(2);
                directionLO.setVisibility(View.VISIBLE);
                orientLO.setVisibility(View.GONE);

            }
        });
     /*   grid_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (grid_Btn.getText() == "grid on"){
                    grid_Btn.setText("grid off");
                    gridLO.setVisibility(View.VISIBLE);
                }else {
                    grid_Btn.setText("grid on");
                    gridLO.setVisibility(View.GONE);
                }
            }
        }); no longer needed*/

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
        center_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setCenter();
            }
        });

        grid_A.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridA();
            }
        });
        grid_B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridB();
            }
        });

        grid_C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridC();
            }
        });

        grid_D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridD();
            }
        });
        grid_F.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridF();
            }
        });
        grid_G.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridG();
            }
        });

        grid_H.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridH();
            }
        });

        grid_I.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ControlCenter.setGridI();
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
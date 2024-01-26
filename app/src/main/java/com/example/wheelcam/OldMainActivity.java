package com.example.wheelcam;
import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.animation.ValueAnimator;import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.view.PreviewView;
import androidx.annotation.NonNull;import android.content.pm.PackageManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;import android.view.ViewTreeObserver;





public class OldMainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private CustomView customView;
    private Button controlButton;
    private int clickCount = 0;
    private ValueAnimator horizontalAnimator, verticalAnimator;
    private PreviewView previewView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.old_activity_main);
        customView = findViewById(R.id.customView);
        previewView = findViewById(R.id.previewView);
        controlButton = findViewById(R.id.controlButton);

        customView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                customView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // starting
                startMovingLine(true);
            }
        });

        // check permission of camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
        }
        // Start moving horizontal line immediately

        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCount++;
                switch (clickCount) {
                    case 1:
                        if (horizontalAnimator != null) {
                            horizontalAnimator.cancel(); // stop horizontal line
                        }
                        startMovingLine(false); // start vertical line
                        break;
                    case 2:
                        if (verticalAnimator != null) {
                            verticalAnimator.cancel(); // stop vertical line
                        }
                        float[] intersection = customView.getIntersectionPoint();
                        showToast("Intersection X: " + intersection[0] + ", Y: " + intersection[1]);
                        String result = "x" + intersection[0]+ "y" + intersection[1];
                        sendIntersectionPoint(result);
                        finish(); // exit the activity
                        break;
                }
            }
        });
    }

    BluetoothSocket bluetoothSocket;

    private void sendIntersectionPoint(String result) {
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity != null && mainActivity.bluetoothSocket != null && mainActivity.bluetoothSocket.isConnected()) {
            try {
                OutputStream outputStream = mainActivity.bluetoothSocket.getOutputStream();
                outputStream.write(result.getBytes());
                showToast("Data sent: " + result);
            } catch (IOException e) {
                Log.e("OldMainActivity", "Error sending data", e);
                showToast("Error sending data");
            }
        } else {
            showToast("Bluetooth is not connected or MainActivity is not available");
        }
    }



    private void startMovingLine(boolean isHorizontal) {
        float startValue = 0;
        float endValue = isHorizontal ? customView.getHeight() : customView.getWidth();
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(7000); // 7 seconds
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (isHorizontal) {
                customView.setHorizontalLineY(value);
            } else {
                customView.setVerticalLineX(value);
            }
        });

        animator.start();

        if (isHorizontal) {
            horizontalAnimator = animator;
        } else {
            verticalAnimator = animator;
        }
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException e) {
                // handle ExecutionException，fail to access CameraProvider
                Log.e("OldMainActivity", "ExecutionException in startCamera", e);
            } catch (InterruptedException e) {
                // handle InterruptedException
                Log.e("OldMainActivity", "InterruptedException in startCamera", e);
                // stay disrupted
                Thread.currentThread().interrupt();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // simulate clicking controlButton
            if (controlButton != null) {
                controlButton.performClick();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void showToast(String message) {
        Toast.makeText(OldMainActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}


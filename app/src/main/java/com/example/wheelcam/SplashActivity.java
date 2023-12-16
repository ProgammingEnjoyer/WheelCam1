package com.example.wheelcam;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView splashIV;
    private Animation splashAnim;
    private int splash_duration = 2500;
    private ImageButton nextBtn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        splashIV = findViewById(R.id.splash_wheel);
        nextBtn = findViewById(R.id.splash_next);
        splashAnim = AnimationUtils.loadAnimation(this, R.anim.side_slide);
        splashIV.startAnimation(splashAnim);
        Handler splashHandler = new Handler();
        Runnable splashRunnable = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
            }
        };
        splashHandler.postDelayed(splashRunnable, splash_duration);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                splashHandler.removeCallbacks(splashRunnable);
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
            }
        });
    }
}

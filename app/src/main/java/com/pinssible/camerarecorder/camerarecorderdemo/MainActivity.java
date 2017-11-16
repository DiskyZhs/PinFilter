package com.pinssible.camerarecorder.camerarecorderdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;

public class MainActivity extends AppCompatActivity {
    private LinearLayout container;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_AUDIO = 3;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA};

    private static String[] PERMISSIONS_MIRCO = {
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.init(getApplication());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.main_container);

        // Check if we have write permission
        checkoPermission();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.height = SizeUtils.dp2px(75);

        container.addView(
                FunctionViewBuilder.getFunctionView(this
                        , "CameraCaptureActivity"
                        , "Camera Preview and Recorder with Filter"
                        , new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isPermissionContain()) {
                                    startActivity(new Intent(MainActivity.this, CameraCaptureActivity.class));
                                }
                            }
                        }), lp
        );

        container.addView(
                FunctionViewBuilder.getFunctionView(this
                        , "PlayerActivity"
                        , "Play Video with filter"
                        , new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isPermissionContain()) {
                                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                                }
                            }
                        }), lp
        );

        container.addView(
                FunctionViewBuilder.getFunctionView(this
                        , "RemuxerActivity"
                        , "Generate mp4 with filter according to other mp4"
                        , new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isPermissionContain()) {
                                    startActivity(new Intent(MainActivity.this, TestRemuxerActivity.class));
                                }
                            }
                        }), lp
        );
    }


    private void checkoPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA,
                    REQUEST_CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_MIRCO,
                    REQUEST_AUDIO);
        }
    }


    private boolean isPermissionContain() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ToastUtils.showShort("Please open write storage,camera and record audio permission!");
            return false;
        }
    }
}

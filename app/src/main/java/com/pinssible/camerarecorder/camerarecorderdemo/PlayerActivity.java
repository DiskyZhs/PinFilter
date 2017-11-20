package com.pinssible.camerarecorder.camerarecorderdemo;

import android.Manifest;
import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.pinssible.librecorder.player.PinMediaPlayer;
import com.pinssible.librecorder.player.SimplePinPlayerView;

import java.security.Permission;

public class PlayerActivity extends Activity {
    //view
    private SimplePinPlayerView previewSurface;
    private Button filterBtn;

    //player
    private PinMediaPlayer player;

    private int filterType = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);

        //view
        previewSurface = findViewById(R.id.player_view);
        filterBtn = findViewById(R.id.btn_filter);

        //player
        String outputPath = "file:///android_asset/test.mp4";
        Uri source = Uri.parse(outputPath);
        try {
            player = new PinMediaPlayer(this, source, true);
            previewSurface.setPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShort("create player fail cause = " + e.toString());
        }


        //filter
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int type = (filterType++) % 17;
                Log.e("Test", "filterType = " + type);
                player.setFilter(type);
            }
        });

    }

}

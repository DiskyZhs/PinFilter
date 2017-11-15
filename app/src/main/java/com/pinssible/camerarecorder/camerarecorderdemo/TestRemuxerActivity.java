package com.pinssible.camerarecorder.camerarecorderdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.pinssible.librecorder.filter.Filters;
import com.pinssible.librecorder.remux.PinRemuxer;
import com.pinssible.librecorder.remux.RemuxerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TestRemuxerActivity extends Activity implements RemuxerFactory.OnRemuxListener {
    private final String TAG = "TestRemuxerActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //View
    private TextView srcText, dstText;
    private Button startBtn;
    private Spinner filterSpanner;
    private ProgressBar progressBar;
    private TextView processTv;

    //path
    String dstPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/20171109.mp4";

    //other
    private HashMap<Integer, String> filtersMap;

    private boolean isMuxing = false;
    private PinRemuxer remuxer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.init(getApplication());
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.layout_test_texture_view_output);

        //权限检测申请
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
        //view
        initView();
    }


    private void initView() {
        srcText = findViewById(R.id.tv_src_path);
        dstText = findViewById(R.id.tv_dst_path);
        startBtn = findViewById(R.id.btn_start);
        filterSpanner = findViewById(R.id.spinner_filter);
        progressBar = findViewById(R.id.progress_remux);
        processTv = findViewById(R.id.mulit_tv_process);
        processTv.setMovementMethod(ScrollingMovementMethod.getInstance());

        initFilters();
        //spinner
        List<String> filters = new ArrayList<>(filtersMap.values());
        Collections.sort(filters, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return getFilter(o1) - getFilter(o2);
            }
        });
        ArrayAdapter filterAdapter = new ArrayAdapter(this, R.layout.layout_filter_item, R.id.tv_filter, filters);
        filterSpanner.setAdapter(filterAdapter);

        //text
        srcText.setText("Asset://test.mp4");
        dstText.setText(dstPath);

        //btn
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMuxing) {
                    remuxer.stop();
                    startBtn.setText("Start");
                    isMuxing = false;
                } else {
                    startRemux();
                    startBtn.setText("Stop");
                    isMuxing = true;
                }
            }
        });

    }

    private void initFilters() {
        filtersMap = new HashMap<>();
        filtersMap.put(Filters.FILTER_NONE, "FILTER_NONE".toLowerCase());
        filtersMap.put(Filters.FILTER_BLACK_WHITE, "FILTER_BLACK_WHITE".toLowerCase());
        filtersMap.put(Filters.FILTER_NIGHT, "FILTER_NIGHT".toLowerCase());
        filtersMap.put(Filters.FILTER_CHROMA_KEY, "FILTER_CHROMA_KEY".toLowerCase());
        filtersMap.put(Filters.FILTER_BLUR, "FILTER_BLUR".toLowerCase());
        filtersMap.put(Filters.FILTER_SHARPEN, "FILTER_SHARPEN".toLowerCase());
        filtersMap.put(Filters.FILTER_EDGE_DETECT, "FILTER_EDGE_DETECT".toLowerCase());
        filtersMap.put(Filters.FILTER_EMBOSS, "FILTER_EMBOSS".toLowerCase());
        filtersMap.put(Filters.FILTER_SQUEEZE, "FILTER_SQUEEZE".toLowerCase());
        filtersMap.put(Filters.FILTER_TWIRL, "FILTER_TWIRL".toLowerCase());
        filtersMap.put(Filters.FILTER_TUNNEL, "FILTER_TUNNEL".toLowerCase());
        filtersMap.put(Filters.FILTER_BULGE, "FILTER_BULGE".toLowerCase());
        filtersMap.put(Filters.FILTER_DENT, "FILTER_DENT".toLowerCase());
        filtersMap.put(Filters.FILTER_FISHEYE, "FILTER_FISHEYE".toLowerCase());
        filtersMap.put(Filters.FILTER_STRETCH, "FILTER_STRETCH".toLowerCase());
        filtersMap.put(Filters.FILTER_MIRROR, "FILTER_MIRROR".toLowerCase());
    }

    private int getFilter(String filterName) {
        if (filtersMap.containsValue(filterName)) {
            for (int filter : filtersMap.keySet()) {
                if (filtersMap.get(filter).equals(filterName)) {
                    return filter;
                }
            }
        }
        return Filters.FILTER_NONE;
    }

    private void startRemux() {
        File dstFile = new File(dstPath);
        if (!dstFile.exists()) {
            try {
                dstFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //src
        try {
            AssetFileDescriptor asd = getAssets().openFd("test.mp4");
            //remuxer
            remuxer = new PinRemuxer(asd, dstPath, this);
            remuxer.start(getFilter(((String) filterSpanner.getSelectedItem()).toLowerCase()));
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onRemuxStart(final long totalPts) {
        Log.e(TAG, "onRemuxStart totalPts = " + totalPts);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                processTv.setText("\n Remux Start");
                processTv.append("\n Video duration = " + totalPts);
                progressBar.setMax((int) (totalPts / 1000));
            }
        });
    }

    @Override
    public void onRemuxProcess(final long pts) {
        Log.e(TAG, "onRemuxProcess pts = " + pts);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress((int) (pts / 1000));
                processTv.append("\n Remuxing current pts = " + pts);
                if (processTv.getLineCount() * processTv.getLineHeight() - processTv.getHeight() > 0)
                    processTv.scrollTo(0, processTv.getLineCount() * processTv.getLineHeight() - processTv.getHeight());
            }
        });
    }

    @Override
    public void onRemuxFinish() {
        Log.e(TAG, "onRemuxFinish");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isMuxing = false;
                startBtn.setText("Start");
                ToastUtils.showShort("Remux Success!");
                processTv.append("\n Remux finish");
                processTv.scrollTo(0, processTv.getLineCount() * processTv.getLineHeight() - processTv.getHeight());
                progressBar.setProgress(0);
            }
        });
    }

    @Override
    public void onRemuxFail(final Exception e) {
        Log.e(TAG, "onRemuxFail e = " + e.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isMuxing = false;
                startBtn.setText("Start");
                ToastUtils.showShort("Remux fail ! cause of e = " + e.toString());
                processTv.append("\n Remux fail cause e = " + e.toString());
                processTv.scrollTo(0, processTv.getLineCount() * processTv.getLineHeight() - processTv.getHeight());
            }
        });
    }
}
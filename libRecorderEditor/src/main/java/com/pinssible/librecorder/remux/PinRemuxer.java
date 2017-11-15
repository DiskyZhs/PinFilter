package com.pinssible.librecorder.remux;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

import com.pinssible.librecorder.filter.Filters;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by ZhangHaoSong on 2017/11/14.
 */

public class PinRemuxer {
    private final String TAG = "PinRemuxer";

    //src
    private String srcPath;
    private FileDescriptor srcFd;
    private AssetFileDescriptor srcAfd;

    //dst
    private String dstPath;

    private RemuxerFactory remuxer;
    private RemuxerFactory.OnRemuxListener remuxListener;

    /**
     * {@link Filters}
     */
    private int filter = Filters.FILTER_NONE;


    public PinRemuxer(String srcPath, String dstPath, RemuxerFactory.OnRemuxListener remuxListener) {
        this.srcPath = srcPath;
        this.dstPath = dstPath;
        this.remuxListener = remuxListener;
        remuxer = new RemuxerFactory();;
    }

    public PinRemuxer(FileDescriptor fd, String dstPath, RemuxerFactory.OnRemuxListener remuxListener) {
        this.srcFd = fd;
        this.dstPath = dstPath;
        this.remuxListener = remuxListener;
        remuxer = new RemuxerFactory();
    }

    public PinRemuxer(AssetFileDescriptor afd, String dstPath, RemuxerFactory.OnRemuxListener remuxListener) {
        this.srcAfd = afd;
        this.dstPath = dstPath;
        this.remuxListener = remuxListener;
        remuxer = new RemuxerFactory();
    }

    public void start(int filter) {
        this.filter = filter;
        start();
    }

    public void start() {
        File dstFile = new File(dstPath);
        if (!dstFile.exists()) {
            try {
                dstFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //launch thread
        Thread myThread = new Thread(new RemuxRunnable());
        myThread.start();
    }

    public void stop() {
        remuxer.stopRemux();
    }

    private class RemuxRunnable implements Runnable {
        @Override
        public void run() {
            //start
            try {
                if (srcAfd != null) {
                    remuxer.startRemux(srcAfd, dstPath, remuxListener, filter);
                } else if (srcFd != null) {
                    remuxer.startRemux(srcFd, dstPath, remuxListener, filter);
                } else {
                    remuxer.startRemux(srcPath, dstPath, remuxListener, filter);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                Log.e(TAG, "test throwable = " + throwable.toString());
            }
        }
    }

}

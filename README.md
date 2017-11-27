# PinFilter
![CocoaPods](https://img.shields.io/badge/Android%20-4.3%2B-brightgreen.svg)   ![CircleCI](https://img.shields.io/circleci/project/github/RedSparr0w/node-csgo-parser.svg)  
  @[Filter, Camera, Player, Recorder]

Introduction
======
- PinFilter is a libary for android which can be used to add filter to mp4.
- PinFilter is based on Android MediaCodec and MediaMuxer,All video decode or encode by hardware.
- So you have to use this in the device on Android 4.3+.

## Function
1. Camera Preview With Filter
2. Capture Camera Preview With Filter ，And Generate MP4
3. Preview Video with Filter
4. Decode MP4 and Regenerate MP4 with Filter

Preview
======
![](ezgif.com-optimize.gif)

Getting started
======
- In your ` build.gradle`:
```
    dependencies {
        compile 'com.pinssible:pin-filter:0.0.2'
    }
```
- Or you can clone the project get the Lib   `libRecorderEditor`,Import it to your project:
```
    dependencies {
        compile project(path: ':libRecorderEditor')
    }
```

Usage
======
- reference demo project
-   ` CameraCaptureActivity` is for Camera preview and recoder with filter 
-  ` PlayerActivity` is for video play with  filter
-  ` TestRemuxerActivity` is for MP4 remux with filter

- Camera Preview and recorder

```
        private AVRecorder recorder;
        private RecorderConfig recorderConfig;
        private PreviewConfig previewConfig;
        private GLTextureView/GlSurfaceView preview;


        //recoder config
        private RecorderConfig createRecorderConfig() {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Neon";
            String outputPath = path + "/NeonRecordTest_" + System.currentTimeMillis() + ".mp4";
            FileUtils.createOrExistsDir(path);
             //file
            File mOutputFile = new File(outputPath);
             //setting
            RecorderConfig.VideoEncoderConfig videoConfig = new RecorderConfig.VideoEncoderConfig(480, 640,
                    5 * 1000 * 1000, EGL14.eglGetCurrentContext());
            RecorderConfig.AudioEncoderConfig audioConfig = new RecorderConfig.AudioEncoderConfig(1, 96 * 1000, 44100);
            //finish callback
            OnMuxFinishListener listener = new OnMuxFinishListener() {
                @Override
                public void onMuxFinish() {
                    //todo
                }

                @Override
                public void onMuxFail(Exception e) {
                    //todo
                }
            };
            return new RecorderConfig(videoConfig, audioConfig,
                    mOutputFile, RecorderConfig.SCREEN_ROTATION.VERTICAL, listener);
        }


        //preview config
        private PreviewConfig createPreviewConfig() {
            return new PreviewConfig(480, 640);
        }


         //init preview
         recorder = new AVRecorder(previewConfig, recorderConfig, preview);


         //start recoder
         recorder.startRecording();

        //stop recoder
        recorder.stopRecording();

        //reset recoder for new recording
        recorder.reset(recorderConfig);

        //take shot
        recorder.takeShot(CallBack callback)

        //change filter
        recorder.setFilter(int filterType);
```

- Mp4 play with filter (Usage as exoPlayer)

```
        private SimplePinPlayerView previewSurface; //a simple play view base on exoplayer SimpleExoPlayerView
        private PinMediaPlayer player;

        //init
        String outputPath = "file:///android_asset/test.mp4";
        Uri source = Uri.parse(outputPath);
        player = new PinMediaPlayer(this, source, isloop);

        //change filter
        player.setFilter(int filterType);
```

- Remux mp4 with filter

```
         private PinRemuxer remuxer;

         //start remuxer
         remuxer = new PinRemuxer(srcPath, dstPath, new OnRemuxListener remuxListener{
                 void onRemuxStart(long totalPts);

                 void onRemuxProcess(long pts);

                 void onRemuxFinish();

                 void onRemuxFail(Exception e);
         });
         remuxer.start(int filterType);
```

Achieve
======
- The filter achieved by Opengl
- The Player with filter is based on [ExoPlayer](https://github.com/google/ExoPlayer)
- The Video Remux is based on Android test [ExtractDecodeEditEncodeMuxTest.java](https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java.)
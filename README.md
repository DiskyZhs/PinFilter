# PinFilter
[![CocoaPods](https://img.shields.io/badge/Android%20-4.3%2B-brightgreen.svg)]()     [![CircleCI](https://img.shields.io/circleci/project/github/RedSparr0w/node-csgo-parser.svg)]()

@[Filter, Camera, Player, Recorder]

##Introduction
- PinFilter is a libary for android which can be used to add filter to mp4.
- PinFilter is based on Android MediaCodec and MediaMuxer,All video decode or encode by hardware.
- So you have to use this in the device on Android 4.3+.

## Function
1. Camera Preview With Filter
2. Capture Camera Preview With Filter ，And Generate MP4
3. Preview Video with Filter
4. Decode MP4 and Regenerate MP4 with Filter

## Preview
![](ezgif.com-optimize.gif)


##Getting started
- In your ` build.gradle`:

    dependencies {
    compile 'com.pinssible:pin-filter:0.0.1'
    }
- Or you can clone the project get the Lib   `libRecorderEditor`,Import it to your project:

    dependencies {
    compile project(path: ':libRecorderEditor')
    }

##Usage
- reference demo project
-   ` CameraCaptureActivity` is for Camera preview and recoder with filter 
-  ` PlayerActivity` is for video play with  filter
-  ` TestRemuxerActivity` is for MP4 remux with filter

##Achieve
- The filter achieved by Opengl
- The Player with filter is based on [ExoPlayer](https://github.com/google/ExoPlayer)
- The Video Remux is based on Android test [ExtractDecodeEditEncodeMuxTest.java](https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java.)
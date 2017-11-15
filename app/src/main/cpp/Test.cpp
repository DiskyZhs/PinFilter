//
// Created by 54340 on 2017/11/7.
//
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL Java_com_pinssible_camerarecorder_camerarecorderdemo_FfmpegTestActivity_helloTest
  (JNIEnv * env, jobject){
      std::string hello = "Hello from C++";
      return env->NewStringUTF(hello.c_str());
  }

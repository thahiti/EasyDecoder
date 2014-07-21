#include "NativeVideoDecoder.h"
#include "EasyAVCDecoder.h"
#include <android/log.h>
#include <errno.h>

#define LOGTAG "FVideoDecoderNative"
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, LOGTAG, __VA_ARGS__);
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN, LOGTAG, __VA_ARGS__);
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOGTAG, __VA_ARGS__);

EasyAVCDecoder * easyDecoder;

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeInit(JNIEnv *env, jobject thiz, jint width, jint height, jboolean doColorConvert){

    easyDecoder = new EasyAVCDecoder();
    easyDecoder->initDecoder(width, height, (int)doColorConvert);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDeinit(JNIEnv *env, jobject thiz){
    easyDecoder->deinitDecoder();
    delete easyDecoder;

    return 0;
}

JNIEXPORT jlong JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDecode(JNIEnv *env, jobject thiz, jbyteArray inputVideoFrame, jint len, jlong ts, jbyteArray resultYUVBuffer){
    int ret=0;

    jbyte * native_inputVideoFrame = (env)->GetByteArrayElements(inputVideoFrame,0);  
    jbyte * native_resultYUVBuffer = (env)->GetByteArrayElements(resultYUVBuffer,0);  
    
    EasyAVCDecoder::YUVFrame result; 
    int resultSize = easyDecoder->decodeFrame((unsigned char*)native_inputVideoFrame, len, ts);
    if(0 < resultSize){
        result = easyDecoder->getYUVFrame();
        memcpy(native_resultYUVBuffer, result.yuv, result.size);
        ret = result.ts;
    }else{
        ret = -1;
    }

    (env)->ReleaseByteArrayElements(inputVideoFrame, native_inputVideoFrame ,JNI_ABORT);
    (env)->ReleaseByteArrayElements(resultYUVBuffer, native_resultYUVBuffer ,JNI_ABORT);
    return ret;
}


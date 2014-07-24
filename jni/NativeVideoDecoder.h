#include <jni.h>
#ifndef __com_rd_mirrorclient_VideoDecoder__
#define __com_rd_mirrorclient_VideoDecoder__
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeInit(JNIEnv *, jobject, jint, jint );
JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDeinit(JNIEnv *, jobject);
JNIEXPORT jlong JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDecode(JNIEnv *, jobject, jbyteArray, jint, jlong, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif


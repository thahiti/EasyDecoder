LOCAL_PATH := $(call my-dir)
FFMPEG := ../../ffmpeg
X264 := ../../x264

include $(CLEAR_VARS)
LOCAL_MODULE            := libavcodec
LOCAL_SRC_FILES         := $(FFMPEG)/output/libavcodec-55.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libavfilter
LOCAL_SRC_FILES         := $(FFMPEG)/output/libavfilter-4.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libavformat
LOCAL_SRC_FILES         := $(FFMPEG)/output/libavformat-55.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libavutil
LOCAL_SRC_FILES         := $(FFMPEG)/output/libavutil-52.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libpostproc
LOCAL_SRC_FILES         := $(FFMPEG)/output/libpostproc-52.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libswresample
LOCAL_SRC_FILES         := $(FFMPEG)/output/libswresample-0.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libswscale
LOCAL_SRC_FILES         := $(FFMPEG)/output/libswscale-2.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE            := libx264
LOCAL_SRC_FILES         := $(X264)/output/libx264.so
include $(PREBUILT_SHARED_LIBRARY)
	
include $(CLEAR_VARS)

LOCAL_SRC_FILES += NativeVideoDecoder.cpp\
				   EasyAVCDecoder.cpp\
				   FScaler.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(FFMPEG)

LOCAL_SHARED_LIBRARIES := libavcodec\
						  libavformat\
						  libavutil\
						  libporstproc\
						  libswresample\
						  libswscale\
						  libx264

LOCAL_CFLAGS = -D__STDC_CONSTANT_MACROS 
LOCAL_MODULE_TAGS := eng 
LOCAL_PRELINK_MODULE:=false
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid
LOCAL_MODULE    := fvideodecoder

include $(BUILD_SHARED_LIBRARY)

#include "NativeVideoDecoder.h"
#include <android/log.h>
#include <errno.h>

extern "C" {
#include "libavutil/imgutils.h"
#include "libavutil/samplefmt.h"
#include "libavutil/timestamp.h"
#include "libavformat/avformat.h"
};

#define LOGTAG "FVideoDecoderNative"
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, LOGTAG, __VA_ARGS__);
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN, LOGTAG, __VA_ARGS__);
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOGTAG, __VA_ARGS__);

class EasyAVCDecoder{
public:
    struct YUVFrame{
        unsigned char * yuv;
        int size;
        long ts;
    };
private:
    AVPacket avpkt;
    AVCodecContext *codecContext;
    AVFrame *frame;
    int frame_count;
    uint8_t *video_dst_data[4];
    int video_dst_linesize[4];
    int video_dst_bufsize;
//    FILE * yuvDumpFileHandle;
    YUVFrame decodedFrame;
public:
    EasyAVCDecoder(){
        decodedFrame.yuv=NULL;
        decodedFrame.size=0;
        decodedFrame.ts=0;
    }

    void initDecoder(int width, int height){
//        yuvDumpFileHandle = fopen("/mnt/sdcard/ffdump.yuv","wb");

        av_register_all();

        AVCodec *codec;

        av_init_packet(&avpkt);

        /* find the mpeg1 video decoder */
        codec = avcodec_find_decoder(AV_CODEC_ID_H264);
        if (!codec) {
            LOGE( "Codec not found\n");
            exit(1);
        }

        codecContext = avcodec_alloc_context3(codec);
        if (!codecContext) {
            LOGE( "Could not allocate video codec context\n");
            exit(1);
        }

        if(codec->capabilities&CODEC_CAP_TRUNCATED)
            codecContext->flags|= CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

        /* For some codecs, such as msmpeg4 and mpeg4, width and height
           MUST be initialized there because this information is not
           available in the bitstream. */

        /* open it */
        if (avcodec_open2(codecContext, codec, NULL) < 0) {
            LOGE( "Could not open codec\n");
            exit(1);
        }
        frame = av_frame_alloc();
        if (!frame) {
            LOGE( "Could not allocate video frame\n");
            exit(1);
        }

        codecContext->width=width;
        codecContext->height=height;
        codecContext->pix_fmt = AV_PIX_FMT_YUV420P;

        /* allocate image where the decoded image will be put */
        int ret = av_image_alloc(video_dst_data, video_dst_linesize,
                codecContext->width, codecContext->height,
                codecContext->pix_fmt, 1);
        if (ret < 0) {
            LOGE( "Could not allocate raw video buffer\n");
        }

        video_dst_bufsize = ret;

        frame_count = 0;

        LOGD("linesize: %d %d %d %d buf size: %d", video_dst_linesize[0], video_dst_linesize[1], video_dst_linesize[2], video_dst_linesize[3], video_dst_bufsize);
    }


    int decode(AVPacket pkt, uint8_t ** buf, int * size, int64_t * ts, int64_t * duration){
        int got_frame;
        int ret = 0;

        got_frame = 0;

        //    pkt.pts = av_rescale_q_rnd((int64_t)pkt.pts,
        //                               inTimebase, getOutputTimebase(),
        //                               (enum AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        //    pkt.dts = av_rescale_q_rnd((int64_t)pkt.dts,
        //                               inTimebase, getOutputTimebase(),
        //                               (enum AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        //
        //    pkt.duration = (int)av_rescale_q((int64_t)pkt.duration,
        //                                     inTimebase, getOutputTimebase());

        /* decode video frame */
        ret = avcodec_decode_video2(codecContext, frame, &got_frame, &pkt);
        if (ret < 0) {
            LOGE( "Error decoding video frame (%s)\n", av_err2str(ret));
            return ret;
        }

        if (got_frame) {
            if(NULL != buf){
                av_image_copy(video_dst_data, video_dst_linesize,
                        (const uint8_t **)(frame->data), frame->linesize,
                        codecContext->pix_fmt, codecContext->width, codecContext->height);
                *buf = video_dst_data[0];
            }

            *size = video_dst_bufsize;

            //        *ts = frame->pkt_pts;
            //        *duration = frame->pkt_duration;

            ret = 0;
        }else{
            ret = -1;
        }

        return ret;
    }

    int decodeFrame(unsigned char * frame, int len, long ts){
        avpkt.data = frame;
        avpkt.size = len;
        avpkt.pts = ts*20;

        int size;
        int64_t timestamp;
        int64_t duration;
        unsigned char * decodedFrameYUV;

        int result = decode(avpkt, &decodedFrameYUV, &size, &timestamp, &duration);

        if(0 == result){
            if(NULL != decodedFrameYUV && 0 < size){
//                LOGD("decoded packet size: %d\n", size);
//                LOGD("decodedFrameYUV: %02x %02x %02x %02x %02x \n", decodedFrameYUV[0],decodedFrameYUV[1],decodedFrameYUV[2],decodedFrameYUV[3],decodedFrameYUV[4]);

/*                if(NULL != yuvDumpFileHandle && NULL != decodedFrameYUV){ 
                    fwrite(decodedFrameYUV, 1, size, yuvDumpFileHandle);
                }else{
                    LOGE("file open failed");
                }
*/
                decodedFrame.yuv = decodedFrameYUV;
                decodedFrame.size = size;
                decodedFrame.ts = timestamp;

                return size;
            }else{
                return 0;
            }
        }else{
            return result;
        }
    }


    YUVFrame getYUVFrame(){
        return decodedFrame;
    }

    void deinitDecoder(){
        //    /* some codecs, such as MPEG, transmit the I and P frame with a
        //     latency of one frame. You must do the following to have a
        //     chance to get the last frame of the video */
        //    avpkt.data = NULL;
        //    avpkt.size = 0;
        //    decode_write_frame(outfilename, codecContext, frame, &frame_count, &avpkt, 1);
        //    
        //    fclose(inputFileHandle);

//        fclose(yuvDumpFileHandle);
        avcodec_close(codecContext);
        av_free(codecContext);
        av_frame_free(&frame);
    }
};

EasyAVCDecoder * easyDecoder;

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeInit(JNIEnv *env, jobject thiz, jint width, jint height){
    LOGD("init called");
    easyDecoder = new EasyAVCDecoder();
    easyDecoder->initDecoder(320, 240);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDeinit(JNIEnv *env, jobject thiz){
    LOGD("deinit called");

    easyDecoder->deinitDecoder();
    return 0;
}

JNIEXPORT jint JNICALL Java_com_rd_mirrorclient_FVideoDecoder_nativeDecode(JNIEnv *env, jobject thiz, jbyteArray inputVideoFrame, jint len, jlong ts, jbyteArray resultYUVBuffer){
    int ret=0;
    jbyte * native_inputVideoFrame = (env)->GetByteArrayElements(inputVideoFrame,0);  
    jbyte * native_resultYUVBuffer = (env)->GetByteArrayElements(resultYUVBuffer,0);  
    EasyAVCDecoder::YUVFrame result; 
    int resultSize = easyDecoder->decodeFrame((unsigned char*)native_inputVideoFrame, len, ts);
    if(0 < resultSize){
        result = easyDecoder->getYUVFrame();
        memcpy(native_resultYUVBuffer, result.yuv, result.size);
        ret = resultSize;
    }

    (env)->ReleaseByteArrayElements(inputVideoFrame, native_inputVideoFrame ,JNI_ABORT);
    (env)->ReleaseByteArrayElements(resultYUVBuffer, native_resultYUVBuffer ,JNI_ABORT);
    return ret;
}


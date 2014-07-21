#include "EasyAVCDecoder.h"
#include <android/log.h>

#define LOGTAG "FVideoDecoderNative"
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, LOGTAG, __VA_ARGS__);
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN, LOGTAG, __VA_ARGS__);
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, LOGTAG, __VA_ARGS__);

EasyAVCDecoder::EasyAVCDecoder(){
    decodedFrame.yuv=NULL;
    decodedFrame.size=0;
    decodedFrame.ts=0;
    scaler = NULL;
}

void EasyAVCDecoder::initDecoder(int width, int height, int doColorConvert){
    av_register_all();
    AVCodec *codec;
    av_init_packet(&avpkt);

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

    if(codec->capabilities&CODEC_CAP_TRUNCATED){
        codecContext->flags|= CODEC_FLAG_TRUNCATED; 
    }

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

    int ret = av_image_alloc(video_dst_data, video_dst_linesize,
            codecContext->width, codecContext->height,
            codecContext->pix_fmt, 1);
    if (ret < 0) {
        LOGE( "Could not allocate raw video buffer\n");
    }

    video_dst_bufsize = ret;
    frame_count = 0;
    
    if(doColorConvert){
        scaler = new FScaler(width, height, AV_PIX_FMT_YUV420P, width, height, AV_PIX_FMT_RGBA);
    }
    
    LOGD("linesize: %d %d %d %d buf size: %d", video_dst_linesize[0], video_dst_linesize[1], video_dst_linesize[2], video_dst_linesize[3], video_dst_bufsize);
}

int EasyAVCDecoder::decode(AVPacket pkt, uint8_t ** buf, int * size, int64_t * ts, int64_t * duration){
    int got_frame;
    int ret = 0;

    got_frame = 0;
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
        ret = 0;
    }else{
        ret = -1;
    }

    return ret;
}

int EasyAVCDecoder::decodeFrame(unsigned char * frame, int len, long ts){
    avpkt.data = frame;
    avpkt.size = len;
    avpkt.pts = ts;

    int size;
    int64_t timestamp;
    int64_t duration;
    unsigned char * decodedFrameYUV;

    int result = decode(avpkt, &decodedFrameYUV, &size, &timestamp, &duration);

    if(0 == result){
        if(NULL != decodedFrameYUV && 0 < size){
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


EasyAVCDecoder::YUVFrame EasyAVCDecoder::getYUVFrame(){
    return decodedFrame;
}

void EasyAVCDecoder::deinitDecoder(){
    avcodec_close(codecContext);
    av_free(codecContext);
    av_frame_free(&frame);

    delete scaler;
}

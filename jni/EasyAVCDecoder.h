extern "C" {
#include "libavutil/imgutils.h"
#include "libavutil/samplefmt.h"
#include "libavutil/timestamp.h"
#include "libavformat/avformat.h"
};

#include "FScaler.h"

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
    FScaler * scaler;
public:
    EasyAVCDecoder();
    void initDecoder(int width, int height, int doColorConvert);
    int decode(AVPacket pkt, uint8_t ** buf, int * size, int64_t * ts, int64_t * duration);
    int decodeFrame(unsigned char * frame, int len, long ts);
    YUVFrame getYUVFrame();
    void deinitDecoder();
};

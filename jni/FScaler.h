//
//  FScaler.h
//  MediaHandler
//
//  Created by randy on 2014. 4. 23..
//  Copyright (c) 2014년 randy. All rights reserved.
//

#ifndef __MediaHandler__FScaler__
#define __MediaHandler__FScaler__

extern "C"{
#include "libavutil/imgutils.h"
#include "libavutil/parseutils.h"
#include "libswscale/swscale.h"
}

class FScaler{
private:
    struct SwsContext *sws_ctx;
    
    int srcWidth;
    int srcHeight;
    int dstWidth;
    int dstHeight;
    int bufferSize;
    
public:
    uint8_t *dst_data[4];
    int dst_linesize[4];
    
    FScaler(int src_w, int src_h, AVPixelFormat src_pix_fmt, int dst_w, int dst_h, AVPixelFormat dst_pix_fmt);
    ~FScaler();
    
    int scale(const uint8_t *const srcSlice[], const int srcStride[],uint8_t ** buf, int * size);
};

#endif /* defined(__MediaHandler__FScaler__) */

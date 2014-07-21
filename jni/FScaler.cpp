//
//  FScaler.cpp
//  MediaHandler
//
//  Created by randy on 2014. 4. 23..
//  Copyright (c) 2014년 randy. All rights reserved.
//

#include "FScaler.h"

FScaler::FScaler(int src_w, int src_h, AVPixelFormat src_pix_fmt, int dst_w, int dst_h, AVPixelFormat dst_pix_fmt) :
    srcWidth(src_w),
    srcHeight(src_h),
    dstWidth(dst_w),
    dstHeight(dst_h)
{
        
    int ret = 0;
    sws_ctx = sws_getContext(src_w, src_h, src_pix_fmt,
                             dst_w, dst_h, dst_pix_fmt,
                             SWS_BILINEAR, NULL, NULL, NULL);
    if (!sws_ctx) {
        fprintf(stderr,
                "Impossible to create scale context for the conversion "
                "fmt:%s s:%dx%d -> fmt:%s s:%dx%d\n",
                av_get_pix_fmt_name(src_pix_fmt), src_w, src_h,
                av_get_pix_fmt_name(dst_pix_fmt), dst_w, dst_h);
    }
    
    if ((ret = av_image_alloc(dst_data, dst_linesize,
                              dst_w, dst_h, dst_pix_fmt, 1)) < 0) {
        fprintf(stderr, "Could not allocate destination image\n");
    }else{
        bufferSize = ret;
    }
}

FScaler::~FScaler(){
    av_freep(&dst_data[0]);
    sws_freeContext(sws_ctx);
}

int FScaler::scale(const uint8_t *const src_data[], const int src_linesize[], uint8_t ** buf, int * size){
    sws_scale(sws_ctx, (const uint8_t * const*)src_data, src_linesize, 0, srcHeight, dst_data, dst_linesize);
    
    *buf = dst_data[0];
    *size = bufferSize;
    
    return 0;
}
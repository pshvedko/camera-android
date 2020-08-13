//
// Created by shved@mail.ru on 29.07.20.
//

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>

#include <time.h>
#include <vpx/vp8cx.h>
#include <libyuv.h>

#include "camera.h"

#ifdef __cplusplus
extern "C" {
#endif

#define TAG "VideoEncoder"

struct video_encoder {
    struct timespec update;
    vpx_codec_ctx_t codec;
    vpx_codec_pts_t id;
    vpx_image_t image;
    jmethodID method;
};

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_video_VideoEncoder_setup(JNIEnv *env, jobject thiz,
                                                       jint width, jint height,
                                                       jint frame_rate, jint bit_rate) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct video_encoder *handler = calloc(1, sizeof(struct video_encoder));
    if (!handler) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    handler->method = (*env)->GetMethodID(env, clazz, "onFrame", "([BIJ)V");
    if (!handler->method) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    if (!vpx_img_alloc(&handler->image, VPX_IMG_FMT_I420, width, height, 64)) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    clock_gettime(CLOCK_REALTIME_COARSE, &handler->update);
    vpx_codec_enc_cfg_t cfg;
    if (vpx_codec_enc_config_default(&vpx_codec_vp8_cx_algo, &cfg, 0) != VPX_CODEC_OK) {
        vpx_img_free(&handler->image);
        free(handler);
        return throw(env, "java/lang/RuntimeException", "");
    }
    cfg.g_w = width;
    cfg.g_h = height;
    cfg.rc_target_bitrate = bit_rate;
    cfg.g_timebase.den = frame_rate;
    cfg.g_timebase.num = 1;
    cfg.g_pass = VPX_RC_ONE_PASS;
    if (vpx_codec_enc_init(&handler->codec, &vpx_codec_vp8_cx_algo, &cfg, 0) != VPX_CODEC_OK) {
        vpx_img_free(&handler->image);
        free(handler);
        return throw(env, "java/lang/RuntimeException", "");
    }
    (*env)->SetLongField(env, thiz, id, (jlong) handler);
}

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_video_VideoEncoder_encode(JNIEnv *env, jobject thiz,
                                                        jbyteArray array, jboolean key) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct video_encoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    struct timespec timestamp;
    clock_gettime(CLOCK_REALTIME_COARSE, &timestamp);
    vpx_enc_frame_flags_t flag = 0;
    if (key) {
        if (timespecseccmp(&handler->update, &timestamp, <)) {
            handler->update = timestamp;
            flag |= VPX_EFLAG_FORCE_KF;
        }
    }
    jbyte *frame = (*env)->GetByteArrayElements(env, array, NULL);
    if (ConvertToI420((unsigned char *) frame, 0,
                      handler->image.planes[VPX_PLANE_Y], handler->image.stride[VPX_PLANE_Y],
                      handler->image.planes[VPX_PLANE_U], handler->image.stride[VPX_PLANE_U],
                      handler->image.planes[VPX_PLANE_V], handler->image.stride[VPX_PLANE_V],
                      0, 0, handler->image.d_w, handler->image.d_h,
                      handler->image.stride[VPX_PLANE_Y],
                      handler->image.d_h, 0, FOURCC_NV21) == 0) {
        if (vpx_codec_encode(&handler->codec, &handler->image, handler->id++, 1, flag,
                             VPX_DL_REALTIME) == VPX_CODEC_OK) {
            vpx_codec_iter_t iterator = NULL;
            for (;;) {
                const vpx_codec_cx_pkt_t *packet = vpx_codec_get_cx_data(&handler->codec,
                                                                         &iterator);
                if (!packet)
                    break;
                else if (packet->kind != VPX_CODEC_CX_FRAME_PKT)
                    continue;
                jbyteArray bytes = (*env)->NewByteArray(env, packet->data.frame.sz);
                if (!bytes)
                    continue;
                (*env)->SetByteArrayRegion(env, bytes, 0, packet->data.frame.sz,
                                           packet->data.frame.buf);
                (*env)->CallVoidMethod(env, thiz, handler->method, bytes,
                                       packet->data.frame.flags & VPX_FRAME_FLAGS, handler->id);
                (*env)->DeleteLocalRef(env, bytes);
            }
        }
    }
    (*env)->ReleaseByteArrayElements(env, array, frame, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_video_VideoEncoder_cleanup(JNIEnv *env, jobject thiz) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct video_encoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler)
        return;
    vpx_img_free(&handler->image);
    free(handler);
}

#ifdef __cplusplus
}
#endif

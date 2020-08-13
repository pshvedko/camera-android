//
// Created by shved@mail.ru on 07.08.20.
//

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>

#include <time.h>
//#include <speex/speex.h>
#include <opus.h>

#include "camera.h"

#ifdef __cplusplus
extern "C" {
#endif

#define TAG "AudioEncoder"

struct audio_encoder {
    jmethodID method;
    OpusEncoder *codec;
    int64_t id;
    int32_t channels;
};

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioEncoder_setup(JNIEnv *env, jobject thiz,
                                                       jint sample_rate, jint channels) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_encoder *handler = calloc(1, sizeof(struct audio_encoder));
    if (!handler) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    handler->method = (*env)->GetMethodID(env, clazz, "onFrame", "([BJ)V");
    if (!handler->method) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    handler->codec = opus_encoder_create(sample_rate, channels, OPUS_APPLICATION_VOIP, NULL);
    if (!handler->codec) {
        return throw(env, "java/lang/NullPointerException", "");
    } else
        handler->channels = channels;
    (*env)->SetLongField(env, thiz, id, (jlong) handler);
}

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioEncoder_encode(JNIEnv *env, jobject thiz,
                                                        jshortArray bytes) {
    if (!bytes)
        return;
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_encoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler)
        return;
    jsize length = (*env)->GetArrayLength(env, bytes);
    if (!length)
        return;
    jshort *frame = (*env)->GetShortArrayElements(env, bytes, NULL);
    if (!frame)
        return;
    uint8_t packet[1024 * 4];
    jint size = opus_encode(handler->codec, frame, length / handler->channels,
                            packet, sizeof(packet));
    if (size > handler->channels) {
        jbyteArray sample = (*env)->NewByteArray(env, size);
        if (sample) {
            (*env)->SetByteArrayRegion(env, sample, 0, size, (jbyte *) packet);
            (*env)->CallVoidMethod(env, thiz, handler->method, sample, handler->id++);
            (*env)->DeleteLocalRef(env, sample);
        }
    }
    (*env)->ReleaseShortArrayElements(env, bytes, frame, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioEncoder_cleanup(JNIEnv *env, jobject thiz) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_encoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler)
        return;
    opus_encoder_destroy(handler->codec);
    free(handler);
}

#undef TAG

#define TAG "AudioDecoder"

struct audio_decoder {
    OpusDecoder *codec;
    jmethodID method;
    int64_t id;
    int32_t channels;
};

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioDecoder_setup(JNIEnv *env, jobject thiz,
                                                       jint sample_rate, jint channels) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_decoder *handler = calloc(1, sizeof(struct audio_decoder));
    if (!handler) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    handler->method = (*env)->GetMethodID(env, clazz, "onFrame", "([S)V");
    if (!handler->method) {
        return throw(env, "java/lang/NullPointerException", "");
    }
    handler->codec = opus_decoder_create(sample_rate, channels, NULL);
    if (!handler->codec) {
        return throw(env, "java/lang/NullPointerException", "");
    } else
        handler->channels = channels;
    (*env)->SetLongField(env, thiz, id, (jlong) handler);
}


JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioDecoder_decode(JNIEnv *env, jobject thiz,
                                                        jbyteArray bytes) {
    if (!bytes)
        return;
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_decoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler)
        return;
    jsize length = (*env)->GetArrayLength(env, bytes);
    if (!length)
        return;
    jbyte *frame = (*env)->GetByteArrayElements(env, bytes, NULL);
    if (!frame)
        return;
    int16_t packet[1024 * 16];
    jint size = opus_decode(handler->codec, (uint8_t *) frame, length, packet,
                            sizeof(packet) / handler->channels,
                            0) * handler->channels;
    if (size > 0) {
        jshortArray sample = (*env)->NewShortArray(env, size);
        if (sample) {
            (*env)->SetShortArrayRegion(env, sample, 0, size, packet);
            (*env)->CallVoidMethod(env, thiz, handler->method, sample);
            (*env)->DeleteLocalRef(env, sample);
        }
    }
    (*env)->ReleaseByteArrayElements(env, bytes, frame, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_ru_nxdomain_camera_codec_audio_AudioDecoder_cleanup(JNIEnv *env, jobject thiz) {
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    jfieldID id = (*env)->GetFieldID(env, clazz, "mHandler", "J");
    struct audio_decoder *handler = (void *) (*env)->GetLongField(env, thiz, id);
    if (!handler)
        return;
    opus_decoder_destroy(handler->codec);
    free(handler);
}

#ifdef __cplusplus
}
#endif

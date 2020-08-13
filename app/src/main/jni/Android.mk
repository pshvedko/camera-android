#
#
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

JNI_PATH := ${LOCAL_PATH}

include ${JNI_PATH}/libcamera/Android.mk
include ${JNI_PATH}/libvpx/build/make/Android.mk
include ${JNI_PATH}/libyuv/Android.mk
include ${JNI_PATH}/libopus/Android.mk
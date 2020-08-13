LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libcamera

LOCAL_SRC_FILES := hello.c video.c camera.c audio.c

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libvpx
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libyuv/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libopus/include

LOCAL_STATIC_LIBRARIES :=  libvpx libopus libyuv

LOCAL_LDLIBS := -llog -landroid -pthread

include $(BUILD_SHARED_LIBRARY)

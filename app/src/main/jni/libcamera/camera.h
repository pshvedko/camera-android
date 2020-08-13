//
// Created by shved on 29.07.20.
//

#ifndef CAMERA_CAMERA_H
#define CAMERA_CAMERA_H

#define LOGV(...)   ((void)__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__))
#define LOGD(...)   ((void)__android_log_print(ANDROID_LOG_DEBUG,   TAG, __VA_ARGS__))
#define LOGI(...)   ((void)__android_log_print(ANDROID_LOG_INFO,    TAG, __VA_ARGS__))
#define LOGW(...)   ((void)__android_log_print(ANDROID_LOG_WARN,    TAG, __VA_ARGS__))
#define LOGE(...)   ((void)__android_log_print(ANDROID_LOG_ERROR,   TAG, __VA_ARGS__))
#define LOGF(...)   ((void)__android_log_print(ANDROID_LOG_FATAL,   TAG, __VA_ARGS__))

#define timespecseccmp(a, b, op) \
    ((a)->tv_sec op (b)->tv_sec)

#define timespeccmp(a, b, op) \
    ((a)->tv_sec == (b)->tv_sec ? (a)->tv_nsec op (b)->tv_nsec : (a)->tv_sec op (b)->tv_sec)

#define timespecadd(a, b) \
    do { \
        (a)->tv_sec  += (b)->tv_sec; \
        (a)->tv_nsec += (b)->tv_nsec; \
        if ((a)->tv_nsec >= 1000000000L) { \
            (a)->tv_nsec -= 1000000000L; \
            (a)->tv_sec  += 1; \
        } \
    } while (0)

#define timespecset(a, s, n) \
    do{ \
        (a)->tv_sec = (s); \
        (a)->tv_nsec = (n); \
    } while (0)


#define VPX_FRAME_FLAGS ( VPX_FRAME_IS_KEY | VPX_FRAME_IS_DROPPABLE | VPX_FRAME_IS_INVISIBLE | VPX_FRAME_IS_FRAGMENT )

void throw(JNIEnv *, const char *, const char *);

#endif //CAMERA_CAMERA_H

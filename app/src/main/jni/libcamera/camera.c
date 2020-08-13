//
// Created by shved on 30.07.20.
//

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <jni.h>

#include "camera.h"

JNIEXPORT void throw(JNIEnv *env, const char *e, const char *m) {
    jclass c = (*env)->FindClass(env, e);
    if (c)
        (*env)->ThrowNew(env, c, m);
}

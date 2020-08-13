package ru.nxdomain.camera.codec;

public abstract class Frame<T> {
    final private T mBytes;

    public Frame(T bytes) {
        mBytes = bytes;
    }

    public T meta() {
        return null;
    }

    public T bytes() {
        return mBytes;
    }

    public void recycle() {
    }
}

package ru.nxdomain.camera.codec;

import java.util.concurrent.LinkedBlockingQueue;


public abstract class Codec<T> extends Thread {
    private final LinkedBlockingQueue<Frame<T>> mQueue = new LinkedBlockingQueue<>();

    protected abstract void codeFrame(Frame<T> frame);

    public void release() {
        interrupt();
        try {
            join();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            try {
                Frame<T> frame = mQueue.take();
                try {
                    codeFrame(frame);
                } finally {
                    frame.recycle();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        for (Frame<T> frame : mQueue) {
            frame.recycle();
        }
    }

    public void addFrame(Frame<T> frame) {
        if (isAlive() && !isInterrupted())
            mQueue.add(frame);
        else
            frame.recycle();
    }

    protected int size() {
        return mQueue.size();
    }
}

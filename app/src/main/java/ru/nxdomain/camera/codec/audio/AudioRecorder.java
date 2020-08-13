package ru.nxdomain.camera.codec.audio;

import android.media.AudioRecord;

import java.util.concurrent.LinkedBlockingDeque;

public class AudioRecorder extends Thread implements AudioFrame.AudioRecycler {
    private final Callback mCallback;
    private final AudioRecord mRecord;
    private final LinkedBlockingDeque<short[]> mBuffer;

    public interface Callback {
        void onRecordSample(short[] sample, AudioRecorder recorder);
    }

    public AudioRecorder(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, Callback callback) {
        super();
        mRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mBuffer = new LinkedBlockingDeque<>();
        mCallback = callback;
    }

    @Override
    public void run() {
        super.run();
        mRecord.startRecording();
        while (!isInterrupted()) {
            try {
                short[] buffer = mBuffer.take();
                if (read(buffer, mRecord))
                    mCallback.onRecordSample(buffer, this);
                else
                    mBuffer.add(buffer);
            } catch (InterruptedException e) {
                break;
            }
        }
        mRecord.stop();
        mRecord.release();
    }

    private boolean read(short[] buffer, AudioRecord record) {
        int offset = 0;
        int length = buffer.length;
        while (length > 0) {
            int size = record.read(buffer, offset, length);
            if (size < 0)
                return false;
            offset += size;
            length -= size;
        }
        return true;
    }

    public void release() {
        interrupt();
        try {
            join();
        } catch (InterruptedException ignored) {
        }
    }

    public void addBuffer(short[] buffer) {
        mBuffer.add(buffer);
    }
}

package ru.nxdomain.camera.codec.video;

import androidx.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import ru.nxdomain.camera.codec.Codec;
import ru.nxdomain.camera.codec.Frame;
import ru.nxdomain.camera.codec.Size;


public class CodeReader extends Codec<byte[]> {
    private int mHeight;
    private int mWidth;
    private QRCodeReader mReader;
    private Callback mCallback;

    public interface Callback {
        void onScannedCode(byte[] bytes);
    }

    public CodeReader(Size size, Callback callback) {
        super();
        mWidth = size.width;
        mHeight = size.height;
        mCallback = callback;
        mReader = new QRCodeReader();
    }

    @Override
    protected void codeFrame(@NonNull Frame<byte[]> frame) {
        LuminanceSource source = new PlanarYUVLuminanceSource(frame.bytes(), mWidth, mHeight,
                mWidth / 2 - mHeight / 2, 0, mHeight, mHeight,
                false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = mReader.decode(bitmap);
            mCallback.onScannedCode(result.getText().getBytes());
        } catch (ReaderException ignored) {
        }
    }
}

package atu.com.avdevdemo.camera;

import android.graphics.Point;
import android.graphics.SurfaceTexture;

/**
 * Created by atu on 2017/11/17.
 */

public interface IACamera {

    void open(int cameraId);

    void setPreviewTexture(SurfaceTexture texture);

    void setConfig(Config config);

    void setOnPreviewFrameCallback(PreviewFrameCallback callback);

    void preview();

    Point getPreviewSize();

    Point getPictureSize();

    boolean close();

    class Config{
        float rate=1.778f; //宽高比
        int minPreviewWidth;
        int minPictureWidth;
    }

    interface PreviewFrameCallback{
        void onPreviewFrame(byte[] bytes, int width, int height);
    }
}

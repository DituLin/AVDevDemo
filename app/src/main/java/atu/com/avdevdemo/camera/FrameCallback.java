package atu.com.avdevdemo.camera;

/**
 * Created by atu on 2017/11/17.
 */

public interface FrameCallback {

    void onFrame(byte[] bytes,long time);
}

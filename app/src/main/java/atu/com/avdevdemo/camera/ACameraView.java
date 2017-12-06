package atu.com.avdevdemo.camera;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import atu.com.avdevdemo.filter.AFilter;

/**
 * Created by atu on 2017/11/17.
 */

public class ACameraView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private CameraDrawer mDrawer;
    private ACamera mCamera;
    private boolean isSetParm=false;
    private int dataWidth=0,dataHeight=0;
    private int cameraId=0;
    private FrameCallback mFrameCallback;

    public ACameraView(Context context) {
        this(context,null);
    }

    public ACameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(true);
        setCameraDistance(100);
        mDrawer = new CameraDrawer(getResources());
        mCamera = new ACamera();
        IACamera.Config config = new IACamera.Config();
        config.minPictureWidth = 720;
        config.minPreviewWidth = 720;
        config.rate = 1.778f;
        mCamera.setConfig(config);
    }

    public void setParams(int type,int ... params){
        mDrawer.setParams(type,params);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSetParm)
            open(cameraId);
    }

    private void open(int cameraId) {
        mCamera.close();
        mCamera.open(cameraId);
        mDrawer.setCameraId(cameraId);
        Point previewSize = mCamera.getPreviewSize();
        dataWidth=previewSize.x;
        dataHeight=previewSize.y;

        for (int i = 0 ; i < 3 ; i++) {
            mCamera.addBuffer(new byte[dataWidth*dataHeight*4]);
        }
        mDrawer.setCamera(mCamera.getCamera());
        mCamera.setOnPreviewFrameCallbackWithBuffer(new ACamera.PreviewFrameCallback() {

            @Override
            public void onPreviewFrame(byte[] bytes, int width, int height) {
                //TODO 增加限制
                if(isSetParm&&mDrawer!=null){
                    mDrawer.update(bytes);
                    requestRender();
                }else{
                    mCamera.addBuffer(bytes);
                }
            }
        });
        mCamera.setPreviewTexture(mDrawer.getTexture());
        mCamera.preview();
    }

    /**
     * switch camera
     */
    public void switchCamera(){
        cameraId=cameraId==0?1:0;
        open(cameraId);
    }



    @Override
    public void onPause() {
        super.onPause();
        mCamera.close();
    }


    public void onDestroy() {
        setPreserveEGLContextOnPause(false);
        onPause();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mDrawer.onSurfaceCreated(gl,config);
        if(!isSetParm){
            open(cameraId);
            stickerInit();
        }
        mDrawer.setPreviewSize(dataWidth,dataHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDrawer.onSurfaceChanged(gl,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isSetParm) {
            mDrawer.onDrawFrame(gl);
        }
    }

    public void setFrameCallback(int width,int height,FrameCallback frameCallback){
        this.mFrameCallback=frameCallback;
        mDrawer.setFrameCallback(width,height,frameCallback);
    }

    public void startRecord(){
        mDrawer.setKeepCallback(true);
    }

    public void stopRecord(){
        mDrawer.setKeepCallback(false);
    }

    public void takePhoto(){
        mDrawer.setOneShotCallback(true);
    }


    /**
     * 增加自定义滤镜
     * @param filter   自定义滤镜
     * @param isBeforeSticker 是否增加在贴纸之前
     */
    public void addFilter(AFilter filter, boolean isBeforeSticker){
        mDrawer.addFilter(filter,isBeforeSticker);
    }

    private void stickerInit(){
        if(!isSetParm&&dataWidth>0&&dataHeight>0) {
            isSetParm = true;
//            mEffect.set(ACameraEffect.SET_IN_WIDTH,dataWidth);
//            mEffect.set(ACameraEffect.SET_IN_HEIGHT,dataHeight);
//            mEffect.setProcessCallback(mcallback);
//            mEffect.setTrackCallback(mTrackCallback);
        }
    }
}

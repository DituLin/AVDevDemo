package atu.com.avdevdemo;

import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import atu.com.avdevdemo.base.BaseActivity;
import atu.com.avdevdemo.camera.ACameraView;
import atu.com.avdevdemo.camera.FrameCallback;
import atu.com.avdevdemo.coder.CameraRecorder;
import atu.com.avdevdemo.view.CircularProgressView;
import butterknife.BindView;


public class MainActivity extends BaseActivity implements FrameCallback {

    public ExecutorService mExecutor;
    private long maxTime=20000;
    private long timeStep=50;
    private long time;
    private boolean recordFlag=false;
    private int type;       //1为拍照，0为录像
    private CameraRecorder mp4Recorder;

    @BindView(R.id.camera_view)
    ACameraView mCameraView;
    @BindView(R.id.circle_progress)
    CircularProgressView mCapture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int setLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void init(Bundle savedInstanceState) {

    }

    @Override
    protected void init() {
        initView();
    }

    private void initView() {

        mExecutor= Executors.newSingleThreadExecutor();
        mCameraView.setFrameCallback(384,640,MainActivity.this);
        mCapture.setTotal((int)maxTime);
        mCapture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        recordFlag=false;
                        time=System.currentTimeMillis();
                        mCapture.postDelayed(captureTouchRunnable,500);
                        break;
                    case MotionEvent.ACTION_UP:
                        recordFlag=false;
                        if(System.currentTimeMillis()-time<500){
                            mCapture.removeCallbacks(captureTouchRunnable);
                            mCameraView.setFrameCallback(720,1280,MainActivity.this);
                            mCameraView.takePhoto();
                        }
                        break;
                }
                return false;
            }
        });
    }



    //录像的Runnable
    private Runnable captureTouchRunnable=new Runnable() {
        @Override
        public void run() {
            recordFlag=true;
            mExecutor.execute(recordRunnable);
        }
    };

    private Runnable recordRunnable=new Runnable() {

        @Override
        public void run() {
            type=0;
            long timeCount=0;
            if(mp4Recorder==null){
                mp4Recorder=new CameraRecorder();
            }
            long time=System.currentTimeMillis();
            String savePath=getPath("video/",time+".mp4");
            mp4Recorder.setSavePath(getPath("video/",time+""),"mp4");
            try {
                mp4Recorder.prepare(384,640);
                mp4Recorder.start();
                mCameraView.setFrameCallback(384,640,MainActivity.this);
                mCameraView.startRecord();
                while (timeCount<=maxTime&&recordFlag){
                    long start=System.currentTimeMillis();
                    mCapture.setProcess((int)timeCount);
                    long end=System.currentTimeMillis();
                    try {
                        Thread.sleep(timeStep-(end-start));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeCount+=timeStep;
                }
                mCameraView.stopRecord();

                if(timeCount<2000){
                    mp4Recorder.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCapture.setProcess(0);
                            Toast.makeText(MainActivity.this,"录像时间太短了",Toast.LENGTH_SHORT).show();

                        }
                    });
                }else{
                    mp4Recorder.stop();
                    recordComplete(type,savePath);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private String getBaseFolder(){
        String baseFolder= Environment.getExternalStorageDirectory()+"/avdev/";
        File f=new File(baseFolder);
        if(!f.exists()){
            boolean b=f.mkdirs();
            if(!b){
                baseFolder=getExternalFilesDir(null).getAbsolutePath()+"/";
            }
        }
        return baseFolder;
    }

    //获取VideoPath
    private String getPath(String path,String fileName){
        String p= getBaseFolder()+path;
        File f=new File(p);
        if(!f.exists()&&!f.mkdirs()){
            return getBaseFolder()+fileName;
        }
        return p+fileName;
    }

    private void recordComplete(int type,final String path){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCapture.setProcess(0);
                Toast.makeText(MainActivity.this,"文件保存路径："+path,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }


    @Override
    public void onFrame(byte[] bytes, long time) {
        mp4Recorder.feedData(bytes,time);
    }


}

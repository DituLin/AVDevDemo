package atu.com.avdevdemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import atu.com.avdevdemo.utlis.ALoger;

import static android.media.MediaFormat.KEY_AAC_PROFILE;
import static android.media.MediaFormat.KEY_BIT_RATE;

/**
 * Created by atu on 2017/10/30.
 */

public class CameraRecorder {

    private static final String TAG = "CameraRecorder";
    private final Object LOCK=new Object();

    private MediaMuxer mMuxer;  //多路复用器，用于音视频混合
    private String path;        //文件保存的路径
    private String postfix;     //文件后缀

    private String audioMime = "audio/mp4a-latm";   //音频编码的Mime
    private int audioRate=128000;   //音频编码的密钥比特率
    private int sampleRate=48000;   //音频采样率
    private int channelCount=2;     //音频编码通道数
    private int channelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int audioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private String videoMime="video/avc";   //视频编码格式
    private int videoRate=2048000;       //视频编码波特率
    private int frameRate=24;           //视频编码帧率
    private int frameInterval=1;        //视频编码关键帧，1秒一关键帧

    private int fpsTime;
    private int width;
    private int height;
    private int colorFormat;
    private int bufferSize;
    private AudioRecord mRecorder;   //录音器
    private MediaCodec mAudioEnc;   //编码器，用于音频编码
    private MediaCodec mVideoEnc;   //编码器，用于视频编码
    private long nanoTime; //记录起始时间
    private Thread mAudioThread;
    private Thread mVideoThread;
    private boolean isRecording;
    private boolean cancelFlag;
    private boolean mStartFlag;
    private boolean hasNewData;

    private byte[] nowFeedData;
    private int mAudioTrack = -1;
    private int mVideoTrack = -1;


    public CameraRecorder() {
        fpsTime=1000/frameRate;
    }

    public void setSavePath(String path,String postfix){
        this.path=path;
        this.postfix=postfix;
    }

    public void addData(byte[] data) {
        this.hasNewData = true;
        this.nowFeedData = data;
    }

    public int preare(int width,int height) throws IOException {
        //初始化 audio
        MediaFormat aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate, channelConfig);
        aFormat.setInteger(KEY_BIT_RATE, audioRate);
//        aFormat.setInteger(KEY_MAX_INPUT_SIZE, 0);
        aFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        aFormat.setInteger(KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        try {
            mAudioEnc = MediaCodec.createEncoderByType(audioMime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAudioEnc.configure(aFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        bufferSize = AudioRecord.getMinBufferSize(sampleRate,channelConfig,audioFormat) * 2;
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,bufferSize);

        //初始化 video
        this.width = width;
        this.height = height;

        MediaCodecInfo mediaCodecInfo = selectCodec(videoMime);
        colorFormat = getColorFormat(mediaCodecInfo);
        MediaFormat vFormat = MediaFormat.createVideoFormat(videoMime,width,height);
        vFormat.setInteger(MediaFormat.KEY_BIT_RATE,videoRate);
        vFormat.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
        vFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
        vFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,colorFormat);

        try {
            mVideoEnc=MediaCodec.createEncoderByType(videoMime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mVideoEnc.configure(vFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        Bundle bundle=new Bundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE,videoRate);
            mVideoEnc.setParameters(bundle);
        }

        mMuxer = new MediaMuxer(path+"."+postfix,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        return 0;
    }


    private int getColorFormat(MediaCodecInfo mediaCodecInfo) {
        int matchedFormat = 0;
        MediaCodecInfo.CodecCapabilities codecCapabilities =
                mediaCodecInfo.getCapabilitiesForType(videoMime);
        for (int i = 0; i < codecCapabilities.colorFormats.length; i++) {
            int format = codecCapabilities.colorFormats[i];
            if (format >= codecCapabilities.COLOR_FormatYUV420Planar &&
                    format <= codecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
                if (format >= matchedFormat) {
                    matchedFormat = format;
                    logColorFormatName(format);
                    break;
                }
            }
        }
        return matchedFormat;
    }

    private void logColorFormatName(int format) {
        switch (format) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                Log.d(TAG, "COLOR_FormatYUV420Flexible");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                Log.d(TAG, "COLOR_FormatYUV420PackedPlanar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                Log.d(TAG, "COLOR_FormatYUV420Planar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                Log.d(TAG, "COLOR_FormatYUV420PackedSemiPlanar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                Log.d(TAG, "COLOR_FormatYUV420SemiPlanar");
                break;
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public int start() throws InterruptedException {
        nanoTime = System.nanoTime();
        synchronized (LOCK) {

            if (mAudioThread != null && mAudioThread.isAlive()) {
                isRecording = false;
                mAudioThread.join();
            }

            if (mVideoThread != null && mVideoThread.isAlive()) {
                mStartFlag = false;
                mVideoThread.join();
            }
            /**************** 录音开始 ************************/
            mAudioEnc.start();
            mRecorder.startRecording();
            isRecording = true;
            mAudioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!cancelFlag) {
                        if (audioStep()) {
                            break;
                        }

                    }
                }
            });
            mAudioThread.start();

            /**************** 录视频开始 ************************/
            mVideoEnc.start();
            mStartFlag = true;
            mVideoThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!cancelFlag) {
                        long time=System.currentTimeMillis();
                        if (nowFeedData != null) {
                            if (videoStep(nowFeedData)) {
                                break;
                            }
                        }
                        long lt=System.currentTimeMillis()-time;
                        if(fpsTime>lt){
                            try {
                                Thread.sleep(fpsTime-lt);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mVideoThread.start();

        }

        return 0;
    }

    public void cancle() {
        cancelFlag=true;
        stop();
        cancelFlag=false;
        File file=new File(path);
        if(file.exists()){
            file.delete();
        }
    }

    public void stop() {
        try {
            synchronized (LOCK) {
                isRecording = false;
                mAudioThread.interrupt();
                mStartFlag = false;
                mVideoThread.interrupt();
                //Audio Stop
                mRecorder.stop();
                mAudioEnc.stop();
                mAudioEnc.release();

                //Video Stop
                mVideoEnc.stop();
                mVideoEnc.release();

                //Muxer Stop
                mVideoTrack = -1;
                mAudioTrack = -1;
                mMuxer.stop();
                mMuxer.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 音频写入
     * @return
     */
    private boolean audioStep() {
        int index = mAudioEnc.dequeueInputBuffer(-1);
        if (index >= 0) {
            final ByteBuffer buffer= getInputBuffer(mAudioEnc,index);
            buffer.clear();
            int length = mRecorder.read(buffer,bufferSize);
            if (length > 0) {
                mAudioEnc.queueInputBuffer(index,0,length,(System.nanoTime()-nanoTime)/1000,isRecording?0:MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else {
                ALoger.d("no audio data");
            }
        }

        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex;
        do{
            outIndex = mAudioEnc.dequeueOutputBuffer(mInfo,0);
            if (outIndex >= 0) {
                ByteBuffer buffer = getOutputBuffer(mAudioEnc,outIndex);
                buffer.position(mInfo.offset);

                if(mAudioTrack>=0&&mVideoTrack>=0&&mInfo.size>0&&mInfo.presentationTimeUs>0){
                    try {
                        mMuxer.writeSampleData(mAudioTrack,buffer,mInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mAudioEnc.releaseOutputBuffer(outIndex,false);
                if((mInfo.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                    ALoger.d(TAG,"audio end");
                    return true;
                }

            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mAudioTrack = mMuxer.addTrack(mAudioEnc.getOutputFormat());
                if (mAudioTrack >= 0 && mVideoTrack >= 0) {
                    ALoger.d("muxer start audio....");
                    mMuxer.start();
                }
            }

        }while (outIndex >= 0);

        return false;
    }


    /**
     * 视频写入
     * @param data
     * @return
     */
    private boolean videoStep(byte[] data) {
        int index = mVideoEnc.dequeueInputBuffer(-1);
        if (index >= 0) {
            ByteBuffer buffer=getInputBuffer(mVideoEnc,index);
            buffer.clear();
            buffer.put(data,0,data.length);
            mVideoEnc.queueInputBuffer(index,0,data.length,(System.nanoTime()-nanoTime)/1000,mStartFlag?0:MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex = mVideoEnc.dequeueOutputBuffer(mInfo,0);
        do {
            if (outIndex >= 0) {
                ByteBuffer buffer = getOutputBuffer(mVideoEnc,outIndex);
                if (mAudioTrack >= 0 && mVideoTrack >= 0 && mInfo.size > 0 && mInfo.presentationTimeUs > 0) {
                    mMuxer.writeSampleData(mVideoTrack,buffer,mInfo);
                }
                mVideoEnc.releaseOutputBuffer(outIndex,false);
                outIndex = mVideoEnc.dequeueOutputBuffer(mInfo,0);
                if((mInfo.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                    ALoger.e(TAG,"video end");
                    return true;
                }
            } else if (outIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mVideoTrack = mMuxer.addTrack(mVideoEnc.getOutputFormat());
                if (mAudioTrack >= 0 && mVideoTrack >= 0) {
                    ALoger.d("muxer start video....");
                    mMuxer.start();
                }
            }

        }while (outIndex >= 0);
        return false;
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     * @param packet 要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

    private ByteBuffer getInputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec,int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        }else{
            return codec.getOutputBuffers()[index];
        }
    }


}

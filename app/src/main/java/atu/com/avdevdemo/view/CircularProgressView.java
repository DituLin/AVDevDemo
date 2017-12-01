package atu.com.avdevdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import atu.com.avdevdemo.utlis.DensityUtils;

/**
 *
 * 圆形进度条
 * Created by atu on 2017/12/1.
 */

public class CircularProgressView extends AppCompatImageView {

    private int mStroke=5;
    private int mProcess=0;
    private int mTotal=100;
    private int mNormalColor=0xFFFFFFFF;
    private int mSecondColor=0xFF00FF00;
    private int mStartAngle=-90;
    private RectF mRectF;

    private Paint mPaint;
    private Drawable mDrawable;

    public CircularProgressView(Context context) {
        this(context,null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init(){
        mStroke= DensityUtils.dp2px(getContext(),mStroke);
        mPaint=new Paint();
        mPaint.setColor(mNormalColor);
        mPaint.setStrokeWidth(mStroke);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mDrawable=new Progress();
        setImageDrawable(mDrawable);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutParams().width== ViewGroup.LayoutParams.WRAP_CONTENT){
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        }else{
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    public void setTotal(int total){
        this.mTotal=total;
        mDrawable.invalidateSelf();
    }

    public void setStroke(float dp){
        this.mStroke= DensityUtils.dp2px(getContext(),dp);
        mPaint.setStrokeWidth(mStroke);
        mDrawable.invalidateSelf();
    }

    public void setProcess(int process){
        this.mProcess=process;
        post(new Runnable() {
            @Override
            public void run() {
                mDrawable.invalidateSelf();
            }
        });
        Log.e("atu","process-->"+process);
    }

    private class Progress extends Drawable{

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width=getWidth();
            int pd=mStroke/2+1;
            if(mRectF==null){
                mRectF=new RectF(pd,pd,width-pd,width-pd);
            }
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mNormalColor);
            canvas.drawCircle(width/2,width/2,width/2-pd,mPaint);
            mPaint.setColor(mSecondColor);
            canvas.drawArc(mRectF,mStartAngle,mProcess*360/(float)mTotal,false,mPaint);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }
}

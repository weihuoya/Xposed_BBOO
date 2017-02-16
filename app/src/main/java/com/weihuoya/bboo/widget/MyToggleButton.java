package com.weihuoya.bboo.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

// facebook rebound
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;

import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;

/**
 * Created by zhangwei on 2016/6/18.
 */
public class MyToggleButton extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(MyToggleButton buttonView, boolean isChecked);
    }

    private OnCheckedChangeListener mOnCheckedChangedListener;
    private boolean mBroadcasting;
    private boolean mChecked;

    private int mWidth;
    private int mHeight;
    private float mOffsetX;
    private float mOffsetY;

    private int mBorderWidth;
    private int mBorderColor;
    private int mColorOn;
    private int mColorOff;
    private int mThumbColor;

    private Paint mPaint;

    private SpringSystem mSpringSystem;
    private Spring mSpring;
    private SimpleSpringListener mSpringListener;

    private int mAnimColor;

    private RectF mThumbRect;
    private RectF mBackgroundRect;
    private RectF mForegroundRect;

    private float mBackRadius;
    private float mForeRadius;
    private float mThumbRadius;

    public MyToggleButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyToggleButton, defStyleAttr, defStyleRes);
        mWidth = a.getDimensionPixelSize(R.styleable.MyToggleButton_my_width, 0);
        mHeight = a.getDimensionPixelSize(R.styleable.MyToggleButton_my_height, 0);
        mBorderWidth = a.getDimensionPixelSize(R.styleable.MyToggleButton_my_borderWidth, 2);
        mBorderColor = a.getColor(R.styleable.MyToggleButton_my_borderColor, Color.parseColor("#dadada"));
        mColorOn = a.getColor(R.styleable.MyToggleButton_my_colorOn, Color.parseColor("#4ebb7f"));
        mColorOff = a.getColor(R.styleable.MyToggleButton_my_colorOff, Color.parseColor("#e0e0e0"));
        mThumbColor = a.getColor(R.styleable.MyToggleButton_my_thumbColor, Color.parseColor("#ffffff"));
        a.recycle();

        mChecked = false;
        mBackgroundRect = new RectF();
        mForegroundRect = new RectF();
        mThumbRect = new RectF();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mSpringSystem = SpringSystem.create();
        mSpring = mSpringSystem.createSpring();
        mSpring.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(50, 7));
        mSpringListener = new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                tween(spring.getCurrentValue());
            }
        };

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
    }

    public MyToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MyToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyToggleButton(Context context) {
        this(context, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSpring.addListener(mSpringListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSpring.removeListener(mSpringListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        if(widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST){
            widthSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, dm);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }

        if(heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST){
            heightSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, dm);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = right - left;
        int height = bottom - top;

        mOffsetX = 0;
        mOffsetY = 0;

        if(mWidth != 0) {
            mWidth = Math.min(mWidth, width);
            mOffsetX = (width - mWidth) * 0.5f;
        }

        if(mHeight != 0) {
            mHeight = Math.min(mHeight, height);
            mOffsetY = (height - mHeight) * 0.5f;
        }

        mBackRadius = Math.min(mWidth, mHeight) * 0.5f;
        mThumbRadius = mBackRadius - mBorderWidth - mBorderWidth;
        mBackgroundRect.set(0, 0, mWidth, mHeight);

        tween(mChecked ? 1.0f : 0.0f);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.save();
        canvas.translate(mOffsetX, mOffsetY);

        mPaint.setColor(mAnimColor);
        canvas.drawRoundRect(mBackgroundRect, mBackRadius, mBackRadius, mPaint);

        mPaint.setColor(mColorOff);
        canvas.drawRoundRect(mForegroundRect, mForeRadius, mForeRadius, mPaint);

        mPaint.setColor(mThumbColor);
        canvas.drawRoundRect(mThumbRect, mThumbRadius, mThumbRadius, mPaint);

        canvas.restore();
    }

    private void tween(final double value) {
        int sr = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, Color.red(mBorderColor), Color.red(mColorOn));
        int sg = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, Color.green(mBorderColor), Color.green(mColorOn));
        int sb = (int) SpringUtil.mapValueFromRangeToRange(value, 0, 1, Color.blue(mBorderColor), Color.blue(mColorOn));
        mAnimColor = Color.rgb(clamp(sr, 0, 255), clamp(sg, 0, 255), clamp(sb, 0, 255));

        int width = mWidth;

        float thumbX = (float) SpringUtil.mapValueFromRangeToRange(
                value, 0, 1, mBackRadius + mBorderWidth + mBorderWidth, width - mBackRadius - mBorderWidth - mBorderWidth);
        mForeRadius = (float) SpringUtil.mapValueFromRangeToRange(1 - value, 0, 1, 0, mBackRadius - mBorderWidth);

        mForegroundRect.set(thumbX - mForeRadius, mBackRadius - mForeRadius, width - mBackRadius + mForeRadius, mBackRadius + mForeRadius);
        mThumbRect.set(thumbX - mThumbRadius, mBackRadius - mThumbRadius, thumbX + mThumbRadius, mBackRadius + mThumbRadius);

        postInvalidate();
    }

    private int clamp(int value, int low, int high) {
        return Math.min(Math.max(value, low), high);
    }

    public void toggle() {
        setChecked(!mChecked, true);
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked, boolean animate) {
        if(mChecked != checked) {
            float value = checked ? 1.0f : 0.0f;
            mChecked = checked;

            if(animate) {
                mSpring.setEndValue(value);
            } else {
                mSpring.setCurrentValue(value);
                tween(value);
            }

            if(!mBroadcasting) {
                mBroadcasting = true;
                if(mOnCheckedChangedListener != null) {
                    mOnCheckedChangedListener.onCheckedChanged(this, mChecked);
                }
                mBroadcasting = false;
            }
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangedListener = listener;
    }
}

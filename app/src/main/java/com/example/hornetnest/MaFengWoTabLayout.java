package com.example.hornetnest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;


import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Administrator on 2020/08/11.
 * 高仿马蜂窝曲线tab
 */
public class MaFengWoTabLayout extends HorizontalScrollView implements ViewPager.OnPageChangeListener {
    private static String TAG = "MaFengWoTabLayout";

    private Context mContext;
    //tab对应的文字区域
    private LinearLayout mTitleContainer;
    //tab对应的下划线区域
    private LinearLayout mIndicatorContainer;

    private ArrayList<String> mTitles;

    private ViewPager mViewPager;

    private int mTabCount;

    private int mCurrentTab;
    private float mCurrentPositionOffset;

    private OnTabSelectListener mListener;

    private Paint mPaint;

    // 画完整正弦路径
    private Path mPath;

    // 截取的正弦路径
    private Path mDstPath;

    // 测量Path 并截取部分的工具
    private PathMeasure mMeasure;
    // 获取整个路径长度
    private float mPathLength;

    // 获取下半部圆弧长度
    private PathMeasure mArcMeasure;

    // 获取下半部圆弧长度
    private float mArcPathLength;

    private int indicatorHeight; // 波浪线最低点和最高点差距

    private int indicatorWidth = 100; // 指示器宽度

    /** 用于绘制显示器 */
    private Rect mIndicatorRect = new Rect();

    public MaFengWoTabLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public MaFengWoTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaFengWoTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        // 下面这个属性非常非常重要
        setFillViewport(true); //设置滚动视图是否可以伸缩其内容以填充视口
        setClipChildren(false);
        setWillNotDraw(false);//重写onDraw方法,需要调用这个方法来清除flag
        init(context);
    }

    public void initPath() {
        mMeasure = new PathMeasure();
        mArcMeasure = new PathMeasure();
        mPath = new Path();
        mDstPath = new Path();
        mPaint = new Paint();
        indicatorHeight = dp2px(6);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(6);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeCap(Paint.Cap.ROUND);//圆形线帽
//        mPaint.setStrokeJoin(Paint.Join.ROUND); //实际效果就是拟合处变成了更加平滑的曲线
    }

    private void init(Context context) {
        initPath();
        View root = LayoutInflater.from(context).inflate(R.layout.layout_ma_feng_wo,this);
        mTitleContainer = root.findViewById(R.id.title_container);
        mIndicatorContainer = root.findViewById(R.id.indicator_container);
    }

    public void initData(ViewPager viewPager, String[] titles) {
        this.mViewPager = viewPager;
        this.mViewPager.removeOnPageChangeListener(this);
        this.mViewPager.addOnPageChangeListener(this);

        mTitles = new ArrayList<>();
        Collections.addAll(mTitles, titles);

//        createHornetView();
        createTextViews();
    }

    // 可以把tab的波浪线给抽出一个view，这里就没有使用
    private void createHornetView() {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        mIndicatorContainer.addView(hornetView, params);
    }

    /**
     * 创建TextViews文本集合
     */
    private void createTextViews() {
        mTitleContainer.removeAllViews();
        this.mTabCount = mTitles == null ? mViewPager.getAdapter().getCount() : mTitles.size();

        View tabView;
        for (int i = 0; i < mTabCount; i++) {
            tabView = View.inflate(mContext, R.layout.layout_tab, null);
            CharSequence pageTitle = mTitles == null ? mViewPager.getAdapter().getPageTitle(i) : mTitles.get(i);
            addTab(i, pageTitle.toString(), tabView);
        }
        updateTabStyles();
    }

    /**
     * 创建并添加tab
     */
    private void addTab(final int position, String title, View tabView) {
        TextView tv_tab_title = (TextView) tabView.findViewById(R.id.tv_tab_title);
        if (tv_tab_title != null) {
            if (title != null) tv_tab_title.setText(title);
        }

        tabView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mTitleContainer.indexOfChild(v);
                if (position != -1) {
                    if (mViewPager.getCurrentItem() != position) {
                        mViewPager.setCurrentItem(position);//点击每一项进行切换
                        if (mListener != null) {
                            mListener.onTabSelect(position);
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onTabReselect(position);
                        }
                    }
                }
            }
        });

        /** 每一个Tab的布局参数 */
        LinearLayout.LayoutParams lp_tab = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

        // 添加tabView
        mTitleContainer.addView(tabView, position, lp_tab);
    }

    private void updateTabStyles() {
        for (int i = 0; i < mTabCount; i++) {
            View v = mTitleContainer.getChildAt(i);
            TextView tv_tab_title = (TextView) v.findViewById(R.id.tv_tab_title);
            if (tv_tab_title != null) {
                tv_tab_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp2px(14));
            }
        }
    }

    public void updateView() {
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPath.reset();
        mDstPath.reset();
        int height = getHeight();
        Log.i(TAG, "高度= " + height);

        //圆角矩形
        // 正弦曲线的顶部和底部横线，做标准
//        canvas.drawLine(0, height - indicatorHeight, 1000, height - indicatorHeight, mPaint);
//        canvas.drawLine(0, height, 1000, height, mPaint);

        int distance, lastDistance = 0;
        int indicatorTop = getHeight() - indicatorHeight;
        int indicatorBottom = getHeight();
        int startY = (indicatorBottom - indicatorTop) / 2 + indicatorTop;

        for (int i = 0; i < this.mTabCount; i++) {
            View currentTabView = mTitleContainer.getChildAt(i);
            float left = currentTabView.getLeft();
            float right = currentTabView.getRight();
            // 表示当前指示器与tab的偏移量
            distance = (int) ((right - left - indicatorWidth) / 2);
            Log.i(TAG, "left = " + left);
            Log.i(TAG, "right = " + right);
            if (i == 0) {
                mPath.moveTo(left + distance, startY);
                // 开始画指示器下半部分弧线
                mPath.rQuadTo(indicatorWidth / 2, indicatorHeight / 2, indicatorWidth, 0);

                // 获取一个指示器的长度，因为不是直线，不能直接用indicatorWidth来表示真实的长度
                mArcMeasure.setPath(mPath, false);
                mArcPathLength = mArcMeasure.getLength();
                Log.i(TAG, "一段圆弧的长度 mArcPathLength = " + mArcPathLength);
            } else {
                int nextX = (distance + lastDistance) / 2;
                // 这里画上半部分弧线，因为这个弧线被拉的比较长，所以没有除以2
                mPath.rQuadTo(nextX, -indicatorHeight, distance + lastDistance, 0);
                // 再继续画指示器下半部分弧线
                mPath.rQuadTo(indicatorWidth / 2, indicatorHeight / 2, indicatorWidth, 0);
            }
            lastDistance = distance;
        }
        mPaint.setColor(Color.RED);
        // 完整的正弦曲线，背景底色
        canvas.drawPath(mPath, mPaint);

        // 测量路径的长度
        mMeasure.setPath(mPath, false);
        mPathLength = mMeasure.getLength();
        Log.i(TAG, "mPathLength = " + mPathLength);

        // 计算偏移量
        View currentTabView = mTitleContainer.getChildAt(this.mCurrentTab);
        float left = currentTabView.getLeft();
        float right = currentTabView.getRight();
        float margin = (right - left - indicatorWidth) / 2;
        // 不是最后一个
        if (this.mCurrentTab < this.mTabCount - 1) {
            View nextTabView = mTitleContainer.getChildAt(this.mCurrentTab + 1);
            float nextTabLeft = nextTabView.getLeft();
            float nextTabRight = nextTabView.getRight();

            left = left + mCurrentPositionOffset * (nextTabLeft - left);
            right = right + mCurrentPositionOffset * (nextTabRight - right);
            float nextMargin = (nextTabRight - nextTabLeft - indicatorWidth) / 2;
            margin = margin + mCurrentPositionOffset * (nextMargin - margin);
        }
        mIndicatorRect.left = (int) (left + margin);
        mIndicatorRect.right = (int) (right - margin);

        float startD = 0;

        // 这种计算方式，是在保证每一个tab的宽度相等、而且每一个指示器长度也相等的情况下
        float cycleLength = (mPathLength - mArcPathLength )/ (this.mTabCount - 1); // 一个周期长度
        Log.i(TAG, "cycleLength = " + cycleLength);
        startD = this.mCurrentTab * cycleLength + cycleLength * this.mCurrentPositionOffset;

        if(startD < 0) {
            startD = 0;
        }
        Log.i(TAG, "startD = " + startD);
        mMeasure.getSegment(startD, startD + mArcPathLength, mDstPath, true);
        mPaint.setColor(Color.BLUE);
        canvas.drawPath(mDstPath, mPaint);
    }

    public void setOnTabSelectListener(OnTabSelectListener listener) {
        this.mListener = listener;
    }

    protected int sp2px(float sp) {
        final float scale = this.mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scale + 0.5f);
    }

    protected int dp2px(float dpValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        Log.i(TAG, "position = " + position);

        this.mCurrentTab = position;
        this.mCurrentPositionOffset = positionOffset;
//        scrollToCurrentTab();
        invalidate();
    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}

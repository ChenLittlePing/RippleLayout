package com.chenlittleping.ripplelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import androidx.annotation.RequiresApi;

import static android.graphics.Canvas.ALL_SAVE_FLAG;

/**
 * 水波纹选中布局
 *
 * @author Chen Xiaoping (562818444@qq.com)
 */
public class RippleLayout extends FrameLayout {

    // 底色画笔
    private Paint normalPaint = new Paint();

    // 水波纹画笔
    private Paint ripplePaint = new Paint();

    // 阴影画笔
    private Paint shadowPaint = new Paint();

    // 最大水波纹半径
    private float longestRadius = 0f;

    // 当前水波纹半径
    private float curRadius = 0f;

    // 裁剪框
    private Path clipPath = new Path();

    // 阴影大小
    private RectF shadowRect = new RectF();

    // 选中状态： 0 未选中， 1 选中
    private int state = 0;

    // 水波纹中心
    private PointF center = new PointF(0f, 0f);

    // 波纹动画插值器
    private Scroller scroller;

    // 默认的padding，留出位置绘制阴影
    private float shadowSpace = 15f;

    // 圆角
    private float radius = 20f;

    // 背景色
    private int normalColor = Color.WHITE;

    // 水波纹颜色
    private int rippleColor = Color.parseColor("#FF2889c3");

    // 阴影颜色
    private int shadowColor = Color.GRAY;

    private IRippleStateChange stateListener;

    public RippleLayout(Context context) {
        super(context);
        init(context, null);
    }

    public RippleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RippleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RippleLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        getXmlAttrs(attrs);

        scroller = new Scroller(context, new DecelerateInterpolator(3f));

        ripplePaint.setColor(rippleColor);
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setAntiAlias(true);

        normalPaint.setColor(normalColor);
        normalPaint.setStyle(Paint.Style.FILL);
        normalPaint.setAntiAlias(true);

        shadowPaint.setColor(Color.TRANSPARENT);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setAntiAlias(true);

        //设置阴影，如果最有的参数color为不透明的，则透明度由shadowPaint的alpha决定
        shadowPaint.setShadowLayer(shadowSpace /5f*4f, 0f, 0f, shadowColor);

        setPadding((int) (shadowSpace + getPaddingLeft()), (int) (shadowSpace + getPaddingTop()),
                (int) (shadowSpace + getPaddingRight()), (int) (shadowSpace + getPaddingBottom()));
    }

    private void getXmlAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ripple_layout);
            rippleColor = ta.getColor(R.styleable.ripple_layout_ripple_color, rippleColor);
            shadowSpace = ta.getDimension(R.styleable.ripple_layout_shadow_space, shadowSpace);
            shadowColor = ta.getColor(R.styleable.ripple_layout_shadow_color, shadowColor);
            normalColor = ta.getColor(R.styleable.ripple_layout_def_bg, normalColor);
            radius = ta.getDimension(R.styleable.ripple_layout_radius, radius);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        shadowRect.set(shadowSpace, shadowSpace, w - shadowSpace, h - shadowSpace);
        clipPath.addRoundRect(shadowRect, radius, radius , Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // 绘制阴影
        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);

        // 新建一个画布层
        int layerId = canvas.saveLayer(shadowRect, null, ALL_SAVE_FLAG);

        // 裁剪 这种方式圆角出现锯齿，更改为下面的颜色混合模式
//        canvas?.clipPath(clipPath)

        // 绘制默认背景色
        canvas.drawPath(clipPath, normalPaint);

        // 设置裁剪模式
        ripplePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        // 绘制水波纹
        canvas.drawCircle(center.x, center.y, curRadius, ripplePaint);

        // 取消裁剪模式
        ripplePaint.setXfermode(null);

        // 将画布绘制到canvas上
        canvas.restoreToCount(layerId);

        super.dispatchDraw(canvas);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            updateChangingArgs();
        } else {
            stopChanging();
        }
    }

    private void updateChangingArgs() {
        curRadius = scroller.getCurrX();
        int tmp = (int) (curRadius / longestRadius * 255);

        if (state == 0) {// 提前隐藏，过渡比较自然
            tmp -= 60;
        }

        if (tmp < 0) tmp = 0;
        if (tmp > 255) tmp = 255;

        ripplePaint.setAlpha(tmp);
        shadowPaint.setAlpha(tmp);

        invalidate();
        if (stateListener != null) {
            stateListener.onRippleChanging(state == 1, Math.abs(curRadius / longestRadius));
        }
    }

    private void stopChanging() {
        center.x = getWidth() / 2f;
        center.y = getHeight() / 2f;

        if (stateListener != null) {
            stateListener.onRippleChanged(state == 1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            center.x = event.getX();
            center.y = event.getY();
            if (state == 0) {
                state = 1;
                expandRipple();
            } else {
                state = 0;
                shrinkRipple();
            }
        }
        return super.onTouchEvent(event);
    }

    private void expandRipple() {
        longestRadius = getLongestRadius();
        scroller.startScroll(0, 0, (int) Math.ceil(longestRadius), 0, 1200);
        invalidate();
        if (stateListener != null) {
            stateListener.onRippleChangeStart(state == 1);
        }
    }

    private void shrinkRipple() {
        scroller.forceFinished(false);
        longestRadius = curRadius;
        scroller.startScroll((int)curRadius, 0, (int)-curRadius, 0, 800);
        invalidate();
        if (stateListener != null) {
            stateListener.onRippleChangeStart(state == 1);
        }
    }

    private float getLongestRadius() {
        if (center.x > getWidth() / 2f) {
            // 计算触摸点到左边两个顶点的距离
            float leftTop = (float) Math.sqrt(Math.pow(center.x, 2f) + Math.pow(center.y, 2f));
            float leftBottom = (float) Math.sqrt(Math.pow(center.x, 2f) + Math.pow((getHeight() - center.y), 2f));
            return leftTop > leftBottom? leftTop : leftBottom;
        } else {
            // 计算触摸点到右边两个顶点的距离
            float rightTop = (float) Math.sqrt(Math.pow(getWidth() - center.x, 2f) + Math.pow(center.y, 2f));
            float rightBottom = (float) Math.sqrt(Math.pow(getWidth() - center.x, 2f) + Math.pow(getHeight() - center.y, 2f));
            return rightTop > rightBottom? rightTop : rightBottom;
        }
    }

    public void changeState() {
        if (state == 0) {
            check();
        } else {
            unCheck();
        }
    }

    public void check() {
        if (state == 1) return;
        state = 1;
        center.x = getWidth() / 2f;
        center.y = getHeight() / 2f;
        expandRipple();
    }

    public void unCheck() {
        if (state == 0) return;
        state = 0;
        shrinkRipple();
    }

    public void setStateListener(IRippleStateChange l) {
        this.stateListener = l;
    }

    public interface IRippleStateChange {
        void onRippleChangeStart(boolean selected);
        void onRippleChanging(boolean selected, float percent);
        void onRippleChanged(boolean selected);
    }
}

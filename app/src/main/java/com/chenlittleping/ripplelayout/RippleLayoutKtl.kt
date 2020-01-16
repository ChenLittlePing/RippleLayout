package com.chenlittleping.ripplelayout

import android.content.Context
import android.graphics.*
import android.graphics.Canvas.ALL_SAVE_FLAG
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * 水波纹选中布局
 *
 * @author Chen Xiaoping (562818444@qq.com)
 *
 */
class RippleLayoutKtl: FrameLayout {

    // 底色画笔
    private val normalPaint = Paint()

    // 水波纹画笔
    private val ripplePaint = Paint()

    // 阴影画笔
    private val shadowPaint = Paint()

    // 最大水波纹半径
    private var longestRadius = 0f

    // 当前水波纹半径
    private var curRadius = 0f

    // 裁剪框
    private val clipPath = Path()

    // 阴影大小
    private val shadowRect = RectF()

    // 选中状态： 0 未选中， 1 选中
    private var state = 0

    // 正在绘制
    private var drawing = false

    // 水波纹中心
    private var center = PointF(0f, 0f)

    // 波纹动画插值器
    private lateinit var scroller: Scroller

    // 默认的padding，留出位置绘制阴影
    private var shadowSpace = 20f

    // 圆角
    private var radius = 20f

    // 背景色
    private var normalColor = Color.WHITE

    // 水波纹颜色
    private var rippleColor = Color.parseColor("#FF2889c3")

    // 阴影颜色
    private var shadowColor = Color.GRAY

    private var stateListener: IRippleStateChange? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        getXmlAttrs(attrs)

        scroller = Scroller(context, DecelerateInterpolator(3f))

        ripplePaint.color = rippleColor
        ripplePaint.style = Paint.Style.FILL
        ripplePaint.isAntiAlias = true

        normalPaint.color = normalColor
        normalPaint.style = Paint.Style.FILL
        normalPaint.isAntiAlias = true

        shadowPaint.color = Color.TRANSPARENT
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.isAntiAlias = true

        //设置阴影，如果最有的参数color为不透明的，则透明度由shadowPaint的alpha决定
        shadowPaint.setShadowLayer(shadowSpace/5f*4f, 0f, 0f, shadowColor)

        setPadding((shadowSpace + paddingLeft).toInt(), (shadowSpace + paddingTop).toInt(),
            (shadowSpace + paddingRight).toInt(), (shadowSpace + paddingBottom).toInt())
    }

    private fun getXmlAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ripple_layout)
            rippleColor = ta.getColor(R.styleable.ripple_layout_ripple_color, rippleColor)
            shadowSpace = ta.getDimension(R.styleable.ripple_layout_shadow_space, shadowSpace)
            shadowColor = ta.getColor(R.styleable.ripple_layout_shadow_color, shadowColor)
            normalColor = ta.getColor(R.styleable.ripple_layout_def_bg, normalColor)
            radius = ta.getDimension(R.styleable.ripple_layout_radius, radius)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shadowRect.set(shadowSpace, shadowSpace, w - shadowSpace, h - shadowSpace)
        clipPath.addRoundRect(shadowRect, radius, radius , Path.Direction.CW)
    }

    override fun dispatchDraw(canvas: Canvas) {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 绘制阴影
        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

        // 新建一个画布层
        val layerId = canvas.saveLayer(shadowRect, null, ALL_SAVE_FLAG)

        // 裁剪 这种方式圆角出现锯齿，更改为下面的颜色混合模式
//        canvas?.clipPath(clipPath)

        // 绘制默认背景色
        canvas.drawPath(clipPath, normalPaint)

        // 设置裁剪模式
        ripplePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        // 绘制水波纹
        canvas.drawCircle(center.x, center.y, curRadius, ripplePaint)

        // 取消裁剪模式
        ripplePaint.xfermode = null

        // 将画布绘制到canvas上
        canvas.restoreToCount(layerId)

        super.dispatchDraw(canvas)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            updateChangingArgs()
        } else {
            stopChanging()
        }
    }

    private fun updateChangingArgs() {
        curRadius = scroller.currX.toFloat()
        var tmp = (curRadius / longestRadius * 255).toInt()

        if (state == 0) {// 提前隐藏，过渡比较自然
            tmp -= 60
        }

        if (tmp < 0) tmp = 0
        if (tmp > 255) tmp = 255

        ripplePaint.alpha = tmp
        shadowPaint.alpha = tmp

        invalidate()

        stateListener?.onRippleChanging(state == 1, abs(curRadius / longestRadius))
    }

    private fun stopChanging() {
        drawing = false
        center.x = width.toFloat() / 2
        center.y = height.toFloat() / 2

        stateListener?.onRippleChanged(state == 1)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            center.x = event.x
            center.y = event.y
            if (state == 0) {
                state = 1
                expandRipple()
            } else {
                state = 0
                shrinkRipple()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun expandRipple() {
        drawing = true
        longestRadius = getLongestRadius()
        scroller.startScroll(0, 0, ceil(longestRadius).toInt(), 0, 1200)
        invalidate()

        stateListener?.onRippleChangeStart(state == 1)
    }

    private fun shrinkRipple() {
        scroller.forceFinished(false)
        longestRadius = curRadius
        scroller.startScroll(curRadius.toInt(), 0, -curRadius.toInt(), 0, 800)
        drawing = true
        invalidate()

        stateListener?.onRippleChangeStart(state == 1)
    }

    private fun getLongestRadius() : Float {
        return if (center.x > width / 2f) {
            // 计算触摸点到左边两个顶点的距离
            val leftTop = sqrt(center.x.pow(2f) + center.y.pow(2f))
            val leftBottom = sqrt(center.x.pow(2f) + (height - center.y).pow(2f))
            if (leftTop > leftBottom) leftTop else leftBottom
        } else {
            // 计算触摸点到右边两个顶点的距离
            val rightTop = sqrt((width - center.x).pow(2f) + center.y.pow(2f))
            val rightBottom = sqrt((width - center.x).pow(2f) + (height - center.y).pow(2f))
            if (rightTop > rightBottom) rightTop else rightBottom
        }.toFloat()
    }

    fun changeState() {
        if (state == 0) {
            check()
        } else {
            unCheck()
        }
    }

    fun check() {
        if (state == 1) return
        state = 1
        center.x = width / 2f
        center.y = height / 2f
        expandRipple()
    }

    fun unCheck() {
        if (state == 0) return
        state = 0
        shrinkRipple()
    }

    fun setStateListener(l: IRippleStateChange) {
        this.stateListener = l
    }

    interface IRippleStateChange {
        fun onRippleChangeStart(selected: Boolean)
        fun onRippleChanging(selected: Boolean, percent: Float)
        fun onRippleChanged(selected: Boolean)
    }
}
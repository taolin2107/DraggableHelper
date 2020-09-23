package me.taolin.demo

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * @author taolin
 * @version v1.0
 * @date 2020/07/16
 * @description 全局可拖动view的helper类
 */
class DraggableHelper(context: Context) {

    // 指定可拖动的view
    private var draggableView: View? = null

    // view可拖动的区域
    private var displayRect = Rect()

    // view的初始坐标
    private var initPosition = Point()

    // 是否自动吸附到屏幕边缘
    private var autoFlipToEdge = true

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParam = WindowManager.LayoutParams()

    private var startX = 0f
    private var startY = 0f
    private var startRawX = 0f
    private var startRawY = 0f

    private var screenWidth = 0
    private var screenHeight = 0

    private var animator: ValueAnimator? = null

    init {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        screenWidth = size.x
        screenHeight = size.y
    }

    fun setDraggableView(view: View?): DraggableHelper {
        draggableView = view
        return this
    }

    // 设置view可拖动的区域
    fun setDisplayRect(rect: Rect): DraggableHelper {
        displayRect.set(rect)
        return this
    }

    // 设置view的初始坐标
    fun setInitPosition(x: Int, y: Int): DraggableHelper {
        initPosition.x = x
        initPosition.y = y
        return this
    }

    // 设置是否自动吸附到屏幕边缘
    fun setAutoFlipToEdge(autoFlip: Boolean): DraggableHelper {
        autoFlipToEdge = autoFlip
        return this
    }

    @SuppressLint("ClickableViewAccessibility")
    fun start() {
        showToolbar()
        // 设置view可以拖动
        draggableView?.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (autoFlipToEdge) {
                        stopFlip()
                    }
                    startX = event.x
                    startY = event.y
                    startRawX = event.rawX
                    startRawY = event.rawY
                    draggableView?.isPressed = true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParam.x = (event.rawX - startX).toInt()
                    layoutParam.y = (event.rawY - startY).toInt()
                    // 限制X坐标不能超出区域
                    if (layoutParam.x < displayRect.left) {
                        layoutParam.x = displayRect.left
                    } else if (layoutParam.x + draggableView!!.width > displayRect.right) {
                        layoutParam.x = displayRect.right - draggableView!!.width
                    }
                    // 限制Y坐标不能超出区域
                    if (layoutParam.y < displayRect.top) {
                        layoutParam.y = displayRect.top
                    } else if (layoutParam.y + draggableView!!.height > displayRect.bottom) {
                        layoutParam.y = displayRect.bottom - draggableView!!.height
                    }
                    windowManager.updateViewLayout(draggableView, layoutParam)
                }

                MotionEvent.ACTION_UP -> {
                    val moveX = event.rawX - startRawX
                    val moveY = event.rawY - startRawY
                    // 当移动区域不超过这个范围时，作为点击事件处理
                    if (abs(moveX) < 5 && abs(moveY) < 5) {
                        draggableView?.performClick()
                    }
                    if (autoFlipToEdge) {
                        flipToEdge()
                    }
                    draggableView?.isPressed = false
                }

                // 拖动结束后，需要贴到屏幕边缘
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    if (autoFlipToEdge) {
                        flipToEdge()
                    }
                    draggableView?.isPressed = false
                }
            }
            true
        }
    }

    fun stop() {
        removeToolbar()
    }

    // 自动贴到屏幕边缘，带动画效果
    private fun flipToEdge() {
        val endX = if ((layoutParam.x + draggableView!!.width / 2) < screenWidth / 2) {
            displayRect.left
        } else {
            displayRect.right - draggableView!!.width
        }
        animator = ValueAnimator.ofInt(layoutParam.x, endX)
        animator?.interpolator = AccelerateDecelerateInterpolator()
        animator?.duration = sqrt(abs(endX - layoutParam.x) * 150f).toLong()
        animator?.addUpdateListener {
            layoutParam.x = it.animatedValue as Int
            windowManager.updateViewLayout(draggableView, layoutParam)
        }
        animator?.start()
    }

    private fun stopFlip() {
        animator?.removeAllUpdateListeners()
        animator?.end()
    }

    // 显示全局浮动view
    private fun showToolbar() {
        layoutParam.type = WindowManager.LayoutParams.TYPE_APPLICATION
        layoutParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParam.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParam.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParam.gravity = Gravity.START or Gravity.TOP
        layoutParam.format = PixelFormat.RGBA_8888
        layoutParam.x = initPosition.x
        layoutParam.y = initPosition.y

        windowManager.addView(draggableView, layoutParam)
    }

    // 移除全局浮动view
    private fun removeToolbar() {
        windowManager.removeView(draggableView)
    }
}

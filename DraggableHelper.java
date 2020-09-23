package me.taolin.demo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * @author taolin
 * @version v1.0
 * @date 2020/07/16
 * @description 全局可拖动view的helper类
 */
public class DraggableHelper {

    // 指定可拖动的view
    private View draggableView = null;

    // view可拖动的区域
    private Rect displayRect = new Rect();

    // view的初始坐标
    private Point initPosition = new Point();

    // 是否自动吸附到屏幕边缘
    private boolean autoFlipToEdge = true;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParam = new WindowManager.LayoutParams();

    private float startX = 0f;
    private float startY = 0f;
    private float startRawX = 0f;
    private float startRawY = 0f;

    private int screenWidth = 0;
    private int screenHeight = 0;

    private ValueAnimator animator = null;

    public DraggableHelper(Context context) {
        Point size = new Point();
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    public DraggableHelper setDraggableView(View view) {
        draggableView = view;
        return this;
    }

    // 设置view可拖动的区域
    public DraggableHelper setDisplayRect(Rect rect) {
        displayRect.set(rect);
        return this;
    }

    // 设置view的初始坐标
    public DraggableHelper setInitPosition(int x, int y) {
        initPosition.x = x;
        initPosition.y = y;
        return this;
    }

    // 设置是否自动吸附到屏幕边缘
    public DraggableHelper setAutoFlipToEdge(boolean autoFlip) {
        autoFlipToEdge = autoFlip;
        return this;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void start() {
        showToolbar();
        // 设置view可以拖动
        draggableView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        if (autoFlipToEdge) {
                            stopFlip();
                        }
                        startX = event.getX();
                        startY = event.getY();
                        startRawX = event.getRawX();
                        startRawY = event.getRawY();
                        draggableView.setPressed(true);
                    }

                    case MotionEvent.ACTION_MOVE: {
                        layoutParam.x = (int) (event.getRawX() - startX);
                        layoutParam.y = (int) (event.getRawY() - startY);
                        // 限制X坐标不能超出区域
                        if (layoutParam.x < displayRect.left) {
                            layoutParam.x = displayRect.left;
                        } else if (layoutParam.x + draggableView.getWidth() > displayRect.right) {
                            layoutParam.x = displayRect.right - draggableView.getWidth();
                        }
                        // 限制Y坐标不能超出区域
                        if (layoutParam.y < displayRect.top) {
                            layoutParam.y = displayRect.top;
                        } else if (layoutParam.y + draggableView.getHeight() > displayRect.bottom) {
                            layoutParam.y = displayRect.bottom - draggableView.getHeight();
                        }
                        windowManager.updateViewLayout(draggableView, layoutParam);
                    }

                    case MotionEvent.ACTION_UP: {
                        int moveX = (int) (event.getRawX() - startRawX);
                        int moveY = (int) (event.getRawY() - startRawY);
                        // 当移动区域不超过这个范围时，作为点击事件处理
                        if (Math.abs(moveX) < 5 && Math.abs(moveY) < 5) {
                            draggableView.performClick();
                        }
                        if (autoFlipToEdge) {
                            flipToEdge();
                        }
                        draggableView.setPressed(false);
                    }

                    // 拖动结束后，需要贴到屏幕边缘
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        if (autoFlipToEdge) {
                            flipToEdge();
                        }
                        draggableView.setPressed(false);
                    }
                }
                return true;
            }
        });
    }

    public void stop() {
        removeToolbar();
    }

    // 自动贴到屏幕边缘，带动画效果
    private void flipToEdge() {
        int endX;
        if ((layoutParam.x + draggableView.getWidth() / 2) < screenWidth / 2) {
            endX = displayRect.left;
        } else {
            endX = displayRect.right - draggableView.getWidth();
        }
        animator = ValueAnimator.ofInt(layoutParam.x, endX);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration((long) Math.sqrt((Math.abs(endX - layoutParam.x) * 150f)));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                layoutParam.x = (int) animation.getAnimatedValue();
                windowManager.updateViewLayout(draggableView, layoutParam);
            }
        });
        animator.start();
    }

    private void stopFlip() {
        animator.removeAllUpdateListeners();
        animator.end();
    }

    // 显示全局浮动view
    private void showToolbar() {
        layoutParam.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParam.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParam.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParam.gravity = Gravity.START | Gravity.TOP;
        layoutParam.format = PixelFormat.RGBA_8888;
        layoutParam.x = initPosition.x;
        layoutParam.y = initPosition.y;

        windowManager.addView(draggableView, layoutParam);
    }

    // 移除全局浮动view
    private void removeToolbar() {
        windowManager.removeView(draggableView);
    }
}

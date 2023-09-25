package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * An image view that allows zooming the image inside
 * credits: https://stackoverflow.com/posts/6650473
 */
class ZoomableImageView(context: Context, attr: AttributeSet?) : AppCompatImageView(context, attr) {
    var bitmapMatrix = Matrix()
    var mode = NONE
    var last = PointF()
    var start = PointF()
    var minScale = 1f
    var maxScale = 4f
    var m: FloatArray
    var redundantXSpace = 0f
    var redundantYSpace = 0f
    var width = 0f
    var height = 0f
    var saveScale = 1f
    var right = 0f
    var bottom = 0f
    var origWidth = 0f
    var origHeight = 0f
    private var bmWidth = 0f
    private var bmHeight = 0f
    private var mScaleDetector: ScaleGestureDetector

    init {
        super.setClickable(true)

        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        bitmapMatrix.setTranslate(1f, 1f)
        m = FloatArray(9)
        imageMatrix = bitmapMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener { _, event ->
            mScaleDetector.onTouchEvent(event)
            bitmapMatrix.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]
            val curr = PointF(event.x, event.y)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    last[event.x] = event.y
                    start.set(last)
                    mode = DRAG
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    last[event.x] = event.y
                    start.set(last)
                    mode = ZOOM
                }

                MotionEvent.ACTION_MOVE ->                         //if the mode is ZOOM or
                    //if the mode is DRAG and already zoomed
                    if (mode == ZOOM || mode == DRAG && saveScale > minScale) {
                        var deltaX = curr.x - last.x // x difference
                        var deltaY = curr.y - last.y // y difference
                        val scaleWidth = (origWidth * saveScale).roundToInt()
                            .toFloat() // width after applying current scale
                        val scaleHeight = (origHeight * saveScale).roundToInt()
                            .toFloat() // height after applying current scale
                        //if scaleWidth is smaller than the views width
                        //in other words if the image width fits in the view
                        //limit left and right movement
                        if (scaleWidth < width) {
                            deltaX = 0f
                            if (y + deltaY > 0) deltaY = -y else if (y + deltaY < -bottom) deltaY =
                                -(y + bottom)
                        } else if (scaleHeight < height) {
                            deltaY = 0f
                            if (x + deltaX > 0) deltaX = -x else if (x + deltaX < -right) deltaX =
                                -(x + right)
                        } else {
                            if (x + deltaX > 0) deltaX = -x else if (x + deltaX < -right) deltaX =
                                -(x + right)
                            if (y + deltaY > 0) deltaY = -y else if (y + deltaY < -bottom) deltaY =
                                -(y + bottom)
                        }
                        //move the image with the matrix
                        bitmapMatrix.postTranslate(deltaX, deltaY)
                        //set the last touch location to the current
                        last[curr.x] = curr.y
                    }

                MotionEvent.ACTION_UP -> {
                    mode = NONE
                    val xDiff = abs(curr.x - start.x).toInt()
                    val yDiff = abs(curr.y - start.y).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()
                }

                MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }
            imageMatrix = bitmapMatrix
            invalidate()
            true
        }
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        bmWidth = bm.width.toFloat()
        bmHeight = bm.height.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        // Fit to screen.
        val scaleX = width / bmWidth
        val scaleY = height / bmHeight
        val scale = scaleX.coerceAtMost(scaleY)
        bitmapMatrix.setScale(scale, scale)
        imageMatrix = bitmapMatrix
        saveScale = 1f

        // Center the image
        redundantYSpace = height - scale * bmHeight
        redundantXSpace = width - scale * bmWidth
        redundantYSpace /= 2f
        redundantXSpace /= 2f
        bitmapMatrix.postTranslate(redundantXSpace, redundantYSpace)
        origWidth = width - 2 * redundantXSpace
        origHeight = height - 2 * redundantYSpace
        right = width * saveScale - width - 2 * redundantXSpace * saveScale
        bottom = height * saveScale - height - 2 * redundantYSpace * saveScale
        imageMatrix = bitmapMatrix
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }
            right = width * saveScale - width - 2 * redundantXSpace * saveScale
            bottom = height * saveScale - height - 2 * redundantYSpace * saveScale
            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                bitmapMatrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2)
                if (mScaleFactor < 1) {
                    bitmapMatrix.getValues(m)
                    val x = m[Matrix.MTRANS_X]
                    val y = m[Matrix.MTRANS_Y]

                    if ((origWidth * saveScale).roundToInt() < width) {
                        if (y < -bottom) bitmapMatrix.postTranslate(
                            0f,
                            -(y + bottom)
                        ) else if (y > 0) bitmapMatrix.postTranslate(0f, -y)
                    } else {
                        if (x < -right) bitmapMatrix.postTranslate(
                            -(x + right),
                            0f
                        ) else if (x > 0) bitmapMatrix.postTranslate(-x, 0f)
                    }
                }
            } else {
                bitmapMatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                bitmapMatrix.getValues(m)
                val x = m[Matrix.MTRANS_X]
                val y = m[Matrix.MTRANS_Y]

                if (mScaleFactor < 1) {
                    if (x < -right) bitmapMatrix.postTranslate(
                        -(x + right),
                        0f
                    ) else if (x > 0) bitmapMatrix.postTranslate(-x, 0f)
                    if (y < -bottom) bitmapMatrix.postTranslate(
                        0f,
                        -(y + bottom)
                    ) else if (y > 0) bitmapMatrix.postTranslate(0f, -y)
                }
            }
            return true
        }
    }

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        const val CLICK = 3
    }
}
package com.aurora.aonev3.ui.views

import com.aurora.aonev3.synthetic.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class SegmentedColourPicker: View {

    interface OnColourPickerChangeListener {
        fun onStopTrackingTouch(colourPicker: SegmentedColourPicker?)
    }

    private val centerX
        get() = paddingLeft + ((width - paddingLeft - paddingRight) / 2)
    private val centerY
        get() = paddingTop + ((height - paddingTop - paddingBottom) / 2)

    private var onColourPickerChangeListener: OnColourPickerChangeListener? = null
    private var backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.colour_wheel)
    private val square: Rect
        get() {
            return if (width < height) {
                Rect(paddingLeft, centerY - centerX + paddingTop, width - paddingRight, centerY + centerX - paddingBottom)
            } else {
                Rect(centerX - centerY + paddingLeft, paddingTop, centerX + centerY - paddingRight, height - paddingBottom)
            }
        }
    private val radius
        get() = square.width() / 2
    var selectedSegment = ColourSegment(4, 0)
    private var selectedColour = Color.WHITE
    private var isShowingPointer = false
    private var touchCoordinates = Pair(0,0)
    private val pointerPaint = Paint()
    private val pointerOutlinePaint = Paint()
    private val pointerRectF: RectF
        get() {
            return RectF(
                touchCoordinates.first - TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    30f,
                    resources.displayMetrics
                ),
                touchCoordinates.second - TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    80f,
                    resources.displayMetrics
                ),
                touchCoordinates.first + TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    30f,
                    resources.displayMetrics
                ),
                touchCoordinates.second - TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f,
                    resources.displayMetrics
                )
            )
        }

    constructor(context: Context): super(context)

    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle)

    init {
        pointerPaint.style = Paint.Style.FILL
        pointerOutlinePaint.style = Paint.Style.STROKE
        pointerOutlinePaint.strokeWidth = 4f
        pointerOutlinePaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(backgroundBitmap, null, square, null)

        if (isShowingPointer) {
            pointerPaint.color = selectedColour
            canvas.drawOval(pointerRectF, pointerPaint)
            canvas.drawOval(pointerRectF, pointerOutlinePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.action ?: return super.onTouchEvent(event)
        val x = event.x
        val y = event.y
        val relX = x - centerX
        val relY = y - centerY
        val pointFromCenter = sqrt(relX.pow(2) + relY.pow(2))
        var angle = -Math.toDegrees(atan2(relY.toDouble(), relX.toDouble()))
        if (angle < 0) {
            angle += 360
        }
        val segment = (pointFromCenter / (radius / 5)).toInt()
        val angleNormalised = (angle / 45).toInt()

        val bitmapX = (relX) * ((backgroundBitmap.width.toFloat() / 2) / (radius)) + backgroundBitmap.width.toFloat() / 2
        val bitmapY = (relY) * ((backgroundBitmap.height.toFloat() / 2) / (radius)) + backgroundBitmap.height.toFloat() / 2
        touchCoordinates = Pair((x).toInt(), (y).toInt())

        debug("relX = $relX, bitmapX = ${bitmapX}, relY = ${relY}")

        return when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                if (segment < 5
                    && bitmapX <= backgroundBitmap.width
                    && bitmapY <= backgroundBitmap.height
                    && bitmapX >= 0
                    && bitmapY >= 0 ) {
                    selectedColour = backgroundBitmap.getPixel(bitmapX.toInt(), bitmapY.toInt())
                    isShowingPointer = true
                } else {
                    isShowingPointer = false
                }
                invalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                selectedSegment = ColourSegment(segment, angleNormalised)
                isShowingPointer = false
                invalidate()

                onStopTrackingTouch()

                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    fun onStopTrackingTouch() {
        onColourPickerChangeListener?.onStopTrackingTouch(this)
    }

    fun setOnColourPickerChangeListener(l: OnColourPickerChangeListener) {
        onColourPickerChangeListener = l
    }
}

data class ColourSegment(private val segment: Int, private val angleNormalised: Int) {

    val hueSat: Pair<Int, Int>
    get() {
        return when {
            angleNormalised == 0 && segment >= 4 -> {
                Pair(0, 254)
            }
            angleNormalised == 0 && segment == 3 -> {
                Pair(0, 248)
            }
            angleNormalised == 0 && segment == 2 -> {
                Pair(0, 230)
            }
            angleNormalised == 0 && segment == 1 -> {
                Pair(0, 180)
            }
            angleNormalised == 0 && segment == 0 -> {
                Pair(0, 127)
            }
            angleNormalised == 1 && segment >= 4 -> {
                Pair(13, 254)
            }
            angleNormalised == 1 && segment == 3 -> {
                Pair(13, 248)
            }
            angleNormalised == 1 && segment == 2 -> {
                Pair(13, 230)
            }
            angleNormalised == 1 && segment == 1 -> {
                Pair(13, 180)
            }
            angleNormalised == 1 && segment == 0 -> {
                Pair(13, 127)
            }
            angleNormalised == 2 && segment >= 4 -> {
                Pair(43, 254)
            }
            angleNormalised == 2 && segment == 3 -> {
                Pair(43, 248)
            }
            angleNormalised == 2 && segment == 2 -> {
                Pair(43, 230)
            }
            angleNormalised == 2 && segment == 1 -> {
                Pair(43, 180)
            }
            angleNormalised == 2 && segment == 0 -> {
                Pair(43, 127)
            }
            angleNormalised == 3 && segment >= 4 -> {
                Pair(110, 254)
            }
            angleNormalised == 3 && segment == 3 -> {
                Pair(110, 248)
            }
            angleNormalised == 3 && segment == 2 -> {
                Pair(110, 230)
            }
            angleNormalised == 3 && segment == 1 -> {
                Pair(110, 180)
            }
            angleNormalised == 3 && segment == 0 -> {
                Pair(110, 127)
            }
            angleNormalised == 4 && segment >= 4 -> {
                Pair(210, 254)
            }
            angleNormalised == 4 && segment == 3 -> {
                Pair(210, 248)
            }
            angleNormalised == 4 && segment == 2 -> {
                Pair(210, 230)
            }
            angleNormalised == 4 && segment == 1 -> {
                Pair(210, 180)
            }
            angleNormalised == 4 && segment == 0 -> {
                Pair(210, 127)
            }
            angleNormalised == 5 && segment >= 4 -> {
                Pair(240, 254)
            }
            angleNormalised == 5 && segment == 3 -> {
                Pair(240, 244)
            }
            angleNormalised == 5 && segment == 2 -> {
                Pair(240, 230)
            }
            angleNormalised == 5 && segment == 1 -> {
                Pair(240, 180)
            }
            angleNormalised == 5 && segment == 0 -> {
                Pair(240, 127)
            }
            angleNormalised == 6 && segment >= 4 -> {
                Pair(259, 254)
            }
            angleNormalised == 6 && segment == 3 -> {
                Pair(259, 244)
            }
            angleNormalised == 6 && segment == 2 -> {
                Pair(259, 230)
            }
            angleNormalised == 6 && segment == 1 -> {
                Pair(259, 180)
            }
            angleNormalised == 6 && segment == 0 -> {
                Pair(259, 127)
            }
            angleNormalised == 7 && segment >= 4 -> {
                Pair(350, 254)
            }
            angleNormalised == 7 && segment == 3 -> {
                Pair(350, 242)
            }
            angleNormalised == 7 && segment == 2 -> {
                Pair(350, 230)
            }
            angleNormalised == 7 && segment == 1 -> {
                Pair(350, 180)
            }
            angleNormalised == 7 && segment == 0 -> {
                Pair(350, 127)
            }
            else -> {
                Pair(0, 254)
            }
        }
    }
    val pointerColour: Int
        get() {
            return when {
                angleNormalised == 0 && segment >= 4 -> {
                    Color.parseColor("#CE021C")
                }
                angleNormalised == 0 && segment == 3 -> {
                    Color.parseColor("#D83549")
                }
                angleNormalised == 0 && segment == 2 -> {
                    Color.parseColor("#E16777")
                }
                angleNormalised == 0 && segment == 1 -> {
                    Color.parseColor("#EB99A4")
                }
                angleNormalised == 0 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 1 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 1 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 1 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 1 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 1 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 2 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 2 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 2 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 2 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 2 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 3 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 3 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 3 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 3 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 3 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 4 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 4 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 4 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 4 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 4 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 5 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 5 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 5 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 5 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 5 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 6 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 6 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 6 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 6 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 6 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 7 && segment >= 4 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 7 && segment == 3 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 7 && segment == 2 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 7 && segment == 1 -> {
                    Color.parseColor("#F4CBD1")
                }
                angleNormalised == 7 && segment == 0 -> {
                    Color.parseColor("#F4CBD1")
                }
                else -> {
                    Color.parseColor("#F4CBD1")
                }
            }
        }
}

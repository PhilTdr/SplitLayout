package de.of14.dev.splitlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

class SplitLayout : ViewGroup {

    private val splitterRect = Rect()

    private var minChildSizePx = 100

    private var lastX = 0
    private var lastY = 0
    private var isDragging = false
    private val tempSplitterRect = Rect()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {

        extractAttributes(context, attrs)
    }

    private fun extractAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SplitLayout)
            orientation = Orientation.parse(a.getInt(R.styleable.SplitLayout_android_orientation, 0))
            minChildSizePx = a.getDimensionPixelSize(R.styleable.SplitLayout_minChildSize, minChildSizePx)
            splitterSize = a.getDimensionPixelSize(R.styleable.SplitLayout_splitterSize, splitterSize)
            isSplitterDraggable = a.getBoolean(R.styleable.SplitLayout_splitterIsDraggable, isSplitterDraggable)

            when (a.peekValue(R.styleable.SplitLayout_splitterPosition)?.type) {
                TypedValue.TYPE_DIMENSION ->
                    splitterPosition = a.getDimensionPixelSize(R.styleable.SplitLayout_splitterPosition, -1)
                        .let { if (it != -1) SplitterPosition.Px(it) else SplitterPosition.Middle }
                TypedValue.TYPE_FLOAT ->
                    splitterPosition = SplitterPosition.Percentage(
                        a.getFloat(R.styleable.SplitLayout_splitterPosition, 0.5f)
                    )
            }

            when (a.peekValue(R.styleable.SplitLayout_splitterBackground)?.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING ->
                    splitterDrawable = a.getDrawable(R.styleable.SplitLayout_splitterBackground)
                        ?: splitterDrawable
                TypedValue.TYPE_INT_COLOR_ARGB8, TypedValue.TYPE_INT_COLOR_ARGB4, TypedValue.TYPE_INT_COLOR_RGB8, TypedValue.TYPE_INT_COLOR_RGB4 ->
                    splitterDrawable = PaintDrawable(a.getColor(R.styleable.SplitLayout_splitterBackground, Color.DKGRAY))
            }

            when (a.peekValue(R.styleable.SplitLayout_splitterDraggingBackground)?.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING ->
                    splitterDraggingDrawable = a.getDrawable(R.styleable.SplitLayout_splitterDraggingBackground)
                        ?: splitterDraggingDrawable
                TypedValue.TYPE_INT_COLOR_ARGB8, TypedValue.TYPE_INT_COLOR_ARGB4, TypedValue.TYPE_INT_COLOR_RGB8, TypedValue.TYPE_INT_COLOR_RGB4 ->
                    splitterDraggingDrawable = PaintDrawable(a.getColor(R.styleable.SplitLayout_splitterDraggingBackground, Color.GRAY))
            }

            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val childViews = getChildViews()
        val splitterPositionPx = splitterPosition.getPx(this)

        if (widthSize > 0 && heightSize > 0) {
            when (orientation) {
                Orientation.Horizontal -> {
                    childViews.first.measure(MeasureSpec.makeMeasureSpec(splitterPositionPx - splitterSize / 2, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY))
                    childViews.second.measure(MeasureSpec.makeMeasureSpec(widthSize - splitterSize / 2 - splitterPositionPx, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY))
                }
                Orientation.Vertical -> {
                    childViews.first.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(splitterPositionPx - splitterSize / 2, MeasureSpec.EXACTLY))
                    childViews.second.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize - splitterSize / 2 - splitterPositionPx, MeasureSpec.EXACTLY))
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val childViews = getChildViews()
        val splitterPositionPx = splitterPosition.getPx(this)

        when (orientation) {
            Orientation.Horizontal -> {
                childViews.first.layout(0, 0, splitterPositionPx - splitterSize / 2, height)
                splitterRect[splitterPositionPx - splitterSize / 2, 0, splitterPositionPx + splitterSize / 2] = height
                childViews.second.layout(splitterPositionPx + splitterSize / 2, 0, r, height)
            }
            Orientation.Vertical -> {
                childViews.first.layout(0, 0, width, splitterPositionPx - splitterSize / 2)
                splitterRect[0, splitterPositionPx - splitterSize / 2, width] = splitterPositionPx + splitterSize / 2
                childViews.second.layout(0, splitterPositionPx + splitterSize / 2, width, height)
            }
        }
    }

    private fun getChildViews(): Pair<View, View> {
        require(childCount == 2) { "SplitLayout must have two child views." }
        val first = getChildAt(0)
        val second = getChildAt(1)
        require(first != null) { "could not get child view at position 0" }
        require(second != null) { "could not get child view at position 0" }
        return Pair(first, second)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isSplitterDraggable) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val toleranceRect = Rect(
                        splitterRect.left - splitterTouchAreaTolerance,
                        splitterRect.top - splitterTouchAreaTolerance,
                        splitterRect.right + splitterTouchAreaTolerance,
                        splitterRect.bottom + splitterTouchAreaTolerance
                    )
                    if (toleranceRect.contains(x, y)) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        isDragging = true
                        tempSplitterRect.set(splitterRect)
                        invalidate(tempSplitterRect)
                        lastX = x
                        lastY = y
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        when (orientation) {
                            Orientation.Horizontal -> {
                                val dx = x - lastX
                                var newLeft = tempSplitterRect.left + dx
                                var newRight = tempSplitterRect.right + dx

                                if (newLeft < minChildSizePx) {
                                    newLeft = minChildSizePx
                                    newRight = minChildSizePx + splitterSize
                                } else if (newRight > width - minChildSizePx) {
                                    newLeft = width - minChildSizePx - splitterSize
                                    newRight = width - minChildSizePx
                                }
                                tempSplitterRect.left = newLeft
                                tempSplitterRect.right = newRight
                            }
                            Orientation.Vertical -> {
                                val dy = y - lastY
                                var newTop = tempSplitterRect.top + dy
                                var newBottom = tempSplitterRect.bottom + dy

                                if (newTop < minChildSizePx) {
                                    newTop = minChildSizePx
                                    newBottom = minChildSizePx + splitterSize
                                } else if (newBottom > height - minChildSizePx) {
                                    newTop = height - minChildSizePx - splitterSize
                                    newBottom = height - minChildSizePx
                                }
                                tempSplitterRect.top = newTop
                                tempSplitterRect.bottom = newBottom
                            }
                        }

                        lastX = tempSplitterRect.centerX()
                        lastY = tempSplitterRect.centerY()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        when (orientation) {
                            Orientation.Horizontal -> {
                                if (lastX < minChildSizePx) {
                                    lastX = minChildSizePx
                                } else if (lastX > width - minChildSizePx) {
                                    lastX = width - minChildSizePx
                                }
                                splitterPosition = SplitterPosition.Px(lastX)
                            }
                            Orientation.Vertical -> {
                                if (lastY < minChildSizePx) {
                                    lastY = minChildSizePx
                                } else if (lastY > height - minChildSizePx) {
                                    lastY = height - minChildSizePx
                                }
                                splitterPosition = SplitterPosition.Px(lastY)
                            }
                        }
                        remeasure()
                        requestLayout()
                        invalidate()
                        resizedObserver?.invoke()
                    }
                }
            }
            return true
        }
        return false
    }

    private fun remeasure() {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        splitterDrawable.bounds = splitterRect
        splitterDrawable.draw(canvas)

        if (isDragging) {
            splitterDraggingDrawable.bounds = tempSplitterRect
            splitterDraggingDrawable.draw(canvas)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Getter & Setter
    // ---------------------------------------------------------------------------------------------

    var resizedObserver: (() -> Unit)? = null

    /**
     * The orientation of the layout.
     */
    var orientation: Orientation = Orientation.Horizontal
        set(value) {
            field = value
            if (childCount == 2) remeasure()
        }

    /**
     * Whether the splitter is draggable.
     */
    var isSplitterDraggable = true

    /**
     * Position of the splitter.
     */
    var splitterPosition: SplitterPosition = SplitterPosition.Middle
        set(value) {
            field = value
            if (childCount == 2) remeasure()
        }

    /**
     * Size of the splitter in pixels.
     */
    var splitterSize: Int = 10
        set(value) {
            if (field != value) {
                field = value
                if (childCount == 2) remeasure()
            }
        }

    /**
     * Tolerance to accept touch events outside of the splitter drawable
     */
    var splitterTouchAreaTolerance: Int = 20
        set(value) {
            if (field != value) {
                field = value
                if (childCount == 2) remeasure()
            }
        }

    /**
     * Drawable of the splitter.
     */
    var splitterDrawable: Drawable = PaintDrawable(Color.DKGRAY)
        set(value) {
            if (field != value) {
                field = value
                if (childCount == 2) remeasure()
            }
        }

    /**
     * Drawable of the splitter while dragging.
     */
    var splitterDraggingDrawable: Drawable = PaintDrawable(Color.GRAY)
        set(value) {
            if (field != value) {
                field = value
                if (childCount == 2) remeasure()
            }
        }

    // ---------------------------------------------------------------------------------------------
    // Enums
    // ---------------------------------------------------------------------------------------------

    enum class Orientation(val value: Int) {
        Horizontal(0), Vertical(1);

        companion object {
            fun parse(value: Int) = values().firstOrNull { it.value == value } ?: Horizontal
        }
    }

    sealed class SplitterPosition {
        object StartMin : SplitterPosition()
        object EndMin : SplitterPosition()
        object Middle : SplitterPosition()
        class Px(val value: Int) : SplitterPosition()
        class Percentage(val value: Float) : SplitterPosition()

        fun getPx(view: SplitLayout): Int {
            if (view.width <= 0 || view.height <= 0) return -1
            return when (this) {
                EndMin -> when (view.orientation) {
                    Orientation.Horizontal -> view.width - view.minChildSizePx
                    Orientation.Vertical -> view.height - view.minChildSizePx
                }
                Middle -> when (view.orientation) {
                    Orientation.Horizontal -> view.width / 2
                    Orientation.Vertical -> view.height / 2
                }
                StartMin -> view.minChildSizePx
                is Px -> value
                is Percentage -> when (view.orientation) {
                    Orientation.Horizontal -> view.width * value
                    Orientation.Vertical -> view.height * value
                }.toInt()
            }
        }
    }

}

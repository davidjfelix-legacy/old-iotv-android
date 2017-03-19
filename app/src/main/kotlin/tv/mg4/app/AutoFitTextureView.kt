package tv.mg4.app

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

class AutoFitTextureView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : TextureView(context, attrs, defStyleAttr) {
    private var mRatioWidth = 0
    private var mRatioHeight = 0

    constructor(context: Context?): this(context, null)
    constructor(context: Context?, attrs: AttributeSet?): this(context, attrs, 0)

    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        mRatioHeight = height
        mRatioWidth = width
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == mRatioHeight || 0 == mRatioWidth) {
            setMeasuredDimension(width, height)
        } else {
            if (width < (height * (mRatioWidth / mRatioHeight))) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }
}
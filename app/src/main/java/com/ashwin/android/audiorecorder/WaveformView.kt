package com.ashwin.android.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

private const val DEFAULT_AMPLITUDE_FACTOR = 75

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val SUB_TAG = WaveformView::class.java.simpleName

    private val paint = Paint()

    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private val width = 9f
    private val radius = 6f
    private var viewWidth = 0f
    private var viewHeight = 400f
    private val d = 6f
    private var maxSpikes = 0
    private var maxAmplitude = viewHeight * DEFAULT_AMPLITUDE_FACTOR

    init {
        paint.color = Color.rgb(244, 81, 30)
//        viewWidth = resources.displayMetrics.widthPixels.toFloat() // - (padding * 2)
//        maxSpikes = (viewWidth / (width + d)).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(APP_TAG, "$SUB_TAG: onSizeChanged( w: $w, h: $h )")
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        maxSpikes = (viewWidth / (width + d)).toInt()
    }

    fun addAmplitude(amplitude: Float) {
        maxAmplitude = Math.max(amplitude, maxAmplitude)
        amplitudes.add(amplitude)

        spikes.clear()

        val amps = amplitudes.takeLast(maxSpikes)
        for (i in amps.indices) {
            val height = normalize(amps[i], 0f, maxAmplitude, 0f, viewHeight)
            Log.d(APP_TAG, "$SUB_TAG: normalized amplitude: $height")
            val left = viewWidth - (i * (width + d))
            val top = (viewHeight/2) - (height/2)
            val right = left + width
            val bottom = top + height
            spikes.add(RectF(left, top, right, bottom))
        }

        invalidate()
    }

    fun clear(): ArrayList<Float> {
        val res = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()
        return res
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }

    // Min-Max Normalisation
    private fun normalize(n: Float, min: Float, max: Float, newMin: Float, newMax: Float): Float {
        return ((n - min)/(max - min)) * (newMax - newMin) + newMin
    }
}

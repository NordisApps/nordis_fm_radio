package com.nordisapps.fmradio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import android.graphics.Paint
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FrequencyScale(
    currentFrequency: Float,
    isPlaying: Boolean,
    onFrequencyChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minFreq: Float = 87.5f,
    maxFreq: Float = 108.0f
) {
    val step = 0.05f
    val density = LocalDensity.current
    val pxPerStep = with(density) { 10.dp.toPx() }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var displayFreq by remember { mutableFloatStateOf(currentFrequency) }

    val colorScheme = MaterialTheme.colorScheme
    val pointerColor = if (isPlaying) {
        colorScheme.primary
    } else {
        colorScheme.onSurfaceVariant
    }
    val labelColor = colorScheme.onSurfaceVariant.toArgb()
    val labelSizePx = with(density) { 12.sp.toPx() }
    val textPaint = remember(labelColor, labelSizePx) {
        Paint().apply {
            color = labelColor
            textSize = labelSizePx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    LaunchedEffect(currentFrequency) {
        displayFreq = currentFrequency
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(isPlaying) {
                if (!isPlaying) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = { onFrequencyChange(displayFreq) },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        val stepsMoved = (dragAccumulator / pxPerStep).toInt()
                        if (stepsMoved != 0) {
                            val newFreq = (displayFreq - stepsMoved * step)
                                .coerceIn(minFreq, maxFreq)
                            displayFreq = (newFreq * 20).roundToInt() / 20f
                            dragAccumulator -= stepsMoved * pxPerStep
                        }
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val tickAreaHeight = 64.dp.toPx()
        val totalSteps = ((maxFreq - minFreq) / step).toInt()
        val offsetSteps = ((displayFreq - minFreq) / step)

        for (i in 0..totalSteps) {
            val freq = minFreq + i * step
            val x = centerX + (i - offsetSteps) * pxPerStep
            if (x < -pxPerStep || x > size.width + pxPerStep) continue

            val isMajor = abs(freq - freq.roundToInt()) < (step / 2)
            val isMid = abs((freq * 4) - (freq * 4).roundToInt()) < (step * 4 / 2) && !isMajor
            val tickHeight = when {
                isMajor -> 36.dp.toPx()
                isMid -> 24.dp.toPx()
                else -> 14.dp.toPx()
            }
            val tickColor = if (isMajor) {
                colorScheme.onSurfaceVariant
            } else {
                colorScheme.outlineVariant
            }

            drawLine(
                color = tickColor,
                start = Offset(x, tickAreaHeight),
                end = Offset(x, tickAreaHeight - tickHeight),
                strokeWidth = 2.dp.toPx()
            )

            if (isMajor) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        freq.roundToInt().toString(),
                        x,
                        tickAreaHeight + labelSizePx,
                        textPaint
                    )
                }
            }
        }
        drawLine(
            color = pointerColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, tickAreaHeight),
            strokeWidth = 2.dp.toPx()
        )
    }
}
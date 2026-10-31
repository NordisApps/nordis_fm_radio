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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordisapps.FrequencyGrid
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun FrequencyScale(
    currentFrequency: Double,
    isPlaying: Boolean,
    onFrequencyChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minFreq: Double = 87.5,
    maxFreq: Double = 108.0,
    step: Double = 0.05
) {
    val density = LocalDensity.current
    val pxPerStep = with(density) { 10.dp.toPx() }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var dragStartFreq by remember { mutableDoubleStateOf(0.0) }
    var displayFreq by remember { mutableDoubleStateOf(currentFrequency) }
    val gridStart = remember(minFreq, step) { FrequencyGrid.start(minFreq, step) }

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

    LaunchedEffect(gridStart, maxFreq) {
        displayFreq = displayFreq.coerceIn(gridStart, maxFreq)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(isPlaying, step, minFreq, maxFreq) {
                if (!isPlaying) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        dragStartFreq = displayFreq
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        val stepsMoved = (dragAccumulator / pxPerStep).roundToInt()
                        val rawFreq = dragStartFreq - stepsMoved * step
                        displayFreq = FrequencyGrid.snap(rawFreq, minFreq, maxFreq, step)
                        onFrequencyChange(displayFreq)
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val tickAreaHeight = 64.dp.toPx()
        val totalSteps = floor((maxFreq - gridStart) / step + 1e-9).toInt()
        val offsetSteps = (displayFreq - gridStart) / step
        val majorEveryNSteps = (1.0 / step).roundToInt().coerceAtLeast(1)
        val firstWholeMhz = ceil(gridStart - 1e-9).toInt()
        val offsetToFirstMajor = ((firstWholeMhz - gridStart) / step).roundToInt()
        val midEveryNSteps = (majorEveryNSteps / 2).coerceAtLeast(1)

        for (i in 0..totalSteps) {
            val x = (centerX + (i - offsetSteps) * pxPerStep).toFloat()
            if (x < -pxPerStep || x > size.width + pxPerStep) continue

            val isMajor =
                i >= offsetToFirstMajor && (i - offsetToFirstMajor) % majorEveryNSteps == 0
            val isMid = !isMajor && i % midEveryNSteps == 0
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
                val wholeMhz = firstWholeMhz + (i - offsetToFirstMajor) / majorEveryNSteps
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        wholeMhz.toString(),
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
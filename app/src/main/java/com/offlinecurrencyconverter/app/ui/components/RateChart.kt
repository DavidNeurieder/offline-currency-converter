package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RateChart(
    dataPoints: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (dataPoints.size < 2) return

    val rates = dataPoints.map { it.second }
    val minRate = rates.min()
    val maxRate = rates.max()
    val range = maxRate - minRate

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val padding = 8.dp.toPx()
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2

        val path = Path()
        val stepX = chartWidth / (dataPoints.size - 1)

        dataPoints.forEachIndexed { index, (_, rate) ->
            val x = padding + index * stepX
            val normalizedRate = if (range > 0) ((rate - minRate) / range).toFloat() else 0.5f
            val y = padding + chartHeight - (normalizedRate * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        dataPoints.forEachIndexed { index, (_, rate) ->
            val x = padding + index * stepX
            val normalizedRate = if (range > 0) ((rate - minRate) / range).toFloat() else 0.5f
            val y = padding + chartHeight - (normalizedRate * chartHeight)

            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

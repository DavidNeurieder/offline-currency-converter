package com.offlinecurrencyconverter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinecurrencyconverter.app.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val GRID_COLOR = Color(0xFFE0E0E0)
private val TOOLTIP_BG = Color(0xF0212121)
private val TOOLTIP_TEXT = Color.White
private val INCREASE_COLOR = Color(0xFF2E7D32)
private val DECREASE_COLOR = Color(0xFFD32F2F)

@Composable
fun RateChart(
    dataPoints: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    if (dataPoints.size < 2) return

    val rates = dataPoints.map { it.second }
    val minRate = rates.min()
    val maxRate = rates.max()
    val range = maxRate - minRate

    val trendColor = if (rates.last() >= rates.first()) INCREASE_COLOR else DECREASE_COLOR

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val inputDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val outputDateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val rateFormat = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 4
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(dataPoints) {
                    if (!interactive) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val paddingPx = 8.dp.toPx()
                            val chartWidth = size.width - paddingPx * 2
                            val stepX = chartWidth / (dataPoints.size - 1)
                            val index = ((offset.x - paddingPx) / stepX)
                                .toInt()
                                .coerceIn(0, dataPoints.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val padding = 8.dp.toPx()
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            val stepX = chartWidth / (dataPoints.size - 1)

            // Grid lines
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = padding + chartHeight * i / gridCount
                drawLine(
                    color = GRID_COLOR,
                    start = Offset(padding, y),
                    end = Offset(size.width - padding, y),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }

            fun pointFor(index: Int): Offset {
                val rate = dataPoints[index].second
                val x = padding + index * stepX
                val normalizedRate = if (range > 0) ((rate - minRate) / range).toFloat() else 0.5f
                val y = padding + chartHeight - (normalizedRate * chartHeight)
                return Offset(x, y)
            }

            // Gradient fill path
            val fillPath = Path()
            val firstPoint = pointFor(0)
            fillPath.moveTo(firstPoint.x, firstPoint.y)
            for (i in 1 until dataPoints.size) {
                val prev = pointFor(i - 1)
                val curr = pointFor(i)
                val controlOffsetX = stepX / 3f
                fillPath.cubicTo(
                    x1 = prev.x + controlOffsetX,
                    y1 = prev.y,
                    x2 = curr.x - controlOffsetX,
                    y2 = curr.y,
                    x3 = curr.x,
                    y3 = curr.y
                )
            }
            fillPath.lineTo(padding + chartWidth, padding + chartHeight)
            fillPath.lineTo(padding, padding + chartHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(trendColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = padding,
                    endY = padding + chartHeight
                )
            )

            // Line path
            val linePath = Path()
            val first = pointFor(0)
            linePath.moveTo(first.x, first.y)
            for (i in 1 until dataPoints.size) {
                val prev = pointFor(i - 1)
                val curr = pointFor(i)
                val controlOffsetX = stepX / 3f
                linePath.cubicTo(
                    x1 = prev.x + controlOffsetX,
                    y1 = prev.y,
                    x2 = curr.x - controlOffsetX,
                    y2 = curr.y,
                    x3 = curr.x,
                    y3 = curr.y
                )
            }
            drawPath(
                path = linePath,
                color = trendColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Crosshair and selected dot
            val selectedIdx = selectedIndex
            if (selectedIdx != null) {
                val point = pointFor(selectedIdx)
                drawLine(
                    color = trendColor.copy(alpha = 0.5f),
                    start = Offset(point.x, padding),
                    end = Offset(point.x, padding + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = trendColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            } else {
                // End dot
                val lastPoint = pointFor(dataPoints.size - 1)
                drawCircle(
                    color = trendColor,
                    radius = 3.dp.toPx(),
                    center = lastPoint
                )
            }
        }

        // Tooltip overlay
        val selectedIdx = selectedIndex
        if (selectedIdx != null && selectedIdx < dataPoints.size) {
            val dateStr = dataPoints[selectedIdx].first
            val rate = dataPoints[selectedIdx].second
            val parsedDate = try { inputDateFormat.parse(dateStr) } catch (_: Exception) { null }
            val displayDate = if (parsedDate != null) outputDateFormat.format(parsedDate) else dateStr

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = rateFormat.format(rate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
fun RateChartSummary(
    dataPoints: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.size < 2) return

    val rates = dataPoints.map { it.second }
    val lastRate = rates.last()
    val firstRate = rates.first()
    val change = lastRate - firstRate
    val changePercent = if (firstRate > 0) (change / firstRate) * 100.0 else 0.0
    val trendColor = if (change >= 0) INCREASE_COLOR else DECREASE_COLOR
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 4
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = numberFormat.format(lastRate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (change >= 0) "+${String.format(Locale.getDefault(), "%.2f", changePercent)}%"
                else String.format(Locale.getDefault(), "%.2f", changePercent) + "%",
                style = MaterialTheme.typography.bodySmall,
                color = trendColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.chart_high),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = numberFormat.format(rates.max()),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.chart_low),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = numberFormat.format(rates.min()),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RateChartDetail(
    dataPoints: List<Pair<String, Double>>,
    minRate: Double,
    maxRate: Double,
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 4
    }
    val trendColor = if (changePercent >= 0) INCREASE_COLOR else DECREASE_COLOR

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RateChart(
            dataPoints = dataPoints,
            interactive = true,
            modifier = Modifier.fillMaxWidth()
        )

        // X-axis labels (first and last dates)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dataPoints.firstOrNull()?.first ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dataPoints.lastOrNull()?.first ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.chart_high),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = numberFormat.format(maxRate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.chart_low),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = numberFormat.format(minRate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.chart_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (changePercent >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.2f", changePercent)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = trendColor
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategorySummary
import com.example.util.CategoryIcons
import com.example.util.CurrencyUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(
    categories: List<CategorySummary>,
    totalPaise: Long,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty() || totalPaise <= 0L) {
        EmptyChartState(message = "No expenses recorded for this period to display chart")
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(208.dp)) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f
                val totalAmount = totalPaise.toDouble()

                categories.forEachIndexed { index, cat ->
                    val sweepAngle = (cat.totalPaise / totalAmount * 360f).toFloat() * animationProgress.value
                    if (sweepAngle > 0.5f) {
                        val baseColor = CategoryIcons.parseColor(cat.colorHex)
                        val isSelected = selectedIndex == index
                        val effectiveStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

                        drawArc(
                            color = baseColor,
                            startAngle = startAngle + 1f,
                            sweepAngle = maxOf(0.1f, sweepAngle - 2f),
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            // Center details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (selectedIndex != null && selectedIndex!! in categories.indices) {
                    val cat = categories[selectedIndex!!]
                    Text(
                        text = cat.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CurrencyUtils.formatInr(cat.totalPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format("%.1f%%", cat.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatInr(totalPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categories.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(6).forEachIndexed { index, cat ->
                val isSelected = selectedIndex == index
                val color = CategoryIcons.parseColor(cat.colorHex)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        selectedIndex = if (isSelected) null else index
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${cat.categoryName}: ${String.format("%.0f%%", cat.percentage)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(
    items: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (items.isEmpty()) {
        EmptyChartState(message = "No expenditure data to graph")
        return
    }

    val maxVal = maxOf(1L, items.maxOf { it.second })
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(600))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 24.dp.toPx()
            val barCount = items.size
            val slotWidth = canvasWidth / barCount
            val barWidth = (slotWidth * 0.55f).coerceIn(8.dp.toPx(), 36.dp.toPx())

            // Baseline
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = 1.dp.toPx()
            )

            items.forEachIndexed { index, pair ->
                val ratio = (pair.second.toFloat() / maxVal.toFloat()) * animationProgress.value
                val barHeight = canvasHeight * ratio
                val left = index * slotWidth + (slotWidth - barWidth) / 2
                val top = canvasHeight - barHeight

                // Draw bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor, barColor.copy(alpha = 0.75f))
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Labels row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { (label, _) ->
                Text(
                    text = label.take(6),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TrendAreaChart(
    points: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (points.size < 2) {
        EmptyChartState(message = "Record expenses over multiple days to view spending trend")
        return
    }

    val maxVal = maxOf(1L, points.maxOf { it.second })
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            val width = size.width
            val height = size.height - 20.dp.toPx()
            val stepX = width / (points.size - 1)

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { i, p ->
                val x = i * stepX
                val normalizedY = (p.second.toFloat() / maxVal.toFloat()) * animationProgress.value
                val y = height - (normalizedY * (height - 16.dp.toPx()))

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevNorm = (points[i - 1].second.toFloat() / maxVal.toFloat()) * animationProgress.value
                    val prevY = height - (prevNorm * (height - 16.dp.toPx()))
                    val cx = (prevX + x) / 2f
                    path.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw Area Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f))
                )
            )

            // Draw Stroke Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw dot points
            points.forEachIndexed { i, p ->
                val x = i * stepX
                val normalizedY = (p.second.toFloat() / maxVal.toFloat()) * animationProgress.value
                val y = height - (normalizedY * (height - 16.dp.toPx()))
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.filterIndexed { index, _ -> index == 0 || index == points.lastIndex || index == points.size / 2 }
                .forEach { (label, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}

@Composable
fun EmptyChartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

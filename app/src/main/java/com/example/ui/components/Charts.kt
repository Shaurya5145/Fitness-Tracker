package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun CaloriesChartCard(
    currentCalories: Int,
    targetCalories: Int,
    weeklyData: List<ChartData>,
    onTargetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header
            Text(
                text = "CALORIES",
                color = Color(0xFF8A8E9A),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currentCalories",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kcal",
                        color = Color(0xFF8A8E9A),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTargetClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Target:",
                    color = Color(0xFF8A8E9A),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$targetCalories Kcal",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Chart Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { data ->
                    ChartBar(data)
                }
            }
        }
    }
}

data class ChartData(
    val day: String,
    val percentage: Int,
    val isHighlighted: Boolean = false
)

@Composable
fun ChartBar(data: ChartData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${data.percentage}%",
            color = if (data.isHighlighted) Color.White else Color(0xFF7A7C83),
            fontSize = 10.sp,
            fontWeight = if (data.isHighlighted) FontWeight.Bold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        val barHeight = 160.dp
        val barWidth = 32.dp
        val fillPercentage = minOf(data.percentage / 100f, 1f)

        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
        ) {
            // Glow effect for highlighted bar
            if (data.isHighlighted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(barWidth / 2),
                            spotColor = Color(0xFF4E95FF),
                            ambientColor = Color(0xFF4E95FF)
                        )
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadius = CornerRadius(size.width / 2, size.width / 2)
                
                // Path for the full background bar (pill shape)
                val rectPath = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, cornerRadius))
                }

                // Draw striped background inside the full bar
                clipPath(rectPath) {
                    // Base background color
                    drawRect(Color(0xFF1E2025))

                    // Draw diagonal stripes
                    val stripeColor = Color(0xFF2E323A)
                    val stripeWidth = 2.dp.toPx()
                    val stripeSpacing = 8.dp.toPx()
                    
                    var startX = -size.height
                    while (startX < size.width) {
                        drawLine(
                            color = stripeColor,
                            start = Offset(startX, size.height),
                            end = Offset(startX + size.height, 0f),
                            strokeWidth = stripeWidth
                        )
                        startX += stripeSpacing + stripeWidth
                    }
                }

                // Path for the filled portion of the bar
                val fillHeight = size.height * fillPercentage
                val fillTop = size.height - fillHeight
                val fillRectPath = Path().apply {
                    addRoundRect(RoundRect(0f, fillTop, size.width, size.height, cornerRadius))
                }

                // Draw the solid filled portion
                val fillColor = if (data.isHighlighted) Color(0xFF4E95FF) else Color(0xFF354A6F)
                drawPath(
                    path = fillRectPath,
                    color = fillColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = data.day,
                color = if (data.isHighlighted) Color.White else Color(0xFF7A7C83),
                fontSize = 12.sp,
                fontWeight = if (data.isHighlighted) FontWeight.Bold else FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // White underline indicator for the highlighted day
            if (data.isHighlighted) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(Color.White, RoundedCornerShape(1.dp))
                )
            } else {
                // Placeholder to keep spacing consistent
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun LineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier.height(200.dp),
    lineColor: Color = Color(0xFF00C6FF),
    targetGoalValue: Float? = null,
    minYOffset: Float = 5f
) {
    if (dataPoints.isEmpty()) return

    val validData = dataPoints.filter { it > 0f }
    if (validData.isEmpty()) return

    val maxData = maxOf(validData.maxOrNull() ?: 1f, targetGoalValue ?: 0f)
    val minData = minOf(validData.minOrNull() ?: 0f, targetGoalValue ?: 1000f)
    
    val paddingElements = if (maxData == minData) minYOffset else (maxData - minData) * 0.2f
    val maxVal = maxData + paddingElements
    val minVal = (minData - paddingElements).coerceAtLeast(0f)
    
    val range = if (maxVal == minVal) 1f else maxVal - minVal

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val paddingX = 10f
        val paddingY = 20f
        
        val effectiveWidth = width - 2 * paddingX
        val effectiveHeight = height - 2 * paddingY

        val stepX = if (dataPoints.size > 1) effectiveWidth / (dataPoints.size - 1) else 0f
        
        val path = Path()
        val fillPath = Path()
        
        val validPoints = dataPoints.mapIndexedNotNull { index, value ->
            if (value > 0f) index to value else null
        }

        // Draw dotted goal line
        if (targetGoalValue != null) {
            val normalizedTargetY = 1 - ((targetGoalValue - minVal) / range)
            val targetY = paddingY + normalizedTargetY * effectiveHeight
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }

        var firstX = 0f
        var firstY = 0f
        var lastX = 0f
        var lastY = 0f

        validPoints.forEachIndexed { i, (dataIndex, value) ->
            val x = if (dataPoints.size == 1) width / 2f else paddingX + dataIndex * stepX
            val normalizedY = 1 - ((value - minVal) / range)
            val y = paddingY + normalizedY * effectiveHeight

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
                firstX = x
                firstY = y
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }
        
        if (validPoints.size > 1) {
            fillPath.lineTo(lastX, height)
            fillPath.lineTo(firstX, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.5f), Color.Transparent),
                    startY = 0f,
                    endY = height
                ),
                style = Fill
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }

        // glowing Blue spheres
        validPoints.forEachIndexed { i, (dataIndex, value) ->
             val x = if (dataPoints.size == 1) width / 2f else paddingX + dataIndex * stepX
             val normalizedY = 1 - ((value - minVal) / range)
             val y = paddingY + normalizedY * effectiveHeight
             
             drawCircle(
                 color = lineColor.copy(alpha = 0.3f),
                 radius = 16f,
                 center = Offset(x, y)
             )
             drawCircle(
                color = Color(0xFF0F0F0F),
                radius = 10f,
                center = Offset(x, y)
             )
             drawCircle(
                color = lineColor,
                radius = 7f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun BarChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier.height(200.dp),
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (dataPoints.isEmpty()) return

    val maxData = dataPoints.maxOrNull() ?: 0f
    val maxVal = if (maxData > 0f) maxData else 1f
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val barWidth = (width / dataPoints.size) * 0.6f
        val spacing = (width / dataPoints.size) * 0.4f
        
        dataPoints.forEachIndexed { index, value ->
            val x = index * (barWidth + spacing) + spacing / 2
            val normalizedY = value / maxVal
            val barHeight = normalizedY * (height - 40f)
            val y = height - 20f - barHeight

            val gradient = Brush.verticalGradient(
                colors = listOf(barColor, barColor.copy(alpha = 0.2f)),
                startY = y,
                endY = height - 20f
            )

            drawRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(width = barWidth, height = barHeight)
            )
            
            // Neon top cap
            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(width = barWidth, height = 4f)
            )
        }
    }
}

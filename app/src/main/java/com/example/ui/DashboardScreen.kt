package com.example.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.LineChart
import com.example.ui.components.BarChart
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.automirrored.filled.List
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FitnessViewModel, navController: NavController) {
    val weightRecords by viewModel.weightRecords.collectAsStateWithLifecycle()
    val fullSessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
    val mealLogs by viewModel.allMealLogs.collectAsStateWithLifecycle()
    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val mappings by viewModel.allExerciseMappings.collectAsStateWithLifecycle()
    val progressPhotos by viewModel.allProgressPhotos.collectAsStateWithLifecycle()
    
    var showAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showAccountDialog) {
        AccountDialog(viewModel = viewModel, onDismiss = { showAccountDialog = false })
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val todayStart = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        
        val todaysWeight = weightRecords.filter { it.dateStamp >= todayStart }.maxByOrNull { it.dateStamp }?.weightKg
        val mealMap = allMeals.associateBy { it.id }
        val todaysCalories = mealLogs.filter { it.dateStamp >= todayStart }.sumOf { mealMap[it.mealId]?.calories ?: 0 }
        
        val sessionDays = fullSessions.map { 
            Calendar.getInstance().apply { timeInMillis = it.session.timestamp }.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSet()
        
        var streak = 0
        var currentCheck = todayStart
        if (sessionDays.contains(currentCheck)) {
            streak++
            currentCheck -= 86400000L
            while(sessionDays.contains(currentCheck)) { streak++; currentCheck -= 86400000L }
        } else {
            currentCheck -= 86400000L
            while(sessionDays.contains(currentCheck)) { streak++; currentCheck -= 86400000L }
        }

        val sevenDaysAgo = todayStart - 7 * 86400000L
        val weeklyCount = fullSessions.count { it.session.timestamp >= sevenDaysAgo }
        
        val lastGroups = fullSessions.maxByOrNull { it.session.timestamp }?.exercises?.mapNotNull { ex ->
            mappings.find { it.exerciseName.equals(ex.exercise.exerciseName.trim(), ignoreCase = true) }?.muscleGroup
        } ?: emptyList()
        val nextWorkout = if (lastGroups.isEmpty()) "Push Day" else if (lastGroups.contains("Chest")) "Pull Day" else if (lastGroups.contains("Back")) "Leg Day" else "Push Day"

        val navigateToTab = { route: String ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            val scope = rememberCoroutineScope()
            var showSyncOptions by remember { mutableStateOf(false) }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Summary", 
                    fontSize = 40.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        IconButton(onClick = { showSyncOptions = true }) {
                            Icon(Icons.Filled.Sync, contentDescription = "Sync Options", tint = Color.White)
                        }
                        DropdownMenu(expanded = showSyncOptions, onDismissRequest = { showSyncOptions = false }) {
                            DropdownMenuItem(
                                text = { Text("Backup to Cloud") },
                                onClick = {
                                    scope.launch {
                                        val success = viewModel.cloudSyncManager.backupToCloud()
                                        snackbarHostState.showSnackbar(if (success) "Backup complete" else "Backup failed")
                                    }
                                    showSyncOptions = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restore from Cloud") },
                                onClick = {
                                    scope.launch {
                                        val success = viewModel.cloudSyncManager.restoreFromCloud()
                                        snackbarHostState.showSnackbar(if (success) "Restore complete" else "Restore failed")
                                    }
                                    showSyncOptions = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showAccountDialog = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account", tint = Color.White)
                    }
                }
            }

            // Hero Dashboard
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when (hour) {
                        in 5..11 -> "Good Morning"
                        in 12..17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }
                    Text(greeting, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(nextWorkout, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Motivational/progress area
                    val missingWorkouts = maxOf(0, 5 - weeklyCount)
                    val insightText = if (missingWorkouts == 0) {
                        "Weekly target reached. Great job!"
                    } else if (missingWorkouts == 1) {
                        "One workout away from your weekly target."
                    } else {
                        "$weeklyCount of 5 workouts completed this week."
                    }
                    
                    Text(insightText, fontSize = 16.sp, color = Color.White)
                    if (streak > 0) {
                        Text("Current streak: $streak days \uD83D\uDD25", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Secondary Metric Cards
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricCard(title = "Weight", value = todaysWeight?.let { "$it kg" } ?: "--", subtitle = "", icon = Icons.Filled.MonitorWeight, color = Color(0xFF0A84FF), modifier = Modifier.weight(1f)) {
                        navigateToTab("weight")
                    }
                    MetricCard(title = "Calories", value = "$todaysCalories", subtitle = "", icon = Icons.Filled.LocalDining, color = Color(0xFFFF9F0A), modifier = Modifier.weight(1f)) {
                        navigateToTab("meals")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricCard(title = "Streak", value = "$streak", subtitle = "", icon = Icons.Filled.LocalFireDepartment, color = Color(0xFFFF453A), modifier = Modifier.weight(1f)) {
                        navigateToTab("workout")
                    }
                    MetricCard(title = "Workouts", value = "$weeklyCount", subtitle = "", icon = Icons.AutoMirrored.Filled.List, color = Color(0xFF0A84FF), modifier = Modifier.weight(1f)) {
                        navigateToTab("workout")
                    }
                }
            }

            // Current Goal Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { navigateToTab("workout") }
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Current Goal", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                            Text("Build Muscle", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFEBEBF5).copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Suggested Next Workout", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(nextWorkout, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // CTA Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Start Workout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A84FF))
                    }
                }
            }

            // Quick Add Buttons
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Quick Actions", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { QuickAddButton(icon = Icons.AutoMirrored.Filled.List, label = "Workout") { navigateToTab("workout") } }
                    item { QuickAddButton(icon = Icons.Filled.Restaurant, label = "Meal") { navigateToTab("meals") } }
                    item { QuickAddButton(icon = Icons.Filled.MonitorWeight, label = "Weight") { navigateToTab("weight") } }
                    item { QuickAddButton(icon = Icons.Filled.CameraAlt, label = "Photo") { navigateToTab("photos") } }
                }
            }

            // Recent Progress Photos
            if (progressPhotos.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("Recent Progress", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("View All", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0A84FF), modifier = Modifier.clickable { navigateToTab("photos") }.padding(bottom = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        modifier = Modifier.fillMaxWidth().clickable { navigateToTab("photos") }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Timeline & Indicator
                            val sortedPhotos = progressPhotos.sortedBy { it.dateStamp }
                            val earliest = sortedPhotos.first()
                            val latest = sortedPhotos.last()
                            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                            
                            val dateRangeText = if (sortedPhotos.size > 1 && sdf.format(java.util.Date(earliest.dateStamp)) != sdf.format(java.util.Date(latest.dateStamp))) {
                                "${sdf.format(java.util.Date(earliest.dateStamp))} → ${sdf.format(java.util.Date(latest.dateStamp))}"
                            } else {
                                "Timeline: ${sdf.format(java.util.Date(latest.dateStamp))}"
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF0A84FF)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateRangeText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val latestPhotos = progressPhotos.sortedByDescending { it.dateStamp }.take(3).reversed()
                                
                                latestPhotos.forEach { photo ->
                                    val imageModel = if (!photo.remoteUrl.isNullOrEmpty()) photo.remoteUrl else File(photo.imagePath)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.7f)
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        AsyncImage(
                                            model = imageModel,
                                            contentDescription = photo.viewType,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Subtle Gradient
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                                        startY = 0f
                                                    )
                                                )
                                        )
                                        // ViewType and Date
                                        Column(
                                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                        ) {
                                            Text(photo.viewType.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text(sdf.format(java.util.Date(photo.dateStamp)), fontSize = 10.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Analytics Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Analytics", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(16.dp))
                WeightProgressionSection(weightRecords)
                Spacer(modifier = Modifier.height(16.dp))
                StrengthProgressionSection(fullSessions)
                Spacer(modifier = Modifier.height(16.dp))
                MuscleGroupSection(fullSessions, mappings)
                Spacer(modifier = Modifier.height(16.dp))
                ConsistencySection(fullSessions)
                Spacer(modifier = Modifier.height(16.dp))
                CaloriesTrendSection(mealLogs, allMeals, weightRecords)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color = Color(0xFF0A84FF), modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() }.height(115.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun QuickAddButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun WeightProgressionSection(weightRecords: List<WeightRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val sorted = weightRecords.sortedBy { it.dateStamp }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Weight Trend", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    if (sorted.isNotEmpty()) {
                        val currentWeight = sorted.last().weightKg
                        Text("$currentWeight kg", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    } else {
                        Text("No data yet", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    }
                }
                
                if (sorted.size > 1) {
                    val diff = sorted.last().weightKg - sorted.first().weightKg
                    val diffText = if(diff > 0) "+${String.format(java.util.Locale.US, "%.1f", diff)}" else String.format(java.util.Locale.US, "%.1f", diff)
                    val diffColor = if(diff <= 0) Color(0xFF34C759) else Color(0xFFFF453A)
                    Box(modifier = Modifier.background(diffColor.copy(alpha=0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("$diffText kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = diffColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (sorted.isNotEmpty()) {
                val points = sorted.map { it.weightKg }
                
                val maxData = points.maxOrNull() ?: 1f
                val minData = points.minOrNull() ?: 0f
                val range = if (maxData == minData) 1f else maxData - minData
                val paddingElements = range * 0.2f
                val maxVal = maxData + paddingElements
                val minVal = (minData - paddingElements).coerceAtLeast(0f)
                val actualRange = maxVal - minVal

                var selectedIndex by remember { mutableStateOf<Int?>(null) }
                
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(weightRecords) {
                    animatedProgress.snapTo(0f)
                    animatedProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(weightRecords) {
                                detectTapGestures { offset ->
                                    val width = size.width.toFloat()
                                    val stepX = if (points.size > 1) width / (points.size - 1) else 0f
                                    if (stepX > 0) {
                                        val clickedIndex = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                        selectedIndex = clickedIndex
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val stepX = if (points.size > 1) width / (points.size - 1) else 0f
                        
                        val mappedPoints = points.mapIndexed { index, value ->
                            val x = if (points.size == 1) width / 2f else index * stepX
                            val normalizedY = 1 - ((value - minVal) / actualRange)
                            val y = normalizedY * height
                            Offset(x, y)
                        }

                        val path = Path()
                        val fillPath = Path()
                        
                        if (mappedPoints.isNotEmpty()) {
                            var prevPoint = mappedPoints.first()
                            path.moveTo(prevPoint.x, prevPoint.y)

                            val drawCount = (mappedPoints.size * animatedProgress.value).toInt().coerceAtLeast(1)
                            
                            for (i in 1 until drawCount) {
                                val point = mappedPoints[i]
                                val controlX = (prevPoint.x + point.x) / 2
                                path.cubicTo(controlX, prevPoint.y, controlX, point.y, point.x, point.y)
                                prevPoint = point
                            }
                            
                            val currentFraction = (mappedPoints.size * animatedProgress.value)
                            if (currentFraction < mappedPoints.size && currentFraction > drawCount) {
                                val fraction = currentFraction - drawCount
                                val point = mappedPoints[drawCount]
                                val interpolatedX = prevPoint.x + (point.x - prevPoint.x) * fraction
                                val interpolatedY = prevPoint.y + (point.y - prevPoint.y) * fraction
                                val controlX = (prevPoint.x + interpolatedX) / 2
                                path.cubicTo(controlX, prevPoint.y, controlX, interpolatedY, interpolatedX, interpolatedY)
                                prevPoint = Offset(interpolatedX, interpolatedY)
                            }

                            fillPath.addPath(path)
                            if (mappedPoints.size > 1 && animatedProgress.value > 0) {
                                fillPath.lineTo(prevPoint.x, height)
                                fillPath.lineTo(mappedPoints.first().x, height)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0A84FF).copy(alpha = 0.5f), Color.Transparent),
                                        startY = 0f,
                                        endY = height
                                    ),
                                    style = Fill
                                )
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF0A84FF),
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            selectedIndex?.let { idx ->
                                if (idx < drawCount || (idx == drawCount && currentFraction > drawCount)) {
                                    val p = mappedPoints[idx]
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.3f),
                                        start = Offset(p.x, 0f),
                                        end = Offset(p.x, height),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                    )
                                    drawCircle(color = Color(0xFF0A84FF), radius = 6.dp.toPx(), center = p)
                                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = p)
                                    drawCircle(color = Color(0xFF0A84FF).copy(alpha = 0.3f), radius = 12.dp.toPx(), center = p)
                                }
                            }
                        }
                    }
                    
                    selectedIndex?.let { idx ->
                        if (idx in sorted.indices) {
                            val w = sorted[idx]
                            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                            Box(modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${w.weightKg} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(sdf.format(java.util.Date(w.dateStamp)), fontSize = 10.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StrengthProgressionSection(fullSessions: List<SessionWithExercises>) {
    // Find all distinct exercises (case-insensitive)
    val allExercises = fullSessions.flatMap { it.exercises }
        .map { it.exercise.exerciseName.trim().lowercase() }
        .distinct()
        .sorted()
    
    var _selectedExercise by remember { mutableStateOf("") }
    val selectedExercise = if (_selectedExercise in allExercises) _selectedExercise else allExercises.firstOrNull() ?: ""
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Exercise Progression", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (allExercises.isEmpty()) {
                Text("No workout data available.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
            } else {
                // Elegant pills for exercise selection
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allExercises) { ex ->
                        val isSelected = ex == selectedExercise
                        val bgColor = if (isSelected) Color(0xFF0A84FF) else Color(0xFF2C2C2E)
                        val textColor = if (isSelected) Color.White else Color(0xFFEBEBF5).copy(alpha = 0.8f)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .clickable { _selectedExercise = ex }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                ex.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Collect max weight * reps volume for the selected exercise per session
                val sessionsWithEx = fullSessions.filter { session ->
                    session.exercises.any { it.exercise.exerciseName.trim().lowercase() == selectedExercise }
                }.sortedBy { it.session.timestamp }
                
                val points = sessionsWithEx.mapNotNull { session ->
                    // Find all sets for this exercise, we can sum across multiple matching exercises if they exist
                    val matchingExercises = session.exercises.filter { it.exercise.exerciseName.trim().lowercase() == selectedExercise }
                    val vol = matchingExercises.sumOf { exInfo -> 
                        exInfo.sets.sumOf { set ->
                            when {
                                set.distance > 0f -> set.distance.toDouble()
                                set.durationMinutes > 0 -> set.durationMinutes.toDouble()
                                set.weightKg > 0f -> (set.weightKg * set.reps).toDouble()
                                else -> set.reps.toDouble()
                            }
                        } 
                    }.toFloat()
                    
                    if (vol > 0f) vol else null
                }

                if (points.isNotEmpty()) {
                    val insightText = if (points.size > 1) {
                        val first = points.first()
                        val last = points.last()
                        if (first > 0) {
                            val percent = ((last - first) / first * 100).toInt()
                            if (percent > 0) "+$percent% stronger overall" else if (percent < 0) "$percent% overall" else "Maintained strength"
                        } else "Volume progression"
                    } else "Volume progression"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text(selectedExercise.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(insightText, fontSize = 13.sp, color = if (insightText.startsWith("+")) Color(0xFF34C759) else Color(0xFFEBEBF5).copy(alpha = 0.6f))
                        }
                        if (points.isNotEmpty()) {
                            Text("${points.last().toInt()} vol", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (points.any { it > 0f }) {
                        LineChart(dataPoints = points, modifier = Modifier.height(140.dp).fillMaxWidth(), lineColor = Color(0xFF0A84FF))
                    } else {
                        Text("No valid volume data.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    }
                } else {
                    Text("No volume data to chart.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun MuscleGroupSection(fullSessions: List<SessionWithExercises>, mappings: List<ExerciseMapping>) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (fullSessions.isEmpty()) {
                Text("Muscle Focus", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No workout data.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                return@Column
            }

            val muscleMap = mutableMapOf<String, Int>()
            val recentMuscleMap = mutableMapOf<String, Int>()
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            
            fullSessions.forEach { session ->
                val sessionGroups = mutableSetOf<String>()
                session.exercises.forEach { ex ->
                    val rawName = ex.exercise.exerciseName.trim()
                    val mappingResult = mappings.find { it.exerciseName.equals(rawName, ignoreCase = true) }?.muscleGroup
                    val group = mappingResult ?: "Other"
                    sessionGroups.add(group)
                }
                sessionGroups.forEach { group ->
                    muscleMap[group] = muscleMap.getOrDefault(group, 0) + 1
                    if (session.session.timestamp >= sevenDaysAgo) {
                        recentMuscleMap[group] = recentMuscleMap.getOrDefault(group, 0) + 1
                    }
                }
            }
            
            val sortedList = muscleMap.entries.sortedByDescending { it.value }
            if (sortedList.isEmpty()) {
                Text("Muscle Focus", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No mapped exercises found.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                return@Column
            }
            
            val mostTrainedRecent = recentMuscleMap.entries.maxByOrNull { it.value }
            val topGroupOverall = sortedList.firstOrNull()?.key
            
            val insightText = if (mostTrainedRecent != null && mostTrainedRecent.value > 0) {
                "${mostTrainedRecent.key} trained ${mostTrainedRecent.value}× this week."
            } else if (topGroupOverall != null) {
                "$topGroupOverall received highest attention overall."
            } else {
                "Balanced training focus."
            }

            Column {
                Text("Muscle Focus", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(insightText, fontSize = 14.sp, color = Color(0xFF34C759))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val maxFreq = sortedList.maxOf { it.value }.toFloat().coerceAtLeast(1f)
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sortedList.take(4).forEach { (group, count) ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(group, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text("$count session" + if (count > 1) "s" else "", fontSize = 13.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { count / maxFreq },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF0A84FF),
                            trackColor = Color(0xFF2C2C2E),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsistencySection(fullSessions: List<SessionWithExercises>) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            
            val buckets = IntArray(4) { 0 }
            
            fullSessions.forEach { session ->
                val diffDays = (now - session.session.timestamp) / dayMs
                if (diffDays in 0..27) {
                    val bucketIndex = 3 - (diffDays / 7).toInt() 
                    if (bucketIndex in 0..3) {
                        buckets[bucketIndex]++
                    }
                }
            }
            
            val labels = listOf("Wk -3", "Wk -2", "Wk -1", "This Wk")
            val maxCount = buckets.maxOrNull() ?: 0
            val isMostConsistent = buckets[3] > 0 && buckets[3] >= buckets.dropLast(1).maxOrNull() ?: 0
            
            val insightText = if (buckets[3] == 0) {
                "No workouts this week."
            } else if (isMostConsistent) {
                "Most consistent week so far."
            } else {
                "${buckets[3]} workouts this week. Keep going!"
            }

            Text("Workout Consistency", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(insightText, fontSize = 14.sp, color = if (isMostConsistent) Color(0xFF34C759) else Color(0xFFEBEBF5).copy(alpha = 0.6f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val animatedProgress = remember { Animatable(0f) }
            LaunchedEffect(buckets) {
                animatedProgress.snapTo(0f)
                animatedProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEachIndexed { index, count ->
                    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    val animatedHeight = progress * animatedProgress.value
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .weight(1f)
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Track
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF2C2C2E))
                            )
                            // Fill
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(animatedHeight.coerceAtLeast(0.01f))
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF0A84FF), Color(0xFF0A84FF).copy(alpha = 0.5f))
                                        )
                                    )
                            )
                            
                            if (count > 0 && animatedProgress.value > 0.8f) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        Text(labels[index], fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun CaloriesTrendSection(mealLogs: List<MealLog>, allMeals: List<Meal>, weightRecords: List<WeightRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Calories vs Weight", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            val todayMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val maxMealDate = mealLogs.maxOfOrNull { it.dateStamp } ?: 0L
            val maxWeightDate = weightRecords.maxOfOrNull { it.dateStamp } ?: 0L
            val endDateMs = maxOf(todayMs, maxMealDate, maxWeightDate)
            
            val last7Days = (6 downTo 0).map { daysAgo ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = endDateMs
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                cal.timeInMillis
            }
            
            val mealMap = allMeals.associateBy { it.id }
            
            val dailyCalories = last7Days.map { dayMs ->
                val logsForDay = mealLogs.filter { it.dateStamp == dayMs }
                logsForDay.sumOf { mealMap[it.mealId]?.calories ?: 0 }.toFloat()
            }
            
            Text("Calories (Last 7 Days)", fontSize = 14.sp, color = Color(0xFF0A84FF))
            BarChart(dataPoints = dailyCalories, labels = emptyList(), modifier = Modifier.height(100.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Weight (Last 7 Days)", fontSize = 14.sp, color = Color(0xFF0A84FF))
            
            val dailyWeights = last7Days.map { dayMs ->
                val wr = weightRecords.find { it.dateStamp == dayMs }
                wr?.weightKg ?: 0f
            }
            
            if (dailyWeights.any { it > 0f }) {
                LineChart(dataPoints = dailyWeights, modifier = Modifier.height(100.dp), lineColor = Color(0xFF0A84FF))
            } else {
                Text("No weight records in this period.", fontSize = 14.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { dayMs ->
                    val label = java.text.SimpleDateFormat("E", java.util.Locale.getDefault()).format(java.util.Date(dayMs))
                    Text(text = label, fontSize = 12.sp, color = Color(0xFFEBEBF5).copy(alpha = 0.6f))
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Meal
import com.example.data.MealLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(viewModel: FitnessViewModel) {
    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val mealLogs by viewModel.mealLogsForSelectedDate.collectAsStateWithLifecycle()
    val allMealLogs by viewModel.allMealLogs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val targetNutrition by viewModel.targetNutrition.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showMealDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var mealNameInput by remember { mutableStateOf("") }
    var caloriesInput by remember { mutableStateOf("") }
    var proteinInput by remember { mutableStateOf("") }
    var reminderHourInput by remember { mutableStateOf<Int?>(null) }
    var reminderMinuteInput by remember { mutableStateOf<Int?>(null) }
    
    var showLogFoodDialog by remember { mutableStateOf<String?>(null) } // holds mealType or null
    var showTargetNutritionDialog by remember { mutableStateOf(false) }
    var targetCaloriesInput by remember { mutableStateOf("") }
    var targetProteinInput by remember { mutableStateOf("") }
    
    val totalCalories = mealLogs.sumOf { log -> allMeals.find { it.id == log.mealId }?.calories ?: 0 }
    val totalProtein = mealLogs.sumOf { log -> (allMeals.find { it.id == log.mealId }?.protein ?: 0f).toDouble() }.toFloat()
    
    val calorieGoal = targetNutrition?.targetCalories ?: 2500
    val proteinGoal = targetNutrition?.targetProtein ?: 150f

    val weeklyData = remember(allMealLogs, selectedDate, allMeals, targetNutrition) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance().apply { 
            timeInMillis = selectedDate 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        
        days.map { day ->
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1
            
            val dayLogs = allMealLogs.filter { it.dateStamp in dayStart..dayEnd }
            val sum = dayLogs.sumOf { log -> allMeals.find { it.id == log.mealId }?.calories ?: 0 }
            
            val percentage = if (calorieGoal > 0) ((sum.toFloat() / calorieGoal) * 100).toInt() else 0
            val isSelectedDate = selectedDate in dayStart..dayEnd
            
            cal.add(Calendar.DAY_OF_MONTH, 1)
            
            com.example.ui.components.ChartData(
                day = day,
                percentage = percentage.coerceAtMost(200), // cap visually
                isHighlighted = isSelectedDate
            )
        }
    }

    val bgDark = Color.Black
    val cardDark = Color(0xFF1C1C1E)
    val textGrey = Color(0xFFEBEBF5).copy(alpha = 0.6f)

    Scaffold(
        containerColor = bgDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(bgDark),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "Nutrition",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 20.dp, top = 40.dp, bottom = 16.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.background(Color.White.copy(alpha=0.15f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DAILY INTAKE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Your Weekly\nProgress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp, lineHeight = 30.sp)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                            val daysTracked = weeklyData.count { it.percentage >= 100 }
                            CircularProgressIndicator(progress = { 1f }, color = Color(0xFF2C2C2E), strokeWidth = 6.dp, modifier = Modifier.fillMaxSize(), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                            CircularProgressIndicator(progress = { daysTracked / 7f }, color = Color(0xFF0A84FF), strokeWidth = 6.dp, modifier = Modifier.fillMaxSize(), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-2).dp)) {
                                Text("$daysTracked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("days", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha=0.8f))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(44.dp).background(Color.Transparent), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Protein", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                                val progress = if (proteinGoal > 0f) (totalProtein / proteinGoal) else 0f
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                    val strokeWidth = 6.dp.toPx()
                                    
                                    // Background Track
                                    drawArc(
                                        color = Color(0xFF2C2C2E),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    
                                    // Foreground Progress (can go over 360)
                                    val sweep = (progress * 360f).toFloat()
                                    // If > 100%, we draw the >360 overlap carefully, or just draw the main arc.
                                    // Android's drawArc wraps automatically if > 360, but drawing it in one go is fine.
                                    // Actually drawArc handles sweepAngle > 360 by just drawing a full circle.
                                    // To show overlap, we might need to draw a full 360 first, then a shadow, then the remainder!
                                    // But if we just draw a SweepAngle it's okay for now. Let's handle up to 360 natively.
                                    drawArc(
                                        color = Color(0xFF0A84FF),
                                        startAngle = -90f,
                                        sweepAngle = sweep.coerceAtMost(360f),
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    
                                    // If progress > 100%, draw the overlapping part with a small drop shadow (using a darker edge)
                                    if (progress > 1f) {
                                        val overSweep = ((progress - 1f) * 360f).coerceAtMost(360f)
                                        drawArc(
                                            color = Color.Black.copy(alpha=0.3f),
                                            startAngle = -90f + overSweep - 5f,
                                            sweepAngle = 5f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = Color(0xFF409CFF), // slightly lighter blue for overlap
                                            startAngle = -90f,
                                            sweepAngle = overSweep,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${totalProtein.toInt()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("g", style = MaterialTheme.typography.bodyLarge, color = textGrey, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
            }

            item {
                com.example.ui.components.CaloriesChartCard(
                    currentCalories = totalCalories,
                    targetCalories = calorieGoal,
                    weeklyData = weeklyData,
                    onTargetClick = {
                        targetCaloriesInput = calorieGoal.toString()
                        targetProteinInput = proteinGoal.toString()
                        showTargetNutritionDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                val calForTitle = Calendar.getInstance().apply { timeInMillis = selectedDate }
                val monthYearStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calForTitle.time)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(monthYearStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { viewModel.setSelectedDate(selectedDate - 86400000L * 7) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setSelectedDate(selectedDate + 86400000L * 7) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val calWeek = Calendar.getInstance().apply { 
                        timeInMillis = selectedDate
                        add(Calendar.DAY_OF_YEAR, -2) // Show 5 days like the image
                    }
                    for (i in 0..4) {
                        val currentLoopDate = calWeek.timeInMillis
                        val c1 = Calendar.getInstance().apply { timeInMillis = currentLoopDate }
                        val c2 = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        val isSelected = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
                        
                        val dayName = SimpleDateFormat("E", Locale.getDefault()).format(calWeek.time).substring(0, 1)
                        val dayNumber = String.format(Locale.getDefault(), "%02d", calWeek.get(Calendar.DAY_OF_MONTH))
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .padding(horizontal = 4.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                .clickable { viewModel.setSelectedDate(currentLoopDate) }
                                .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(dayName, style = MaterialTheme.typography.labelMedium, color = if (isSelected) Color.White else textGrey)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(dayNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        calWeek.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    TextButton(onClick = { /* See all */ }) {
                        Text("See all", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                    }
                }
            }

            val types = listOf("Early Morning", "Breakfast", "Post Workout", "Lunch", "Snacks", "Dinner")
            types.forEach { type ->
                item {
                    val mappedMealType = type
                    val logsForType = mealLogs.filter { it.mealType == mappedMealType }
                    val typeCal = logsForType.sumOf { log -> allMeals.find { it.id == log.mealId }?.calories ?: 0 }
                    
                    val emoji = when(type) {
                        "Early Morning" -> "☕"
                        "Breakfast" -> "🍳"
                        "Post Workout" -> "💪"
                        "Lunch" -> "🍲"
                        "Snacks" -> "🍎"
                        "Dinner" -> "🥗"
                        else -> "🍽️"
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardDark)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(60.dp).background(Color(0xFF2A2D36), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 32.sp)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(type, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${typeCal} kcal", style = MaterialTheme.typography.labelMedium, color = textGrey, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { showLogFoodDialog = mappedMealType },
                                    modifier = Modifier.size(44.dp).background(Color(0xFF2A2D36), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                                }
                            }
                            
                            if (logsForType.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp)
                                ) {
                                    logsForType.forEach { log ->
                                        val meal = allMeals.find { it.id == log.mealId }
                                        if (meal != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(meal.name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                    Text("${meal.calories} kcal • ${meal.protein}g protein", style = MaterialTheme.typography.labelSmall, color = textGrey)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteMealLogById(log.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        editingMeal = null
                        mealNameInput = ""
                        caloriesInput = ""
                        proteinInput = ""
                        reminderHourInput = null
                        reminderMinuteInput = null
                        showMealDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp)
                ) {
                    Text("+ Create Food Template", color = Color(0xFF0A84FF))
                }
            }
        }
    }

    if (showLogFoodDialog != null) {
        var aiInput by remember { mutableStateOf("") }
        val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
        val aiError by viewModel.aiError.collectAsStateWithLifecycle()
        
        val currentLogDate = showLogFoodDialog
        val context = androidx.compose.ui.platform.LocalContext.current
        val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null && currentLogDate != null) {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                viewModel.logMealWithAIFromImage(base64, "image/jpeg", aiInput, currentLogDate) {
                    aiInput = ""
                    showLogFoodDialog = null
                }
            }
        }
        val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null && currentLogDate != null) {
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, source1 ->
                            decoder.isMutableRequired = true
                            val maxDim = 800
                            val scale = if (info.size.width > maxDim || info.size.height > maxDim) {
                                maxDim.toFloat() / kotlin.math.max(info.size.width, info.size.height)
                            } else 1.0f
                            decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                        }
                    } else {
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, stream)
                    val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                    viewModel.logMealWithAIFromImage(base64, "image/jpeg", aiInput, currentLogDate) {
                        aiInput = ""
                        showLogFoodDialog = null
                    }
                } catch(e: Exception) { e.printStackTrace() }
            }
        }

        AlertDialog(
            onDismissRequest = { if (!aiLoading) showLogFoodDialog = null },
            title = { Text("Log to $showLogFoodDialog") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        label = { Text("Log with AI \u2728") },
                        placeholder = { Text("e.g. 2 eggs & toast") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        enabled = !aiLoading,
                        trailingIcon = {
                            if (!aiLoading) {
                                Row {
                                    IconButton(onClick = { cameraLauncher.launch(null) }) {
                                        Icon(Icons.Filled.CameraAlt, contentDescription = "Take Photo")
                                    }
                                    IconButton(onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload Photo")
                                    }
                                }
                            }
                        }
                    )
                    
                    if (aiLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing with AI...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Button(
                            onClick = { 
                                currentLogDate?.let { date ->
                                    viewModel.logMealWithAI(aiInput, date) {
                                        aiInput = ""
                                        showLogFoodDialog = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            enabled = aiInput.isNotBlank()
                        ) {
                            Text("Log Text & Save Template")
                        }
                    }
                    
                    val currentAiError = aiError
                    if (currentAiError != null) {
                        Text(currentAiError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Or select a template:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (allMeals.filter { it.isTemplate }.isEmpty()) {
                        Text("No food templates available. Create one first!")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            items(allMeals.filter { it.isTemplate }) { meal ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            currentLogDate?.let { date ->
                                                viewModel.logMeal(meal.id, date)
                                                showLogFoodDialog = null
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(meal.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                            Text("${meal.calories} kcal • ${meal.protein}g protein", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = {
                                            editingMeal = meal
                                            mealNameInput = meal.name
                                            caloriesInput = meal.calories.toString()
                                            proteinInput = meal.protein.toString()
                                            reminderHourInput = meal.reminderHour
                                            reminderMinuteInput = meal.reminderMinute
                                            showMealDialog = true
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit Template", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogFoodDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showTargetNutritionDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTargetNutritionDialog = false },
            title = { Text("Set Target Nutrition") },
            text = {
                Column {
                    OutlinedTextField(
                        value = targetCaloriesInput,
                        onValueChange = { targetCaloriesInput = it },
                        label = { Text("Daily Calories") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetProteinInput,
                        onValueChange = { targetProteinInput = it },
                        label = { Text("Daily Protein (g)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cals = targetCaloriesInput.toIntOrNull() ?: 2500
                    val prot = targetProteinInput.toFloatOrNull() ?: 150f
                    viewModel.updateTargetNutrition(cals, prot)
                    showTargetNutritionDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetNutritionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMealDialog) {
        val title = if (editingMeal == null) "Add Food Template" else "Edit Template"
        AlertDialog(
            onDismissRequest = { showMealDialog = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mealNameInput,
                        onValueChange = { mealNameInput = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it },
                        label = { Text("Calories (kcal)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val timeText = if (reminderHourInput != null && reminderMinuteInput != null) {
                        val h = reminderHourInput ?: 0
                        val m = reminderMinuteInput ?: 0
                        val amPm = if (h >= 12) "PM" else "AM"
                        val h12 = if (h % 12 == 0) 12 else h % 12
                        String.format("%02d:%02d %s", h12, m, amPm)
                    } else {
                        "Set Reminder Time"
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            val h = reminderHourInput ?: cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val m = reminderMinuteInput ?: cal.get(java.util.Calendar.MINUTE)
                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                reminderHourInput = hourOfDay
                                reminderMinuteInput = minute
                            }, h, m, false).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Reminder")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(timeText)
                    }
                    
                    if (reminderHourInput != null) {
                        TextButton(
                            onClick = { 
                                reminderHourInput = null
                                reminderMinuteInput = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear Reminder")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (mealNameInput.isNotBlank()) {
                        val calories = caloriesInput.toIntOrNull() ?: 0
                        val protein = proteinInput.toFloatOrNull() ?: 0f
                        val currentEditingMeal = editingMeal
                        if (currentEditingMeal != null) {
                            val updatedMeal = currentEditingMeal.copy(
                                name = mealNameInput.trim(),
                                calories = calories,
                                protein = protein,
                                reminderHour = reminderHourInput,
                                reminderMinute = reminderMinuteInput
                            )
                            viewModel.updateMeal(updatedMeal, context)
                        } else {
                            viewModel.addMeal(mealNameInput.trim(), calories, protein, reminderHourInput, reminderMinuteInput, context)
                        }
                        showMealDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentEditingMeal = editingMeal
                    if (currentEditingMeal != null) {
                        TextButton(onClick = { 
                            viewModel.deleteMeal(currentEditingMeal, context)
                            showMealDialog = false 
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showMealDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

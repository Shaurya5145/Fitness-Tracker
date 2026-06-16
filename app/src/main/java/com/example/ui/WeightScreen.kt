package com.example.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LineChart
import java.text.SimpleDateFormat
import java.util.*

val ColorGlassBackground = Color.White.copy(alpha = 0.04f)
val ColorGlassBorder = Color.White.copy(alpha = 0.08f)
val ColorCyanBlueGradient = Brush.horizontalGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)))
val ColorGreenGoldGradient = Brush.horizontalGradient(listOf(Color(0xFF0BA360), Color(0xFFF3C72B)))
val ColorRedOrangeGradient = Brush.horizontalGradient(listOf(Color(0xFFFF0844), Color(0xFFFFB199)))
val BrandBlue = Color(0xFF007AFF)

@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    padding: Dp = 16.dp,
    alpha: Float = 0.04f
) = this
    .background(Color.White.copy(alpha = alpha), RoundedCornerShape(cornerRadius))
    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius))
    .padding(padding)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(viewModel: FitnessViewModel) {
    val weightRecords by viewModel.weightRecords.collectAsStateWithLifecycle()
    val targetWeight by viewModel.targetWeight.collectAsStateWithLifecycle()
    
    var showSheet by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var selectedDateToEdit by remember { mutableStateOf<Long?>(null) }
    
    var showTargetDialog by remember { mutableStateOf(false) }
    var targetWeightInput by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val sortedRecords = weightRecords.sortedBy { it.dateStamp }
    val currentWeight = sortedRecords.lastOrNull()?.weightKg ?: 0f
    val lastDateStr = if (sortedRecords.isNotEmpty()) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(sortedRecords.last().dateStamp))
    } else {
        "No data"
    }
    
    val weekStart = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val thisWeekRecords = weightRecords.filter { it.dateStamp >= weekStart }
    val weeklyAverage = if (thisWeekRecords.isNotEmpty()) thisWeekRecords.map { it.weightKg }.average().toFloat() else 0f
    
    val monthStart = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
    val monthRecords = sortedRecords.filter { it.dateStamp >= monthStart }
    val startOfMonthRecord = monthRecords.firstOrNull()?.weightKg
    val monthlyChange = if (startOfMonthRecord != null && currentWeight > 0f) currentWeight - startOfMonthRecord else 0f

    val prevWeight = if (sortedRecords.size >= 2) sortedRecords[sortedRecords.size - 2].weightKg else currentWeight
    val dailyDiff = currentWeight - prevWeight

    Scaffold(
        containerColor = Color(0xFF050505), // Ultra-deep dark mode
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    weightInput = ""
                    selectedDateToEdit = null
                    showSheet = true 
                },
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .background(ColorCyanBlueGradient, CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF4FACFE)),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Weight", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            
            // Header
            item {
                Column {
                    Text(
                        "Weight Wellness",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Last updated: $lastDateStr",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Hero Section (Current Weight)
            item {
                Column(modifier = Modifier.fillMaxWidth().glassCard(padding = 32.dp)) {
                    Text("Current Weight", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (currentWeight > 0f) "%.1f".format(currentWeight) else "--",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp,
                            color = Color.White,
                            letterSpacing = (-2).sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("kg", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 28.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 10.dp))
                        
                        Spacer(Modifier.weight(1f))
                        if (sortedRecords.size >= 2 && dailyDiff != 0f) {
                            val icon = if (dailyDiff > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(bottom = 18.dp).size(28.dp))
                        }
                    }
                    if (sortedRecords.size >= 2) {
                        val sign = if (dailyDiff > 0) "+" else ""
                        Text("$sign%.1f kg from yesterday".format(dailyDiff), fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
            
            // Metrics Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 7D Average
                    Column(modifier = Modifier.weight(1f).glassCard(padding = 20.dp), horizontalAlignment = Alignment.Start) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (weeklyAverage > 0f) "%.1f kg".format(weeklyAverage) else "--", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("7D Average", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    // 30D Change
                    Column(modifier = Modifier.weight(1f).glassCard(padding = 20.dp), horizontalAlignment = Alignment.Start) {
                        Icon(Icons.Filled.Timeline, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(12.dp))
                        val sign = if (monthlyChange > 0) "+" else ""
                        val text = if (startOfMonthRecord != null) "$sign%.1f kg".format(monthlyChange) else "--"
                        
                        if (startOfMonthRecord != null && monthlyChange != 0f) {
                           Text(
                               text = text, 
                               fontWeight = FontWeight.SemiBold, 
                               fontSize = 20.sp,
                               style = androidx.compose.ui.text.TextStyle(
                                   brush = if (monthlyChange < 0) ColorGreenGoldGradient else ColorRedOrangeGradient
                               )
                           )
                        } else {
                            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White)
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        Text("30D Change", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            // Weight Trend Chart
            item {
                Column(modifier = Modifier.fillMaxWidth().glassCard(cornerRadius = 32.dp, padding = 24.dp).animateContentSize()) {
                    Text("Weight Trend", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    if (sortedRecords.size >= 2) {
                        val points = sortedRecords.map { it.weightKg }
                        LineChart(
                            dataPoints = points, 
                            modifier = Modifier.height(180.dp).fillMaxWidth(),
                            targetGoalValue = targetWeight?.targetWeightKg
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            Text(sdf.format(Date(sortedRecords.first().dateStamp)), fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                            Text(sdf.format(Date(sortedRecords.last().dateStamp)), fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            Text("Log more weight entries to see your trend.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // Goal Progress card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            targetWeightInput = targetWeight?.targetWeightKg?.toString() ?: ""
                            showTargetDialog = true
                        }
                        .glassCard(padding = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Personal Target", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        
                        val tw = targetWeight
                        if (tw != null) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                            Text("${tw.targetWeightKg} kg by ${sdf.format(Date(tw.targetDateStamp))}", fontSize = 14.sp, color = BrandBlue)
                            
                            val startWeight = tw.startWeightKg.takeIf { it > 0f } ?: (sortedRecords.firstOrNull()?.weightKg ?: currentWeight)
                            val diffTotal = startWeight - tw.targetWeightKg
                            val diffCurrent = currentWeight - tw.targetWeightKg
                            
                            val isLosing = diffTotal > 0
                            val progressRaw = if (diffTotal == 0f) 1f else {
                                if (isLosing) {
                                    if (currentWeight <= tw.targetWeightKg) 1f
                                    else 1f - (diffCurrent / diffTotal).coerceIn(0f, 1f)
                                } else {
                                    if (currentWeight >= tw.targetWeightKg) 1f
                                    else 1f - (diffCurrent / diffTotal).coerceIn(0f, 1f)
                                }
                            }
                            // ensure never negative
                            val progress = progressRaw.coerceIn(0.01f, 1f)
                            
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color.White.copy(alpha = 0.1f), strokeWidth = 5.dp)
                                    val animatedProgress by animateFloatAsState(targetValue = progress, label = "goal_progress")
                                    CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), color = BrandBlue, strokeWidth = 5.dp, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    val text = if (isLosing) "%.1f kg to target".format(diffCurrent.coerceAtLeast(0f)) else "%.1f kg to target".format((-diffCurrent).coerceAtLeast(0f))
                                    Text(if (progress >= 1f) "Goal Achieved!" else text, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        } else {
                            Text("No target set. Tap to set one.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Goal", tint = Color.White.copy(alpha = 0.2f))
                }
            }

            // Activity Log
            if (sortedRecords.isNotEmpty()) {
                item {
                    Text("Activity Log", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(sortedRecords.reversed().take(15).withIndex().toList(), key = { it.value.dateStamp }) { indexedValue ->
                    val record = indexedValue.value
                    // To compare with previous entry chronologically (next in reversed list):
                    val prevRec = sortedRecords.reversed().getOrNull(indexedValue.index + 1)
                    val recDiff = if (prevRec != null) record.weightKg - prevRec.weightKg else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            weightInput = record.weightKg.toString()
                            selectedDateToEdit = record.dateStamp
                            showSheet = true
                        }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text(sdf.format(Date(record.dateStamp)), fontSize = 15.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(110.dp))
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text("%.1f kg".format(record.weightKg), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (recDiff != 0f) {
                                val ic = if (recDiff > 0) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                                val c = if (recDiff > 0) Color(0xFFFFB199) else Color(0xFF0BA360)
                                Icon(ic, contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        IconButton(onClick = { viewModel.deleteWeightRecord(record) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Color(0xFF1E1E1E), // Soft dark gray for the sheet
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                Text(if (selectedDateToEdit == null) "Log Daily Weight" else "Edit Weight", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = BrandBlue
                    )
                )
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(
                        onClick = { showSheet = false },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            val weight = weightInput.toFloatOrNull()
                            if (weight != null) {
                                viewModel.addWeightRecord(weight, selectedDateToEdit ?: System.currentTimeMillis())
                                showSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Entry", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = { Text("Set Target Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    OutlinedTextField(
                        value = targetWeightInput,
                        onValueChange = { targetWeightInput = it },
                        label = { Text("Target Weight (kg)", color = Color.White.copy(alpha=0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color.White.copy(alpha=0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val dateToShow = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        Text("Target Date: ${sdf.format(Date(dateToShow))}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val weight = targetWeightInput.toFloatOrNull()
                    val date = datePickerState.selectedDateMillis
                    if (weight != null && date != null) {
                        val currentW = weightRecords.sortedBy { it.dateStamp }.lastOrNull()?.weightKg ?: 0f
                        viewModel.updateTargetWeight(weight, date, currentW)
                        showTargetDialog = false
                    }
                }) {
                    Text("Save", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("Cancel", color = Color.White, fontSize = 16.sp)
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

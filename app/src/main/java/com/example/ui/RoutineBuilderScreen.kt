package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.data.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    initialRoutineIndex: Long? = null,
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onSaveRoutine: () -> Unit
) {
    val routines by viewModel.allRoutinesWithExercises.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var routineName by remember { mutableStateOf("") }
    var folderId by remember { mutableStateOf("My Routines") }
    var routineDescription by remember { mutableStateOf("") }
    var showDescription by remember { mutableStateOf(false) }
    
    var exercises by remember { mutableStateOf(listOf<RoutineExerciseWithTargets>()) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showExerciseSelection by remember { mutableStateOf(false) }

    // Init state if editing
    LaunchedEffect(initialRoutineIndex) {
        if (initialRoutineIndex != null) {
            val existing = routines.find { it.routine.id == initialRoutineIndex }
            if (existing != null) {
                routineName = existing.routine.name
                folderId = existing.routine.folderId
                routineDescription = existing.routine.description
                showDescription = routineDescription.isNotBlank()
                exercises = existing.exercises.sortedBy { it.exercise.orderIndex }
            }
        }
    }

    val hasChanges = true // Simplified
    val canSave = routineName.isNotBlank() && exercises.isNotEmpty() && exercises.all { it.targets.isNotEmpty() }

    BackHandler {
        if (hasChanges && exercises.isNotEmpty()) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Discard this routine?", color = Color.White) },
            text = { Text("Your exercises and set targets will not be saved.", color = Color.Gray) },
            containerColor = Color(0xFF161618),
            confirmButton = {
                TextButton(onClick = { 
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text("Discard", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Keep editing", color = Color(0xFF0A84FF))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialRoutineIndex == null) "Create Routine" else "Edit Routine", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges && exercises.isNotEmpty()) showUnsavedDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val r = Routine(
                                id = initialRoutineIndex ?: 0L,
                                name = routineName,
                                description = routineDescription,
                                folderId = folderId,
                                estimatedDuration = exercises.size * 10
                            )
                            viewModel.saveRoutine(r, exercises)
                            onSaveRoutine()
                        },
                        enabled = canSave
                    ) {
                        Text("Save", color = if (canSave) Color(0xFF0A84FF) else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000))
            )
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main content
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Routine Details Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Routine name", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = routineName,
                                onValueChange = { routineName = it },
                                placeholder = { Text("e.g. Push Day", color = Color.DarkGray) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF202024),
                                    unfocusedContainerColor = Color(0xFF202024),
                                    focusedIndicatorColor = Color(0xFF0A84FF),
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Folder details
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { /* TODO open folder select */ }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Folder", color = Color.White, fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(folderId, color = Color.Gray, fontSize = 16.sp)
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Select Folder", tint = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (showDescription) {
                                TextField(
                                    value = routineDescription,
                                    onValueChange = { routineDescription = it },
                                    placeholder = { Text("Add instructions, tempo or progression notes", color = Color.DarkGray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF202024),
                                        unfocusedContainerColor = Color(0xFF202024),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            } else {
                                Text(
                                    text = "Add routine description",
                                    color = Color(0xFF0A84FF),
                                    fontSize = 15.sp,
                                    modifier = Modifier.clickable { showDescription = true }.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                if (exercises.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Build your routine", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add exercises, choose your target sets and configure your rest periods.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showExerciseSelection = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("+ Add Exercise", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    itemsIndexed(exercises) { index, exWithTargets ->
                        RoutineExerciseBlock(
                            exercise = exWithTargets,
                            onUpdate = { updated ->
                                val mut = exercises.toMutableList()
                                mut[index] = updated
                                exercises = mut
                            },
                            onRemove = {
                                val mut = exercises.toMutableList()
                                mut.removeAt(index)
                                exercises = mut
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val mut = exercises.toMutableList()
                                    Collections.swap(mut, index, index - 1)
                                    exercises = mut
                                }
                            },
                            onMoveDown = {
                                if (index < exercises.size - 1) {
                                    val mut = exercises.toMutableList()
                                    Collections.swap(mut, index, index + 1)
                                    exercises = mut
                                }
                            }
                        )
                    }
                }
            }

            // Sticky Bottom
            if (exercises.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Color(0xFF161618).copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    val totalSets = exercises.sumOf { it.targets.size }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${exercises.size} exercises · $totalSets sets", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("~${exercises.size * 10} min", color = Color.Gray, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { showExerciseSelection = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202024)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("+ Add Exercise", color = Color(0xFF0A84FF), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        
        if (showExerciseSelection) {
            ExerciseLibrarySheet(
                onDismiss = { showExerciseSelection = false },
                onExercisesSelected = { selectedNames ->
                    val newExercises = selectedNames.mapIndexed { idx, name ->
                        val ex = RoutineExercise(
                            routineId = 0L,
                            exerciseName = name,
                            orderIndex = exercises.size + idx
                        )
                        RoutineExerciseWithTargets(
                            exercise = ex,
                            targets = listOf(SetTarget(routineExerciseId = 0L, orderIndex = 0, targetReps = 10, targetWeight = 20f))
                        )
                    }
                    exercises = exercises + newExercises
                }
            )
        }
    }
}

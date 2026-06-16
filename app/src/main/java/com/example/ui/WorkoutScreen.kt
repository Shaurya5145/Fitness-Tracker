package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.togetherWith
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(viewModel: FitnessViewModel) {
    val workoutSessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
    val routines by viewModel.allRoutinesWithExercises.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var showRoutineBuilder by remember { mutableStateOf(false) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }

    androidx.compose.animation.AnimatedContent(
        targetState = Triple(selectedSessionId, showRoutineBuilder, editingRoutineId),
        transitionSpec = {
            if (targetState.second || targetState.third != null) {
                // Sliding to routine builder
                androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) togetherWith androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it })
            } else if (initialState.second || initialState.third != null) {
                // Returning from routine builder
                androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) togetherWith androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it })
            } else {
                androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
            }
        },
        label = "WorkoutNavigation"
    ) { (currentId, isBuilder, currRoutineId) ->
        if (isBuilder || currRoutineId != null) {
            RoutineBuilderScreen(
                initialRoutineIndex = currRoutineId,
                viewModel = viewModel,
                onBack = {
                    showRoutineBuilder = false
                    editingRoutineId = null
                },
                onSaveRoutine = {
                    showRoutineBuilder = false
                    editingRoutineId = null
                }
            )
        } else if (currentId == null) {
            WorkoutList(
                sessions = workoutSessions,
                routines = routines,
                viewModel = viewModel,
                selectedDate = selectedDate,
                onDateChange = { viewModel.setSelectedDate(it) },
                onSessionClick = { selectedSessionId = it },
                onAddSession = { name, timestamp -> viewModel.addWorkoutSession(name, timestamp) },
                onDuplicateSession = { session -> viewModel.duplicateWorkoutSession(session, selectedDate) },
                onDeleteSession = { session -> viewModel.deleteWorkoutSession(session.session) },
                onRenameSession = { session, newName -> viewModel.updateWorkoutSessionName(session.session, newName) },
                onCreateRoutineClick = { showRoutineBuilder = true },
                onEditRoutineClick = { editingRoutineId = it },
                onStartRoutine = { routine ->
                    // Convert routine to session and start
                    viewModel.createSessionFromRoutineAsync(routine) { sessionId ->
                        selectedSessionId = sessionId
                    }
                }
            )
        } else {
            val fullSession = workoutSessions.find { it.session.id == currentId }
            if (fullSession != null) {
                WorkoutDetail(
                    sessionWithExercises = fullSession,
                    allSessions = workoutSessions,
                    onBack = { 
                        viewModel.cleanUpEmptySets(currentId)
                        selectedSessionId = null 
                    },
                    onAddExercise = { exerciseName -> viewModel.addExerciseToSession(currentId, exerciseName) },
                    onAddSet = { exerciseId, setNumber, reps, weight, isDropSet, duration, distance ->
                        viewModel.addSetToExercise(exerciseId, setNumber, reps, weight, isDropSet, duration, distance)
                    },
                    onUpdateSet = { set -> viewModel.updateSet(set) },
                    onUpdateSets = { sets -> viewModel.updateSets(sets) },
                    onUpdateExercise = { exercise -> viewModel.updateExercise(exercise) },
                    onDeleteExercise = { exerciseId ->
                        val exerciseObj = fullSession.exercises.find { it.exercise.id == exerciseId }?.exercise
                        if (exerciseObj != null) {
                            viewModel.deleteExercise(exerciseObj)
                        }
                    },
                    onDeleteSession = {
                        viewModel.deleteWorkoutSession(fullSession.session)
                        selectedSessionId = null
                    },
                    onFinishSession = {
                        viewModel.finishWorkoutSession(fullSession.session)
                    },
                    onUpdateSessionNotes = { newNotes ->
                        viewModel.updateWorkoutSessionNotes(fullSession.session, newNotes)
                    },
                    restTimerSeconds = restTimerSeconds,
                    onStartRestTimer = { viewModel.startRestTimer(it) },
                    onStopRestTimer = { viewModel.stopRestTimer() },
                    onAddRestTime = { viewModel.addRestTime(it) }
                )
            } else {
                LaunchedEffect(Unit) {
                    selectedSessionId = null
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutList(
    sessions: List<SessionWithExercises>,
    routines: List<RoutineWithExercises>,
    viewModel: FitnessViewModel,
    selectedDate: Long,
    onDateChange: (Long) -> Unit,
    onSessionClick: (Long) -> Unit,
    onAddSession: (String, Long) -> Unit,
    onDuplicateSession: (SessionWithExercises) -> Unit,
    onDeleteSession: (SessionWithExercises) -> Unit,
    onRenameSession: (SessionWithExercises, String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onEditRoutineClick: (Long) -> Unit,
    onStartRoutine: (RoutineWithExercises) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedSessionsForDelete by remember { mutableStateOf(setOf<SessionWithExercises>()) }
    var sessionToRename by remember { mutableStateOf<SessionWithExercises?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filteredSessions = sessions.filter {
        it.session.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectMode) "${selectedSessionsForDelete.size} Selected" else "Workouts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    if (isSelectMode) {
                        if (selectedSessionsForDelete.isNotEmpty()) {
                            IconButton(onClick = {
                                selectedSessionsForDelete.forEach { onDeleteSession(it) }
                                selectedSessionsForDelete = emptySet()
                                isSelectMode = false
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = {
                            isSelectMode = false
                            selectedSessionsForDelete = emptySet()
                        }) {
                            Text("Cancel", color = Color.White)
                        }
                    } else {
                        TextButton(onClick = { isSelectMode = true }) {
                            Text("Select", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSessionDialog = true },
                containerColor = Color(0xFF1E88E5),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Workout Session", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).clickable { onAddSession("Empty Workout", System.currentTimeMillis()) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFF0A84FF).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Start Empty Workout", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Build your session as you train", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("My Routines", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("+ New Routine", color = Color(0xFF0A84FF), fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onCreateRoutineClick() }.padding(8.dp))
                }
            }
            
            if (routines.isEmpty()) {
                item {
                    Text("No routines yet. Create one to get started.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(routines, key = { it.routine.id }) { routineWithEx ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).clickable { onEditRoutineClick(routineWithEx.routine.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(routineWithEx.routine.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
                                var expandedMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { expandedMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Routine Options", tint = Color.Gray)
                                    }
                                    androidx.compose.material3.DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }, containerColor = Color(0xFF202024)) {
                                        androidx.compose.material3.DropdownMenuItem(text = { Text("Edit routine", color = Color.White) }, onClick = { expandedMenu = false; onEditRoutineClick(routineWithEx.routine.id) })
                                        androidx.compose.material3.DropdownMenuItem(text = { Text("Delete routine", color = Color(0xFFE53935)) }, onClick = { expandedMenu = false; viewModel.deleteRoutine(routineWithEx.routine) })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val totalSets = routineWithEx.exercises.sumOf { it.targets.size }
                            Text("${routineWithEx.exercises.size} exercises · ${totalSets} sets · ~${routineWithEx.routine.estimatedDuration} min", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onStartRoutine(routineWithEx) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF).copy(alpha = 0.15f), contentColor = Color(0xFF0A84FF)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Routine", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("History", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            if (sessions.isEmpty()) {
                item {
                    Text("No workout history yet.", color = Color.Gray)
                }
            } else {
                items(sessions.sortedByDescending { it.session.timestamp }) { sessionData ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .clickable { onSessionClick(sessionData.session.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val dateString = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sessionData.session.timestamp))
                            val titleName = sessionData.session.name.ifBlank { "Workout" }
                            Text(text = titleName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(text = dateString, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (sessionData.exercises.isNotEmpty()) {
                                val maxDisplay = if (sessionData.exercises.size <= 3) sessionData.exercises.size else 2
                                sessionData.exercises.take(maxDisplay).forEach { ex ->
                                    val setsCount = ex.sets.size
                                    Text(
                                        text = "${setsCount} × ${ex.exercise.exerciseName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                if (sessionData.exercises.size > 3) {
                                    Text("+${sessionData.exercises.size - 2} more exercises", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                }
                            } else {
                                Text("No exercises logged", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSessionDialog) {
        var sessionNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSessionDialog = false },
            title = { Text("New Workout Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enter a name for your new workout session.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    OutlinedTextField(
                        value = sessionNameInput,
                        onValueChange = { sessionNameInput = it },
                        placeholder = { Text("Workout Name (e.g. Leg Day)", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        onAddSession(sessionNameInput.trim(), selectedDate)
                        showAddSessionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Start Workout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSessionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    val currentSessionToRename = sessionToRename
    if (currentSessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Workout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF1A1A1A),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSession(currentSessionToRename, renameInput.trim())
                        sessionToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Rename", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetail(
    sessionWithExercises: SessionWithExercises,
    allSessions: List<SessionWithExercises>,
    onBack: () -> Unit,
    onAddExercise: (String) -> Unit,
    onAddSet: (Long, Int, Int, Float, Boolean, Int, Float) -> Unit,
    onUpdateSet: (com.example.data.ExerciseSet) -> Unit,
    onUpdateSets: (List<com.example.data.ExerciseSet>) -> Unit,
    onUpdateExercise: (com.example.data.WorkoutExercise) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onDeleteSession: () -> Unit,
    onFinishSession: () -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    restTimerSeconds: Int,
    onStartRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onAddRestTime: (Int) -> Unit
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(sessionWithExercises.session.timestamp))
                    val titleName = sessionWithExercises.session.name.ifBlank { "Workout" }
                    Column {
                        Text(titleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteSession) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Session", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    val isFinished = sessionWithExercises.session.endTimeStamp > 0L
                    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
                    
                    LaunchedEffect(isFinished) {
                        if (!isFinished) {
                            while (true) {
                                kotlinx.coroutines.delay(1000)
                                currentTime = System.currentTimeMillis()
                            }
                        }
                    }

                    val durationMillis = if (isFinished && sessionWithExercises.session.startTimeStamp > 0L) {
                        sessionWithExercises.session.endTimeStamp - sessionWithExercises.session.startTimeStamp
                    } else if (isFinished && sessionWithExercises.session.startTimeStamp == 0L) {
                        sessionWithExercises.session.endTimeStamp - sessionWithExercises.session.timestamp
                    } else if (sessionWithExercises.session.startTimeStamp > 0L) {
                        currentTime - sessionWithExercises.session.startTimeStamp
                    } else {
                        0L
                    }
                    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60

                    Card(
                        modifier = Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DURATION", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hours > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                                           else String.format(Locale.getDefault(), "00:%02d:%02d", minutes, seconds),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Light
                                )
                            }
                            if (!isFinished) {
                                Button(
                                    onClick = { onFinishSession() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5), contentColor = Color.White),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text("Finish", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item {
                    var notesText by remember(sessionWithExercises.session.notes) { mutableStateOf(sessionWithExercises.session.notes) }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { 
                            notesText = it
                            onUpdateSessionNotes(it)
                        },
                        placeholder = { Text("Session notes...", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                items(sessionWithExercises.exercises, key = { it.exercise.id }) { exerciseWithSets ->
                    ExerciseCard(
                        exerciseWithSets = exerciseWithSets,
                        allSessions = allSessions,
                        onAddSet = { exerciseId, setNumber, reps, weight, isDropSet, duration, distance -> 
                            onAddSet(exerciseId, setNumber, reps, weight, isDropSet, duration, distance)
                        },
                        onUpdateSet = { set -> 
                            onUpdateSet(set)
                        },
                        onUpdateSets = onUpdateSets,
                        onUpdateExercise = onUpdateExercise,
                        onDeleteExercise = onDeleteExercise,
                        onStartRestTimer = { onStartRestTimer(90) }
                    )
                }
                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212).copy(alpha = 0.5f), contentColor = Color(0xFF1E88E5)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.3f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Exercise", modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Exercise", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                RestTimerOverlay(
                    secondsLeft = restTimerSeconds,
                    onStop = onStopRestTimer,
                    onAdd30s = { onAddRestTime(30) }
                )
            }
        }
    }

    if (showAddExerciseDialog) {
        var exerciseNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Add Exercise", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("What exercise do you want to add?", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    OutlinedTextField(
                        value = exerciseNameInput,
                        onValueChange = { exerciseNameInput = it },
                        placeholder = { Text("Exercise Name (e.g. Bench Press)", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        if (exerciseNameInput.isNotBlank()) {
                            onAddExercise(exerciseNameInput.trim())
                            showAddExerciseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun RestTimerOverlay(
    secondsLeft: Int,
    onStop: () -> Unit,
    onAdd30s: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = secondsLeft > 0,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeOut()
    ) {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeString = String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
        
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
        
        Card(
            modifier = Modifier
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(0.95f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212).copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1E88E5).copy(alpha = pulseAlpha))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("REST TIMER", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E88E5), letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(timeString, style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Light)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAdd30s,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text("+30s", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = onStop, 
                        modifier = Modifier.size(48.dp).background(Color(0xFF1E88E5).copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Stop", tint = Color(0xFF1E88E5), modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

fun isCardioExercise(name: String): Boolean {
    val words = name.lowercase().split(Regex("\\W+"))
    val cardioWords = setOf("treadmill", "run", "running", "cardio", "walk", "walking", "cycle", "cycling", "bike", "biking", "elliptical", "stair", "stairs", "rowing", "swim", "swimming", "stepper")
    return words.any { it in cardioWords }
}

@Composable
fun ExerciseCard(
    exerciseWithSets: ExerciseWithSets,
    allSessions: List<SessionWithExercises>,
    onAddSet: (Long, Int, Int, Float, Boolean, Int, Float) -> Unit,
    onUpdateSet: (com.example.data.ExerciseSet) -> Unit,
    onUpdateSets: (List<com.example.data.ExerciseSet>) -> Unit,
    onUpdateExercise: (com.example.data.WorkoutExercise) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onStartRestTimer: () -> Unit
) {
    val isCardio = remember(exerciseWithSets.exercise.exerciseName) {
        isCardioExercise(exerciseWithSets.exercise.exerciseName)
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            var showEditName by remember { mutableStateOf(false) }

            if (showEditName) {
                var editName by remember { mutableStateOf(exerciseWithSets.exercise.exerciseName) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Exercise Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateExercise(exerciseWithSets.exercise.copy(exerciseName = editName.trim()))
                        }
                        showEditName = false
                    }, modifier = Modifier.background(Color(0xFF1E88E5).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)) {
                        Icon(Icons.Filled.Check, contentDescription = "Save Edit", tint = Color(0xFF1E88E5))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exerciseWithSets.exercise.exerciseName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E88E5), // Signature Blue
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.clickable { showEditName = true }.weight(1f)
                    )
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(32.dp).background(Color(0xFFE53935).copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Exercise", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                    }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Remove Exercise?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = { Text("Are you sure you want to remove ${exerciseWithSets.exercise.exerciseName} from this workout?", color = Color.LightGray) },
                            containerColor = Color(0xFF1A1A1A),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                            confirmButton = {
                                Button(onClick = {
                                    showDeleteConfirm = false
                                    onDeleteExercise(exerciseWithSets.exercise.id)
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            }
                        )
                    }
                }
            }
            
            if (!showEditName && !isCardio) {
                val best1RM = exerciseWithSets.sets.maxOfOrNull { it.weightKg * (1f + it.reps / 30f) } ?: 0f
                if (best1RM > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Est. 1RM: ${String.format(java.util.Locale.getDefault(), "%.1f", best1RM)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            val previousPerformance = remember(exerciseWithSets.exercise.exerciseName, allSessions) {
                val currentSessionTime = allSessions.find { it.session.id == exerciseWithSets.exercise.sessionId }?.session?.timestamp ?: 0L
                allSessions
                    .filter { it.session.id != exerciseWithSets.exercise.sessionId && it.session.timestamp <= currentSessionTime }
                    .sortedByDescending { it.session.timestamp }
                    .firstNotNullOfOrNull { session ->
                        session.exercises.find { it.exercise.exerciseName.equals(exerciseWithSets.exercise.exerciseName, ignoreCase = true) && it.sets.isNotEmpty() }
                    }
            }

            val prevSetsMap = remember(previousPerformance) {
                previousPerformance?.sets?.associateBy { it.setNumber } ?: emptyMap()
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SET", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.4f), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 1.sp)
                    Text("PREVIOUS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 1.sp)
                    Text(if (isCardio) "MINS" else "KG", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 1.sp)
                    Text(if (isCardio) "KM" else "REPS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 1.sp)
                    if (!isCardio) Text("DROP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.4f), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 1.sp) else Spacer(modifier = Modifier.weight(0.4f))
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.weight(0.5f), tint = Color.Gray)
                }
                
                val completedStates = remember { mutableStateMapOf<Long, Boolean>() }
                
                exerciseWithSets.sets.sortedBy { it.setNumber }.forEach { set ->
                    val isCompleted = completedStates[set.id] ?: false
                    val rowColor by animateColorAsState(targetValue = if (isCompleted) Color(0xFF1E88E5).copy(alpha = 0.1f) else Color.Transparent)
                    val rowAlpha by animateFloatAsState(targetValue = if (isCompleted) 0.5f else 1f)
                    
                    val prev = prevSetsMap[set.setNumber]
                    val prevStr = if (prev != null) {
                        if (isCardio) {
                            val dMins = if (prev.durationMinutes > 0) prev.durationMinutes.toString() else "-"
                            val dKm = if (prev.distance > 0f) prev.distance.toString() else "-"
                            "${dMins}m × ${dKm}km"
                        } else {
                            val wKg = if (prev.weightKg > 0f) prev.weightKg.let { if (it % 1 == 0f) "${it.toInt()}" else "$it" } else "-"
                            val r = if (prev.reps > 0) prev.reps.toString() else "-"
                            val dropTxt = if (prev.isDropSet) " (Drop)" else ""
                            "${wKg}kg × $r$dropTxt"
                        }
                    } else "-"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(rowColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(vertical = 6.dp)
                            .alpha(rowAlpha),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Set column
                        Box(modifier = Modifier.weight(0.4f), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${set.setNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCompleted) Color.White else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Previous column
                        Text(
                            text = prevStr,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        // KG column
                        var editWeight by remember(set.weightKg, set.durationMinutes) { mutableStateOf(if (isCardio) (if (set.durationMinutes > 0) set.durationMinutes.toString() else "") else (if (set.weightKg > 0f) set.weightKg.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } else "")) }
                        
                        Box(modifier = Modifier.weight(0.7f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                            val inputBg by animateColorAsState(if (isCompleted) Color.Transparent else Color(0xFF1E1E1E))
                            androidx.compose.foundation.text.BasicTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.White, fontWeight = FontWeight.SemiBold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(inputBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                    .padding(vertical = 12.dp)
                            )
                        }
                        
                        // REPS column
                        var editReps by remember(set.reps, set.distance) { mutableStateOf(if (isCardio) (if (set.distance > 0f) set.distance.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } else "") else (if (set.reps > 0) set.reps.toString() else "")) }
                        
                        LaunchedEffect(editWeight, editReps) {
                            kotlinx.coroutines.delay(1000)
                            val dur = if (isCardio) editWeight.toIntOrNull() ?: 0 else 0
                            val dist = if (isCardio) editReps.toFloatOrNull() ?: 0f else 0f
                            val w = if (isCardio) 0f else editWeight.toFloatOrNull() ?: 0f
                            val r = if (isCardio) 0 else editReps.toIntOrNull() ?: 0
                            if (w != set.weightKg || r != set.reps || dur != set.durationMinutes || dist != set.distance) {
                                onUpdateSet(set.copy(reps = r, weightKg = w, durationMinutes = dur, distance = dist))
                            }
                        }

                        Box(modifier = Modifier.weight(0.7f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                            val inputBg by animateColorAsState(if (isCompleted) Color.Transparent else Color(0xFF1E1E1E))
                            androidx.compose.foundation.text.BasicTextField(
                                value = editReps,
                                onValueChange = { editReps = it },
                                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.White, fontWeight = FontWeight.SemiBold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(inputBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                    .padding(vertical = 12.dp)
                            )
                        }
                        
                        // Drop column
                        if (!isCardio) {
                            Box(modifier = Modifier.weight(0.4f), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Checkbox(
                                    checked = set.isDropSet,
                                    onCheckedChange = { checked ->
                                        val dur = editWeight.toIntOrNull() ?: 0
                                        val dist = editReps.toFloatOrNull() ?: 0f
                                        val w = editWeight.toFloatOrNull() ?: 0f
                                        val r = editReps.toIntOrNull() ?: 0
                                        onUpdateSet(set.copy(isDropSet = checked, reps = r, weightKg = w, durationMinutes = dur, distance = dist))
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF1E88E5),
                                        uncheckedColor = Color.DarkGray,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(0.4f))
                        }
                        
                        // Check column
                        Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                            val checkBg by animateColorAsState(if (isCompleted) Color(0xFF1E88E5) else Color(0xFF1A1A1A))
                            val checkTint by animateColorAsState(if (isCompleted) Color.White else Color.Gray)
                            
                            IconButton(
                                onClick = { 
                                    val dur = if (isCardio) editWeight.toIntOrNull() ?: 0 else 0
                                    val dist = if (isCardio) editReps.toFloatOrNull() ?: 0f else 0f
                                    val w = if (isCardio) 0f else editWeight.toFloatOrNull() ?: 0f
                                    val r = if (isCardio) 0 else editReps.toIntOrNull() ?: 0
                                    onUpdateSet(set.copy(reps = r, weightKg = w, durationMinutes = dur, distance = dist))
                                    if (!isCompleted) {
                                        onStartRestTimer()
                                    }
                                    completedStates[set.id] = !isCompleted
                                },
                                modifier = Modifier.size(40.dp).background(checkBg, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            ) {
                                androidx.compose.animation.AnimatedContent(targetState = isCompleted, label = "Check") { checked ->
                                    if (checked) {
                                        Icon(Icons.Filled.Check, contentDescription = "Uncomplete Set", tint = checkTint, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Filled.Check, contentDescription = "Complete Set", tint = checkTint, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { 
                    val nextSet = (exerciseWithSets.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                    val isPrevDrop = prevSetsMap[nextSet]?.isDropSet ?: false
                    onAddSet(exerciseWithSets.exercise.id, nextSet, 0, 0f, isPrevDrop, 0, 0f)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFF1E88E5)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1E88E5).copy(alpha = 0.3f))
            ) {
                Text(if (isCardio) "+ Add Entry" else "+ Add Set", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

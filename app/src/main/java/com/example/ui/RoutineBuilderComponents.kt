package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear

@Composable
fun RoutineExerciseBlock(
    exercise: RoutineExerciseWithTargets,
    onUpdate: (RoutineExerciseWithTargets) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.DragHandle, contentDescription = "Drag", tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        exercise.exercise.exerciseName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (exercise.exercise.note.isNotBlank()) {
                        Text(exercise.exercise.note, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    containerColor = Color(0xFF202024),
                    modifier = Modifier.background(Color(0xFF202024), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(text = { Text("Move Up", color = Color.White) }, onClick = { expandedMenu = false; onMoveUp() })
                    DropdownMenuItem(text = { Text("Move Down", color = Color.White) }, onClick = { expandedMenu = false; onMoveDown() })
                    DropdownMenuItem(text = { Text("Remove Exercise", color = Color(0xFFE53935)) }, onClick = { expandedMenu = false; onRemove() })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Table Header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SET", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(0.2f))
            Text("KG", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.3f))
            Text("REPS", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.3f))
            Spacer(modifier = Modifier.weight(0.1f)) // delete/options
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Set list
        exercise.targets.forEachIndexed { setIndex, target ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Set type / number
                Box(modifier = Modifier.weight(0.2f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = if (target.setType == "Normal") "${setIndex + 1}" else target.setType.take(1),
                        color = if (target.setType == "Warm-up") Color(0xFFFF9800) else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color(0xFF202024), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // Weight Target
                var editWeight by remember(target.targetWeight) { mutableStateOf(if (target.targetWeight > 0) target.targetWeight.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } else "") }
                Box(modifier = Modifier.weight(0.3f).padding(horizontal = 4.dp)) {
                    BasicTextField(
                        value = editWeight,
                        onValueChange = { 
                            editWeight = it
                            val w = it.toFloatOrNull() ?: 0f
                            val mutTargets = exercise.targets.toMutableList()
                            mutTargets[setIndex] = target.copy(targetWeight = w)
                            onUpdate(exercise.copy(targets = mutTargets))
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, color = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF202024), RoundedCornerShape(10.dp)).padding(vertical = 12.dp),
                        decorationBox = { innerTextField -> 
                            if (editWeight.isEmpty()) Text("-", color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                            innerTextField() 
                        }
                    )
                }
                
                // Reps Target
                var editReps by remember(target.targetReps) { mutableStateOf(if (target.targetReps > 0) target.targetReps.toString() else "") }
                Box(modifier = Modifier.weight(0.3f).padding(horizontal = 4.dp)) {
                    BasicTextField(
                        value = editReps,
                        onValueChange = { 
                            editReps = it
                            val r = it.toIntOrNull() ?: 0
                            val mutTargets = exercise.targets.toMutableList()
                            mutTargets[setIndex] = target.copy(targetReps = r)
                            onUpdate(exercise.copy(targets = mutTargets))
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, color = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF202024), RoundedCornerShape(10.dp)).padding(vertical = 12.dp),
                        decorationBox = { innerTextField -> 
                            if (editReps.isEmpty()) Text("-", color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                            innerTextField() 
                        }
                    )
                }
                
                IconButton(
                    onClick = {
                        val mutTargets = exercise.targets.toMutableList()
                        mutTargets.removeAt(setIndex)
                        onUpdate(exercise.copy(targets = mutTargets))
                    },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Clear, contentDescription = "Delete Set", tint = Color.DarkGray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "+ Add Set",
            color = Color(0xFF0A84FF),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().clickable {
                val mutTargets = exercise.targets.toMutableList()
                val prev = mutTargets.lastOrNull()
                mutTargets.add(
                    SetTarget(
                        routineExerciseId = exercise.exercise.id,
                        orderIndex = mutTargets.size,
                        targetWeight = prev?.targetWeight ?: 0f,
                        targetReps = prev?.targetReps ?: 0
                    )
                )
                onUpdate(exercise.copy(targets = mutTargets))
            }.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibrarySheet(
    onDismiss: () -> Unit,
    onExercisesSelected: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val commonExercises = listOf(
        "Bench Press", "Squat", "Deadlift", "Overhead Press", "Barbell Row",
        "Pull Up", "Dumbbell Curl", "Tricep Extension", "Leg Press", "Lat Pulldown"
    ).filter { it.contains(searchQuery, ignoreCase = true) }
    
    val selectedExercises = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.9f).padding(horizontal = 16.dp)) {
            Text("Select Exercises", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF202024),
                    unfocusedContainerColor = Color(0xFF202024),
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Allow adding a custom typed exercise if not in list
            if (searchQuery.isNotBlank() && !commonExercises.any { it.equals(searchQuery, ignoreCase = true) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        if (!selectedExercises.contains(searchQuery)) selectedExercises.add(searchQuery) else selectedExercises.remove(searchQuery)
                    }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Add \"$searchQuery\"", color = Color(0xFF0A84FF))
                    if (selectedExercises.contains(searchQuery)) Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = "", tint = Color(0xFF0A84FF))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(commonExercises.size) { index ->
                    val ex = commonExercises[index]
                    val isSelected = selectedExercises.contains(ex)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isSelected) selectedExercises.remove(ex) else selectedExercises.add(ex)
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ex, color = Color.White, fontSize = 16.sp)
                        if (isSelected) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = "Selected", tint = Color(0xFF0A84FF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    onExercisesSelected(selectedExercises.toList())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedExercises.isNotEmpty()
            ) {
                Text("Add ${selectedExercises.size} Exercises", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

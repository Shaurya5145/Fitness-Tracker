package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ProgressPhoto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotosScreen(viewModel: FitnessViewModel) {
    val context = LocalContext.current
    val progressPhotos by viewModel.allProgressPhotos.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()
    
    // Selected tab
    var selectedTab by remember { mutableStateOf("Timeline") } // Timeline, Comparison

    LaunchedEffect(uploadError) {
        uploadError?.let {
            Toast.makeText(context, "Cloudinary: $it", Toast.LENGTH_LONG).show()
            viewModel.clearUploadError()
        }
    }
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Photos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Photo")
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Tabs
            TabRow(selectedTabIndex = if (selectedTab == "Timeline") 0 else 1) {
                    Tab(
                        selected = selectedTab == "Timeline",
                        onClick = { selectedTab = "Timeline" },
                        text = { Text("Timeline") }
                    )
                    Tab(
                        selected = selectedTab == "Comparison",
                        onClick = { selectedTab = "Comparison" },
                        text = { Text("Before & After") }
                    )
                }

                if (selectedTab == "Timeline") {
                    TimelineView(progressPhotos, viewModel)
                } else {
                    ComparisonView(progressPhotos)
                }
            }
        }

    if (showAddDialog) {
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var viewType by remember { mutableStateOf("Front") }
        var weightInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            selectedUri = uri
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Progress") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedUri != null) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("Tap to select photo")
                            }
                        }
                    }
                    
                    Column {
                        Text("Angle", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Front", "Side", "Back").forEach { type ->
                                FilterChip(
                                    selected = viewType == type,
                                    onClick = { viewType = type },
                                    label = { Text(type) }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg) - Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes - Optional") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            selectedUri?.let { uri ->
                                val savedPath = saveImageToInternalStorage(context, uri)
                                if (savedPath != null) {
                                    val w = weightInput.toFloatOrNull()
                                    viewModel.addProgressPhoto(System.currentTimeMillis(), savedPath, viewType, w, notesInput)
                                }
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                showAddDialog = false
                                isSaving = false
                            }
                        }
                    },
                    enabled = selectedUri != null && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimelineView(photos: List<ProgressPhoto>, viewModel: FitnessViewModel) {
    if (photos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No photos logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val groupedPhotos = photos.sortedByDescending { it.dateStamp }.groupBy { 
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(Date(it.dateStamp))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        groupedPhotos.forEach { (month, monthPhotos) ->
            item {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            val photosByDate = monthPhotos.groupBy { 
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it.dateStamp))
            }
            
            photosByDate.forEach { (dateStr, datePhotos) ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(dateStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            datePhotos.forEach { photo ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val imageModel: Any = if (File(photo.imagePath).exists()) {
                                        File(photo.imagePath)
                                    } else if (!photo.remoteUrl.isNullOrEmpty()) {
                                        photo.remoteUrl.replaceFirst("/upload/", "/upload/q_auto,f_auto/")
                                    } else {
                                        File(photo.imagePath)
                                    }

                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = photo.viewType,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                            Text(photo.viewType, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { viewModel.deleteProgressPhoto(photo) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (photo.weightKg != null) {
                                            Text("${photo.weightKg} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        if (photo.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(photo.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
fun ComparisonView(photos: List<ProgressPhoto>) {
    var selectedComparisonViewType by remember { mutableStateOf("Front") }
    
    // Check if we should default to another view if Front doesn't have enough photos
    LaunchedEffect(photos) {
        if (photos.filter { it.viewType == selectedComparisonViewType }.size < 2) {
            val availableView = listOf("Front", "Side", "Back").firstOrNull { type ->
                photos.filter { it.viewType == type }.size >= 2
            }
            if (availableView != null) {
                selectedComparisonViewType = availableView
            }
        }
    }

    val typePhotos = photos.filter { it.viewType == selectedComparisonViewType }.sortedBy { it.dateStamp }
    
    // Tabs for selecting view type if multiple have enough photos
    val viewTypesWithEnoughPhotos = listOf("Front", "Side", "Back").filter { type ->
        photos.filter { it.viewType == type }.size >= 2
    }

    if (typePhotos.size < 2) {
        if (viewTypesWithEnoughPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Log at least two photos of the same angle to use the comparison slider.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var sliderPosition by remember { mutableStateOf(0.5f) }
    val firstPhoto = typePhotos.first()
    val lastPhoto = typePhotos.last()

    val modelBefore: Any = if (File(firstPhoto.imagePath).exists()) File(firstPhoto.imagePath) else if (!firstPhoto.remoteUrl.isNullOrEmpty()) firstPhoto.remoteUrl.replaceFirst("/upload/", "/upload/q_auto,f_auto/") else File(firstPhoto.imagePath)
    val modelAfter: Any = if (File(lastPhoto.imagePath).exists()) File(lastPhoto.imagePath) else if (!lastPhoto.remoteUrl.isNullOrEmpty()) lastPhoto.remoteUrl.replaceFirst("/upload/", "/upload/q_auto,f_auto/") else File(lastPhoto.imagePath)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (viewTypesWithEnoughPhotos.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                viewTypesWithEnoughPhotos.forEach { type ->
                    FilterChip(
                        selected = selectedComparisonViewType == type,
                        onClick = { selectedComparisonViewType = type },
                        label = { Text(type) }
                    )
                }
            }
        } else {
            Text("Transformation (${selectedComparisonViewType})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))) {
            val width = maxWidth
            
            // AFTER (Background)
            AsyncImage(
                model = modelAfter,
                contentDescription = "After",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // BEFORE (Foreground, clipped)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(width * sliderPosition)
                    .clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    })
            ) {
                AsyncImage(
                    model = modelBefore,
                    contentDescription = "Before",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight().width(width) // Need to be full width inside to scale correctly
                )
            }
            
            // Slider Line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .offset { IntOffset((width.toPx() * sliderPosition).roundToInt() - 2.dp.toPx().toInt(), 0) }
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newPosition = sliderPosition + (dragAmount.x / width.toPx())
                            sliderPosition = newPosition.coerceIn(0f, 1f)
                        }
                    }
            ) {
                // Handle graphic
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                        .offset(x = (-14).dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            // Labels
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(color = Color.Black.copy(alpha=0.5f), shape = RoundedCornerShape(4.dp)) {
                    Text("Before", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
                Surface(color = Color.Black.copy(alpha=0.5f), shape = RoundedCornerShape(4.dp)) {
                    Text("After", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        val beforeDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(firstPhoto.dateStamp))
        val afterDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(lastPhoto.dateStamp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Start ($beforeDate)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (firstPhoto.weightKg != null) Text("${firstPhoto.weightKg} kg", fontWeight = FontWeight.Bold)
            }
            
            if (firstPhoto.weightKg != null && lastPhoto.weightKg != null) {
                val diff = lastPhoto.weightKg - firstPhoto.weightKg
                val color = if (diff < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${if(diff > 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", diff)} kg", fontWeight = FontWeight.Bold, color = color)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current ($afterDate)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lastPhoto.weightKg != null) Text("${lastPhoto.weightKg} kg", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        if (bitmap == null) return null
        
        val fileName = "progress_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        
        // Compress bitmap locally (e.g. 80% JPEG quality) to save disk space and network bandwidth
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        
        outputStream.flush()
        outputStream.close()
        
        file.absolutePath
    } catch (e: Exception) {
        Log.e("ProgressPhotos", "Failed to save image to internal storage", e)
        null
    }
}
